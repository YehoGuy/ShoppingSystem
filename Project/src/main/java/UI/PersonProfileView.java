package UI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import DTOs.MemberDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DTOs.rolesDTO;

@Route(value = "profile", layout = AppLayoutBasic.class)
public class PersonProfileView extends VerticalLayout {

    private MemberDTO member;
    private Map<String, ShopDTO> knownShops = new HashMap<>();

    public PersonProfileView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        this.member = mockMember(); // Replace with real data
        this.knownShops = mockShopLookup(); // Replace with real data

        H1 title = new H1("👤 " + member.getUsername() + "'s Profile");
        add(title);

        addPersonalDetails();
        addRoleOverview();
        addPurchaseHistory();
    }

    private void addPersonalDetails() {
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(false);
        infoLayout.setSpacing(false);
        infoLayout.setWidth("60%");
        infoLayout.getStyle()
                .set("border", "1px solid #aaa")
                .set("border-radius", "8px")
                .set("padding", "10px")
                .set("margin-bottom", "20px")
                .set("background-color", "#f3f3f3");

        infoLayout.add(new Span("🆔 Member ID: " + member.getMemberId()));
        infoLayout.add(new Span("📧 Email: " + member.getEmail()));
        infoLayout.add(new Span("📞 Phone: " + member.getPhoneNumber()));

        add(infoLayout);
    }

    private void addRoleOverview() {
        Map<String, List<rolesDTO>> rolesByShop = groupRolesByShop(member.getRoles());

        H2 roleSection = new H2("🛍️ Shop Roles");
        add(roleSection);

        if (rolesByShop.isEmpty()) {
            add(new Span("You have no roles in any shops."));
        } else {
            for (String shopName : rolesByShop.keySet()) {
                ShopDTO shop = knownShops.getOrDefault(shopName, new ShopDTO(shopName, Map.of(), Map.of(), List.of()));
                List<rolesDTO> roles = rolesByShop.get(shopName);
                add(createShopCard(shop, roles));
            }
        }
    }

    private void addPurchaseHistory() {
        List<Integer> history = member.getOrderHistory();

        H2 historyTitle = new H2("🧾 Purchase History");
        add(historyTitle);

        if (history == null || history.isEmpty()) {
            add(new Span("No purchases yet."));
            return;
        }

        VerticalLayout historyLayout = new VerticalLayout();
        historyLayout.setPadding(false);
        historyLayout.setSpacing(false);
        historyLayout.setWidth("60%");
        historyLayout.getStyle()
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "10px")
                .set("background-color", "#fafafa");

        for (Integer purchaseId : history) {
            Anchor link = new Anchor("/receipt/" + purchaseId, "• View Receipt #" + purchaseId);
            link.getStyle().set("text-decoration", "none");
            historyLayout.add(link);
        }

        add(historyLayout);
    }

    private VerticalLayout createShopCard(ShopDTO shop, List<rolesDTO> roles) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("60%");
        card.getStyle()
            .set("border", "1px solid #ccc")
            .set("border-radius", "8px")
            .set("padding", "10px")
            .set("margin-bottom", "15px")
            .set("background-color", "#f9f9f9");

        Span name = new Span("🏪 " + shop.getName());
        name.getStyle().set("font-size", "18px").set("font-weight", "600");

        Span rating = new Span("⭐ Average Rating: " + calculateAverageRating(shop.getReviews()));

        VerticalLayout rolesList = new VerticalLayout();
        rolesList.setPadding(false);
        rolesList.setSpacing(false);
        rolesList.add(new Span("🧑‍💼 Roles:"));
        for (rolesDTO role : roles) {
            Span roleLabel = new Span("- " + role.getRoleName());
            roleLabel.getStyle().set("font-weight", "bold");
            rolesList.add(roleLabel);

            if (role.getPermissions().size() > 0) {
                List<String> permissions = role.getPermissions();
                VerticalLayout permList = new VerticalLayout();
                permList.setSpacing(false);
                permList.setPadding(false);
                for (String perm : permissions) {
                    permList.add(new Span("   • " + perm));
                }
                rolesList.add(permList);
            }
        }

        Button openButton = new Button("Edit Shop", e -> UI.getCurrent().navigate("/editShop/" + shop.getName()));
        card.add(name, rating, rolesList, openButton);
        return card;
    }

    private Map<String, List<rolesDTO>> groupRolesByShop(List<rolesDTO> roles) {
        Map<String, List<rolesDTO>> rolesByShop = new HashMap<>();
        for (rolesDTO role : roles) {
            String shopName = role.getShopName();
            rolesByShop.computeIfAbsent(shopName, k -> new java.util.ArrayList<>()).add(role);
        }
        return rolesByShop;
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToDouble(ShopReviewDTO::getRating).average().orElse(0.0);
    }

    // ---------- MOCK DATA BELOW ----------

    private MemberDTO mockMember() {
        List<rolesDTO> roles = List.of(
            new rolesDTO("Manager", List.of("Manage Inventory", "View Sales"), "Fresh Mart", "john_doe"),
            new rolesDTO("Manager", List.of("Process Payments"), "Beauty Hub", "john_doe"),
            new rolesDTO("Manager", List.of("View Products"), "ElectroMax", "john_doe")
        );
        return new MemberDTO(
            1,
            "john_doe",
            "pass",
            "john@example.com",
            "123456789",
            roles,
            List.of(101, 102, 108), // Mock purchase history
            List.of()
        );
    }

    private Map<String, ShopDTO> mockShopLookup() {
        return Map.of(
            "Fresh Mart", new ShopDTO("Fresh Mart", Map.of(), Map.of(), List.of()),
            "Beauty Hub", new ShopDTO("Beauty Hub", Map.of(), Map.of(), List.of()),
            "ElectroMax", new ShopDTO("ElectroMax", Map.of(), Map.of(), List.of())
        );
    }
}
