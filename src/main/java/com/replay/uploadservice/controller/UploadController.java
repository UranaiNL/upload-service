package com.replay.uploadservice.controller;

import com.replay.uploadservice.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/api/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String uploadReplay(@RequestParam("videoFile")MultipartFile videoFile){
        return uploadService.uploadVideoFile(videoFile);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getServerDetails(){
        return uploadService.getServerDetails();
    }
}
