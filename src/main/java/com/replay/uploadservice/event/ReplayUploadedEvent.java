package com.replay.uploadservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReplayUploadedEvent {
    private String id;
    private String uploaderId;
    private String p1Username;
    private String p2Username;
    private String p1CharacterId;
    private String p2CharacterId;
}
