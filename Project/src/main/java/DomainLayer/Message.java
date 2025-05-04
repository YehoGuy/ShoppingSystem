package DomainLayer;

public class Message {
    private final int messageId;
    private final int senderId; // User ID of the sender
    private final int receiverId; // User/Shop ID of the receiver (depends on userToUser flag)
    private final String content; // Message content
    private final String timestamp; // Timestamp of when the message was sent
    private final boolean userToUser; // true if the message is between users, false if it's User to Shop
    private final int previousMessageId; // ID of the previous message in the conversation, -1 if none
    private boolean isDeleted; // Flag to indicate if the message is deleted
    private final Object lock = new Object(); // Synchronization lock

    public Message(int messageId, int senderId, int receiverId, String content, String timestamp, boolean userToUser, int previousMessageId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.userToUser = userToUser;
        this.previousMessageId = previousMessageId;
        this.isDeleted = false;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isUserToUser() {
        return userToUser;
    }

    public int getPreviousMessageId() {
        return previousMessageId;
    }

    public boolean isDeleted() {
        synchronized (lock) {
            return isDeleted;
        }
    }

    public void delete() {
        synchronized (lock) {
            isDeleted = true;
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
