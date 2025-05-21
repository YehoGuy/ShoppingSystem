package UI;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import DTOs.MessageDTO;
import DTOs.rolesDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "messages", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class MessageView extends VerticalLayout implements BeforeEnterObserver {
    private static final String BASE_URL = "http://localhost:8080/api/messages";
    private static final String NOTIFICATIONS_URL = "http://localhost:8080/api/users/notifications";
    private static final String PENDING_ROLES_URL = "http://localhost:8080/api/users/getPendingRoles";

    private final RestTemplate rest = new RestTemplate();
    private final VerticalLayout threadContainer = new VerticalLayout();
    private final ComboBox<String> userSelector;
    private int lastMessageId = -1;

    // Mock directory; replace with real user lookup if available
    private final int currentUserId = 1;
    private final Map<Integer, String> userDirectory = Map.of(2, "alice", 3, "bob", 4, "charlie");
    private final Map<String, Integer> usernameToId = userDirectory.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private int currentChatUserId = 2;

    public MessageView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout page = new HorizontalLayout();
        page.setWidthFull();

        // Left panel: Messaging thread
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("30%");
        leftPanel.add(new H1("📨 Messaging"));

        userSelector = new ComboBox<>("Select User to Chat");
        userSelector.setItems(userDirectory.values());
        userSelector.setValue(userDirectory.get(currentChatUserId));
        userSelector.addValueChangeListener(e -> {
            currentChatUserId = usernameToId.get(e.getValue());
            loadAndDisplayConversation();
        });

        threadContainer.setWidth("100%");
        threadContainer.getStyle()
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "10px")
                .set("background-color", "#f9f9f9");

        TextArea messageArea = new TextArea("Your Message");
        messageArea.setWidth("100%");
        messageArea.setPlaceholder("Type your message...");

        Button sendButton = new Button("Send", e -> {
            String content = messageArea.getValue().trim();
            if (!content.isEmpty()) {
                sendMessageToUser(currentChatUserId, content, lastMessageId);
                messageArea.clear();
                loadAndDisplayConversation();
            }
        });

        leftPanel.add(userSelector, threadContainer, messageArea, sendButton);
        page.add(leftPanel);

        // Middle panel: Notifications
        VerticalLayout middlePanel = new VerticalLayout();
        middlePanel.setWidth("30%");
        middlePanel.add(new H1("🔔 Notifications"));
        VerticalLayout notificationsContainer = new VerticalLayout();
        loadNotifications(notificationsContainer);
        middlePanel.add(notificationsContainer);
        page.add(middlePanel);

        // Right panel: Pending roles
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("30%");
        rightPanel.add(new H1("👤 Pending Roles"));
        VerticalLayout pendingRolesContainer = new VerticalLayout();
        loadPendingRoles(pendingRolesContainer);
        rightPanel.add(pendingRolesContainer);
        page.add(rightPanel);

        page.setWidthFull();
        page.setHeightFull();
        page.getStyle()
                .set("display", "flex")
                .set("align-items", "stretch")
                .set("justify-content", "space-between");

        add(page);
        loadAndDisplayConversation();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m => m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return (String) VaadinSession.getCurrent().getAttribute("userId");
    }

    private void loadAndDisplayConversation() {
        threadContainer.removeAll();
        String token = getToken();
        try {
            ResponseEntity<MessageDTO[]> sent = rest.getForEntity(
                    BASE_URL + "/sender/" + currentUserId + "?authToken=" + token,
                    MessageDTO[].class);
            ResponseEntity<MessageDTO[]> received = rest.getForEntity(
                    BASE_URL + "/receiver/" + currentUserId + "?authToken=" + token,
                    MessageDTO[].class);

            List<MessageDTO> convo = Stream.concat(Arrays.stream(sent.getBody()), Arrays.stream(received.getBody()))
                    .filter(m -> (m.getSenderId() == currentUserId && m.getReceiverId() == currentChatUserId)
                            || (m.getSenderId() == currentChatUserId && m.getReceiverId() == currentUserId))
                    .sorted(Comparator.comparing(MessageDTO::getMessageId))
                    .collect(Collectors.toList());

            lastMessageId = -1;
            convo.forEach(msg -> {
                HorizontalLayout line = new HorizontalLayout();
                line.setWidthFull();
                Span who = new Span(msg.getSenderId() == currentUserId ? "You:" : "User #" + msg.getSenderId() + ":");
                Span text = new Span(msg.isDeleted() ? "(deleted)" : msg.getContent());
                Span time = new Span("🕓 " + msg.getTimestamp());
                time.getStyle().set("margin-left", "auto").set("font-size", "smaller");
                line.add(who, text, time);
                threadContainer.add(line);
                lastMessageId = msg.getMessageId();
            });
        } catch (Exception ex) {
            Notification.show("Error loading messages: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void sendMessageToUser(int receiverId, String content, int previousId) {
        String token = getToken();
        String url = BASE_URL + "/user?authToken=" + token
                + "&receiverId=" + receiverId
                + "&content=" + encode(content)
                + "&previousMessageId=" + previousId;
        try {
            ResponseEntity<String> resp = rest.postForEntity(url, null, String.class);
            if (resp.getStatusCode() == HttpStatus.ACCEPTED) {
                Notification.show("✅ Message sent", 2000, Notification.Position.MIDDLE);
            } else {
                Notification.show("⚠️ " + resp.getBody(), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("❗ Error sending message: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private String getToken() {
        return VaadinSession.getCurrent().getAttribute("authToken").toString();
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private void loadNotifications(VerticalLayout container) {

        container.removeAll();
        String token = getToken();
        try {
            ResponseEntity<String[]> resp = rest.getForEntity(
                    NOTIFICATIONS_URL + "?authToken=" + token, String[].class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                String[] notes = resp.getBody();
                if (notes.length > 0) {
                    for (String note : notes) {
                        Span span = new Span("• " + note);
                        span.getStyle().set("margin-bottom", "5px").set("font-size", "bigger");
                        container.add(span);
                    }
                } else {
                    container.add(new Span("No new notifications."));
                }
            } else {
                Notification.show("⚠️ Failed to load notifications: " + resp.getStatusCode());
            }
        } catch (Exception ex) {
            Notification.show("❗ Error loading notifications: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadPendingRoles(VerticalLayout container) {
        container.removeAll();
        String token = getToken();

        try {
            // 1) Fetch rolesDTO[]
            ResponseEntity<rolesDTO[]> resp = rest.getForEntity(
                    PENDING_ROLES_URL + "?authToken=" + token,
                    rolesDTO[].class);

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                rolesDTO[] roles = resp.getBody();

                if (roles.length > 0) {
                    for (rolesDTO dto : roles) {
                        int shopId = dto.getShopId();
                        String desc = dto.getRoleName() + " @ " + dto.getShopName();

                        HorizontalLayout row = new HorizontalLayout();
                        Span span = new Span(desc);

                        // 2) Accept button
                        Button accept = new Button("Accept", e -> {
                            rest.postForEntity(
                                    "http://localhost:8080/api/users/roles/" + shopId + "/accept"
                                            + "?authToken=" + token,
                                    null, Void.class);
                            row.remove();
                        });
                        accept.getStyle()
                                .set("background-color", "#4CAF50")
                                .set("color", "white");

                        // 3) Reject button
                        Button reject = new Button("Reject", e -> {
                            rest.postForEntity(
                                    "http://localhost:8080/api/users/roles/" + shopId + "/decline"
                                            + "?authToken=" + token,
                                    null, Void.class);
                            row.remove();
                        });
                        reject.getStyle()
                                .set("background-color", "#f44336")
                                .set("color", "white");

                        row.add(span, accept, reject);
                        row.getStyle()
                                .set("margin-bottom", "5px")
                                .set("align-items", "center");

                        container.add(row);
                    }
                } else {
                    container.add(new Span("No pending roles."));
                }

            } else {
                Notification.show("⚠️ Failed to load pending roles: " + resp.getStatusCode());
            }

        } catch (Exception ex) {
            Notification.show("❗ Error loading pending roles: " + ex.getMessage(),
                    3000, Notification.Position.MIDDLE);
        }
    }
}