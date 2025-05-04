package DomainLayer.Shop.Discount;

import java.util.Map;

import DomainLayer.Item.ItemCategory;

public class PolicyLeaf implements Policy{

    private final TriPredicate<Map<Integer,Integer>,Map<Integer,Double>,Map<Integer,ItemCategory>> predicate;

    public PolicyLeaf(TriPredicate<Map<Integer,Integer>, Map<Integer,Double>,Map<Integer,ItemCategory>> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory) {
        return predicate.test(items, prices, itemsCategory);
    }
    
}
