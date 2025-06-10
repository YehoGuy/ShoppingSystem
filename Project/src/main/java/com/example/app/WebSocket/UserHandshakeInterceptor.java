package com.example.app.WebSocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class UserHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            String query = request.getURI().getQuery(); // e.g., "userId=123"
            System.out.println("Handshake query: " + query);

            if (query != null && query.startsWith("userId=")) {
                String userId = query.split("=")[1];
                StompPrincipal principal = new StompPrincipal(userId);
                attributes.put("user", principal);
                System.out.println("Added StompPrincipal to attributes: " + principal.getName());
            }
        } catch (Exception e) {
            e.printStackTrace(); // fallback if parsing fails
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
