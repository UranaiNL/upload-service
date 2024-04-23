package com.replay.uploadservice.service;


import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.replay.uploadservice.config.RabbitMQConfig;
import com.replay.uploadservice.dto.ReplayRequest;
import com.replay.uploadservice.dto.UploadRequest;
import com.replay.uploadservice.dto.UploadResponse;
import com.replay.uploadservice.event.ReplayUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final WebClient.Builder webClientBuilder;
    private final RabbitTemplate rabbitTemplate;

    public String uploadReplay(MultipartFile videoFile, ReplayRequest replayRequest){
        Map<String, String> serverDetails = getServerDetails();
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Prepare the request body
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("sess_id", serverDetails.get("sess_id"));
            requestBody.add("file_0", videoFile.getResource());

            // Create the HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Define RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            // Set up the url defined in the server details
            String uploadUrl = (serverDetails.get("result") + "?upload_type=file&utype=reg");
            System.out.println(uploadUrl);

            // Make the POST request to the specified URL
            ResponseEntity<UploadResponse[]> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    UploadResponse[].class
            );

            // Check the response status
            if (response.getStatusCode() == HttpStatus.OK) {
                UploadResponse[] uploadResponses = response.getBody();
                System.out.println("Response body:" + Arrays.toString(response.getBody()));
                if (uploadResponses != null && uploadResponses.length > 0) {
                    if(Objects.equals(uploadResponses[0].getFileStatus(), "OK")){
                        replayRequest.setFileCode(uploadResponses[0].getFileCode());
                        String metadata = createMetadata(replayRequest);
                        ReplayUploadedEvent event = ReplayUploadedEvent.builder()
                                .id(metadata)
                                .uploaderId(replayRequest.getUploaderId())
                                .p1Username(replayRequest.getP1Username())
                                .p2Username(replayRequest.getP2Username())
                                .p1CharacterId(replayRequest.getP1CharacterId())
                                .p2CharacterId(replayRequest.getP2CharacterId())
                                .build();
                        rabbitTemplate.convertAndSend(RabbitMQConfig.REPLAY_EXCHANGE, RabbitMQConfig.REPLAY_ROUTING_KEY_UPLOAD, event);
                        return metadata;
                    }
                    throw new Exception("UPLOAD STATUS: NOT OK!");
                }
                throw new Exception("Upload response was empty!");
            }
            throw new Exception("HttpStatus not OK!");
        } catch (Exception e) {
            return "Failed to upload replay: " + e.getMessage();
        }
    }

    public String uploadReplayToCloud(UploadRequest uploadRequest) throws Exception {
        // Environment Files
        String projectId = System.getenv("PROJECT_ID");
        String bucketName = System.getenv("BUCKET_NAME");

        // Get the file name so we can use it later.
        String fileName = uploadRequest.getVideo().getOriginalFilename();
        assert fileName != null;

        // Actual Upload Stuff
        try (InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("gkey.json")) {
            assert credentialsStream != null;
            log.info(credentialsStream.toString());
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(credentialsStream))
                    .setProjectId(projectId)
                    .build()
                    .getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("video/mp4").build();

            // Setting up basic preconditions.
            Storage.BlobWriteOption precondition;
            if (storage.get(bucketName, fileName) == null) {
                // For a target object that does not yet exist, set the DoesNotExist precondition.
                // This will cause the request to fail if the object is created before the request runs.
                precondition = Storage.BlobWriteOption.doesNotExist();
            } else {
                // If the destination already exists in your bucket, instead set a generation-match
                // precondition. This will cause the request to fail if the existing object's generation
                // changes before the request runs.
                precondition = Storage.BlobWriteOption.generationMatch(storage.get(bucketName, fileName).getGeneration());
            }
            Blob blob = storage.createFrom(blobInfo, uploadRequest.getVideo().getInputStream(), precondition);
            log.info("File: " + fileName + "uploaded successfully to bucket:" + bucketName + "with identifier: ");
            return blob.getBlobId().toGsUtilUriWithGeneration();
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    public Map<String, String> getServerDetails(){
        RestTemplate serverDetailsTemplate = new RestTemplate();
        ResponseEntity<Map> serverDetailsResponse = serverDetailsTemplate.exchange(
                "https://mp4upload.com/api/upload/server?key=458798pdjkypeei8tual7e",
                HttpMethod.GET,
                null,
                Map.class
        );

        Map<String, String> serverDetails = new HashMap<>();

        if(serverDetailsResponse.getStatusCode().is2xxSuccessful()){
            Map responseBody = serverDetailsResponse.getBody();
            if(responseBody != null){
                serverDetails.put("sess_id", (String) responseBody.get("sess_id"));
                serverDetails.put("result", (String) responseBody.get("result"));
            }
        }
        System.out.println(serverDetails);
        return serverDetails;
    }

    public String createMetadata(ReplayRequest replayRequest) throws Exception {
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
}
