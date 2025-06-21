package UI;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.atmosphere.interceptor.AtmosphereResourceStateRecovery.B;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

import DTOs.ItemDTO;
import DTOs.MemberDTO;
import DTOs.RecieptDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import UI.AdminView.ItemGridRow;
import UI.AdminView.ShopGridRow;
import UI.AdminView.UserGridRow;

import org.springframework.http.*;




import java.util.Arrays;





@Route(value = "history", layout = AppLayoutBasic.class)

public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final String api;
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
    

    public ShopHistoryView(@Value("${url.api}") String api) {
        this.api = api;
        this.purchaseHistoryUrl = api + "/purchases/shops";
        this.usersUrl = api + "/users";
        this.itemsUrl = api + "/items";
        

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        receiptsLayout.setWidthFull();
        receiptsLayout.setSpacing(true);
        receiptsLayout.setPadding(true);
        add(receiptsLayout);
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

        H1 header = new H1("Purchase History for Shop ID: " + shopId);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        add(header);

        Button refreshButton = new Button("refresh", e -> buildView());
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            refreshButton.setVisible(false);
        }
        add(refreshButton);

        // H3 title = new H3("Purchase History for Shop ID: " + shopId);
        // title.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        // receiptsLayout.add(title);
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
                    receiptsLayout.add(new H3("No purchase history for this shop."));
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
        

        for (RecieptDTO reciept : reciepts) {
            List<String> foritems = new ArrayList<>();
            VerticalLayout singleReciept = new VerticalLayout();
            Map<Integer, Integer> items = reciept.getItems();
            for(int itemId : items.keySet()) 
            {
                foritems.add(matchItemName(itemId) + ":" + items.get(itemId));
            }
            singleReciept.add(
                    new Span("PurchaseId: " + reciept.getPurchaseId()),
                    new Span("UserName: " + matchUserName(reciept.getUserId())), //!!!!
                    new Span(foritems.toString()),
                    new Span("Shipping Address: " + reciept.getAddress()),
                    new Span("Price: $" + reciept.getPrice()));
            singleReciept.getStyle().set("border", "1px solid #ccc");
            singleReciept.getStyle().set("padding", "10px");
            singleReciept.getStyle().set("margin-bottom", "10px");
            receiptsLayout.add(singleReciept);
        }
        add(receiptsLayout);

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



    private void addReceiptCard(RecieptDTO r) {
        // card container
        com.vaadin.flow.component.html.Section card = new com.vaadin.flow.component.html.Section();
        card.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.LARGE);
        card.setWidth("80%");

        // header info
        H3 buyer = new H3("Buyer ID: " + r.getUserId());
        H3 totalPrice = new H3("Total Price: $" + r.getPrice());
        H3 completed = new H3("Completed: " + r.isCompleted());
        String ts = r.getTimestampOfRecieptGeneration()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        H3 date = new H3("Date: " + ts);

        HorizontalLayout header = new HorizontalLayout(buyer, totalPrice, completed, date);
        header.setWidthFull();
        header.getStyle().set("spacing", "var(--lumo-space-m)");
        header.getStyle().set("padding", "var(--lumo-space-m)");

        // items grid
        Grid<Map.Entry<Integer, Integer>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey).setHeader("Item ID");
        grid.addColumn(Map.Entry::getValue).setHeader("Quantity");
        grid.setItems(r.getItems().entrySet());
        grid.setWidthFull();
        grid.getColumns().forEach(c -> c.setAutoWidth(true));

        Details details = new Details("View Items", grid);
        details.setWidthFull();

        // assemble
        VerticalLayout box = new VerticalLayout(header, details);
        box.setWidthFull();
        card.add(box);
        receiptsLayout.add(card);
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
        String url = api + "/users/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = rest.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }

}
