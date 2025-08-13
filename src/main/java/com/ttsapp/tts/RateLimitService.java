package com.ttsapp.tts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitProperties rateLimitProperties;

    // Caches for tracking usage
    private final Cache<String, AtomicInteger> minuteCache;
    private final Cache<String, AtomicInteger> hourlyRequestCache;
    private final Cache<String, AtomicInteger> hourlyCharacterCache;

    public RateLimitService(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
        
        this.minuteCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(10000)
                .build();

        this.hourlyRequestCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10000)
                .build();

        this.hourlyCharacterCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10000)
                .build();
    }

    public boolean isAllowed(String clientIdentifier, int textLength) {
        // Check if rate limiting is enabled
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        try {
            // Check per-minute rate limit
            AtomicInteger minuteCount = minuteCache.get(clientIdentifier + ":minute", 
                k -> new AtomicInteger(0));
            
            if (minuteCount.incrementAndGet() > rateLimitProperties.getMaxRequestsPerMinute()) {
                logger.warn("Rate limit exceeded for client {}: {} requests per minute", 
                           clientIdentifier, minuteCount.get());
                return false;
            }

            // Check hourly request limit
            AtomicInteger hourlyRequestCount = hourlyRequestCache.get(clientIdentifier + ":hour:requests", 
                k -> new AtomicInteger(0));
            
            if (hourlyRequestCount.incrementAndGet() > rateLimitProperties.getMaxRequestsPerHour()) {
                logger.warn("Hourly request limit exceeded for client {}: {} requests per hour", 
                           clientIdentifier, hourlyRequestCount.get());
                return false;
            }

            // Check hourly character limit
            AtomicInteger hourlyCharCount = hourlyCharacterCache.get(clientIdentifier + ":hour:chars", 
                k -> new AtomicInteger(0));
            
            if (hourlyCharCount.addAndGet(textLength) > rateLimitProperties.getMaxCharactersPerHour()) {
                logger.warn("Hourly character limit exceeded for client {}: {} characters per hour", 
                           clientIdentifier, hourlyCharCount.get());
                return false;
            }

            logger.debug("Rate limit check passed for client {}: minute={}, hourly_requests={}, hourly_chars={}", 
                        clientIdentifier, minuteCount.get(), hourlyRequestCount.get(), hourlyCharCount.get());
            
            return true;

        } catch (Exception e) {
            logger.error("Error checking rate limit for client {}", clientIdentifier, e);
            // Fail open - allow the request if there's an error
            return true;
        }
    }

    public RateLimitInfo getRateLimitInfo(String clientIdentifier) {
        AtomicInteger minuteCount = minuteCache.getIfPresent(clientIdentifier + ":minute");
        AtomicInteger hourlyRequestCount = hourlyRequestCache.getIfPresent(clientIdentifier + ":hour:requests");
        AtomicInteger hourlyCharCount = hourlyCharacterCache.getIfPresent(clientIdentifier + ":hour:chars");

        return new RateLimitInfo(
            minuteCount != null ? minuteCount.get() : 0,
            rateLimitProperties.getMaxRequestsPerMinute(),
            hourlyRequestCount != null ? hourlyRequestCount.get() : 0,
            rateLimitProperties.getMaxRequestsPerHour(),
            hourlyCharCount != null ? hourlyCharCount.get() : 0,
            rateLimitProperties.getMaxCharactersPerHour()
        );
    }

    public static class RateLimitInfo {
        public final int currentMinuteRequests;
        public final int maxMinuteRequests;
        public final int currentHourlyRequests;
        public final int maxHourlyRequests;
        public final int currentHourlyCharacters;
        public final int maxHourlyCharacters;

        public RateLimitInfo(int currentMinuteRequests, int maxMinuteRequests,
                           int currentHourlyRequests, int maxHourlyRequests,
                           int currentHourlyCharacters, int maxHourlyCharacters) {
            this.currentMinuteRequests = currentMinuteRequests;
            this.maxMinuteRequests = maxMinuteRequests;
            this.currentHourlyRequests = currentHourlyRequests;
            this.maxHourlyRequests = maxHourlyRequests;
            this.currentHourlyCharacters = currentHourlyCharacters;
            this.maxHourlyCharacters = maxHourlyCharacters;
        }

        public int getRemainingMinuteRequests() {
            return Math.max(0, maxMinuteRequests - currentMinuteRequests);
        }

        public int getRemainingHourlyRequests() {
            return Math.max(0, maxHourlyRequests - currentHourlyRequests);
        }

        public int getRemainingHourlyCharacters() {
            return Math.max(0, maxHourlyCharacters - currentHourlyCharacters);
        }
    }
}
