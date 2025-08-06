package com.example.app.WebSocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionUserRegistry {

    private final Map<String, Integer> sessionIdToUserId = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> userIdToSessions = new ConcurrentHashMap<>();

    public void register(String sessionId, Integer userId) {
        sessionIdToUserId.put(sessionId, userId);
        userIdToSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(sessionId);
    }

    public void unregister(String sessionId) {
        Integer userId = sessionIdToUserId.remove(sessionId);
        if (userId != null) {
            List<String> sessions = userIdToSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userIdToSessions.remove(userId);
                }
            }
        }
    }

    public List<String> getSessionsForUser(Integer userId) {
        return userIdToSessions.getOrDefault(userId, Collections.emptyList());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        unregister(sessionId);
    }
}
