package com.replay.uploadservice.controller;

import com.replay.uploadservice.dto.UploadRequest;
import com.replay.uploadservice.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/api/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public String uploadReplayToCloud(HttpServletRequest request) throws Exception {
        MultipartFile video = ((MultipartHttpServletRequest) request).getFile("video");
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setUploaderId(request.getParameter("uploaderId"));
        uploadRequest.setP1Username(request.getParameter("p1Username"));
        uploadRequest.setP2Username(request.getParameter("p2Username"));
        uploadRequest.setP1CharacterId(request.getParameter("p1CharacterId"));
        uploadRequest.setP2CharacterId(request.getParameter("p2CharacterId"));
        uploadRequest.setGameId(request.getParameter("gameId"));
        uploadRequest.setVideo(video);
        return uploadService.uploadReplayToCloud(uploadRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @RabbitListener(queues = "replay_queue")
    public String testConnection(){
        return "Connected";
    }
}
