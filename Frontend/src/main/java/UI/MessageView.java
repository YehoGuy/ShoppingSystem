package UI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
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

import DTOs.MemberDTO;
import DTOs.MessageDTO;
import DTOs.rolesDTO;

@Route(value = "messages", layout = AppLayoutBasic.class)

public class MessageView extends VerticalLayout implements BeforeEnterObserver {

    private final String BASE_URL;
    private final String MSG_BASE_URL;
    private final String NOTIFICATIONS_URL;
    private final String PENDING_ROLES_URL;
    private final String GET_BY_RECIVER;
    private final String ROLES_URL;

    private final RestTemplate rest = new RestTemplate();
    private final VerticalLayout threadContainer = new VerticalLayout();
    private final ComboBox<String> userSelector;
    private int lastMessageId = -1;

    // Mock directory; replace with real user lookup if available
    private final Map<Integer, String> userDirectory;
    private final Map<String, Integer> usernameToId;
    private List<MessageDTO> allmessages;
    private int currentChatUserId = 2;

    private final int thisUserId = Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());

    private final String token;

    public MessageView(@Value("${url.api}") String baseUrl) {
        this.BASE_URL = baseUrl;
        this.MSG_BASE_URL = BASE_URL + "/messages";
        this.NOTIFICATIONS_URL = BASE_URL + "/users/notifications";
        this.PENDING_ROLES_URL = BASE_URL + "/users/getPendingRoles";
        this.ROLES_URL = BASE_URL + "/users/roles/";
        this.GET_BY_RECIVER = this.MSG_BASE_URL + "/receiver?authToken=";

        this.token = getToken();
        ResponseEntity<MemberDTO[]> allmem = rest.getForEntity(
            BASE_URL + "/users/allmembers?token=" + token, MemberDTO[].class);

        ResponseEntity<MessageDTO[]> allmessagesRe = rest.getForEntity(
                GET_BY_RECIVER + token,
                MessageDTO[].class);
        this.allmessages = Arrays.asList(allmessagesRe.getBody());

        userDirectory = allmem.getBody() != null ? Stream.of(allmem.getBody())
                .collect(Collectors.toMap(MemberDTO::getMemberId, MemberDTO::getUsername)) : new HashMap<>();

        usernameToId = userDirectory.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

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
            loadAndDisplayConversation(currentChatUserId);
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
                loadAndDisplayConversation(currentChatUserId);
            }
        });
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            sendButton.setVisible(false);
        }

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

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (token == null) {
            event.forwardTo("login");
        }

        handleSuspence();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void loadAndDisplayConversation(int fromId) {
        threadContainer.removeAll();
        try {
            List<MessageDTO> conversation = allmessages.stream()
                    .filter(msg -> (msg.getSenderId() == fromId || msg.getReceiverId() == fromId) &&
                            (msg.getSenderId() != thisUserId || msg.getReceiverId() == thisUserId))
                    .collect(Collectors.toList());

            conversation.sort(Comparator.comparing(MessageDTO::getTimestamp));

            for (MessageDTO msg : conversation) {
                HorizontalLayout line = new HorizontalLayout();
                line.setWidthFull();
                String whoStr = msg.getSenderId() == fromId ? userDirectory.get(msg.getSenderId()) + ":" : "You:";
                Span who = new Span(whoStr);
                Span text = new Span(msg.getContent());
                Span time = new Span("🕓 " + msg.getTimestamp().toString());
                time.getStyle().set("margin-left", "auto").set("font-size", "smaller");
                line.add(who, text, time);
                threadContainer.add(line);
                lastMessageId = msg.getMessageId();
            }

        } catch (Exception ex) {
            Notification.show("Error loading messages", 3000, Notification.Position.MIDDLE);
        }
    }

    // Helper method to get current user id as int

    private void sendMessageToUser(int receiverId, String content, int previousId) {
        String url = this.MSG_BASE_URL + "/user?authToken=" + token
                + "&receiverId=" + receiverId
                + "&content=" + encode(content)
                + "&previousMessageId=" + previousId;
        try {
            ResponseEntity<String> resp = rest.postForEntity(url, null, String.class);
            if (resp.getStatusCode() == HttpStatus.ACCEPTED) {
                Notification.show("✅ Message sent", 2000, Notification.Position.MIDDLE);
            } else {
                Notification.show("⚠️", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("❗ Error sending message", 3000, Notification.Position.MIDDLE);
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
        try {
            ResponseEntity<String[]> resp = rest.getForEntity(
                    NOTIFICATIONS_URL + "?authToken=" + token, String[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
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
                Notification.show("⚠️ Failed to load notifications");
            }
        } catch (Exception ex) {
            Notification.show("❗ Error loading notifications", 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadPendingRoles(VerticalLayout container) {
        container.removeAll();

        try {
            // 1) Fetch rolesDTO[]
            ResponseEntity<rolesDTO[]> resp = rest.getForEntity(
                    PENDING_ROLES_URL + "?authToken=" + token,
                    rolesDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                rolesDTO[] roles = resp.getBody();

                if (roles.length > 0) {
                    for (rolesDTO dto : roles) {
                        int shopId = dto.getShopId();
                        DTOs.ShopDTO shop = rest.getForObject(
                                BASE_URL + "/shops/" + shopId + "?token=" + token,
                                DTOs.ShopDTO.class);
                        String shopName = shop.getName();
                        String desc = dto.getRoleName() + " @ " + shopName;

                        HorizontalLayout row = new HorizontalLayout();
                        Span span = new Span(desc);

                        // 2) Accept button
                        Button accept = new Button("Accept", e -> {
                            rest.postForEntity(
                                    ROLES_URL + shopId + "/accept?token=" + token,
                                    null,
                                    Void.class);
                            row.remove();
                        });
                        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                            accept.setVisible(false);
                        }
                        accept.getStyle()
                                .set("background-color", "#4CAF50")
                                .set("color", "white");

                        // 3) Reject button
                        Button reject = new Button("Reject", e -> {
                            rest.postForEntity(
                                    ROLES_URL + shopId + "/decline?token=" + token,
                                    null,
                                    Void.class);
                            row.remove();

                        });
                        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                            reject.setVisible(false);
                        }

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
                Notification.show("⚠️ Failed to load pending roles");
            }

        } catch (Exception ex) {
            Notification.show("❗ Error loading pending roles",
                    3000, Notification.Position.MIDDLE);
        }
    }

    private void handleSuspence() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = BASE_URL + "/users/" 
            + userId + "/isSuspended?token=" + token;

        ResponseEntity<Boolean> response = rest.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}