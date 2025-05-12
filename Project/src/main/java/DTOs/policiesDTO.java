package DTOs;

public class policiesDTO {
    private String policyName; // The name of the policy.
    private boolean isDiscount;
    private String description; // A description of the policy.
    private String shopName; // The name of the shop associated with the policy.

    public policiesDTO(String policyName, String description, String shopName, boolean isDiscount) {
        this.isDiscount = isDiscount;
        this.policyName = policyName;
        this.description = description;
        this.shopName = shopName;
    }

    public boolean isDiscount() {
        return isDiscount;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getDescription() {
        return description;
    }

    public String getShopName() {
        return shopName;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "policyName='" + policyName + '\'' +
                ", description='" + description + '\'' +
                ", shopName='" + shopName + '\'' +
                '}';
    }

}
