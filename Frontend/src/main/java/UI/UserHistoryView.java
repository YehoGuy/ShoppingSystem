package UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.AddressDTO;
import DTOs.BidRecieptDTO;
import DTOs.ItemDTO;
import DTOs.MemberDTO;
import DTOs.RecieptDTO;
import DTOs.BidRecieptDTO;

@Route(value = "userHistory", layout = AppLayoutBasic.class)

public class UserHistoryView extends VerticalLayout implements BeforeEnterObserver {
    
    private final String apiBase;
    private final String usersUrl;
    private final String itemsUrl;

    private final RestTemplate rest = new RestTemplate();
    private final VerticalLayout receiptsLayout = new VerticalLayout();
    private List<RecieptDTO> reciepts;
    private String token;

    private List<ItemDTO> itemDTOs = new ArrayList<>();

    public UserHistoryView(@Value("${url.api}") String apiBase) {
        this.apiBase = apiBase;
        this.usersUrl = apiBase + "/users";
        this.itemsUrl = apiBase + "/items";
        
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        receiptsLayout.setWidthFull();
        receiptsLayout.setSpacing(true);
        receiptsLayout.setPadding(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
            return;
        }

        handleSuspence();
        buildView();
    }
    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void buildView() {
        removeAll();
        setPadding(true);
        setSpacing(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        H1 header = new H1("🛒 Purchase History for " + getUserName());
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        header.getStyle().set("color", "#1976d2");
        add(header);

        Button refreshButton = new Button("🔄 Refresh", e -> buildView());
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            refreshButton.setVisible(false);
        }
        add(refreshButton);

        // Add the receipts layout to the main view
        add(receiptsLayout);

        loadMember();
        loadItems();
        loadReceipts();
    }

