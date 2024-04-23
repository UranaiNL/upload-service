package com.replay.uploadservice.controller;

import com.replay.uploadservice.dto.ReplayRequest;
import com.replay.uploadservice.dto.UploadRequest;
import com.replay.uploadservice.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/api/upload")
public class UploadController {

    private final UploadService uploadService;

//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public String uploadReplay(@RequestParam("videoFile") MultipartFile videoFile){
//        return uploadService.uploadVideoFile(videoFile);
//    }

    // RequestBody & RequestParams & ModelAttribute don't work, so we have to resort to HttpServletRequest instead! :)
    // Hours wasted so far: 2 hours!
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String uploadReplay(HttpServletRequest request) {
        MultipartFile videoFile = ((MultipartHttpServletRequest) request).getFile("videoFile");

        ReplayRequest replayRequest = new ReplayRequest();
        replayRequest.setUploaderId(request.getParameter("uploaderId"));
        replayRequest.setFileCode("");
        replayRequest.setP1Username(request.getParameter("p1Username"));
        replayRequest.setP2Username(request.getParameter("p2Username"));
        replayRequest.setP1CharacterId(request.getParameter("p1CharacterId"));
        replayRequest.setP2CharacterId(request.getParameter("p2CharacterId"));
        replayRequest.setGameId(request.getParameter("gameId"));

        return uploadService.uploadReplay(videoFile, replayRequest);
    }

    @PostMapping("/test")
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
    public Map<String, String> getServerDetails(){
        return uploadService.getServerDetails();
    }
}
