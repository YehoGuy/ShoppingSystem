package UI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import DTOs.MessageDTO;

@Route(value = "messages", layout = AppLayoutBasic.class)
public class MessageView extends VerticalLayout implements BeforeEnterObserver {
    private final List<MessageDTO> messageStore = new ArrayList<>();
    private final VerticalLayout threadContainer = new VerticalLayout();

    private final int currentUserId = 1; // mock current user
    private final Map<Integer, String> userDirectory = Map.of(
            2, "alice", 3, "bob", 4, "charlie");
    private final Map<String, Integer> usernameToId = userDirectory.entrySet()
            .stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private int currentChatUserId = 2; // default conversation

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public MessageView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout page = new HorizontalLayout();
        page.setWidthFull();

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("30%");

        H1 title = new H1("📨 Messaging");
        leftPanel.add(title);

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
                        getNow(), true, getLastMessageId(currentChatUserId), false);
                messageStore.add(newMsg);
                messageArea.clear();
                displayThreadForUser(currentChatUserId);
            }
        });

        leftPanel.add(userSelector, threadContainer, messageArea, sendButton);

        page.add(leftPanel);

        VerticalLayout middlePanel = new VerticalLayout();
        middlePanel.setWidth("30%");

        H1 notificationsTitle = new H1("🔔 Notifications");
        middlePanel.add(notificationsTitle);

        // Placeholder for notifications
        VerticalLayout notificationsContainer = new VerticalLayout();
        notificationsContainer.setWidth("100%");

        loadNotifications(notificationsContainer);
        middlePanel.add(notificationsContainer);
        page.add(middlePanel);

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("30%");

        H1 pendingRolesTitle = new H1("👤 Pending Roles");
        rightPanel.add(pendingRolesTitle);

        // Placeholder for pending roles
        VerticalLayout pendingRolesContainer = new VerticalLayout();
        pendingRolesContainer.setWidth("100%");
        loadPendingRoles(pendingRolesContainer);
        rightPanel.add(pendingRolesContainer);
        page.add(rightPanel);

        page.setWidthFull();
        page.setHeightFull();
        page.getStyle().set("display", "flex")
                .set("align-items", "stretch")
                .set("justify-content", "space-between");
        add(page);
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

    private void loadNotifications(VerticalLayout notificationsContainer) {
        // Placeholder for loading notifications
        List<String> notifications = new ArrayList<>();
        notifications.add("an admin deleted shop #1");
        notifications.add("an admin deleted shop #2");
        notifications.add("a problem with your order #3");
        notifications.add("a problem with your order #4");

        if (!notifications.isEmpty()) {
            notifications.forEach(notification -> {
                Span notificationSpan = new Span("• " + notification);
                notificationSpan.getStyle().set("margin-bottom", "5px")
                        .set("font-size", "bigger");
                notificationsContainer.add(notificationSpan);
            });
        } else
            notificationsContainer.add(new Span("No new notifications."));
    }

    private void loadPendingRoles(VerticalLayout pendingRolesContainer) {
        // Placeholder for loading pending roles
        List<String> pendingRoles = new ArrayList<>();
        pendingRoles.add("User #2: Pending Role 1");
        pendingRoles.add("User #3: Pending Role 2");
        pendingRoles.add("User #4: Pending Role 3");

        if (!pendingRoles.isEmpty()) {
            pendingRoles.forEach(role -> {
                HorizontalLayout roleLayout = new HorizontalLayout();
                Span roleSpan = new Span(role);

                Button acceptButton = new Button("Accept", e -> {
                    pendingRolesContainer.remove(roleLayout);
                });
                acceptButton.getStyle().set("background-color", "#4CAF50").set("color", "white");

                Button rejectButton = new Button("Reject", e -> {
                    pendingRolesContainer.remove(roleLayout);
                });
                rejectButton.getStyle().set("background-color", "#f44336").set("color", "white");

                roleLayout.add(roleSpan, acceptButton, rejectButton);
                roleLayout.getStyle().set("margin-bottom", "5px")
                        .set("align-items", "center");
                pendingRolesContainer.add(roleLayout);
            });
        } else
            pendingRolesContainer.add(new Span("No pending roles."));
    }
}
