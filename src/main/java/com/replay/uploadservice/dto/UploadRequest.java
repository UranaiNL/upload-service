package com.replay.uploadservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadRequest {
    private String uploaderId;
    private String p1Username;
    private String p2Username;
    private String p1CharacterId;
    private String p2CharacterId;
    private String gameId;

    @JsonIgnore
    private MultipartFile video;
}
