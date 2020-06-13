package se.radiodata.srlatestepisode.types.sr;

import lombok.Value;

import java.util.List;

@Value
public class SrProgramsResponse {
    private final List<Program> programs;
}
