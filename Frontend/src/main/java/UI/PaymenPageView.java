package UI;

import com.vaadin.flow.router.Route;

import DTOs.PaymentMethodDTO;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "payment")
public class PaymenPageView extends VerticalLayout implements BeforeEnterObserver {
    // This class will be used to create the payment page view
    // It will contain the UI components and layout for the payment page
    // It will also handle the payment processing logic

    List<PaymentMethodDTO> paymentMethods = new ArrayList<>();
    double totalAmount = 0.0; // Total amount to be paid
    String orderId = ""; // Order ID for the payment

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if the user is logged in
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public PaymenPageView() {
        // Initialize the payment methods
        paymentMethods.add(new PaymentMethodDTO("Credit Card", "Card", "Visa, MasterCard, American Express"));
        paymentMethods.add(new PaymentMethodDTO("PayPal", "Online", "PayPal account required"));
        paymentMethods.add(new PaymentMethodDTO("Bank Transfer", "Transfer", "Bank account details required"));
        paymentMethods.add(new PaymentMethodDTO("Cash on Delivery", "Cash", "Pay in cash upon delivery"));

        // Initialize the total amount and order ID
        totalAmount = 100.0; // Example amount
        orderId = "ORD123456"; // Example order ID

        // Set up the layout and components for the payment page
        setUpLayout();
    }

    private void setUpLayout() {
        // Set the layout for the payment page
        setSizeFull();
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        // Add components to the layout
        add(new H1("Payment Page"));
        add(new H3("Order number: " + orderId + " Total Amount: $" + totalAmount));
        add(new H3("Please select a payment method:"));

        // Add payment method options to the layout
        // Create a vertical layout for the buttons
        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        buttonLayout.getStyle().setBorder("5px solid #ccc");
        buttonLayout.getStyle().set("border-radius", "10px");
        buttonLayout.getStyle().set("padding", "10px");
        buttonLayout.getStyle().set("background-color", "#f9f9f9");

        // Add buttons to the layout
        for (PaymentMethodDTO method : paymentMethods) {
            Button methodButton = new Button(method.getMethodName(), event -> {
                handlePaymentMethodSelection(method);
            });
            methodButton.setWidthFull();
            buttonLayout.add(methodButton);
        }

        // Create a scroller and add the button layout to it
        Div scroller = new Div(buttonLayout);
        scroller.getStyle().set("overflow-y", "auto");
        scroller.setHeightFull();
        scroller.setWidth("300px");

        add(scroller);
    }

    private void handlePaymentMethodSelection(PaymentMethodDTO method) {
        // Handle the payment method selection logic here
        // For example, redirect to a payment gateway or process the payment
        System.out.println("Selected payment method: " + method.getMethodName());
        // Add your payment processing logic here
    }

}
