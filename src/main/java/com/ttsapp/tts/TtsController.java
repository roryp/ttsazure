package com.ttsapp.tts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false, defaultValue = "1.0") double speed,
            @RequestParam(required = false, defaultValue = "mp3") String format,
            Model model) {
        
        logger.info("TTS request: voice={}, style={}, speed={}, format={}, text_length={}", 
                   voice, style, speed, format, text.length());

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

            // Validate speed parameter
            if (speed < 0.25 || speed > 4.0) {
                model.addAttribute("error", "Speed must be between 0.25 and 4.0");
                model.addAttribute("voices", AVAILABLE_VOICES);
                return "index";
            }

            // Validate format parameter
            if (!format.equals("mp3") && !format.equals("wav") && !format.equals("opus")) {
                model.addAttribute("error", "Invalid audio format. Supported: mp3, wav, opus");
                model.addAttribute("voices", AVAILABLE_VOICES);
                return "index";
            }

            // Generate speech
            byte[] audioData = openAIService.generateSpeech(text, voice, style, speed, format);
            
            // Store audio
            String audioId = audioStore.store(audioData);

            // Add attributes to model
            model.addAttribute("voices", AVAILABLE_VOICES);
            model.addAttribute("lastText", text);
            model.addAttribute("lastVoice", voice);
            model.addAttribute("lastStyle", style);
            model.addAttribute("lastSpeed", speed);
            model.addAttribute("lastFormat", format);
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
            model.addAttribute("lastSpeed", speed);
            model.addAttribute("lastFormat", format);
        }

        return "index";
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<byte[]> streamAudio(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download,
            @RequestParam(required = false, defaultValue = "mp3") String format) {
        
        logger.info("Audio request: id={}, download={}, format={}", id, download, format);

        byte[] audioData = audioStore.retrieve(id);
        if (audioData == null) {
            logger.warn("Audio not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        
        // Set appropriate content type based on format
        switch (format.toLowerCase()) {
            case "wav" -> headers.setContentType(MediaType.valueOf("audio/wav"));
            case "opus" -> headers.setContentType(MediaType.valueOf("audio/opus"));
            default -> headers.setContentType(MediaType.valueOf("audio/mpeg"));
        }
        
        headers.setContentLength(audioData.length);
        
        // Add streaming headers to prevent caching and enable proper audio streaming
        headers.set("Accept-Ranges", "bytes");
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        headers.set("Access-Control-Allow-Origin", "*");
        
        if (download) {
            String fileExtension = format.toLowerCase().equals("mp3") ? "mp3" : 
                                  format.toLowerCase().equals("wav") ? "wav" : 
                                  format.toLowerCase().equals("opus") ? "opus" : "mp3";
            headers.setContentDispositionFormData("attachment", "audio_" + id + "." + fileExtension);
        } else {
            headers.set("Content-Disposition", "inline");
        }

        logger.info("Serving audio: id={}, size={} bytes, download={}, format={}", id, audioData.length, download, format);
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
