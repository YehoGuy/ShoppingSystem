package DTOs;

public class MessageDTO {
    private int messageId;
    private int senderId;
    private int receiverId;
    private String content;
    private String timestamp;
    private boolean userToUser;
    private int previousMessageId;
    private boolean isDeleted;

    // Required no-arg constructor for Jackson
    public MessageDTO() {}

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

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isUserToUser() { return userToUser; }
    public void setUserToUser(boolean userToUser) { this.userToUser = userToUser; }

    public int getPreviousMessageId() { return previousMessageId; }
    public void setPreviousMessageId(int previousMessageId) { this.previousMessageId = previousMessageId; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

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
