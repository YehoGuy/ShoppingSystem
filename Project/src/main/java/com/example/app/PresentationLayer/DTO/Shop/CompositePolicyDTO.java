package com.example.app.PresentationLayer.DTO.Shop;

import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;

import jakarta.validation.constraints.NotNull;

public class CompositePolicyDTO {
    private CompositePolicyDTO compoPolicy1;
    private CompositePolicyDTO compoPolicy2;
    private LeafPolicyDTO leafPolicy1;
    private LeafPolicyDTO leafPolicy2;
    private @NotNull Operator operator;

    public CompositePolicyDTO() {
    }

    public CompositePolicyDTO(CompositePolicyDTO compoPolicy1,
            CompositePolicyDTO compoPolicy2,
            LeafPolicyDTO leafPolicy1,
            LeafPolicyDTO leafPolicy2,
            Operator operator) {
        this.compoPolicy1 = compoPolicy1;
        this.compoPolicy2 = compoPolicy2;
        this.leafPolicy1 = leafPolicy1;
        this.leafPolicy2 = leafPolicy2;
        this.operator = operator;
    }

    public CompositePolicyDTO getCompoPolicy1() {
        return compoPolicy1;
    }

    public void setCompoPolicy1(CompositePolicyDTO compoPolicy1) {
        this.compoPolicy1 = compoPolicy1;
    }

    public CompositePolicyDTO getCompoPolicy2() {
        return compoPolicy2;
    }

    public void setCompoPolicy2(CompositePolicyDTO compoPolicy2) {
        this.compoPolicy2 = compoPolicy2;
    }

    public LeafPolicyDTO getLeafPolicy1() {
        return leafPolicy1;
    }

    public void setLeafPolicy1(LeafPolicyDTO leafPolicy1) {
        this.leafPolicy1 = leafPolicy1;
    }

    public LeafPolicyDTO getLeafPolicy2() {
        return leafPolicy2;
    }

    public void setLeafPolicy2(LeafPolicyDTO leafPolicy2) {
        this.leafPolicy2 = leafPolicy2;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        if (compoPolicy1 != null && compoPolicy2 != null) {
            return "(" + compoPolicy1 + " " + operator + " " + compoPolicy2 + ")";
        } else if (compoPolicy1 != null && leafPolicy1 != null) {
            return "(" + compoPolicy1 + " " + operator + " " + leafPolicy1 + ")";
        } else if (leafPolicy1 != null && leafPolicy2 != null) {
            return "(" + leafPolicy1 + " " + operator + " " + leafPolicy2 + ")";
        } else if (compoPolicy2 != null && leafPolicy2 != null) {
            return "(" + compoPolicy2 + " " + operator + " " + leafPolicy2 + ")";
        } else {
            return "(" + leafPolicy1 + " " + operator + " " + compoPolicy2 + ")";
        }
    }

    public static CompositePolicyDTO fromDomain(Policy p) {
        if (p == null) {
            return null; // handle null case
        }
        if (p instanceof PolicyComposite pc) {
            CompositePolicyDTO left = fromDomain(pc.getPolicy1());
            CompositePolicyDTO right = fromDomain(pc.getPolicy2());
            return new CompositePolicyDTO(left, right, null, null, pc.getOperator());
        }
        // leaf node
        if (p instanceof PolicyLeaf lf) {
            // leaf → wrap into a composite‐DTO so UI can render uniformly
            LeafPolicyDTO leafDto = new LeafPolicyDTO(lf.getThreshold(), lf.getItemId(), lf.getCategory(),
                    lf.getBasketValue());
            return new CompositePolicyDTO(null, null, leafDto, null, null);
        }
        throw new IllegalArgumentException("Unknown Policy type: " + p.getClass());
    }
}
