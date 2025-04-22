package DomainLayer;

public class Message {
    final private int messageId;
    final private int senderId; // User ID of the sender
    final private int receiverId; // User/Shop ID of the receiver (depends on userToUser flag)
    final private String content; // Message content
    final private String timestamp; // Timestamp of when the message was sent
    final private boolean userToUser; // true if the message is between users, false if it's User to Shop
    final private int previousMessageId; // ID of the previous message in the conversation, -1 if none
    private boolean isDeleted; // Flag to indicate if the message is deleted

    public Message(int messageId, int senderId, int receiverId, String content, String timestamp, boolean userToUser, int previousMessageId) {
        this.messageId = messageId; // Initialize message ID
        this.senderId = senderId; // Initialize sender ID
        this.receiverId = receiverId; // Initialize receiver ID
        this.content = content; // Initialize message content
        this.timestamp = timestamp; // Initialize timestamp
        this.userToUser = userToUser; // Initialize userToUser flag
        this.previousMessageId = previousMessageId; // Initialize previous message ID
        this.isDeleted = false; // Initialize deleted flag to false
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

    public int getPreviousMessageId() {
        return previousMessageId; // Return previous message ID
    }

    public boolean isDeleted() {
        return isDeleted; // Return deleted flag
    }

    public void delete() {
        isDeleted = true; // Set deleted flag
    }

    public String toString() {
        return "Message{" +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}'; // Return string representation of the message
    }
}
