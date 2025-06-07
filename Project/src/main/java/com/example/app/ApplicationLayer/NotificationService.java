package com.example.app.ApplicationLayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.WebSocket.*;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SessionUserRegistry registry;

    private UserService userService;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setService(UserService us) {
        this.userService = us;
    }

    public void sendToUser(Integer userId, String title, String message) {
        List<String> sessionIds = registry.getSessionsForUser(userId);
        System.out.println("sendToUser: userId=" + userId + ", title=" + title + ", message=" + message);
        if (!((Member) userService.getUserById(userId)).isConnected()) {
            userService.addNotification(userId, title, message);
            return;
        }
        System.out.println("Sending to user: " + userId + ", sessionIds=" + sessionIds);
        for (String sessionId : sessionIds) {
            System.out.println("Sending to sessionId=" + sessionId);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/notifications",
                    title + '\n' + message,
                    createHeaders(sessionId));
        }
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}
