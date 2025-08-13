package com.ttsapp.tts;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class TtsController {

    private static final Logger logger = LoggerFactory.getLogger(TtsController.class);
    
    private static final List<String> AVAILABLE_VOICES = List.of(
        "alloy", "ash", "ballad", "coral", "echo", "fable", "nova", "onyx", "sage", "shimmer", "verse"
    );

    private final OpenAIService openAIService;
    private final AudioStore audioStore;
    private final VibeService vibeService;
    private final RateLimitService rateLimitService;
    private final ClientIdentifierService clientIdentifierService;
    private final RateLimitProperties rateLimitProperties;

    public TtsController(OpenAIService openAIService, AudioStore audioStore, VibeService vibeService,
                        RateLimitService rateLimitService, ClientIdentifierService clientIdentifierService,
                        RateLimitProperties rateLimitProperties) {
        this.openAIService = openAIService;
        this.audioStore = audioStore;
        this.vibeService = vibeService;
        this.rateLimitService = rateLimitService;
        this.clientIdentifierService = clientIdentifierService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("voices", AVAILABLE_VOICES);
        model.addAttribute("vibes", vibeService.getRandomVibes(6));
        model.addAttribute("allVibes", vibeService.getAllVibes());
        return "index";
    }

    @PostMapping("/tts")
    public String generateTts(
            @RequestParam String text,
            @RequestParam(required = false) String style,
            @RequestParam String voice,
            @RequestParam(required = false, defaultValue = "mp3") String format,
            HttpServletRequest request,
            Model model) {
        
        String clientId = clientIdentifierService.getClientIdentifier(request);
        logger.info("TTS request from client {}: voice={}, style={}, format={}, text_length={}", 
                   clientId, voice, style, format, text.length());

        try {
            // Validate input
            if (text == null || text.trim().isEmpty()) {
                model.addAttribute("error", "Text cannot be empty");
                model.addAttribute("voices", AVAILABLE_VOICES);
                model.addAttribute("vibes", vibeService.getRandomVibes(6));
                return "index";
            }

            if (!AVAILABLE_VOICES.contains(voice)) {
                model.addAttribute("error", "Invalid voice selected");
                model.addAttribute("voices", AVAILABLE_VOICES);
                model.addAttribute("vibes", vibeService.getRandomVibes(6));
                return "index";
            }

            // Limit text length
            if (text.length() > 4000) {
                text = text.substring(0, 4000);
                model.addAttribute("warning", "Text was truncated to 4000 characters");
            }

            // Rate limiting check
            if (!rateLimitService.isAllowed(clientId, text.length())) {
                RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId);
                String errorMessage = String.format(
                    "Rate limit exceeded. You have used %d/%d requests this minute, %d/%d requests this hour, and %d/%d characters this hour. Please try again later.",
                    rateLimitInfo.currentMinuteRequests, rateLimitInfo.maxMinuteRequests,
                    rateLimitInfo.currentHourlyRequests, rateLimitInfo.maxHourlyRequests,
                    rateLimitInfo.currentHourlyCharacters, rateLimitInfo.maxHourlyCharacters
                );
                
                model.addAttribute("error", errorMessage);
                model.addAttribute("voices", AVAILABLE_VOICES);
                model.addAttribute("vibes", vibeService.getRandomVibes(6));
                model.addAttribute("lastText", text);
                model.addAttribute("lastVoice", voice);
                model.addAttribute("lastStyle", style);
                model.addAttribute("lastFormat", format);
                model.addAttribute("rateLimitInfo", rateLimitInfo);
                return "index";
            }

            // Validate format parameter
            if (!format.equals("mp3") && !format.equals("wav") && !format.equals("opus")) {
                model.addAttribute("error", "Invalid audio format. Supported: mp3, wav, opus");
                model.addAttribute("voices", AVAILABLE_VOICES);
                model.addAttribute("vibes", vibeService.getRandomVibes(6));
                return "index";
            }

            // Generate speech
            byte[] audioData = openAIService.generateSpeech(text, voice, style, format);
            
            // Store audio
            String audioId = audioStore.store(audioData);

            // Get rate limit info for display
            RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId);

            // Add attributes to model
            model.addAttribute("voices", AVAILABLE_VOICES);
            model.addAttribute("vibes", vibeService.getRandomVibes(6));
            model.addAttribute("lastText", text);
            model.addAttribute("lastVoice", voice);
            model.addAttribute("lastStyle", style);
            model.addAttribute("lastFormat", format);
            model.addAttribute("audioId", audioId);
            model.addAttribute("audioSize", formatFileSize(audioData.length));
            model.addAttribute("success", "Voice generated successfully!");
            model.addAttribute("rateLimitInfo", rateLimitInfo);

            logger.info("TTS generated successfully for client {}, audio ID: {}, size: {} bytes", 
                       clientId, audioId, audioData.length);

        } catch (Exception e) {
            logger.error("Error generating TTS for client {}", clientId, e);
            model.addAttribute("error", "Failed to generate voice: " + e.getMessage());
            model.addAttribute("voices", AVAILABLE_VOICES);
            model.addAttribute("vibes", vibeService.getRandomVibes(6));
            model.addAttribute("lastText", text);
            model.addAttribute("lastVoice", voice);
            model.addAttribute("lastStyle", style);
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
    public Map<String, Object> health() {
        return Map.of(
            "status", "OK",
            "audioCacheSize", audioStore.size(),
            "rateLimits", Map.of(
                "enabled", rateLimitProperties.isEnabled(),
                "maxRequestsPerMinute", rateLimitProperties.getMaxRequestsPerMinute(),
                "maxRequestsPerHour", rateLimitProperties.getMaxRequestsPerHour(),
                "maxCharactersPerHour", rateLimitProperties.getMaxCharactersPerHour()
            )
        );
    }

    @GetMapping("/api/rate-limit-status")
    @ResponseBody
    public ResponseEntity<RateLimitService.RateLimitInfo> getRateLimitStatus(HttpServletRequest request) {
        String clientId = clientIdentifierService.getClientIdentifier(request);
        RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId);
        return ResponseEntity.ok(rateLimitInfo);
    }

    @GetMapping("/api/vibes")
    @ResponseBody
    public List<VibeService.Vibe> getVibes(@RequestParam(required = false, defaultValue = "6") int count) {
        return vibeService.getRandomVibes(count);
    }

    @GetMapping("/api/vibe/{name}")
    @ResponseBody
    public ResponseEntity<VibeService.Vibe> getVibe(@PathVariable String name) {
        return vibeService.getVibeByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/quick-tts")
    @ResponseBody
    public ResponseEntity<?> quickTts(
            @RequestParam String text,
            @RequestParam String voice,
            @RequestParam(required = false) String style,
            @RequestParam(required = false, defaultValue = "mp3") String format,
            HttpServletRequest request) {
        
        String clientId = clientIdentifierService.getClientIdentifier(request);
        logger.info("Quick TTS request from client {}: voice={}, style={}, format={}, text_length={}", 
                   clientId, voice, style, format, text.length());

        try {
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty"));
            }

            if (!AVAILABLE_VOICES.contains(voice)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid voice selected"));
            }

            // Rate limiting check
            if (!rateLimitService.isAllowed(clientId, text.length())) {
                RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId);
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Rate limit exceeded",
                    "details", String.format(
                        "You have used %d/%d requests this minute, %d/%d requests this hour, and %d/%d characters this hour",
                        rateLimitInfo.currentMinuteRequests, rateLimitInfo.maxMinuteRequests,
                        rateLimitInfo.currentHourlyRequests, rateLimitInfo.maxHourlyRequests,
                        rateLimitInfo.currentHourlyCharacters, rateLimitInfo.maxHourlyCharacters
                    ),
                    "remainingMinuteRequests", rateLimitInfo.getRemainingMinuteRequests(),
                    "remainingHourlyRequests", rateLimitInfo.getRemainingHourlyRequests(),
                    "remainingHourlyCharacters", rateLimitInfo.getRemainingHourlyCharacters()
                ));
            }

            // Generate speech
            byte[] audioData = openAIService.generateSpeech(text, voice, style, format);
            String audioId = audioStore.store(audioData);

            RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId);

            return ResponseEntity.ok().body(Map.of(
                    "audioId", audioId,
                    "audioUrl", "/audio/" + audioId + "?format=" + format,
                    "size", audioData.length,
                    "rateLimitInfo", Map.of(
                        "remainingMinuteRequests", rateLimitInfo.getRemainingMinuteRequests(),
                        "remainingHourlyRequests", rateLimitInfo.getRemainingHourlyRequests(),
                        "remainingHourlyCharacters", rateLimitInfo.getRemainingHourlyCharacters()
                    )
            ));

        } catch (Exception e) {
            logger.error("Error generating quick TTS for client {}", clientId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate voice: " + e.getMessage()));
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
