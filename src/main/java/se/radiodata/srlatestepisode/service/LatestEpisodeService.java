package se.radiodata.srlatestepisode.service;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.radiodata.srlatestepisode.types.LatestEpisode;
import se.radiodata.srlatestepisode.types.sr.Program;
import se.radiodata.srlatestepisode.types.sr.SrEpisodeResponse;
import se.radiodata.srlatestepisode.types.sr.SrProgramsResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LatestEpisodeService {
    Pattern publishDatePattern = Pattern.compile("\\d+");
    private final WebClient webClient;
    Map<String, Integer> programIds = new ConcurrentHashMap<>();

    public LatestEpisodeService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<LatestEpisode> getLatestEpisode(String programName) {
        return getProgramId(programName).flatMap(this::getLatestEpisode);
    }

    private Mono<Integer> getProgramId(String name) {
        if (!programIds.containsKey(name)) {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/programs/index")
                            .queryParam("pagination", "false")
                            .queryParam("format", "json")
                            .build())
                    .retrieve().bodyToMono(SrProgramsResponse.class)
                    .doOnNext(allPrograms -> allPrograms.getPrograms()
                            .forEach(program -> programIds.put(program.getName().toLowerCase(), program.getId())))
                    .flatMapIterable(SrProgramsResponse::getPrograms)
                    .filter(program -> name.equalsIgnoreCase(program.getName()))
                    .next()
                    .map(Program::getId);
        }
        return Mono.justOrEmpty(programIds.get(name));
    }

    private Mono<LatestEpisode> getLatestEpisode(Integer programId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/episodes/getlatest")
                        .queryParam("programId", programId)
                        .queryParam("format", "json")
                        .build())
                .retrieve().bodyToMono(SrEpisodeResponse.class)
                .map(response -> {
                    String microsoftDateString = response.getEpisode().getPublishdateutc();
                    Matcher matcher = publishDatePattern.matcher(microsoftDateString);
                    matcher.find();
                    Long epochMillis = Long.valueOf(matcher.group());
                    LatestEpisode latestEpisode = LatestEpisode.builder()
                            .title(response.getEpisode().getTitle())
                            .programName(response.getEpisode().getProgram().getName())
                            .description(response.getEpisode().getDescription())
                            .publicationTimeEpochMillisUTC(epochMillis)
                            .build();
                    return latestEpisode;
                });
    }
}
