package com.example.app.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.IMessageRepository;
import com.example.app.DomainLayer.Message;

@Repository
public class MessageRepository implements IMessageRepository {
    private Map<Integer, Message> messages; // Map to store messages with their IDs as keys
    private AtomicInteger nextId; // Counter for generating unique message IDs

    public MessageRepository() {
        messages = new ConcurrentHashMap<>(); // Initialize the map
        nextId = new AtomicInteger(1); // Start ID counter at 1
    }

    @Override
    public void addMessage(int senderId, int receiverId, String content, String timestamp, boolean userToUser,
            int previousMessageId) {
        if (content == null || content.isEmpty()) {
            throw new OurRuntime("unable to send - message is empty."); // Validate message content
        }
        int id = nextId.getAndIncrement(); // Get the next unique ID
        Message message = new Message(id, senderId, receiverId, content, timestamp, userToUser, previousMessageId);
        messages.put(id, message); // Add the message to the map with a unique ID
    }

    @Override
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages.values()); // Return all messages as a list
    }

    @Override
    public Message getMessageById(int id) {
        try {
            return messages.get(id); // Return the message with the specified ID, or null if not found
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Message with ID " + id + " not found."); // Handle case where message
                                                                                          // ID is not found
        }
    }

    @Override
    public void deleteMessage(int id, int senderId) {
        Message message = messages.get(id); // Get the message with the specified ID
        if (message == null) {
            throw new OurRuntime("Message with ID " + id + " not found."); // Handle case where message ID is not found
        } else if (message.getSenderId() != senderId) {
            throw new OurRuntime("You are not authorized to delete this message."); // Handle case where user is not
                                                                                    // authorized to delete the message
        } else {
            message.delete(); // Mark the message as deleted
        }
    }

    @Override
    public void updateMessage(int id, String content, String timestamp) {
        try {
            Message message = messages.get(id); // Get the message with the specified ID
            if (message == null) {
                throw new IndexOutOfBoundsException("Message with ID " + id + " not found."); // Handle case where
                                                                                              // message ID is not found
            }
            message = new Message(id, message.getSenderId(), message.getReceiverId(), content, timestamp,
                    message.isUserToUser(), message.getPreviousMessageId()); // Create a new message object with updated
                                                                             // content
            messages.put(message.getMessageId(), message); // Update the message in the map
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Message with ID " + id + " not found."); // Handle case where message
                                                                                          // ID is not found
        }
    }

    @Override
    public List<Message> getMessagesBySenderId(int senderId) {
        List<Message> result = new ArrayList<>(); // List to store messages sent by the specified user
        for (Message message : messages.values()) {
            if (message.getSenderId() == senderId && !message.isDeleted()) {
                result.add(message); // Add the message to the result list if it matches the sender ID
            }
        }
        return result; // Return the list of messages sent by the specified user
    }

    @Override
    public List<Message> getMessagesByReceiverId(int receiverId) {
        List<Message> result = new ArrayList<>(); // List to store messages received by the specified user
        for (Message message : messages.values()) {
            if (message.getReceiverId() == receiverId && !message.isDeleted()) {
                result.add(message); // Add the message to the result list if it matches the receiver ID
            }
        }
        return result; // Return the list of messages received by the specified user
    }

    @Override
    public Message getPreviousMessage(int messageId) {
        Message message = messages.get(messageId); // Get the message with the specified ID
        if (message != null) {
            if (message.getPreviousMessageId() == -1) {
                return null; // Return null if there is no previous message
            }
            message = messages.get(message.getPreviousMessageId()); // Get the previous message
            if (message != null && !message.isDeleted()) {
                return message; // Return the previous message if it exists and is not deleted
            }
            return null; // Return null if the previous message is deleted or not found
        }
        return null; // Return null if the message or previous message is not found
    }

    private Message getPreviousMessageNotMatterWhat(int messageId) {
        Message message = messages.get(messageId); // Get the message with the specified ID
        if (message != null) {
            if (message.getPreviousMessageId() == -1) {
                return null; // Return null if there is no previous message
            }
            message = messages.get(message.getPreviousMessageId()); // Get the previous message
            return message; // Return the previous message if it exists
        }
        return null; // Return null if the message or previous message is not found
    }

    @Override
    public List<Message> getFullConversation(int messageId) {
        List<Message> conversation = new ArrayList<>();
        Message current = messages.get(messageId);
        while (current != null) {
            // only include non-deleted messages
            if (!current.isDeleted()) {
                conversation.add(current);
            }
            int prevId = current.getPreviousMessageId();
            if (prevId == -1) {
                break;   // no more history
            }
            current = messages.get(prevId);
        }
        return conversation;
    }


    public boolean isMessagePrevious(int previousMessageId, int senderId, int receiverId) {
        if (previousMessageId == -1)
            return true;
        Message message = messages.get(previousMessageId); // Get the message with the specified ID
        if (message != null) {
            if ((message.getSenderId() == senderId && message.getReceiverId() == receiverId)
                    || (message.getSenderId() == receiverId && message.getReceiverId() == senderId)) {
                return true; // Return true if the message matches the sender and receiver IDs
            }
        }
        return false; // Return false if the message is not found or does not match the IDs
    }

}
