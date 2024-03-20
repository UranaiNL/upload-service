package com.replay.uploadservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponse {
    @JsonProperty("file_code")
    private String fileCode;

    @JsonProperty("file_status")
    private String fileStatus;
}
