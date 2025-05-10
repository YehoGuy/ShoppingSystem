package UI;

import DTOs.MessageDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "messages", layout = AppLayoutBasic.class)
public class MessageView extends VerticalLayout {
    private final List<MessageDTO> messageStore = new ArrayList<>();
    private final VerticalLayout threadContainer = new VerticalLayout();

    private final int currentUserId = 1; // mock current user
    private final Map<Integer, String> userDirectory = Map.of(
        2, "alice", 3, "bob", 4, "charlie"
    );
    private final Map<String, Integer> usernameToId = userDirectory.entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private int currentChatUserId = 2; // default conversation

    public MessageView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("📨 Messaging");
        add(title);

        setupMockMessages();

        ComboBox<String> userSelector = new ComboBox<>("Select User to Chat");
        userSelector.setItems(userDirectory.values());
        userSelector.setValue(userDirectory.get(currentChatUserId));
        userSelector.addValueChangeListener(event -> {
            currentChatUserId = usernameToId.getOrDefault(event.getValue(), 2);
            displayThreadForUser(currentChatUserId);
        });

        threadContainer.setWidth("70%");
        threadContainer.getStyle().set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "10px")
                .set("background-color", "#f9f9f9");

        displayThreadForUser(currentChatUserId);

        TextArea messageArea = new TextArea("Your Message");
        messageArea.setWidth("100%");
        messageArea.setPlaceholder("Type your message...");

        Button sendButton = new Button("Send", e -> {
            String content = messageArea.getValue().trim();
            if (!content.isEmpty()) {
                MessageDTO newMsg = new MessageDTO(
                        generateId(), currentUserId, currentChatUserId, content,
                        getNow(), true, getLastMessageId(currentChatUserId), false
                );
                messageStore.add(newMsg);
                messageArea.clear();
                displayThreadForUser(currentChatUserId);
            }
        });

        add(userSelector, threadContainer, messageArea, sendButton);
    }


    private void setupMockMessages() {
        messageStore.add(new MessageDTO(1, 2, 1, "Hey! Can I ask a question?", "2024-05-01 13:12", true, -1, false));
        messageStore.add(new MessageDTO(2, 1, 2, "Sure, go ahead!", "2024-05-01 13:14", true, 1, false));
        messageStore.add(new MessageDTO(3, 2, 1, "What are the store hours?", "2024-05-01 13:15", true, 2, false));
    }

    private void displayThreadForUser(int otherUserId) {
        threadContainer.removeAll();

        List<MessageDTO> thread = messageStore.stream()
                .filter(m -> (m.getSenderId() == currentUserId && m.getReceiverId() == otherUserId)
                          || (m.getSenderId() == otherUserId && m.getReceiverId() == currentUserId))
                .sorted(Comparator.comparing(MessageDTO::getMessageId))
                .collect(Collectors.toList());

        for (MessageDTO msg : thread) {
            HorizontalLayout messageLine = new HorizontalLayout();
            messageLine.setWidthFull();

            Span who = new Span(msg.getSenderId() == currentUserId ? "You: " : "User #" + msg.getSenderId() + ":");
            who.getStyle().set("font-weight", "bold");

            Span text = new Span(msg.isDeleted() ? "(deleted)" : msg.getContent());
            Span time = new Span("🕓 " + msg.getTimestamp());
            time.getStyle().set("margin-left", "auto").set("font-size", "smaller");

            messageLine.add(who, text, time);
            threadContainer.add(messageLine);
        }
    }

    private int generateId() {
        return messageStore.stream().mapToInt(MessageDTO::getMessageId).max().orElse(0) + 1;
    }

    private String getNow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private int getLastMessageId(int otherUserId) {
        return messageStore.stream()
                .filter(m -> (m.getSenderId() == currentUserId && m.getReceiverId() == otherUserId)
                          || (m.getSenderId() == otherUserId && m.getReceiverId() == currentUserId))
                .mapToInt(MessageDTO::getMessageId).max().orElse(-1);
    }
}
