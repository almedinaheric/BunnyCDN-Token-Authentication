# Bunny.net URL Signing and TUS Video Upload Helper

This project provides a helper/example implementation for signing Bunny.net URLs and uploading videos using the TUS protocol. It includes a `BunnyTokenSigner` for generating signed URLs and a `BunnyService` for handling video uploads. You can copy these components into your project and use them as-is or customize them to fit your needs.

## Features

- **URL Signing**: Generate signed URLs for secure video streaming using Bunny.net's token authentication.
- **TUS Video Upload**: Upload videos to Bunny.net using the TUS protocol for resumable and reliable uploads.

## Prerequisites

Before using this helper, ensure you have completed the following steps:

1. **Create a Video Library**:
    - Go to [Bunny.net](https://dash.bunny.net/) and create a new video library.

2. **Set Up Video Storage Configuration**:
    - Select regions based on your target audience and cost considerations.
    - ⚠️ **Important**: Once the library is created, you can add new regions, but you cannot deselect previously selected regions. Choose carefully!

3. **Enable Token Authentication**:
    - Token authentication is required for signing URLs. Enable it in your Bunny.net dashboard:
      [Token Authentication](https://dash.bunny.net/cdn/{libraryId}/security/token-authentication).

## How to Use

1. **Copy the Code**:
    - Copy the `BunnyTokenSigner` and `BunnyService` classes into your project.
    - Ensure you have the required dependencies (e.g., `org.apache.httpcomponents`, `io.tus.java.client`, `commons-codec`, etc.).

2. **Configure Application Properties**:
   Add the following properties to your `application.properties` file. Replace the dummy values with your actual Bunny.net credentials:

   ```properties
   bunny.url=https://video.bunnycdn.com/library/%s/videos
   bunny.tusupload.url=https://video.bunnycdn.com/tusupload
   bunny.cdn.hostname=${BUNNY_CDN_HOSTNAME}
   bunny.libraryId=${BUNNY_LIBRARYID}
   bunny.apiKey=${BUNNY_APIKEY}
   bunny.token.security.key=${BUNNY_TOKEN_SECURITY_KEY}
   ```

   You can find these values in your Bunny.net dashboard:
    - **Library ID**: Go to https://dash.bunny.net/stream/{libraryId}/api
    - **API Key**: Go to https://dash.bunny.net/stream/{libraryId}/api
    - **Token Security Key**: Go to https://dash.bunny.net/cdn/{libraryId}/security/token-authentication

3. **Enable Token Authentication**:
    - Token authentication must be enabled in your Bunny.net dashboard. Go to https://dash.bunny.net/cdn/{libraryId}/security/token-authentication and enable it for your library.

4. **Use the Service**:
    - Use the `BunnyService` to upload videos and generate signed URLs.
    - Example usage:
      ```java
      // Generate a signed URL for a video
      var signedUrl = bunnyService.getVideoLink("video-id");
 
      // Upload a video
      bunnyService.uploadVideo("path/to/video.mp4", "video.mp4");
      ```

## Example Configuration

Here’s an example (dummy values) of how to configure the properties in your `application.properties` file:

```properties
# Bunny.net Configuration
bunny.libraryId=123456
bunny.cdn.hostname=vz-example-e.b-cdn.net
bunny.apiKey=12345678-1234-1234-1234-123456789123
bunny.token.security.key=12345678-1234-1234-1234-123456789123
```
