package com.ttsapp.tts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

@Controller
public class TtsController {

    private static final Logger logger = LoggerFactory.getLogger(TtsController.class);
    
    private static final List<String> AVAILABLE_VOICES = List.of(
        "alloy", "echo", "fable", "onyx", "nova", "shimmer"
    );

    private final OpenAIService openAIService;
    private final AudioStore audioStore;

    public TtsController(OpenAIService openAIService, AudioStore audioStore) {
        this.openAIService = openAIService;
        this.audioStore = audioStore;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("voices", AVAILABLE_VOICES);
        return "index";
    }

    @PostMapping("/tts")
    public String generateTts(
            @RequestParam String text,
            @RequestParam(required = false) String style,
            @RequestParam String voice,
            @RequestParam(required = false, defaultValue = "false") boolean autoplay,
            Model model) {
        
        logger.info("TTS request: voice={}, style={}, autoplay={}, text_length={}", 
                   voice, style, autoplay, text.length());

        try {
            // Validate input
            if (text == null || text.trim().isEmpty()) {
                model.addAttribute("error", "Text cannot be empty");
                model.addAttribute("voices", AVAILABLE_VOICES);
                return "index";
            }

            if (!AVAILABLE_VOICES.contains(voice)) {
                model.addAttribute("error", "Invalid voice selected");
                model.addAttribute("voices", AVAILABLE_VOICES);
                return "index";
            }

            // Limit text length
            if (text.length() > 1000) {
                text = text.substring(0, 1000);
                model.addAttribute("warning", "Text was truncated to 1000 characters");
            }

            // Generate speech
            byte[] audioData = openAIService.generateSpeech(text, voice, style);
            
            // Store audio and create inline data
            String audioId = audioStore.store(audioData);
            String audioInline = Base64.getEncoder().encodeToString(audioData);

            // Add attributes to model
            model.addAttribute("voices", AVAILABLE_VOICES);
            model.addAttribute("lastText", text);
            model.addAttribute("lastVoice", voice);
            model.addAttribute("lastStyle", style);
            model.addAttribute("autoplay", autoplay);
            model.addAttribute("audioInline", audioInline);
            model.addAttribute("audioId", audioId);
            model.addAttribute("audioSize", formatFileSize(audioData.length));
            model.addAttribute("success", "Voice generated successfully!");

            logger.info("TTS generated successfully, audio ID: {}, size: {} bytes", audioId, audioData.length);

        } catch (Exception e) {
            logger.error("Error generating TTS", e);
            model.addAttribute("error", "Failed to generate voice: " + e.getMessage());
            model.addAttribute("voices", AVAILABLE_VOICES);
            model.addAttribute("lastText", text);
            model.addAttribute("lastVoice", voice);
            model.addAttribute("lastStyle", style);
        }

        return "index";
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<byte[]> streamAudio(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        
        logger.info("Audio request: id={}, download={}", id, download);

        byte[] audioData = audioStore.retrieve(id);
        if (audioData == null) {
            logger.warn("Audio not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.setContentLength(audioData.length);
        
        if (download) {
            headers.setContentDispositionFormData("attachment", "audio_" + id + ".mp3");
        }

        logger.info("Serving audio: id={}, size={} bytes, download={}", id, audioData.length, download);
        return ResponseEntity.ok()
                .headers(headers)
                .body(audioData);
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK - Audio cache size: " + audioStore.size();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
