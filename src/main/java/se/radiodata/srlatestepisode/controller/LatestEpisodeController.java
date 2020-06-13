package se.radiodata.srlatestepisode.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.radiodata.srlatestepisode.service.LatestEpisodeService;
import se.radiodata.srlatestepisode.types.LatestEpisode;

@Slf4j
@RestController
public class LatestEpisodeController {

    private final LatestEpisodeService latestEpisodeService;

    public LatestEpisodeController(LatestEpisodeService latestEpisodeService) {
        this.latestEpisodeService = latestEpisodeService;
    }

    @GetMapping("/program/{programName}/latest")
    public Mono<ResponseEntity<LatestEpisode>> getLatestEpisode(@PathVariable("programName") String programName) {
        return latestEpisodeService.getLatestEpisode(programName)
                .map(latest -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(latest))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(e -> log.error(e.getMessage(), e))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

}
