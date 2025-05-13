package DTOs;

import Domain.Operator;

public class CompositePolicyDTO {
    private CompositePolicyDTO compoPolicy1;
    private CompositePolicyDTO compoPolicy2;
    private LeafPolicyDTO leafPolicy1;
    private LeafPolicyDTO leafPolicy2;
    private Operator operator;

    public CompositePolicyDTO(CompositePolicyDTO compoPolicy1, CompositePolicyDTO compoPolicy2,
            LeafPolicyDTO leafPolicy1, LeafPolicyDTO leafPolicy2, Operator operator) {
        this.compoPolicy1 = compoPolicy1;
        this.compoPolicy2 = compoPolicy2;
        this.leafPolicy1 = leafPolicy1;
        this.leafPolicy2 = leafPolicy2;
        this.operator = operator;
    }

    public CompositePolicyDTO getCompoPolicy1() {
        return compoPolicy1;
    }

    public CompositePolicyDTO getCompoPolicy2() {
        return compoPolicy2;
    }

    public LeafPolicyDTO getLeafPolicy1() {
        return leafPolicy1;
    }

    public LeafPolicyDTO getLeafPolicy2() {
        return leafPolicy2;
    }

    public Operator getOperator() {
        return operator;
    }

    public String toString() {
        if (compoPolicy1 != null && compoPolicy2 != null) {
            return "(" + compoPolicy1.toString() + " " + operator.toString() + " " + compoPolicy2.toString() + ")";
        } else if (compoPolicy1 != null && leafPolicy1 != null) {
            return "(" + compoPolicy1.toString() + " " + operator.toString() + " " + leafPolicy1.toString() + ")";
        } else if (compoPolicy2 != null && leafPolicy1 != null) {
            return "(" + leafPolicy1.toString() + " " + operator.toString() + " " + compoPolicy2.toString() + ")";
        } else if (leafPolicy1 != null && leafPolicy2 != null) {
            return "(" + leafPolicy1.toString() + " " + operator.toString() + " " + leafPolicy2.toString() + ")";
        } else if (compoPolicy2 != null && leafPolicy2 != null) {
            return "(" + compoPolicy2.toString() + " " + operator.toString() + " " + leafPolicy2.toString() + ")";
        } else {
            return "(" + leafPolicy1.toString() + " " + operator.toString() + " " + compoPolicy2.toString() + ")";
        }
    }
}
