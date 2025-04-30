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

    public Boolean sendMessageToUser(String token, int receiverId, String content, int previousMessageId) {
        try {
            LoggerService.logMethodExecution("sendMessageToUser", token, receiverId, content, previousMessageId);
            int senderId = authTokenService.ValidateToken(token);
            userService.validateMemberId(senderId);
            userService.validateMemberId(receiverId);
            if (!messageRepository.isMessagePrevious(previousMessageId, senderId, receiverId)) {
                throw new OurArg("Previous message with ID " + previousMessageId + " isn't proper previous message.");
            }
            messageRepository.addMessage(senderId, receiverId, content, LocalDate.now().toString(), true, previousMessageId);
            LoggerService.logMethodExecutionEnd("sendMessageToUser", true);
            
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("sendMessageToUser", e);
            throw new OurArg("sendMessageToUser" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToUser", e);
            throw new OurRuntime("sendMessageToUser" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("sendMessageToUser", e, token, receiverId, content, previousMessageId);
            throw new OurRuntime("Error sending message to user: " + e.getMessage(), e);
        }
    }

    public Boolean sendMessageToShop(String token, int receiverId, String content, int previousMessageId) {
        try {
            LoggerService.logMethodExecution("sendMessageToShop", token, receiverId, content, previousMessageId);
            int userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            Shop s = shopService.getShop(receiverId, token);
            if (s == null) {
                throw new OurRuntime("Shop with ID " + receiverId + " doesn't exist.");
            }
            if (!messageRepository.isMessagePrevious(previousMessageId, userId, receiverId)) {
                throw new OurArg("Previous message with ID " + previousMessageId + " isn't proper previous message.");
            }
            messageRepository.addMessage(userId, receiverId, content, LocalDate.now().toString(), false, previousMessageId);
            LoggerService.logMethodExecutionEnd("sendMessageToShop", true);
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("sendMessageToShop", e);
            throw new OurArg("sendMessageToShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("sendMessageToShop", e);
            throw new OurRuntime("sendMessageToShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("sendMessageToShop", e, token, receiverId, content, previousMessageId);
            throw new OurRuntime("Error sending message to shop: " + e.getMessage(), e);
        }
    }

    public Boolean deleteMessage(String token, int messageId) {
        try {
            LoggerService.logMethodExecution("deleteMessage", token, messageId);
            int senderId = authTokenService.ValidateToken(token);
            userService.validateMemberId(senderId);
            messageRepository.deleteMessage(messageId, senderId);
            LoggerService.logMethodExecutionEnd("deleteMessage", true);
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("deleteMessage", e);
            throw new OurArg("deleteMessage" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("deleteMessage", e);
            throw new OurRuntime("deleteMessage" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("deleteMessage", e, token, messageId);
            throw new OurRuntime("Error deleting message: " + e.getMessage(), e);
        }
    }

    public Boolean updateMessage(String token, int messageId, String content) {
        try {
            LoggerService.logMethodExecution("updateMessage", token, messageId, content);
            messageRepository.updateMessage(messageId, content, LocalDate.now().toString());
            LoggerService.logMethodExecutionEnd("updateMessage", true);
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("updateMessage", e);
            throw new OurArg("updateMessage" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMessage", e);
            throw new OurRuntime("updateMessage" + e.getMessage());
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
                    continue;
                }
                output += message.toString() + "\n";
            }
            LoggerService.logMethodExecutionEnd("getFullConversation", output);
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getFullConversation", e);
            throw new OurArg("getFullConversation" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getFullConversation", e);
            throw new OurRuntime("getFullConversation" + e.getMessage());
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
            LoggerService.logMethodExecutionEnd("getMessagesBySenderId", output);
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getMessagesBySenderId", e);
            throw new OurArg("getMessagesBySenderId" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessagesBySenderId", e);
            throw new OurRuntime("getMessagesBySenderId" + e.getMessage());
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
            LoggerService.logMethodExecutionEnd("getMessagesByReceiverId", output);
            return output;
        } catch (OurArg e) {
            LoggerService.logDebug("getMessagesByReceiverId", e);
            throw new OurArg("getMessagesByReceiverId" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessagesByReceiverId", e);
            throw new OurRuntime("getMessagesByReceiverId" + e.getMessage());
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
                throw new OurArg("Message with ID " + messageId + " not found.");
            }
            LoggerService.logMethodExecutionEnd("getMessageById", message.toString());
            return message.toString();
        } catch (OurArg e) {
            LoggerService.logDebug("getMessageById", e);
            throw new OurArg("getMessageById" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getMessageById", e);
            throw new OurRuntime("getMessageById" + e.getMessage());
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
                throw new OurArg("No previous message found!");
            }
            LoggerService.logMethodExecutionEnd("getPreviousMessage", message.toString());
            return message.toString();
        } catch (OurArg e) {
            LoggerService.logDebug("getPreviousMessage", e);
            throw new OurArg("getPreviousMessage" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPreviousMessage", e);
            throw new OurRuntime("getPreviousMessage" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getPreviousMessage", e, token, messageId);
            throw new OurRuntime("Error getting previous message: " + e.getMessage(), e);
        }
    }
}
