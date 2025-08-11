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
import java.util.HashMap;
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
        return generateSpeech(text, voice, style, 1.0, "mp3");
    }

    public byte[] generateSpeech(String text, String voice, String style, double speed, String format) throws IOException, InterruptedException {
        logger.info("Generating speech for text length: {}, voice: {}, style: {}, speed: {}, format: {}", 
                   text.length(), voice, style, speed, format);

        // Process text for better speech synthesis
        String processedText = processTextForSpeech(text);
        logger.debug("Processed text: {}", processedText);

        // Prepare request body with all parameters
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", processedText);
        requestBody.put("voice", voice);
        requestBody.put("speed", speed);
        requestBody.put("response_format", format);

        // Add instructions parameter if style is provided
        if (style != null && !style.trim().isEmpty()) {
            String instructions = formatStyleInstructions(style.trim());
            requestBody.put("instructions", instructions);
            logger.debug("Using style instructions: {}", instructions);
        }

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

    /**
     * Formats the style into proper instructions for the TTS API
     */
    private String formatStyleInstructions(String style) {
        String lowerStyle = style.toLowerCase();
        
        // Map common style keywords to detailed voice instructions
        return switch (lowerStyle) {
            case "excited", "enthusiastic", "energetic" -> 
                "Voice Affect: Excited, energetic, vibrant; embody enthusiasm and high energy.\n\n" +
                "Tone: Upbeat, animated, spirited; convey genuine excitement and positivity.\n\n" +
                "Pacing: Slightly faster than normal, with dynamic rhythm and emphasis.\n\n" +
                "Emotion: Joyful and enthusiastic; express genuine excitement and engagement.";
                
            case "calm", "peaceful", "relaxed" -> 
                "Voice Affect: Soft, gentle, soothing; embody tranquility and peace.\n\n" +
                "Tone: Calm, reassuring, peaceful; convey warmth and serenity.\n\n" +
                "Pacing: Slow, deliberate, and unhurried; use gentle pauses for relaxation.\n\n" +
                "Emotion: Deeply soothing and comforting; express genuine kindness and care.";
                
            case "sad", "melancholy", "somber" -> 
                "Voice Affect: Subdued, reflective, gentle; embody melancholy and introspection.\n\n" +
                "Tone: Soft, contemplative, slightly somber; convey empathy and understanding.\n\n" +
                "Pacing: Slower than normal, with thoughtful pauses and gentle delivery.\n\n" +
                "Emotion: Compassionate and understanding; express gentle sadness and empathy.";
                
            case "angry", "frustrated", "annoyed" -> 
                "Voice Affect: Intense, forceful, sharp; embody controlled anger and frustration.\n\n" +
                "Tone: Firm, direct, assertive; convey strong emotion while remaining articulate.\n\n" +
                "Pacing: Varied, with emphasis on key words and controlled intensity.\n\n" +
                "Emotion: Frustrated but controlled; express anger while maintaining clarity.";
                
            case "whisper", "whispering", "quiet" -> 
                "Voice Affect: Extremely soft, intimate, hushed; embody secrecy and closeness.\n\n" +
                "Tone: Gentle, conspiratorial, intimate; convey privacy and confidentiality.\n\n" +
                "Pacing: Slower and more deliberate, with careful articulation despite low volume.\n\n" +
                "Emotion: Intimate and personal; express closeness and confidential sharing.";
                
            case "dramatic", "theatrical" -> 
                "Voice Affect: Bold, expressive, commanding; embody theatrical presence and drama.\n\n" +
                "Tone: Powerful, emphatic, captivating; convey importance and gravitas.\n\n" +
                "Pacing: Varied for dramatic effect, with strategic pauses and emphasis.\n\n" +
                "Emotion: Passionate and compelling; express intensity and dramatic flair.";
                
            case "friendly", "warm", "welcoming" -> 
                "Voice Affect: Warm, approachable, genuine; embody friendliness and openness.\n\n" +
                "Tone: Cordial, inviting, pleasant; convey genuine care and interest.\n\n" +
                "Pacing: Natural and comfortable, with warm inflection and welcoming rhythm.\n\n" +
                "Emotion: Genuinely friendly and caring; express warmth and authentic connection.";
                
            case "professional", "formal", "business" -> 
                "Voice Affect: Polished, authoritative, confident; embody professionalism and competence.\n\n" +
                "Tone: Clear, articulate, respectful; convey expertise and reliability.\n\n" +
                "Pacing: Measured and precise, with clear enunciation and appropriate pauses.\n\n" +
                "Emotion: Confident and trustworthy; express professionalism and competence.";
                
            case "mysterious", "secretive" -> 
                "Voice Affect: Enigmatic, intriguing, subtle; embody mystery and intrigue.\n\n" +
                "Tone: Hushed, conspiratorial, captivating; convey secrets and hidden knowledge.\n\n" +
                "Pacing: Deliberate and measured, with strategic pauses to build suspense.\n\n" +
                "Emotion: Intriguing and enigmatic; express mystery and hidden depths.";
                
            case "cheerful", "happy", "joyful" -> 
                "Voice Affect: Bright, uplifting, positive; embody joy and happiness.\n\n" +
                "Tone: Cheerful, optimistic, radiant; convey genuine happiness and positivity.\n\n" +
                "Pacing: Lively and buoyant, with upbeat rhythm and joyful inflection.\n\n" +
                "Emotion: Genuinely happy and uplifting; express joy and positive energy.";
                
            case "serious", "stern", "grave" -> 
                "Voice Affect: Authoritative, weighty, solemn; embody gravity and importance.\n\n" +
                "Tone: Serious, measured, commanding; convey significance and weight.\n\n" +
                "Pacing: Deliberate and controlled, with emphasis on important points.\n\n" +
                "Emotion: Solemn and authoritative; express gravity and serious intent.";
                
            case "playful", "fun", "silly" -> 
                "Voice Affect: Light, bouncy, animated; embody playfulness and fun.\n\n" +
                "Tone: Playful, mischievous, entertaining; convey humor and lightheartedness.\n\n" +
                "Pacing: Varied and dynamic, with playful rhythm and animated delivery.\n\n" +
                "Emotion: Fun-loving and spirited; express playfulness and joy.";
                
            case "sarcastic", "ironic" -> 
                "Voice Affect: Dry, pointed, subtly mocking; embody sarcasm and irony.\n\n" +
                "Tone: Sardonic, witty, slightly condescending; convey clever mockery.\n\n" +
                "Pacing: Normal with strategic emphasis on ironic points and subtle pauses.\n\n" +
                "Emotion: Cleverly sarcastic; express wit and subtle mockery.";
                
            default -> 
                "Voice Affect: Expressive and engaging; embody the " + style + " quality throughout.\n\n" +
                "Tone: Appropriate to " + style + " context; convey the intended emotional state.\n\n" +
                "Pacing: Natural but adjusted to suit " + style + " delivery.\n\n" +
                "Emotion: Authentic " + style + " expression; maintain consistency throughout.";
        };
    }
}
