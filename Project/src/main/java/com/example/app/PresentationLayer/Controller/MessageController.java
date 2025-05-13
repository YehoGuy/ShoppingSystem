package com.example.app.PresentationLayer.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.ApplicationLayer.Message.MessageService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Base path: /api/messages   (JSON in / JSON out)
 *
 * Endpoints:
 * 1. POST   /user             params: authToken, receiverId, content, previousMessageId
 * 2. POST   /shop             params: authToken, receiverId, content, previousMessageId
 * 3. DELETE /{messageId}      params: authToken
 * 4. PATCH  /{messageId}      params: authToken, content
 * 5. GET    /{messageId}/conversation  params: authToken
 * 6. GET    /sender/{senderId}        params: authToken
 * 7. GET    /receiver/{receiverId}    params: authToken
 * 8. GET    /{messageId}              params: authToken
 * 9. GET    /{messageId}/previous     params: authToken
 *
 * Note: service returns status messages or error strings prefixed with "Error".
 */
@RestController
@RequestMapping("/api/messages")
@Validated
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/user")
    public ResponseEntity<String> sendToUser(
            @RequestParam @NotBlank String authToken,
            @RequestParam @Min(1) int receiverId,
            @RequestParam @NotBlank String content,
            @RequestParam @Min(0) int previousMessageId) {
        String result = messageService.sendMessageToUser(authToken, receiverId, content, previousMessageId);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @PostMapping("/shop")
    public ResponseEntity<String> sendToShop(
            @RequestParam @NotBlank String authToken,
            @RequestParam @Min(1) int receiverId,
            @RequestParam @NotBlank String content,
            @RequestParam @Min(0) int previousMessageId) {
        String result = messageService.sendMessageToShop(authToken, receiverId, content, previousMessageId);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int messageId) {
        String result = messageService.deleteMessage(authToken, messageId);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<String> updateMessage(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int messageId,
            @RequestParam @NotBlank String content) {
        String result = messageService.updateMessage(authToken, messageId, content);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{messageId}/conversation")
    public ResponseEntity<String> getConversation(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int messageId) {
        String result = messageService.getFullConversation(authToken, messageId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<String> getBySender(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int senderId) {
        String result = messageService.getMessagesBySenderId(authToken, senderId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<String> getByReceiver(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int receiverId) {
        String result = messageService.getMessagesByReceiverId(authToken, receiverId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<String> getById(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int messageId) {
        String result = messageService.getMessageById(authToken, messageId);
        if ("Message not found!".equals(result)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{messageId}/previous")
    public ResponseEntity<String> getPrevious(
            @RequestParam @NotBlank String authToken,
            @PathVariable @Min(1) int messageId) {
        String result = messageService.getPreviousMessage(authToken, messageId);
        if ("No previous message found!".equals(result)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
