package UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.ItemDTO;
import DTOs.MemberDTO;
import DTOs.RecieptDTO;
import DTOs.BidRecieptDTO;
import org.springframework.http.*;
import java.util.Arrays;

@Route(value = "history", layout = AppLayoutBasic.class)

public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final String apiBase;
    private final String purchaseHistoryUrl;
    private final String usersUrl;
    private final String itemsUrl;

    private final RestTemplate rest = new RestTemplate();
    private final VerticalLayout receiptsLayout = new VerticalLayout();
    private List<RecieptDTO> reciepts;
    private String token;
    private int shopId;

    private List<MemberDTO> memberDTOs = new ArrayList<>();
    private List<ItemDTO> itemDTOs = new ArrayList<>();

    public ShopHistoryView(@Value("${url.api}") String apiBase) {
        this.apiBase = apiBase;
        this.purchaseHistoryUrl = apiBase + "/purchases/shops";
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
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.shopId = parameter;
        buildView();

    }

    private void buildView() {
        removeAll();
        setPadding(true);
        setSpacing(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        H1 header = new H1("🏪 Purchase History for " + getShopName(shopId));
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

        loadUsers();
        loadItems();
        loadReceipts();
    }

    private void loadReceipts() {
        receiptsLayout.removeAll();
        String url = purchaseHistoryUrl + "/" + shopId
                + "?authToken=" + VaadinSession.getCurrent().getAttribute("authToken");
        try {
            ResponseEntity<RecieptDTO[]> resp = rest.getForEntity(url, RecieptDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                RecieptDTO[] recieptsBodyDtos = resp.getBody();
                reciepts = List.of(recieptsBodyDtos);
                
                if (reciepts == null || reciepts.size() == 0) {
                    VerticalLayout emptyState = new VerticalLayout();
                    emptyState.setAlignItems(Alignment.CENTER);
                    emptyState.getStyle().set("padding", "40px");
                    emptyState.getStyle().set("background-color", "#f5f5f5");
                    emptyState.getStyle().set("border-radius", "8px");
                    
                    H3 emptyMessage = new H3("📭 No purchase history found for this shop");
                    emptyMessage.getStyle().set("color", "#666");
                    emptyMessage.getStyle().set("text-align", "center");
                    
                    emptyState.add(emptyMessage);
                    receiptsLayout.add(emptyState);
                } else {
                    displayReciepts();
                }
                // receiptsLayout.removeAll();
            }
            else {
                Notification.show("Failed to load purchase history");
            }

        } catch (Exception e) {
            receiptsLayout.add(new H3("Error loading history"));
            Notification.show("Error loading purchase history");
            return;
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
            
            // Customer section
            Span customerLabel = new Span("👤 Customer:");
            customerLabel.getStyle().set("font-weight", "bold");
            customerLabel.getStyle().set("margin-top", "8px");
            
            String customerName = matchUserName(reciept.getUserId());
            Span customer = new Span(customerName);
            customer.getStyle().set("color", "#666");
            customer.getStyle().set("margin-left", "8px");
            
            HorizontalLayout customerLayout = new HorizontalLayout(customerLabel, customer);
            customerLayout.setSpacing(false);
            receiptCard.add(customerLayout);
            
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

    private String matchUserName(int userId) {
        for (MemberDTO member : memberDTOs) {
            if (member.getMemberId() == userId) {
                return member.getUsername(); 
            }
        }
        return "Unknown Item";
    }

    private String matchItemName(int itemId) {
        for (ItemDTO item : itemDTOs) {
            if (item.getId() == itemId) {
                return item.getName(); // Assuming ItemDTO has a getName() method
            }
        }
        return "Unknown Item"; // Return a default value if no match is found
    }

    private void loadUsers() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = usersUrl + "/allmembers?token=" + token;

            ResponseEntity<MemberDTO[]> response = rest.exchange(
                    url, HttpMethod.GET, request, MemberDTO[].class);

            memberDTOs = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();
        } catch (Exception e) {
            Notification.show("Failed to load users");
        }
    }

    private void loadItems() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = itemsUrl + "/all?token=" + token;

            ResponseEntity<ItemDTO[]> response = rest.exchange(
                    url, HttpMethod.GET, request, ItemDTO[].class);

            itemDTOs = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

        } catch (Exception e) {
            Notification.show("Failed to load items");
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
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = apiBase + "/users/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = rest.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
    
    private String getShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/shops/" + shopId + "?token=" + token;
        ResponseEntity<JsonNode> resp = rest.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            JsonNode.class
        );
        return (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null)
             ? resp.getBody().path("name").asText("")
             : "";
    }

    private String formatAddress(DTOs.AddressDTO address) {
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
