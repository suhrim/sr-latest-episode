package se.radiodata.srlatestepisode.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.radiodata.srlatestepisode.config.ApplicationConfig;
import se.radiodata.srlatestepisode.types.LatestEpisode;
import se.radiodata.srlatestepisode.types.sr.Episode;
import se.radiodata.srlatestepisode.types.sr.Program;
import se.radiodata.srlatestepisode.types.sr.SrEpisodeResponse;
import se.radiodata.srlatestepisode.types.sr.SrProgramsResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class LatestEpisodeServiceTest {
    public static final long PUBLICATION_TIME = 1591958562162l;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static MockWebServer mockSrApi;
    private LatestEpisodeService serviceUnderTest;
    private final ApplicationConfig applicationConfig = new ApplicationConfig();

    @AfterEach
    void tearDown() throws IOException {
        mockSrApi.shutdown();
    }

    @BeforeEach
    void initialize() throws IOException {
        mockSrApi = new MockWebServer();
        mockSrApi.start();
        String baseUrl = String.format("http://localhost:%s", mockSrApi.getPort());
        WebClient webClient = applicationConfig.getWebClient(baseUrl);
        serviceUnderTest = new LatestEpisodeService(webClient);
    }

    @Test
    public void testSuccessfulCall() throws JsonProcessingException {
        Program program = new Program(1, "program1");
        SrProgramsResponse allPrograms = new SrProgramsResponse(List.of(program));

        Episode episode = getEpisode(program, "A test description", "A test title", PUBLICATION_TIME);
        SrEpisodeResponse srEpisodeResponse = new SrEpisodeResponse(episode);

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(allPrograms)));

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(srEpisodeResponse)));

        LatestEpisode actualResult = serviceUnderTest.getLatestEpisode("program1").block();
        assertEquals(episode.getTitle(), actualResult.getTitle());
        assertEquals(episode.getDescription(), actualResult.getDescription());
        assertEquals(program.getName(), actualResult.getProgramName());
        assertEquals(PUBLICATION_TIME, actualResult.getPublicationTimeEpochMillisUTC());
    }

    @Test
    public void testProgramNotFound() throws JsonProcessingException {
        Program program = new Program(1, "program1");
        SrProgramsResponse allPrograms = new SrProgramsResponse(List.of(program));

        Episode episode = getEpisode(program, "A test description", "A test title", PUBLICATION_TIME);
        SrEpisodeResponse srEpisodeResponse = new SrEpisodeResponse(episode);

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(allPrograms)));


        Mono<LatestEpisode> actualResult = serviceUnderTest.getLatestEpisode("program2");
        assertNull(actualResult.block());
    }

    @Test
    public void testMultipleCallsSameProgram() throws JsonProcessingException {
        Program program = new Program(1, "program1");
        SrProgramsResponse allPrograms = new SrProgramsResponse(List.of(program));

        Episode firstEpisode = getEpisode(program, "A test description", "A test title", PUBLICATION_TIME);

        Episode secondEpisode = getEpisode(program, "Another test description", "Another test title", PUBLICATION_TIME + 1000);

        SrEpisodeResponse firstSrEpisodeResponse = new SrEpisodeResponse(firstEpisode);
        SrEpisodeResponse secondSrEpisodeResponse = new SrEpisodeResponse(secondEpisode);

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(allPrograms)));

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(firstSrEpisodeResponse)));

        mockSrApi.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(secondSrEpisodeResponse)));

        LatestEpisode firstResult = serviceUnderTest.getLatestEpisode("program1").block();
        LatestEpisode secondResult = serviceUnderTest.getLatestEpisode("program1").block();

        assertEquals(firstEpisode.getTitle(), firstResult.getTitle());
        assertEquals(firstEpisode.getDescription(), firstResult.getDescription());
        assertEquals(program.getName(), firstResult.getProgramName());
        assertEquals(PUBLICATION_TIME, firstResult.getPublicationTimeEpochMillisUTC());

        assertEquals(secondEpisode.getTitle(), secondResult.getTitle());
        assertEquals(secondEpisode.getDescription(), secondResult.getDescription());
        assertEquals(program.getName(), secondResult.getProgramName());
        assertEquals(PUBLICATION_TIME + 1000, secondResult.getPublicationTimeEpochMillisUTC());

        assertEquals(3, mockSrApi.getRequestCount());
    }


    @Test
    public void testLimitedConcurrentConnections() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            mockSrApi.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            executorService.execute(() -> {
                log.error("before");
                serviceUnderTest.getLatestEpisode("any-name").block();
                log.error("after");
            });
        }
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        assertEquals(3, mockSrApi.getRequestCount());
    }

    private Episode getEpisode(Program program, String description, String title, long publishDateUtc) {
        return Episode.builder()
                .program(program)
                .description(description)
                .title(title)
                .publishdateutc(String.format("\\Date(%d)", publishDateUtc))
                .build();
    }
}