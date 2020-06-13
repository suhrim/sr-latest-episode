package se.radiodata.srlatestepisode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.radiodata.srlatestepisode")
public class SrLatestEpisodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SrLatestEpisodeApplication.class, args);
    }
}
