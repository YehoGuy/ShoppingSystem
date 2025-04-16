public interface IMessageRepository{
    /**
     * Adds a message to the repository.
     * @param message The message to add.
     */
    void addMessage(Message message);

    /**
     * Gets all messages from the repository.
     * @return A list of all messages.
     */
    List<Message> getAllMessages();

    /**
     * Gets a message by its ID.
     * @param id The ID of the message to get.
     * @return The message with the specified ID, or null if not found.
     */
    Message getMessageById(int id);

    /**
     * Deletes a message by its ID.
     * @param id The ID of the message to delete.
     */
    void deleteMessage(int id);

    /**
     * Updates a message in the repository.
     * @param message The message to update.
     */
    void updateMessage(Message message);
}