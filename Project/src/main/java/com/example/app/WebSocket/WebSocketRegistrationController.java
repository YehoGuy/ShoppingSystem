package com.example.app.WebSocket;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketRegistrationController {

    private final SessionUserRegistry registry;

    public WebSocketRegistrationController(SessionUserRegistry registry) {
        this.registry = registry;
    }

    @MessageMapping("/register")
    public void registerUser(@Header("simpSessionId") String sessionId, Integer userId) {
        registry.register(sessionId, userId);
        System.out.println("Registered userId=" + userId + " with sessionId=" + sessionId);
    }
}
