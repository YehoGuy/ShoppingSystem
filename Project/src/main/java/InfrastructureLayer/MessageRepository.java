package InfrastructureLayer;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import DomainLayer.IMessageRepository;
import DomainLayer.Message;

public class MessageRepository implements IMessageRepository {
    private Map<Integer, Message> messages; // Map to store messages with their IDs as keys
    private AtomicInteger nextId; // Counter for generating unique message IDs

    public MessageRepository() {
        messages = new ConcurrentHashMap<>(); // Initialize the map
        nextId = new AtomicInteger(1); // Start ID counter at 1
    }

    @Override
    public void addMessage(int senderId, int receiverId, String content, String timestamp, boolean userToUser) {
        int id = nextId.getAndIncrement(); // Get the next unique ID
        Message message = new Message(id, senderId, receiverId, content, timestamp, userToUser); // Create a new message object
        messages.put(id, message); // Add the message to the map with a unique ID
    }

    @Override
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages.values()); // Return all messages as a list
    }

    @Override
    public Message getMessageById(int id) {
        try{
            return messages.get(id); // Return the message with the specified ID, or null if not found
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Message with ID " + id + " not found."); // Handle case where message ID is not found
        }
    }

    @Override
    public void deleteMessage(int id) {
        messages.remove(id); // Remove the message with the specified ID from the map
    }

    @Override
    public void updateMessage(int id, int senderId, int receiverId, String content, String timestamp, boolean userToUser) {
        try{
            Message message = messages.get(id); // Get the message with the specified ID
            messages.put(message.getMessageId(), message); // Update the message in the map
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Message with ID " + id + " not found."); // Handle case where message ID is not found
        }
    }

    @Override
    public List<Message> getMessagesBySenderId(int senderId) {
        List<Message> result = new ArrayList<>(); // List to store messages sent by the specified user
        for (Message message : messages.values()) {
            if (message.getSenderId() == senderId) {
                result.add(message); // Add the message to the result list if it matches the sender ID
            }
        }
        return result; // Return the list of messages sent by the specified user
    }

    @Override
    public List<Message> getMessagesByReceiverId(int receiverId) {
        List<Message> result = new ArrayList<>(); // List to store messages received by the specified user
        for (Message message : messages.values()) {
            if (message.getReceiverId() == receiverId) {
                result.add(message); // Add the message to the result list if it matches the receiver ID
            }
        }
        return result; // Return the list of messages received by the specified user
    }
}
