package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;

import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Operator;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("composite")
public class PolicyComposite extends Policy {

    private Policy policy1;
    private Policy policy2;

    private final Operator operator;

    public PolicyComposite(Operator operator) {
        this.policy1 = null;
        this.policy2 = null;
        this.operator = operator;
    }

    public PolicyComposite(Policy policy1, Operator operator) {
        this.policy1 = policy1;
        this.policy2 = null;
        this.operator = operator;
    }

    public PolicyComposite(Policy policy1, Policy policy2, Operator operator) {
        this.policy1 = policy1;
        this.policy2 = policy2;
        this.operator = operator;
    }

    public void addPolicy(Policy policy) {
        if (policy1 == null) {
            policy1 = policy;
        } else if (policy2 == null) {
            policy2 = policy;
        } else {
            throw new IllegalStateException("Cannot add more than two policies to a composite");
        }
    }

    @Override
    public boolean test(Map<Integer, Integer> items, Map<Integer, Double> prices,
            Map<Integer, ItemCategory> itemsCategory) {
        if (policy1 == null && policy2 == null) {
            return true;
        }
        if (policy1 == null) {
            return policy2.test(items, prices, itemsCategory);
        }
        if (policy2 == null) {
            return policy1.test(items, prices, itemsCategory);
        }
        boolean result1 = policy1.test(items, prices, itemsCategory);
        boolean result2 = policy2.test(items, prices, itemsCategory);

        switch (operator) {
            case AND:
                return result1 && result2;
            case OR:
                return result1 || result2;
            case XOR:
                return result1 ^ result2;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }

    public Policy getPolicy1() {
        return policy1;
    }

    public Policy getPolicy2() {
        return policy2;
    }

    public Operator getOperator() {
        return operator;
    }

}
