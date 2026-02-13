package com.rag.ownermanual.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Chat/LLM API health check.
 *
*/
@Component
public class ChatModelHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ChatModelHealthIndicator.class);
    private static final String HEALTH_PROBE_MESSAGE = "Reply with exactly: ok";

    private final ChatModel chatModel;

    public ChatModelHealthIndicator(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Health health() {
        try {
            // Keep prompt tiny to minimize tokens/cost; we only care that a response arrives.
            String response = chatModel.call(HEALTH_PROBE_MESSAGE);
            boolean hasResponse = response != null && !response.isBlank();
            return Health.up()
                    .withDetail("responseReceived", hasResponse)
                    .build();
        } catch (Exception e) {
            log.warn("Chat/LLM health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
