package com.replay.uploadservice.service;


import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.google.cloud.video.transcoder.v1.*;
import com.replay.uploadservice.config.RabbitMQConfig;
import com.replay.uploadservice.dto.UploadRequest;
import com.replay.uploadservice.event.ReplayUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final WebClient.Builder webClientBuilder;
    private final RabbitTemplate rabbitTemplate;

    public String[] uploadReplayToCloud(UploadRequest uploadRequest) throws Exception {
        // Environment Files
        String projectId = System.getenv("PROJECT_ID");
        String bucketName = System.getenv("BUCKET_NAME");

        // Get the file name so we can use it later, also set up an inputname to the raw folder so we can use it.
        String fileName = uploadRequest.getVideo().getOriginalFilename();
        assert fileName != null;
        String inputName = String.format("raw/%s", fileName);

        // Actual Upload Stuff
        try (InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("gkey.json")) {
            assert credentialsStream != null;
            // log.info(credentialsStream.toString());
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
            String publicUrl = String.format("https://storage.googleapis.com/%s/transcoded/%s", bucketName, fileName);
            String inputUri = String.format("gs://%s/%s", bucketName, inputName);
            log.info("InputURI: " + inputUri);

            // Rabbit communication
            ReplayUploadedEvent event = ReplayUploadedEvent.builder()
                    .uploaderId(uploadRequest.getUploaderId())
                    .publicUrl(publicUrl)
                    .p1Username(uploadRequest.getP1Username())
                    .p2Username(uploadRequest.getP2Username())
                    .p1CharacterId(uploadRequest.getP1CharacterId())
                    .p2CharacterId(uploadRequest.getP2CharacterId())
                    .gameId(uploadRequest.getGameId())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLAY_EXCHANGE, RabbitMQConfig.REPLAY_ROUTING_KEY_UPLOAD, event);

            String metadata = createMetadata(event);
            String jobStarted = startTranscodeJob(fileName, inputUri);
            return new String[]{
                    metadata,
                    jobStarted,
            };
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public String createMetadata(ReplayUploadedEvent replayRequest) throws Exception {
        try {
            ResponseEntity<String> responseEntity = webClientBuilder.build().post()
                    .uri("http://replay-service/api/replay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(replayRequest))
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            assert responseEntity != null;
            if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
                return responseEntity.getBody();
            } else {
                throw new Exception("Replay metadata wasn't stored! Status code: " + responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public String startTranscodeJob(String fileName, String inputUri) {
        String projectId = System.getenv("PROJECT_ID");
        String bucketName = System.getenv("BUCKET_NAME");
        String location = System.getenv("LOCATION");
        String preset = "preset/web-hd";
        String outputUri = String.format("gs://%s/transcoded/%s/", bucketName, fileName);
        log.info("OutputURI: " + outputUri);
        try (InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("gkey.json")) {
            assert credentialsStream != null;

            // Authenticate the TranscoderServiceClient
            TranscoderServiceSettings settings = TranscoderServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(credentialsStream)))
                    .build();

            // Build and create the job.
            try (TranscoderServiceClient transcoderServiceClient = TranscoderServiceClient.create(settings)) {
                CreateJobRequest createJobRequest = CreateJobRequest.newBuilder()
                        .setJob(Job.newBuilder()
                                .setInputUri(inputUri)
                                .setOutputUri(outputUri)
                                .setTemplateId(preset)
                                .build())
                        .setParent(LocationName.of(projectId, location).toString())
                        .build();

                // Send the job creation request and process the response.
                Job job = transcoderServiceClient.createJob(createJobRequest);
                String message = "Transcode job started: " + job.getName() + " with state: " + job.getState();
                System.out.println(message);
                return message;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}