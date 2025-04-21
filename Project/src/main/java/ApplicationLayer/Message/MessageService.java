package ApplicationLayer.Message;

import java.time.LocalDate;
import java.util.List;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.User.UserService;
import DomainLayer.IMessageRepository;
import DomainLayer.Message;

public class MessageService {

    public IMessageRepository messageRepository;
    private AuthTokenService authTokenService;
    private UserService userService; 

    public MessageService(IMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageService(IMessageRepository messageRepository, AuthTokenService authTokenService, UserService userService) {
        this.messageRepository = messageRepository;
        this.authTokenService = authTokenService;
        this.userService = userService; 
    }

    public String sendMessageToUser( String token, int senderId /**(shouldnt be here) */, int receiverId, String content, int previousMessageId) {
        // need to validate the token and get the senderId from it
        try {
            messageRepository.addMessage(senderId, receiverId, content, LocalDate.now().toString(), true, previousMessageId);
            return "Message sent successfully!";
        } catch (Exception e) {
            throw new RuntimeException("Error sending message to user: " + e.getMessage(), e);
        }
    }

    public String sendMessageToShop(String token, int senderId /**(shouldnt be here) */, int receiverId, String content, int previousMessageId) {
        // need to validate the token and get the senderId from it
        try {
            messageRepository.addMessage(senderId, receiverId, content, LocalDate.now().toString(), false, previousMessageId);
            return "Message sent successfully!";
        } catch (Exception e) {
            throw new RuntimeException("Error sending message to shop: " + e.getMessage(), e);
        }
    }

    public String deleteMessage(String token, int messageId) {
        // need to validate the token and get the senderId from it
        // need to check if the senderId is the same as the one in the message
        try {
            messageRepository.deleteMessage(messageId);
            return "Message deleted successfully!";
        } catch (Exception e) {
            throw new RuntimeException("Error deleting message: " + e.getMessage(), e);
        }
    }

    public String updateMessage(String token, int messageId, String content) {
        // need to validate the token and get the senderId from it
        try {
            messageRepository.updateMessage(messageId, content, LocalDate.now().toString());
            return "Message updated successfully!";
        } catch (Exception e) {
            throw new RuntimeException("Error updating message: " + e.getMessage(), e);
        }
    }

    public String getFullConversation(String token, int messageId) {
        // need to validate the token and get the senderId from it
        // need to check if the senderId is the same as the one in the message
        // can make it prettier: get username of the sender of each message (for this i need userService)
        try {
            String output = "Full conversation:\n";
            List<Message> messages = messageRepository.getFullConversation(messageId);
            for (Message message : messages) {
                if (message.isDeleted()) {
                    output += "Message deleted\n";
                    continue;
                }
                output += message.toString() + "\n";
            }
            return output;

        } catch (Exception e) {
            throw new RuntimeException("Error getting full conversation: " + e.getMessage(), e);
        }
    }

    public String getMessagesBySenderId(String token, int senderId) {
        // need to validate the token and get the senderId from it
        try {
            String output = "Messages sent by user " + senderId + ":\n";
            List<Message> messages = messageRepository.getMessagesBySenderId(senderId);
            for (Message message : messages) {
                output += message.toString() + "\n";
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Error getting messages by sender ID: " + e.getMessage(), e);
        }
    }

    public String getMessagesByReceiverId(String token, int receiverId) {
        // need to validate the token and get the senderId from it
        try {
            String output = "Messages received by user " + receiverId + ":\n";
            List<Message> messages = messageRepository.getMessagesByReceiverId(receiverId);
            for (Message message : messages) {
                output += message.toString() + "\n";
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Error getting messages by receiver ID: " + e.getMessage(), e);
        }
    }

    public String getMessageById(String token, int messageId) {
        // need to validate the token and get the senderId from it
        try {
            Message message = messageRepository.getMessageById(messageId);
            if (message == null) {
                return "Message not found!";
            }
            return message.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error getting message by ID: " + e.getMessage(), e);
        }
    }

    public String getPreviousMessage(String token, int messageId) {
        // need to validate the token and get the senderId from it
        // need to check if the senderId is the same as the one in the message
        try {
            Message message = messageRepository.getPreviousMessage(messageId);
            if (message == null) {
                return "No previous message found!";
            }
            return message.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error getting previous message: " + e.getMessage(), e);
        }
    }
    
}
