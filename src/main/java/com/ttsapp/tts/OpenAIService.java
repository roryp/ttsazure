package com.ttsapp.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private static final String API_VERSION = "2025-03-01-preview";

    private final String endpoint;
    private final String deployment;
    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.deployment}") String deployment,
            @Value("${azure.openai.model}") String model,
            @Value("${azure.openai.api-key}") String apiKey) {
        this.endpoint = endpoint;
        this.deployment = deployment;
        this.model = model;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        
        logger.info("OpenAI Service initialized with endpoint: {}, deployment: {}, model: {}", 
                    endpoint, deployment, model);
    }

    public byte[] generateSpeech(String text, String voice, String style) throws IOException, InterruptedException {
        logger.info("Generating speech for text length: {}, voice: {}, style: {}", 
                   text.length(), voice, style);

        // Prepare request body
        String input = style != null && !style.trim().isEmpty() 
            ? style.trim() + ": " + text 
            : text;

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "input", input,
            "voice", voice
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        logger.debug("Request body: {}", requestBodyJson);

        // Build the request
        String url = String.format("%s/openai/deployments/%s/audio/speech?api-version=%s", 
                                   endpoint, deployment, API_VERSION);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .timeout(Duration.ofMinutes(2))
                .build();

        // Send the request
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body());
            logger.error("OpenAI API error: {} - {}", response.statusCode(), errorBody);
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + errorBody);
        }

        byte[] audioData = response.body();
        logger.info("Successfully generated speech audio, size: {} bytes", audioData.length);
        
        return audioData;
    }
}
