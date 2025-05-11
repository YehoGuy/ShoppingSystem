package PresentationLayer.DTO.Message;


import jakarta.validation.constraints.NotBlank;

/**
 
Simple notification payload for WebSocket / push endpoints.*/
public record NotificationDTO(
        @NotBlank String title,
        @NotBlank String message) {

    /* ---------- Domain ➜ DTO ---------- /
    public static NotificationDTO fromDomain(DomainLayer.Notification n) {
        return new NotificationDTO(n.getTitle(), n.getMessage());
    }

    / ---------- DTO ➜ Domain ---------- */
    public DomainLayer.Notification toDomain() {
        return new DomainLayer.Notification(title, message);
    }
}