    private void loadReceipts() {
        receiptsLayout.removeAll();
        String url = apiBase + "/purchases/users/" + getUserId() 
        + "?authToken=" + token;
        try {
            ResponseEntity<RecieptDTO[]> response = rest.getForEntity(url, RecieptDTO[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                reciepts = Arrays.asList(response.getBody());
                if (reciepts.isEmpty()) {
                    VerticalLayout emptyState = new VerticalLayout();
                    emptyState.setAlignItems(Alignment.CENTER);
                    emptyState.getStyle().set("padding", "40px");
                    emptyState.getStyle().set("background-color", "#f5f5f5");
                    emptyState.getStyle().set("border-radius", "8px");
                    emptyState.setWidthFull();
                    
                    H3 emptyMessage = new H3("📭 No purchase history found");
                    emptyMessage.getStyle().set("color", "#666");
                    emptyMessage.getStyle().set("text-align", "center");
                    emptyMessage.getStyle().set("margin", "0");
                    
                    Span emptySubtext = new Span("Start shopping to see your purchase history here!");
                    emptySubtext.getStyle().set("color", "#999");
                    emptySubtext.getStyle().set("text-align", "center");
                    emptySubtext.getStyle().set("margin-top", "8px");
                    
                    emptyState.add(emptyMessage, emptySubtext);
                    receiptsLayout.add(emptyState);
                } else {
                    displayReciepts();
                }
            } else {
                Notification.show("Failed to load purchase history.");
            }
        } catch (Exception e) {
            Notification.show("Error loading purchase history: " + e.getMessage());
        }
    }
    private void displayReciepts() {
        receiptsLayout.removeAll();
        
        for (RecieptDTO reciept : reciepts) {
            // Skip non-completed bid receipts (if this is a bid receipt)
            if (isBidReceipt(reciept) && !reciept.isCompleted()) {
                continue; // Skip this receipt
            }
            
            // Create a card-like container for each receipt
            VerticalLayout receiptCard = new VerticalLayout();
            receiptCard.getStyle().set("border", "2px solid #e0e0e0");
            receiptCard.getStyle().set("border-radius", "8px");
            receiptCard.getStyle().set("padding", "16px");
            receiptCard.getStyle().set("margin-bottom", "16px");
            receiptCard.getStyle().set("background-color", "#f9f9f9");
            receiptCard.setWidthFull();
            
            // Header with purchase ID and price
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(JustifyContentMode.BETWEEN);
            
            H3 purchaseId = new H3("🛍️ Purchase #" + reciept.getPurchaseId());
            purchaseId.getStyle().set("margin", "0");
            purchaseId.getStyle().set("color", "#2196f3");

            H3 price = new H3("💰 $" + String.format("%.2f", reciept.getPrice()));
            price.getStyle().set("margin", "0");
            price.getStyle().set("color", "#4caf50");
            
            header.add(purchaseId, price);
            receiptCard.add(header);
            
            // Items section
            if (reciept.getItems() != null && !reciept.getItems().isEmpty()) {
                Span itemsLabel = new Span("📦 Items:");
                itemsLabel.getStyle().set("font-weight", "bold");
                itemsLabel.getStyle().set("margin-top", "8px");
                receiptCard.add(itemsLabel);
                
                VerticalLayout itemsList = new VerticalLayout();
                itemsList.getStyle().set("padding-left", "16px");
                itemsList.setSpacing(false);
                
                for (Map.Entry<Integer, Integer> item : reciept.getItems().entrySet()) {
                    String itemName = matchItemName(item.getKey());
                    Span itemInfo = new Span("• " + itemName + " (Quantity: " + item.getValue() + ")");
                    itemInfo.getStyle().set("color", "#666");
                    itemsList.add(itemInfo);
                }
                receiptCard.add(itemsList);
            }
            
            // Address section
            if (reciept.getAddress() != null) {
                Span addressLabel = new Span("🚚 Shipping Address:");
                addressLabel.getStyle().set("font-weight", "bold");
                addressLabel.getStyle().set("margin-top", "8px");
                
                String addressText = formatAddress(reciept.getAddress());
                Span address = new Span(addressText);
                address.getStyle().set("color", "#666");
                address.getStyle().set("font-style", "italic");
                
                receiptCard.add(addressLabel, address);
            }
            
            receiptsLayout.add(receiptCard);
        }
    }

    private String matchItemName(int itemId) {
        for (ItemDTO item : itemDTOs) {
            if (item.getId() == itemId) {
                return item.getName(); // Assuming ItemDTO has a getName() method
            }
        }
        return "Unknown Item"; // Return a default value if no match is found
    }

    private void loadItems() {
        try {
            String authToken = getToken();
            HttpHeaders headers = getHeaders(authToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = itemsUrl + "/all?token=" + authToken;

            ResponseEntity<ItemDTO[]> response = rest.exchange(
                    url, HttpMethod.GET, request, ItemDTO[].class);

            itemDTOs = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

        } catch (Exception e) {
            Notification.show("Failed to load items");
        }
    }

    private void loadMember() {
        try {
            String authToken = getToken();
            HttpHeaders headers = getHeaders(authToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = usersUrl + "/" + getUserId() + "?token=" + authToken;

            ResponseEntity<MemberDTO> response = rest.exchange(
                    url, HttpMethod.GET, request, MemberDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Member data loaded successfully - could be used for additional features
            } else {
                Notification.show("Failed to load member information");
            }
        } catch (Exception e) {
            Notification.show("Error loading member information: " + e.getMessage());
        }
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    private void handleSuspence() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (authToken == null) {
            return;
        }
        String url = apiBase + "/users/" + userId + "/isSuspended?token=" + authToken;
        ResponseEntity<Boolean> response = rest.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
    
    private String getUserName() {
        String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
        Integer userId = getUserId();
        if (authToken == null || userId == null) {
            return "Unknown User";
        }
        String url = usersUrl + "/" + userId + "?token=" + authToken;
        try {
            ResponseEntity<JsonNode> response = rest.getForEntity(url, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode userNode = response.getBody();
                return userNode.path("username").asText("Unknown User");
            } else {
                return "Unknown User";
            }
        } catch (Exception e) {
            return "Unknown User";
        }
    }

    private String formatAddress(AddressDTO address) {
        if (address == null) {
            return "N/A";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Add street and house number
        if (address.getStreet() != null && !address.getStreet().isEmpty()) {
            sb.append(address.getStreet());
            if (address.getHouseNumber() != null && !address.getHouseNumber().isEmpty()) {
                sb.append(" ").append(address.getHouseNumber());
            }
        }
        
        // Add apartment number if available
        if (address.getApartmentNumber() != null && !address.getApartmentNumber().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Apt ").append(address.getApartmentNumber());
        }
        
        // Add city
        if (address.getCity() != null && !address.getCity().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getCity());
        }
        
        // Add zip code
        if (address.getZipCode() != null && !address.getZipCode().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(address.getZipCode());
        }
        
        // Add country
        if (address.getCountry() != null && !address.getCountry().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getCountry());
        }
        
        return sb.length() > 0 ? sb.toString() : "No address provided";
    }

    /**
     * Check if a receipt is a bid receipt.
     * This is a heuristic method - you may need to adjust the logic based on 
     * how your backend distinguishes bid receipts from regular receipts.
     */
    private boolean isBidReceipt(RecieptDTO receipt) {
        return receipt.getClass().equals(BidRecieptDTO.class);
    }

}
