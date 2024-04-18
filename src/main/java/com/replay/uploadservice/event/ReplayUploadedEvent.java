package com.replay.uploadservice.event;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ReplayUploadedEvent {
    private String id;
    private String uploaderId;
    private String p1Username;
    private String p2Username;
    private String p1CharacterId;
    private String p2CharacterId;
}
