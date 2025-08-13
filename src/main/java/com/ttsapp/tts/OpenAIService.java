package com.ttsapp.tts;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
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
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private static final String API_VERSION = "2025-03-01-preview";
    private static final String COGNITIVE_SERVICES_SCOPE = "https://cognitiveservices.azure.com/.default";

    private final String endpoint;
    private final String deployment;
    private final String model;
    private final TokenCredential credential;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.deployment}") String deployment,
            @Value("${azure.openai.model}") String model) {
        this.endpoint = endpoint;
        this.deployment = deployment;
        this.model = model;
        this.credential = new DefaultAzureCredentialBuilder()
                .build();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        
        logger.info("OpenAI Service initialized with endpoint: {}, deployment: {}, model: {} using managed identity", 
                    endpoint, deployment, model);
    }

    public byte[] generateSpeech(String text, String voice, String style) throws IOException, InterruptedException {
        return generateSpeech(text, voice, style, "mp3");
    }

    public byte[] generateSpeech(String text, String voice, String style, String format) throws IOException, InterruptedException {
        logger.info("Generating speech for text length: {}, voice: {}, style: {}, format: {}", 
                text.length(), voice, style, format);
        
        // Get access token for Azure Cognitive Services
        TokenRequestContext tokenRequestContext = new TokenRequestContext()
                .addScopes(COGNITIVE_SERVICES_SCOPE);
        AccessToken token = credential.getToken(tokenRequestContext).block();
        
        if (token == null) {
            throw new RuntimeException("Failed to obtain access token from managed identity");
        }
        
        // Process text for better speech synthesis
        String processedText = processTextForSpeech(text);
        logger.debug("Processed text: {}", processedText);

        // Prepare request body with all parameters
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", processedText);
        requestBody.put("voice", voice);
        requestBody.put("response_format", format);

        // Add instructions parameter if style is provided
        if (style != null && !style.trim().isEmpty()) {
            requestBody.put("instructions", style.trim());
            logger.debug("Using style instructions: {}", style.trim());
        }

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        logger.debug("Request body: {}", requestBodyJson);

        // Build the request
        String url = String.format("%s/openai/deployments/%s/audio/speech?api-version=%s", 
                                   endpoint, deployment, API_VERSION);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token.getToken())
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

    /**
     * Process text for better speech synthesis
     */
    private String processTextForSpeech(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String processedText = text;
        
        // Escape special XML characters for better TTS processing
        processedText = processedText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
        
        // Add natural pauses for better speech flow
        processedText = processedText
            .replace(". ", ". <break time=\"0.5s\"/> ")
            .replace("! ", "! <break time=\"0.5s\"/> ")
            .replace("? ", "? <break time=\"0.5s\"/> ")
            .replace(", ", ", <break time=\"0.2s\"/> ");
        
        return processedText;
    }
}
