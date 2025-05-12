package UI;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import java.util.Map;

import DTOs.PurchaseDTO;
import DTOs.AddressDTO;

@Route("receipt")
public class ReceiptView extends VerticalLayout implements HasUrlParameter<Integer> {

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer purchaseId) {
        setPadding(true);
        setSpacing(true);

        if (purchaseId == null) {
            add(new Span("❌ No purchase ID provided."));
            return;
        }

        // Replace this with actual service call
        PurchaseDTO purchase = mockPurchaseById(purchaseId);

        if (purchase == null) {
            add(new Span("❌ Purchase #" + purchaseId + " not found."));
            return;
        }

        add(new H1("🧾 Receipt for Purchase #" + purchaseId));

        Map<Integer, Integer> items = purchase.getItems(); // itemId -> quantity
        AddressDTO address = purchase.getShippingAddress();
        double total = purchase.getPrice();

        items.forEach((id, qty) ->
            add(new Span("• Item #" + id + " x" + qty))
        );

        add(new Span("📦 Shipping To: " + address.getStreet() + " " + address.getHouseNumber() +
                     ", " + address.getCity()));
        add(new Span("💰 Total Paid: $" + total));
    }

    // Replace this with real service logic
    private PurchaseDTO mockPurchaseById(int id) {
        return new PurchaseDTO(
            id,
            1,
            5,
            Map.of(1, 2, 2, 3),
            new AddressDTO("USA", "NY", "Main St", "101", "2A", "10001"),
            true,
            null,
            23.5
        );
    }
}
