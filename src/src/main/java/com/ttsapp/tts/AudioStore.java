package com.ttsapp.tts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class AudioStore {

    private static final Logger logger = LoggerFactory.getLogger(AudioStore.class);
    
    private final Cache<String, byte[]> audioCache;

    public AudioStore() {
        this.audioCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
        
        logger.info("Audio store initialized with 10-minute TTL");
    }

    public String store(byte[] audioData) {
        String id = UUID.randomUUID().toString();
        audioCache.put(id, audioData);
        logger.debug("Stored audio with ID: {}, size: {} bytes", id, audioData.length);
        return id;
    }

    public byte[] retrieve(String id) {
        byte[] audioData = audioCache.getIfPresent(id);
        if (audioData != null) {
            logger.debug("Retrieved audio with ID: {}, size: {} bytes", id, audioData.length);
        } else {
            logger.warn("Audio not found for ID: {}", id);
        }
        return audioData;
    }

    public void remove(String id) {
        audioCache.invalidate(id);
        logger.debug("Removed audio with ID: {}", id);
    }

    public long size() {
        return audioCache.estimatedSize();
    }
}
