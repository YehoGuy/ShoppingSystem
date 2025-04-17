package DomainLayer;

public class Message {
    final private int messageId;
    final private int senderId; // User ID of the sender
    final private int receiverId; // User ID of the receiver
    final private String content; // Message content
    final private String timestamp; // Timestamp of when the message was sent
    final private boolean userToUser; // true if the message is between users, false if it's User to Shop

    public Message(int messageId, int senderId, int receiverId, String content, String timestamp, boolean userToUser) {
        this.messageId = messageId; // Initialize message ID
        this.senderId = senderId; // Initialize sender ID
        this.receiverId = receiverId; // Initialize receiver ID
        this.content = content; // Initialize message content
        this.timestamp = timestamp; // Initialize timestamp
        this.userToUser = userToUser; // Initialize userToUser flag
    }

    public int getMessageId() {
        return messageId; // Return message ID
    }

    public int getSenderId() {
        return senderId; // Return sender ID
    }

    public int getReceiverId() {
        return receiverId; // Return receiver ID
    }

    public String getContent() {
        return content; // Return message content
    }

    public String getTimestamp() {
        return timestamp; // Return timestamp
    }

    public boolean isUserToUser() {
        return userToUser; // Return userToUser flag
    }
}
