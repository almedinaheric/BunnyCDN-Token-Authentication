package com.mybunny.bunnynet.bunnyNet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybunny.bunnynet.bunnyNet.payloads.BunnyUploadRequestDto;
import com.mybunny.bunnynet.bunnyNet.payloads.BunnyVideoResponseDto;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.util.Map;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class BunnyService {

    @Value("${bunny.url}")
    private String bunnyUrl;

    @Value("${bunny.tusupload.url}")
    private String bunnyTusUploadUrl;

    @Value("${bunny.cdn.hostname}")
    private String bunnyCdnHostname;

    @Value("${bunny.libraryId}")
    private String bunnyLibraryId;

    @Value("${bunny.apiKey}")
    private String bunnyApiKey;

    @Value("${bunny.token.security.key}")
    private String bunnyTokenSecurityKey;

    private final BunnyTokenSigner tokenSigner;

    /**
     * Generates a signed video URL for streaming.
     *
     * @param videoId The ID of the video.
     * @return A signed URL for the video's playlist.
     */
    public String getVideoLink(String videoId) {
        var allowedPath = "/" + videoId + "/";
        var videoUrl = String.format("https://%s/%s/playlist.m3u8", bunnyCdnHostname, videoId);
        return tokenSigner.signUrl(videoUrl, bunnyTokenSecurityKey, allowedPath);
    }

    /**
     * Generates a signed URL for a video thumbnail.
     *
     * @param videoId       The ID of the video.
     * @param thumbnailName The name of the thumbnail file.
     * @return A signed URL for the video thumbnail.
     */
    public String getVideoThumbnailLink(String videoId, String thumbnailName) {
        var allowedPath = "/" + videoId + "/";
        var videoUrl = String.format("https://%s/%s/%s", bunnyCdnHostname, videoId, thumbnailName);
        return tokenSigner.signUrl(videoUrl, bunnyTokenSecurityKey, allowedPath);
    }

    /**
     * Asynchronously uploads a video file to Bunny.net.
     *
     * @param filePath The path to the video file.
     * @param fileName The name of the video file.
     */
    @Async
    @SneakyThrows
    public void uploadVideo(String filePath, String fileName) {
        var file = new File(filePath);
        file.deleteOnExit();
        try {
            var video = createVideo(fileName); // Create a video entry on Bunny.net
            uploadVideo(file, fileName, video.getGuid()); // Upload the video file
        } catch (Exception e) {
            throw new BadRequestException("Failed to upload video");
        } finally {
            file.delete();
        }
    }

    /**
     * Creates a video entry on Bunny.net.
     *
     * @param fileName The name of the video file.
     * @return The response containing the video details.
     */
    @SneakyThrows
    private BunnyVideoResponseDto createVideo(String fileName) {
        try {
            var url = String.format(bunnyUrl, bunnyLibraryId);
            var body = new BunnyUploadRequestDto(fileName);
            var response = makePostRequest(url, body);
            var objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.getBody(), BunnyVideoResponseDto.class);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create video");
        }
    }

    /**
     * Uploads a video file to Bunny.net using the TUS protocol.
     *
     * @param file    The video file to upload.
     * @param title   The title of the video.
     * @param videoId The ID of the video entry created on Bunny.net.
     */
    @SneakyThrows
    private void uploadVideo(File file, String title, String videoId) {
        var expirationTime = tokenSigner.calculateExpirationTimestamp();
        var authorizationSignature = tokenSigner.generateAuthorizationSignature(bunnyLibraryId, bunnyApiKey, expirationTime, videoId);
        var client = new TusClient();
        client.setUploadCreationURL(new URI(bunnyTusUploadUrl).toURL());
        client.setHeaders(Map.of(
                "AuthorizationSignature", authorizationSignature,
                "AuthorizationExpire", expirationTime,
                "VideoId", videoId,
                "LibraryId", bunnyLibraryId
        ));
        var upload = new TusUpload(file);
        upload.setMetadata(Map.of(
                "filetype", "video/mp4",
                "title", title
        ));
        // Upload chunks until the entire file is uploaded
        var uploader = client.createUpload(upload);
        while (uploader.uploadChunk() > -1) {
            // Optionally, log progress or handle interruptions here
        }
        uploader.finish();
    }

    /**
     * Retrieves video details from Bunny.net.
     *
     * @param videoId The ID of the video.
     * @return The response containing the video details.
     */
    @SneakyThrows
    private BunnyVideoResponseDto getVideo(String videoId) {
        try {
            var url = String.format(bunnyUrl, bunnyLibraryId) + "/" + videoId;
            var response = makeGetRequest(url);
            return response.getBody();
        } catch (Exception e) {
            throw new BadRequestException("Failed to get video");
        }
    }

    /**
     * Makes a GET request to the Bunny.net API.
     *
     * @param url The URL to make the request to.
     * @return The response entity containing the video details.
     */
    private ResponseEntity<BunnyVideoResponseDto> makeGetRequest(String url) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AccessKey", bunnyApiKey);
        var request = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, request, BunnyVideoResponseDto.class);
    }

    /**
     * Makes a POST request to the Bunny.net API.
     *
     * @param url  The URL to make the request to.
     * @param body The request body.
     * @return The response entity containing the API response.
     */
    private ResponseEntity<String> makePostRequest(String url, Object body) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AccessKey", bunnyApiKey);
        var request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, String.class);
    }

}