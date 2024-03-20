package com.replay.uploadservice.service;


import com.replay.uploadservice.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    public String uploadVideoFile(MultipartFile videoFile){
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
                        return uploadResponses[0].getFileCode();
                    }
                    throw new Exception("UPLOAD STATUS: NOT OK!");
                }
                throw new Exception("Upload response was empty!");
            }
            throw new Exception("HttpStatus not OK!");
        } catch (Exception e) {
            return "Failed to upload file: " + e.getMessage();
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
}
