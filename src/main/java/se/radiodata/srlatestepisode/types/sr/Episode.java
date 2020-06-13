package se.radiodata.srlatestepisode.types.sr;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Episode {
    private final String description;
    private final String title;
    private final Program program;
    private final String publishdateutc;

}
