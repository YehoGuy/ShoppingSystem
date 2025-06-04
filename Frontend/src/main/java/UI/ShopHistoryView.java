package UI;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import DTOs.RecieptDTO;

@Route(value = "history", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    @Value("${url.api}/purchases/shops")
    private String PURCHASE_HISTORY_URL;

    private final RestTemplate rest = new RestTemplate();
    private final VerticalLayout receiptsLayout = new VerticalLayout();
    private String token;
    private int shopId;

    public ShopHistoryView() {
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
        token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
            return;
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m => m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.shopId = parameter;
        loadReceipts();
    }

    private void loadReceipts() {
        receiptsLayout.removeAll();

        String url = PURCHASE_HISTORY_URL + "/" + shopId + "?authToken="
                + VaadinSession.getCurrent().getAttribute("authToken");
        try {
            ResponseEntity<RecieptDTO[]> resp = rest.getForEntity(url, RecieptDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                RecieptDTO[] receipts = resp.getBody();
                if (receipts.length == 0) {
                    receiptsLayout.add(new H3("No purchase history for this shop."));
                } else {
                    for (RecieptDTO r : receipts) {
                        addReceiptCard(r);
                    }
                }
            } else {
                receiptsLayout.add(new H3("Failed to load history: HTTP " + resp.getStatusCode()));
            }
        } catch (Exception ex) {
            receiptsLayout.add(new H3("Error loading history: " + ex.getMessage()));
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

}
