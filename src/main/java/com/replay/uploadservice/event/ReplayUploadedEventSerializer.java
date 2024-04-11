package com.replay.uploadservice.event;

import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplayUploadedEventSerializer implements Serializer<ReplayUploadedEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, ReplayUploadedEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing ReplayUploadedEvent", e);
        }
    }
}
