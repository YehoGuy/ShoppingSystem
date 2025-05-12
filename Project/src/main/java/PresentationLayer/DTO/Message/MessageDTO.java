package PresentationLayer.DTO.Message;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 
Transport object for chat messages over the API.*/
public record MessageDTO(
        @Positive int messageId,
        @Positive int senderId,
        @Positive int receiverId,
        @NotBlank String content,
        @NotBlank String timestamp,       // ISO‑8601 string, e.g. "2025-05-07T13:45:30Z"
        boolean userToUser,
        int previousMessageId,
        boolean deleted) {

    /* ---------- Domain ➜ DTO ---------- /
    public static MessageDTO fromDomain(DomainLayer.Message m) {
        return new MessageDTO(
                m.getMessageId(),
                m.getSenderId(),
                m.getReceiverId(),
                m.getContent(),
                m.getTimestamp(),
                m.isUserToUser(),
                m.getPreviousMessageId(),
                m.isDeleted());
    }

    / ---------- DTO ➜ Domain ---------- */
    public DomainLayer.Message toDomain() {
        DomainLayer.Message m = new DomainLayer.Message(
                messageId,
                senderId,
                receiverId,
                content,
                timestamp,
                userToUser,
                previousMessageId);
        if (deleted) m.delete();
        return m;
    }
}