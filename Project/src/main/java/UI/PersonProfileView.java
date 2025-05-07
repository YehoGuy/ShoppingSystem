package UI;

import java.util.*;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import DTOs.MemberDTO;
import DTOs.RoleDTO;
import DTOs.ShopDTO;
import DTOs.ItemDTO;
import DTOs.ShopReviewDTO;

@Route(value = "profile", layout = AppLayoutBasic.class)
public class PersonProfileView extends VerticalLayout {

    private MemberDTO member;
    private Map<String, ShopDTO> knownShops = new HashMap<>();

    public PersonProfileView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        this.member = mockMember(); // Replace with real data in production
        this.knownShops = mockShopLookup(); // Replace with real shop lookup

        H1 title = new H1("👤 " + member.getUsername() + "'s Shop Roles");
        add(title);

        Map<String, List<RoleDTO>> rolesByShop = groupRolesByShop(member.getRoles());

        if (rolesByShop.isEmpty()) {
            add(new Span("You are not associated with any shops."));
        } else {
            for (String shopName : rolesByShop.keySet()) {
                ShopDTO shop = knownShops.getOrDefault(shopName, new ShopDTO(shopName, Map.of(), Map.of(), List.of()));
                List<RoleDTO> roles = rolesByShop.get(shopName);
                add(createShopCard(shop, roles));
            }
        }
    }

    private VerticalLayout createShopCard(ShopDTO shop, List<RoleDTO> roles) {
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
        for (RoleDTO role : roles) {
            Span roleLabel = new Span("- " + role.getRoleName());
            roleLabel.getStyle().set("font-weight", "bold");
            rolesList.add(roleLabel);

            if (role.getDescription().contains("Permissions:")) {
                String[] parts = role.getDescription().split("Permissions:");
                if (parts.length > 1) {
                    String[] permissions = parts[1].split(",");
                    VerticalLayout permList = new VerticalLayout();
                    permList.setSpacing(false);
                    permList.setPadding(false);
                    for (String perm : permissions) {
                        permList.add(new Span("   • " + perm.trim()));
                    }
                    rolesList.add(permList);
                }
            }
        }

        Button openButton = new Button("Edit Shop", e -> Notification.show("Opening: " + shop.getName( ) + " for editing."));

        card.add(name, rating, rolesList, openButton);
        return card;
    }

    private Map<String, List<RoleDTO>> groupRolesByShop(List<RoleDTO> roles) {
        return roles.stream()
                .filter(r -> r.getDescription() != null && r.getDescription().contains(" at "))
                .collect(Collectors.groupingBy(this::extractShopNameFromDescription));
    }

    private String extractShopNameFromDescription(RoleDTO role) {
        String desc = role.getDescription();
        int atIndex = desc.indexOf(" at ");
        int newline = desc.indexOf("\n", atIndex);
        return newline > 0 ? desc.substring(atIndex + 4, newline).trim()
                           : desc.substring(atIndex + 4).trim();
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToDouble(ShopReviewDTO::getRating).average().orElse(0.0);
    }

    // ---------- MOCK DATA BELOW ----------

    private MemberDTO mockMember() {
        List<RoleDTO> roles = List.of(
            new RoleDTO("Owner", "Owner at Fresh Mart\nPermissions: manageItems, setPolicy, manageOwners, manageManagers, getHistory"),
            new RoleDTO("Manager", "Manager at Beauty Hub\nPermissions: manageItems, getStaffInfo, handleMessages"),
            new RoleDTO("Staff", "Staff at ElectroMax\nPermissions: handleMessages")
        );
        return new MemberDTO(1, "john_doe", "pass", "john@example.com", "123456789", roles, List.of(), List.of());
    }

    private Map<String, ShopDTO> mockShopLookup() {
        return Map.of(
            "Fresh Mart", new ShopDTO("Fresh Mart", Map.of(), Map.of(), List.of()),
            "Beauty Hub", new ShopDTO("Beauty Hub", Map.of(), Map.of(), List.of()),
            "ElectroMax", new ShopDTO("ElectroMax", Map.of(), Map.of(), List.of())
        );
    }
}
