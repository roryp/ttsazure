package com.ttsapp.tts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class VibeService {

    private static final Logger logger = LoggerFactory.getLogger(VibeService.class);
    private final List<Vibe> vibes;
    private final Random random = new Random();

    public VibeService() throws IOException {
        this.vibes = loadVibes();
        logger.info("Loaded {} vibes", vibes.size());
    }

    private List<Vibe> loadVibes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("vibes.json");
        
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(inputStream, new TypeReference<List<Vibe>>() {});
        }
    }

    public List<Vibe> getAllVibes() {
        return vibes;
    }

    public Optional<Vibe> getVibeByName(String name) {
        return vibes.stream()
                .filter(vibe -> vibe.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<Vibe> getRandomVibes(int count) {
        if (count >= vibes.size()) {
            return vibes;
        }
        
        return vibes.stream()
                .sorted((a, b) -> random.nextInt(3) - 1) // Random shuffle
                .limit(count)
                .toList();
    }

    public Vibe getRandomVibe() {
        if (vibes.isEmpty()) {
            return null;
        }
        return vibes.get(random.nextInt(vibes.size()));
    }

    public static class Vibe {
        private String name;
        private String description;
        private String script;

        // Default constructor for Jackson
        public Vibe() {}

        public Vibe(String name, String description, String script) {
            this.name = name;
            this.description = description;
            this.script = script;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }
}
