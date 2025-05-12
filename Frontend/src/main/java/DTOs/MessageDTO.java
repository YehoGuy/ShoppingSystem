package DTOs;

public class MessageDTO {
    private final int messageId;
    private final int senderId;
    private final int receiverId;
    private final String content;
    private final String timestamp;
    private final boolean userToUser;
    private final int previousMessageId;
    private final boolean isDeleted;

    public MessageDTO(int messageId, int senderId, int receiverId, String content,
                      String timestamp, boolean userToUser, int previousMessageId, boolean isDeleted) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.userToUser = userToUser;
        this.previousMessageId = previousMessageId;
        this.isDeleted = isDeleted;
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
        return isDeleted;
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "messageId=" + messageId +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", content='" + content + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", userToUser=" + userToUser +
                ", previousMessageId=" + previousMessageId +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
