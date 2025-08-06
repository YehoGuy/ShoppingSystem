package com.example.app.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user"); // for broadcasting
        config.setApplicationDestinationPrefixes("/app"); // for @MessageMapping
        config.setUserDestinationPrefix("/user"); // for user-specific messages
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setHandshakeHandler(new CustomHandshakeHandler()) // ✅ use your custom handshake handler
                .addInterceptors(new UserHandshakeInterceptor()) // ✅ add your custom interceptor
                .setAllowedOrigins("http://localhost:8081") // ✅ allow your frontend
                .withSockJS(); // enable SockJS fallback
    }
}
