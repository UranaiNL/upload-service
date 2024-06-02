package com.replay.uploadservice.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.replay.uploadservice.config.RabbitMQConfig;
import com.replay.uploadservice.dto.UploadRequest;
import com.replay.uploadservice.event.ReplayUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final AmqpTemplate rabbitTemplate;

    public String uploadReplayToCloud(UploadRequest uploadRequest) throws Exception {
        // Environment Files
        String projectId = System.getenv("PROJECT_ID");
        String bucketName = System.getenv("BUCKET_NAME");
        log.info(projectId, " is the id & the bucket is" , bucketName);

        // Get the file name so we can use it later, also set up an inputname to the raw folder so we can use it.
        String fileName = uploadRequest.getVideo().getOriginalFilename();
        assert fileName != null;
        String inputName = String.format("raw/%s", fileName);

        // Actual Upload Stuff
        try (InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("gkey.json")) {
            log.warn("Trying to get the credential stream!");
            assert credentialsStream != null;
            log.info(credentialsStream.toString());
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(credentialsStream))
                    .setProjectId(projectId)
                    .build()
                    .getService();
            BlobId blobId = BlobId.of(bucketName, inputName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("video/mp4").build();

            // Setting up basic preconditions.
            Storage.BlobWriteOption precondition;
            if (storage.get(bucketName, inputName) == null) {
                // For a target object that does not yet exist, set the DoesNotExist precondition.
                precondition = Storage.BlobWriteOption.doesNotExist();
            } else {
                // If the destination already exists in your bucket, instead set a generation-match precondition.
                precondition = Storage.BlobWriteOption.generationMatch(storage.get(bucketName, inputName).getGeneration());
            }

            // Create the storage object
            Blob blob = storage.createFrom(blobInfo, uploadRequest.getVideo().getInputStream(), precondition);
            assert blob != null;
            log.info("File: " + fileName + "uploaded to bucket:" + bucketName + " successfully!");

            // Get the url and uri for other services
            String publicUrl = String.format("https://storage.googleapis.com/%s/transcoded/%s/manifest.mpd", bucketName, fileName);
            String inputUri = String.format("gs://%s/%s", bucketName, inputName);
            log.info("InputURI: " + inputUri);

            // Rabbit communication channels
            ReplayUploadedEvent event = ReplayUploadedEvent.builder()
                    .uploaderId(uploadRequest.getUploaderId())
                    .publicUrl(publicUrl)
                    .p1Username(uploadRequest.getP1Username())
                    .p2Username(uploadRequest.getP2Username())
                    .p1CharacterId(uploadRequest.getP1CharacterId())
                    .p2CharacterId(uploadRequest.getP2CharacterId())
                    .gameId(uploadRequest.getGameId())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLAY_EXCHANGE, RabbitMQConfig.REPLAY_ROUTING_KEY, event);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SUBSCRIPTION_EXCHANGE, RabbitMQConfig.SUBSCRIPTION_ROUTING_KEY, event);
            return publicUrl;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
}