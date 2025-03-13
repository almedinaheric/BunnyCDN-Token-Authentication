package com.mybunny.bunnynet.bunnyNet.services;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Service
public class BunnyTokenSigner {

    /**
     * Signs a URL by generating a token and appending it to the URL along with expiration and parameters.
     *
     * @param url         The URL to be signed.
     * @param securityKey The security key used for generating the token.
     * @param allowedPath The allowed path for the token (optional).
     * @return The signed URL with the token and additional parameters.
     */
    @SneakyThrows
    public String signUrl(String url, String securityKey, String allowedPath) {
        var uri = new URI(url);
        var temp = new URI(url).toURL();
        var expires = calculateExpirationTimestamp();
        var parametersMap = parseUrlParameters(uri);
        var signaturePath = determineSignaturePath(temp, allowedPath, parametersMap);
        var parameterData = buildParameterData(parametersMap);
        var parameterDataUrl = buildParameterDataUrl(parametersMap);
        var hashableBase = securityKey + signaturePath + expires + parameterData;
        var token = generateToken(hashableBase);
        return buildSignedUrl(temp, token, expires, parameterDataUrl);
    }

    /**
     * Generates an authorization signature using SHA-256 hashing.
     *
     * @param libraryId      The library ID.
     * @param apiKey         The API key.
     * @param expirationTime The expiration time for the token.
     * @param videoId        The video ID.
     * @return The SHA-256 hash of the concatenated input data.
     */
    public String generateAuthorizationSignature(String libraryId, String apiKey, String expirationTime, String videoId) {
        var data = libraryId + apiKey + expirationTime + videoId;
        return DigestUtils.sha256Hex(data);
    }

    /**
     * Calculates the expiration timestamp for the token (current time + 24 hours).
     *
     * @return The expiration timestamp as a string.
     */
    public String calculateExpirationTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond() + 86400L);
    }

    /**
     * Parses the query parameters from the given URI into a map.
     *
     * @param uri The URI from which to extract parameters.
     * @return A map containing the query parameters.
     */
    private Map<String, String> parseUrlParameters(URI uri) {
        Map<String, String> parametersMap = new HashMap<>();
        URLEncodedUtils.parse(uri, StandardCharsets.US_ASCII)
                .forEach(param -> parametersMap.put(param.getName(), param.getValue()));
        return parametersMap;
    }

    /**
     * Determines the signature path based on the allowed path or the URL path.
     *
     * @param temp          The URL object.
     * @param allowedPath   The allowed path (optional).
     * @param parametersMap The map of query parameters.
     * @return The signature path to be used for token generation.
     */
    private String determineSignaturePath(URL temp, String allowedPath, Map<String, String> parametersMap) {
        if (allowedPath != null) {
            parametersMap.put("token_path", allowedPath);
            return allowedPath;
        }
        return temp.getPath();
    }

    /**
     * Builds a string of sorted key-value pairs from the parameters map.
     *
     * @param parametersMap The map of query parameters.
     * @return A string of key-value pairs joined by "&".
     */
    private String buildParameterData(Map<String, String> parametersMap) {
        return String.join("&", sortByKeys(parametersMap).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new));
    }

    /**
     * Builds a URL-encoded string of sorted key-value pairs from the parameters map.
     *
     * @param parametersMap The map of query parameters.
     * @return A URL-encoded string of key-value pairs prefixed with "&".
     */
    private String buildParameterDataUrl(Map<String, String> parametersMap) {
        return sortByKeys(parametersMap).entrySet().stream()
                .map(entry -> "&" + entry.getKey() + "=" + encodeValue(entry.getValue()))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Generates a token by hashing the input string using SHA-256 and encoding it in Base64 URL format.
     *
     * @param hashableBase The input string to be hashed.
     * @return The Base64 URL-encoded token.
     */
    private String generateToken(String hashableBase) {
        var hash = DigestUtils.sha256(hashableBase);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Constructs the final signed URL by appending the token, expiration, and parameters.
     *
     * @param temp            The URL object.
     * @param token           The generated token.
     * @param expires         The expiration timestamp.
     * @param parameterDataUrl The URL-encoded parameter string.
     * @return The fully constructed signed URL.
     */
    private String buildSignedUrl(URL temp, String token, String expires, String parameterDataUrl) {
        return temp.getProtocol() + "://" + temp.getHost() + "/bcdn_token=" + token + "&expires="
                + expires + parameterDataUrl + temp.getPath();
    }

    /**
     * URL-encodes a given value using ASCII encoding.
     *
     * @param value The value to be encoded.
     * @return The URL-encoded value.
     */
    private static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    /**
     * Sorts a map by its keys in natural order.
     *
     * @param map The map to be sorted.
     * @return A new TreeMap containing the sorted entries.
     */
    private <K extends Comparable<? super K>, V> Map<K, V> sortByKeys(Map<K, V> map) {
        return new TreeMap<>(map);
    }
}