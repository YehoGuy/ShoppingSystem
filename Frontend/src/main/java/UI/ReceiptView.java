package UI;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.AddressDTO;
import DTOs.RecieptDTO;


@Route("receipt")
@JsModule("./js/notification-client.js")
public class ReceiptView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${url.api}/purchases")
    private String URL;
        
    private RecieptDTO receipt;

    public ReceiptView() {
        configureLayout(); // keep this as is

        Integer purchaseId = (Integer) VaadinSession.getCurrent().getAttribute("purchaseId");

        if (purchaseId == null) {
            mockReceiptById(0);
        } else {
            setReceipt();
        }

        add(new H1("🧾 Receipt"));
        buildReceiptView(); // 🔸 This is where you display the pretty receipt
    }

    private void configureLayout() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
        if (VaadinSession.getCurrent().getAttribute("purchaseId") == null) {
            event.forwardTo("home");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m -> m.connectNotifications($0))",
                getUserId());
        handleSuspence();
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    private void buildReceiptView() {

        if (receipt == null) {
            add(new Paragraph("No receipt data available."));
            return;
        }

        AddressDTO address = receipt.getAddress();

        // Create a styled container for the receipt
        Div receiptContainer = new Div();
        receiptContainer.getStyle()
                .set("border", "1px solid #ccc")
                .set("padding", "20px")
                .set("border-radius", "10px")
                .set("max-width", "400px")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background-color", "#fafafa");

        receiptContainer.add(
                createLabeledLine("🧾 Purchase ID", receipt.getPurchaseId()),
                createLabeledLine("👤 User ID", receipt.getUserId()),
                createLabeledLine("🏬 Store ID", receipt.getStoreId()),
                new Paragraph("📦 Shipping Address:"),
                createIndentedLine(address.getStreet() + " " + address.getHouseNumber()),
                createIndentedLine(address.getCity() + ", " + address.getCountry()),
                createLabeledLine("💰 Total Price", "$" + String.format("%.2f", receipt.getPrice())));

        add(receiptContainer);
    }

    // Helper methods to format the lines
    private Paragraph createLabeledLine(String label, Object value) {
        Paragraph line = new Paragraph();
        line.getElement().setProperty("innerHTML", "<strong>" + label + ":</strong> " + value);
        line.getStyle().set("margin", "8px 0");
        return line;
    }

    private Paragraph createIndentedLine(String text) {
        Paragraph line = new Paragraph(text);
        line.getStyle().set("margin", "4px 0 4px 16px");
        return line;
    }

    private void mockReceiptById(int id) {
        receipt = new RecieptDTO(
                id, id, id,
                new HashMap<>(),
                new AddressDTO("Street", "HouseNumber", "City", "Country", "here", "here"),
                false,
                null,
                80.0,
                null);
    }

    private void setReceipt() {
        String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
        Integer purchaseId = (Integer) VaadinSession.getCurrent().getAttribute("purchaseId");

        if (authToken == null || purchaseId == null) {
            Notification.show("Missing token or purchase ID.");
            return;
        }

        String url = URL + "/" + purchaseId;
        RestTemplate restTemplate = new RestTemplate();

        // Add authToken as header if needed, or modify URL with query param
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken); // or custom header
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RecieptDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RecieptDTO.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                receipt = response.getBody();
            } else {
                Notification.show("Failed to fetch receipt: " + response.getStatusCode());
            }
        } catch (Exception e) {
            Notification.show("Error fetching receipt: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
    private void handleSuspence() {
        RestTemplate restTemplate = new RestTemplate();

        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = "http://localhost:8080/api/users" + "/"+userId+"/suspension?token=" +token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            throw new RuntimeException(
                "Failed to check admin status: HTTP " + response.getStatusCode().value()
            );
        }
    }
}
