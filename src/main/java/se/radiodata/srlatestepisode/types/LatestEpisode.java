package se.radiodata.srlatestepisode.types;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LatestEpisode {
    private final String description;
    private final String title;
    private final String programName;
    private final Long publicationTimeEpochMillisUTC;
}
