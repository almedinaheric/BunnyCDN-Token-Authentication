package com.mybunny.bunnynet.bunnyNet.payloads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BunnyVideoResponseDto {
    private String guid;

    // Additional properties from the Bunny.net API response can be added here.
    // Refer to the official documentation for the complete list of fields:
    // https://docs.bunny.net/reference/video_getvideo
}
