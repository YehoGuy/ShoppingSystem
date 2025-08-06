package com.example.app.DBLayer.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.IMessageRepository;
import com.example.app.DomainLayer.Message;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * JPA-backed implementation of IMessageRepository.
 * Manages message IDs via an internal counter initialized from the database.
 */
@Repository
@Profile("!no-db & !test")
public class MessageRepositoryDBImpl implements IMessageRepository {

    private final MessageRepositoryDB jpaRepo;
    // private final AtomicInteger idCounter = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager entityManager;

    public MessageRepositoryDBImpl(@Lazy @Autowired MessageRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void addMessage(int senderId,
            int receiverId,
            String content,
            String timestamp,
            boolean userToUser,
            int previousMessageId) {

        Message message = new Message(senderId, receiverId, content, timestamp, userToUser,
                previousMessageId);
        jpaRepo.save(message);
    }

    @Override
    public List<Message> getAllMessages() {
        return jpaRepo.findAll();
    }

    @Override
    public Message getMessageById(int id) {
        return jpaRepo.findById(id).orElse(null);
    }

    @Override
    public List<Message> getMessagesBySenderId(int senderId) {
        return jpaRepo.findBySenderId(senderId);
    }

    @Override
    public List<Message> getMessagesByReceiverId(int receiverId) {
        return jpaRepo.findByReceiverId(receiverId);
    }

    @Override
    public Message getPreviousMessage(int messageId) {
        Message current = getMessageById(messageId);
        if (current == null) {
            return null;
        }
        int prevId = current.getPreviousMessageId();
        return prevId >= 0 ? getMessageById(prevId) : null;
    }

    @Override
    public List<Message> getFullConversation(int messageId) {
        List<Message> conversation = new ArrayList<>();
        Message current = getMessageById(messageId);
        while (current != null) {
            conversation.add(0, current);
            int prevId = current.getPreviousMessageId();
            if (prevId < 0) {
                break;
            }
            current = getMessageById(prevId);
        }
        return conversation;
    }

    @Override
    public boolean isMessagePrevious(int previousMessageId,
            int senderId,
            int receiverId) {
        if (previousMessageId < 0) {
            return true;
        }
        Message previous = getMessageById(previousMessageId);
        if (previous == null) {
            return false;
        }
        return (previous.getSenderId() == senderId && previous.getReceiverId() == receiverId)
                || (previous.getSenderId() == receiverId && previous.getReceiverId() == senderId);
    }

    @Override
    public void updateMessage(int messageId, String newContent, String newTimestamp) {
        // Update the row in-place, no need to recreate the entity
        int rows = entityManager.createQuery(
                "UPDATE Message m SET m.content = :content, m.timestamp = :ts WHERE m.messageId = :id")
                .setParameter("content", newContent)
                .setParameter("ts", newTimestamp)
                .setParameter("id", messageId)
                .executeUpdate();

        if (rows > 0) {
            entityManager.clear(); // purge first-level cache so a fresh read sees the new values
        }
    }

    @Override
    public void deleteMessage(int messageId, int userId) {
        Message message = getMessageById(messageId);
        if (message != null &&
                (message.getSenderId() == userId || message.getReceiverId() == userId)) {

            jpaRepo.deleteById(messageId); // use primary-key delete to avoid detached entity issues
        }
    }

    @Override
    public List<Message> getUserConversations(int userId) {
        List<Message> output = getMessagesBySenderId(userId);
        output.addAll(getMessagesByReceiverId(userId));
        return output;
    }

}
