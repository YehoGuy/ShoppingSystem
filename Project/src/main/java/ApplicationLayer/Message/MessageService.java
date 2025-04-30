package ApplicationLayer.Message;

import java.time.LocalDate;
import java.util.List;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.OurArg;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.User.UserService;
import ApplicationLayer.Shop.ShopService;
import DomainLayer.IMessageRepository;
import DomainLayer.Message;
import DomainLayer.Shop.Shop;

public class MessageService {

    public IMessageRepository messageRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ShopService shopService;

    public MessageService(IMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void setService(AuthTokenService authTokenService, UserService userService, ShopService shopService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.shopService = shopService;
    }

    public String sendMessageToUser(String token, int receiverId, String content, int previousMessageId) {
        // need to validate the token and get the senderId from it
        try {
            LoggerService.logMethodExecution("sendMessageToUser", token, receiverId, content, previousMessageId);
            int senderId = authTokenService.ValidateToken(token); // get the senderId from the token
            userService.validateMemberId(senderId); // validate the senderId
            userService.validateMemberId(receiverId); // validate the receiverId
            if (!messageRepository.isMessagePrevious(previousMessageId, senderId, receiverId)) {
                OurRuntime e = new OurRuntime(
                        "Previous message with ID " + previousMessageId + " isn't proper previous message.");
                LoggerService.logDebug("sendMessageToUser", e); // Log the debug message
                return "Error sending message to user: " + e.getMessage(); // Return the error message
            }
            messageRepository.addMessage(senderId, receiverId, content, LocalDate.now().toString(), true,
                    previousMessageId);
            LoggerService.logMethodExecutionEnd("sendMessageToUser", "Message send successfully!"); // Log the success
            return "Message sent successfully!";
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToUser", e);
            return "Error sending message to user: " + e.getMessage(); // Return the error message
        } catch (Exception e) {
            LoggerService.logError("sendMessageToUser", e, token, receiverId, content, previousMessageId); // Log the
                                                                                                           // error
            return "Error sending message to user: " + e.getMessage(); // Return the error message
        }
    }

    public String sendMessageToShop(String token, int receiverId, String content, int previousMessageId) {
        // need to validate the token and get the senderId from it
        try {
            LoggerService.logMethodExecution("sendMessageToShop", token, receiverId, content, previousMessageId);
            int userId = authTokenService.ValidateToken(token); // get the senderId from the token
            userService.validateMemberId(userId); // validate the senderId
            Shop s = shopService.getShop(receiverId, token); // validate the receiverId
            if (s == null) {
                OurRuntime e = new OurRuntime("Shop with ID " + receiverId + " doesn't exist."); // Create an exception
                LoggerService.logDebug("sendMessageToShop", e); // Log the error
                return "Error sending message to shop: " + e.getMessage(); // Return the error message
            }
            if (!messageRepository.isMessagePrevious(previousMessageId, userId, receiverId)) {
                OurRuntime e = new OurRuntime(
                        "Previous message with ID " + previousMessageId + " isn't proper previous message.");
                LoggerService.logDebug("sendMessageToUser", e); // Log the error
                return "Error sending message to user: " + e.getMessage(); // Return the error message
            }
            messageRepository.addMessage(userId, receiverId, content, LocalDate.now().toString(), false,
                    previousMessageId);
            LoggerService.logMethodExecutionEnd("sendMessageToShop", "Message send successfully!"); // Log the success
            return "Message sent successfully!";
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToShop", e); // Log the error
            return "Error sending message to shop: " + e.getMessage(); // Return the error message
        } catch (Exception e) {
            LoggerService.logError("sendMessageToShop", e, token, receiverId, content, previousMessageId);
            return "Error sending message to shop: " + e.getMessage(); // Return the error message
        }
    }

    // from here the functions are not necessary for the project, so they aren't
    // implemented correctly.

    public String deleteMessage(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("deleteMessage", token, messageId); // Log the method execution
            int senderId = authTokenService.ValidateToken(token); // get the senderId from the token
            userService.validateMemberId(senderId); // validate the senderId
            messageRepository.deleteMessage(messageId, senderId);
            LoggerService.logMethodExecutionEnd("deleteMessage", "Message deleted successfully!"); // Log the success
            return "Message deleted successfully!";
        } catch (OurRuntime e) {
            LoggerService.logDebug("deleteMessage", e); // Log the error
            return "Error deleting message: " + e.getMessage(); // Return the error message
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
        // can make it prettier: get username of the sender of each message (for this i
        // need userService)
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
