package DomainLayer.Shop.Discount;

import java.util.Map;

import DomainLayer.Item.ItemCategory;

public interface Policy {

    boolean test(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory);
}
