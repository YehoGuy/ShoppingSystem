package DTOs;

import java.io.Serializable;
import java.util.Objects;
import Domain.ItemCategory;
import Domain.Operator;

public class PoliciesDTO implements Serializable {
    private Integer              itemId;        // which item this policy applies to
    private ItemCategory         itemCategory;  // if category‐level
    private CompositePolicyDTO   policy;        // the full policy tree

    public PoliciesDTO() {}

    public PoliciesDTO(Integer itemId,
                       ItemCategory itemCategory,
                       CompositePolicyDTO policy) {
        this.itemId       = itemId;
        this.itemCategory = itemCategory;
        this.policy       = policy;
    }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public ItemCategory getItemCategory() { return itemCategory; }
    public void setItemCategory(ItemCategory itemCategory) { this.itemCategory = itemCategory; }

    public CompositePolicyDTO getPolicy() { return policy; }
    public void setPolicy(CompositePolicyDTO policy) { this.policy = policy; }

    @Override
    public String toString() {
        if (policy == null) return "No Policy";
        return policy.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PoliciesDTO)) return false;
        PoliciesDTO that = (PoliciesDTO) o;
        return Objects.equals(itemId, that.itemId)
            && itemCategory == that.itemCategory
            && Objects.equals(policy, that.policy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, itemCategory, policy);
    }
}
