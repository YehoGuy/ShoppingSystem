package com.example.app.PresentationLayer.NotificationHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocketConfig
 * This class configures the WebSocket message broker for the application.
 * It sets up the message broker, the application destination prefixes,
 * and the STOMP endpoints for WebSocket communication.
 * It also configures the transport settings for WebSocket messages.
 * The message broker is configured to use a simple in-memory broker
 * with a dedicated scheduler for heartbeats and message dispatch.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** 
     * A dedicated scheduler for broker heartbeats & message dispatch. 
     * Increase poolSize to match your expected parallelism.
     */
    @Bean
    public ThreadPoolTaskScheduler brokerScheduler() {
        ThreadPoolTaskScheduler tps = new ThreadPoolTaskScheduler();
        tps.setPoolSize(16);
        tps.setThreadNamePrefix("ws-broker-");
        tps.initialize();
        return tps;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10_000,10_000})    // send & expect heartbeats every 10s
                .setTaskScheduler(brokerScheduler());             // use our bigger pool
        registry.setApplicationDestinationPrefixes("/app");     // messages from client
        registry.setUserDestinationPrefix("/user");             // for convertAndSendToUser
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /** Optional: tune raw WebSocket transport limits */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(64 * 1024)      // 64 KB max per STOMP frame
           .setSendBufferSizeLimit(512 * 1024)  // 512 KB per client buffer
           .setSendTimeLimit(20_000);           // 20 s before timing out send
    }
}

