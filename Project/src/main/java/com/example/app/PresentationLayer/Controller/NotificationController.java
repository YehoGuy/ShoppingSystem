package com.example.app.PresentationLayer.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.app.ApplicationLayer.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send/{userId}")
    public void sendNotification(@PathVariable String userId, @RequestBody String message) {
        this.notificationService.sendToUser(userId, message);
    }
}
