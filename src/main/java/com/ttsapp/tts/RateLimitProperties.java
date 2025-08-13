package com.ttsapp.tts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    
    private int maxRequestsPerMinute = 10;
    private int maxRequestsPerHour = 100;
    private int maxCharactersPerHour = 50000;
    private boolean enabled = true;

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public int getMaxRequestsPerHour() {
        return maxRequestsPerHour;
    }

    public void setMaxRequestsPerHour(int maxRequestsPerHour) {
        this.maxRequestsPerHour = maxRequestsPerHour;
    }

    public int getMaxCharactersPerHour() {
        return maxCharactersPerHour;
    }

    public void setMaxCharactersPerHour(int maxCharactersPerHour) {
        this.maxCharactersPerHour = maxCharactersPerHour;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
