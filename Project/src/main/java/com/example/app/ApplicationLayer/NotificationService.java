package com.example.app.ApplicationLayer;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private UserService userService;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setService(UserService userService) {
        this.userService = userService;
    }

    public void sendToUser(int userId, String title, String message) {
        try {
            User user = userService.getUserById(userId);
            if (user instanceof Member) {
                Member member = (Member) user;
                if (member.isConnected()) {
                    messagingTemplate.convertAndSend("/topic/notifications/" + userId, message);
                } else {
                    userService.addNotification(userId, title, message);
                }
            }
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, message);
        } catch (Exception e) {
            // Handle exception
            throw new RuntimeException("Error sending notification", e);
        }
    }
}
