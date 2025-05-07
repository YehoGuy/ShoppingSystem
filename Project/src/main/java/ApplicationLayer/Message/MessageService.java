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
        try {
            LoggerService.logMethodExecution("sendMessageToUser", token, receiverId, content, previousMessageId);
            int senderId = authTokenService.ValidateToken(token);
            userService.validateMemberId(senderId);
            userService.validateMemberId(receiverId);
            if (!messageRepository.isMessagePrevious(previousMessageId, senderId, receiverId)) {
                throw new OurRuntime("Previous message with ID " + previousMessageId + " isn't proper previous message.");
            }
            messageRepository.addMessage(senderId, receiverId, content, LocalDate.now().toString(), true, previousMessageId);
            userService.messageNotification(receiverId, senderId, true);
            LoggerService.logMethodExecutionEnd("sendMessageToUser", "Message sent successfully!");
            return "Message sent successfully!";
        } catch (OurArg e) {
            LoggerService.logDebug("sendMessageToUser", e);
            return "Error sending message to user: " + e.getMessage();
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToUser", e);
            return "Error sending message to user: " + e.getMessage();
        } catch (Exception e) {
            LoggerService.logError("sendMessageToUser", e, token, receiverId, content, previousMessageId);
            throw new OurRuntime("Error sending message to user: " + e.getMessage(), e);
        }
    }

    public String sendMessageToShop(String token, int receiverId, String content, int previousMessageId) {
        try {
            LoggerService.logMethodExecution("sendMessageToShop", token, receiverId, content, previousMessageId);
            int userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            Shop s = shopService.getShop(receiverId, token);
            if (s == null) {
                throw new OurRuntime("Shop with ID " + receiverId + " doesn't exist.");
            }
            if (!messageRepository.isMessagePrevious(previousMessageId, userId, receiverId)) {
                throw new OurRuntime("Previous message with ID " + previousMessageId + " isn't proper previous message.");
            }
            messageRepository.addMessage(userId, receiverId, content, LocalDate.now().toString(), false, previousMessageId);
            userService.messageNotification(userId, receiverId, false);
            LoggerService.logMethodExecutionEnd("sendMessageToShop", "Message sent successfully!");
            return "Message sent successfully!";
        } catch (OurArg e) {
            LoggerService.logDebug("sendMessageToShop", e);
            return "Error sending message to shop: " + e.getMessage();
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToShop", e);
            return "Error sending message to shop: " + e.getMessage();
        } catch (Exception e) {
            LoggerService.logError("sendMessageToShop", e, token, receiverId, content, previousMessageId);
            throw new OurRuntime("Error sending message to shop: " + e.getMessage(), e);
        }
    }

    public String deleteMessage(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("deleteMessage", token, messageId);
            int senderId = authTokenService.ValidateToken(token);
            userService.validateMemberId(senderId);
            messageRepository.deleteMessage(messageId, senderId);
            LoggerService.logMethodExecutionEnd("deleteMessage", "Message deleted successfully!");
            return "Message deleted successfully!";
        } catch (OurArg e) {
            LoggerService.logDebug("deleteMessage", e);
            return "Error deleting message: " + e.getMessage();
        } catch (OurRuntime e) {
            LoggerService.logDebug("deleteMessage", e);
            return "Error deleting message: " + e.getMessage();
        } catch (Exception e) {
            LoggerService.logError("deleteMessage", e, token, messageId);
            throw new OurRuntime("Error deleting message: " + e.getMessage(), e);
        }
    }

    public String updateMessage(String token, int messageId, String content) {
        try {
            LoggerService.logMethodExecution("updateMessage", token, messageId, content);
            messageRepository.updateMessage(messageId, content, LocalDate.now().toString());
            LoggerService.logMethodExecutionEnd("updateMessage", "Message updated successfully!");
            return "Message updated successfully!";
        } catch (OurArg e) {
            LoggerService.logDebug("updateMessage", e);
            return "Error updating message: " + e.getMessage();
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMessage", e);
            return "Error updating message: " + e.getMessage();
        } catch (Exception e) {
            LoggerService.logError("updateMessage", e, token, messageId, content);
            throw new OurRuntime("Error updating message: " + e.getMessage(), e);
        }
    }

    public String getFullConversation(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("getFullConversation", token, messageId);
            String output = "Full conversation:\n";
            List<Message> messages = messageRepository.getFullConversation(messageId);
            for (Message message : messages) {
                if (message.isDeleted()) {
                    output += "Message deleted\n";
                } else {
                    output += message.toString() + "\n";
                }
            }
            LoggerService.logMethodExecutionEnd("getFullConversation", "Conversation retrieved successfully.");
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getFullConversation", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getFullConversation", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getFullConversation", e, token, messageId);
            throw new OurRuntime("Error getting full conversation: " + e.getMessage(), e);
        }
    }

    public String getMessagesBySenderId(String token, int senderId) {
        try {
            LoggerService.logMethodExecution("getMessagesBySenderId", token, senderId);
            String output = "Messages sent by user " + senderId + ":\n";
            List<Message> messages = messageRepository.getMessagesBySenderId(senderId);
            for (Message message : messages) {
                output += message.toString() + "\n";
            }
            LoggerService.logMethodExecutionEnd("getMessagesBySenderId", "Messages retrieved successfully.");
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getMessagesBySenderId", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessagesBySenderId", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getMessagesBySenderId", e, token, senderId);
            throw new OurRuntime("Error getting messages by sender ID: " + e.getMessage(), e);
        }
    }

    public String getMessagesByReceiverId(String token, int receiverId) {
        try {
            LoggerService.logMethodExecution("getMessagesByReceiverId", token, receiverId);
            String output = "Messages received by user " + receiverId + ":\n";
            List<Message> messages = messageRepository.getMessagesByReceiverId(receiverId);
            for (Message message : messages) {
                output += message.toString() + "\n";
            }
            LoggerService.logMethodExecutionEnd("getMessagesByReceiverId", "Messages retrieved successfully.");
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getMessagesByReceiverId", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessagesByReceiverId", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getMessagesByReceiverId", e, token, receiverId);
            throw new OurRuntime("Error getting messages by receiver ID: " + e.getMessage(), e);
        }
    }

    public String getMessageById(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("getMessageById", token, messageId);
            Message message = messageRepository.getMessageById(messageId);
            if (message == null) {
                LoggerService.logMethodExecutionEnd("getMessageById", "Message not found.");
                return "Message not found!";
            }
            LoggerService.logMethodExecutionEnd("getMessageById", "Message retrieved successfully.");
            return message.toString();
        } catch (OurArg e) {
            LoggerService.logDebug("getMessageById", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessageById", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getMessageById", e, token, messageId);
            throw new OurRuntime("Error getting message by ID: " + e.getMessage(), e);
        }
    }

    public String getPreviousMessage(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("getPreviousMessage", token, messageId);
            Message message = messageRepository.getPreviousMessage(messageId);
            if (message == null) {
                LoggerService.logMethodExecutionEnd("getPreviousMessage", "No previous message found.");
                return "No previous message found!";
            }
            LoggerService.logMethodExecutionEnd("getPreviousMessage", "Previous message retrieved successfully.");
            return message.toString();
        } catch (OurArg e) {
            LoggerService.logDebug("getPreviousMessage", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPreviousMessage", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getPreviousMessage", e, token, messageId);
            throw new OurRuntime("Error getting previous message: " + e.getMessage(), e);
        }
    }
}
