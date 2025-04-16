public class MessageRepository implements IMessageRepository {
    private Map<Integer, Message> messages; // Map to store messages with their IDs as keys
    private int nextId; // Counter for generating unique message IDs

    public MessageRepository() {
        messages = new HashMap<>(); // Initialize the map
        nextId = 1; // Start ID counter at 1
    }

    @Override
    public void addMessage(Message message) {
        messages.put(nextId, message); // Add the message to the map with a unique ID
        nextId++; // Increment the ID counter for the next message
    }

    @Override
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages.values()); // Return all messages as a list
    }

    @Override
    public Message getMessageById(int id) {
        return messages.get(id); // Return the message with the specified ID, or null if not found
    }

    @Override
    public void deleteMessage(int id) {
        messages.remove(id); // Remove the message with the specified ID from the map
    }

    @Override
    public void updateMessage(Message message) {
        messages.put(message.getMessageId(), message); // Update the message in the map
    }
}
