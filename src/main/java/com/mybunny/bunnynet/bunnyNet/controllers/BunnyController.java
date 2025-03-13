package com.mybunny.bunnynet.bunnyNet.controllers;

import com.mybunny.bunnynet.bunnyNet.services.BunnyService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/videos/")
@AllArgsConstructor
public class BunnyController {

    private final BunnyService service;

    /**
     * Retrieves a signed URL for streaming a video.
     *
     * @param bunnyVideoId The ID of the video on Bunny.net.
     * @return A signed URL for streaming the video.
     */
    @GetMapping("{id}/link")
    public String getVideoLink(@PathVariable("id") String bunnyVideoId) {
        return service.getVideoLink(bunnyVideoId);
    }

    /**
     * Handles video uploads by accepting a multipart file and initiating the upload process.
     *
     * @param file The video file to upload.
     * @return A message indicating that the upload is in progress.
     * @throws IOException If there is an issue with file handling.
     */
    @PostMapping(value = "upload", consumes = MULTIPART_FORM_DATA_VALUE)
    public String uploadVideo(@RequestPart("video") MultipartFile file) throws IOException {
        var tempFile = File.createTempFile("upload_", ".tmp");
        file.transferTo(tempFile);
        service.uploadVideo(tempFile.getAbsolutePath(), file.getOriginalFilename());
        return "Video upload is in progress...";
    }
}
