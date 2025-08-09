package com.example.app.DomainLayer;

import java.util.List;

public interface IMessageRepository {
    /**
     * Adds a message to the repository.
     * 
     * @param message The message to add.
     */
    void addMessage(int senderId, int receiverId, String content, String timestamp, boolean userToUser,
            int previousMessageId);

    /**
     * Gets all messages from the repository.
     * 
     * @return A list of all messages.
     */
    List<Message> getAllMessages();

    /**
     * Gets a message by its ID.
     * 
     * @param id The ID of the message to get.
     * @return The message with the specified ID, or null if not found.
     */
    Message getMessageById(int id);

    /**
     * Deletes a message by its ID.
     * 
     * @param id       The ID of the message to delete.
     * @param senderId The ID of the sender.
     */
    void deleteMessage(int id, int senderId);

    /**
     * Updates a message in the repository.
     * 
     * @param message The message to update.
     */
    void updateMessage(int id, String content, String timestamp);

    /**
     * Gets all messages sent by a specific user.
     * 
     * @param senderId The ID of the sender.
     * @return A list of messages sent by the specified user.
     */
    List<Message> getMessagesBySenderId(int senderId);

    /**
     * Gets all messages received by a specific user.
     * 
     * @param receiverId The ID of the receiver.
     * @return A list of messages received by the specified user.
     */
    List<Message> getMessagesByReceiverId(int receiverId);

    /**
     * get the message that this message was sent as response to
     * 
     * @param messageId
     * @return
     */
    Message getPreviousMessage(int messageId);

    /**
     * get all the messages in the conversation that this message is part of, up to
     * that message
     * 
     * @param messageId
     * @return
     */
    List<Message> getFullConversation(int messageId);

    /**
     * check if the message with the given id is a response to the message with the
     * given id
     * 
     * @param previousMessageId the id of the message that this message is a
     *                          response to
     * @param senderId          the id of the sender of the next message
     * @param receiverId        the id of the receiver of the next message
     * @return true if the new message is a response to the message with the given
     *         id, false otherwise
     */
    boolean isMessagePrevious(int previousMessageId, int senderId, int receiverId);

    List<Message> getUserConversations(int userId);
}