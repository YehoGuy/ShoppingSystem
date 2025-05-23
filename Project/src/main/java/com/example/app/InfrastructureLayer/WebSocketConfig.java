package com.example.app.InfrastructureLayer;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This maps to /ws in your frontend
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefixes for outgoing messages (server -> client)
        config.enableSimpleBroker("/topic");
        // Prefixes for incoming messages (client -> server)
        config.setApplicationDestinationPrefixes("/app");
    }
}
