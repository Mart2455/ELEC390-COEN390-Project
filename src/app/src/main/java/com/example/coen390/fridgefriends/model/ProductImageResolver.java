package com.example.coen390.fridgefriends.model;

import java.util.HashMap;
import java.util.Map;

public class ProductImageResolver {

    private static final Map<String, String> IMAGE_MAP = new HashMap<>();
    static {
        // Vegetables & Greens
        IMAGE_MAP.put("carrot", "https://images.unsplash.com/photo-1598170845058-12ef4a457c15?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("carrots", "https://images.unsplash.com/photo-1598170845058-12ef4a457c15?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("lettuce", "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("salad", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("broccoli", "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("tomato", "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("tomatoes", "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("onion", "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("onions", "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("potato", "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("potatoes", "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("cucumber", "https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pepper", "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("peppers", "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("spinach", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("garlic", "https://images.unsplash.com/photo-1540148426945-6cf22a6b2383?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("mushroom", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("mushrooms", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("corn", "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("celery", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("zucchini", "https://images.unsplash.com/photo-1590165482129-1b8b27698780?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("eggplant", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("cabbage", "https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("ginger", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=300&q=80");

        // Fruits
        IMAGE_MAP.put("strawberry", "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("strawberries", "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("apple", "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("apples", "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("banana", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("bananas", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("orange", "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("oranges", "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("blueberry", "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("blueberries", "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("raspberry", "https://images.unsplash.com/photo-1577069861033-55d042a5f325?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("grape", "https://images.unsplash.com/photo-1537640538966-79f369143f8f?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("grapes", "https://images.unsplash.com/photo-1537640538966-79f369143f8f?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("lemon", "https://images.unsplash.com/photo-1534531141161-e41d133a8b50?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("lemons", "https://images.unsplash.com/photo-1534531141161-e41d133a8b50?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("lime", "https://images.unsplash.com/photo-1534531141161-e41d133a8b50?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("watermelon", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("avocado", "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("peach", "https://images.unsplash.com/photo-1595126730719-13683f06822c?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pear", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("mango", "https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pineapple", "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("kiwi", "https://images.unsplash.com/photo-1585059819970-07f71438afe1?auto=format&fit=crop&w=300&q=80");

        // Snacks, Sweets & Bakery
        IMAGE_MAP.put("cookie", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("cookies", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("biscuit", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("oreo", "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("chocolate", "https://images.unsplash.com/photo-1511381939415-e44015466834?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("candy", "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("chips", "https://images.unsplash.com/photo-1566478989037-eec170784d0b?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("popcorn", "https://images.unsplash.com/photo-1578849278619-e73505e9610f?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pretzel", "https://images.unsplash.com/photo-1536591375315-1989565823e3?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("nuts", "https://images.unsplash.com/photo-1536591375315-1989565823e3?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("bread", "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("toast", "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("bagel", "https://images.unsplash.com/photo-1585478259715-876acc5be8eb?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("croissant", "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("donut", "https://images.unsplash.com/photo-1551024709-8f23befc6f87?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("cake", "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pie", "https://images.unsplash.com/photo-1519869325930-281384150729?auto=format&fit=crop&w=300&q=80");

        // Dairy & Refrigerated
        IMAGE_MAP.put("milk", "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("cheese", "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("butter", "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("yogurt", "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("yoghurt", "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("egg", "https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("eggs", "https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("ice cream", "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?auto=format&fit=crop&w=300&q=80");

        // Meats & Fish
        IMAGE_MAP.put("chicken", "https://images.unsplash.com/photo-1587593810167-a84920ea0781?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("beef", "https://images.unsplash.com/photo-1603048588665-791ca8aea617?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("steak", "https://images.unsplash.com/photo-1603048588665-791ca8aea617?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pork", "https://images.unsplash.com/photo-1602470520998-f4a52199a3d6?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("bacon", "https://images.unsplash.com/photo-1528607929212-2636ec44253e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("sausage", "https://images.unsplash.com/photo-1585325701165-351af916e581?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("salmon", "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("fish", "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("shrimp", "https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?auto=format&fit=crop&w=300&q=80");

        // Pantry, Meals & Sauces
        IMAGE_MAP.put("pasta sauce", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("sauce", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pasta", "https://images.unsplash.com/photo-1621996346565-e3d5d6288015?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("spaghetti", "https://images.unsplash.com/photo-1621996346565-e3d5d6288015?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("rice", "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("soup", "https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("burger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("taco", "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("ketchup", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=300&q=80");

        // Beverages & Drinks
        IMAGE_MAP.put("juice", "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("soda", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("coke", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("water", "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("coffee", "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?auto=format&fit=crop&w=300&q=80");
        IMAGE_MAP.put("tea", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=300&q=80");
    }

    public static String resolveImageUrl(String itemName, String category) {
        if (itemName != null && !itemName.trim().isEmpty()) {
            String lower = itemName.toLowerCase().trim();
            // 1. Direct contains check
            for (Map.Entry<String, String> entry : IMAGE_MAP.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            // 2. Singular check if ends with 's' or 'es'
            String singular = lower;
            if (singular.endsWith("ies") && singular.length() > 4) {
                singular = singular.substring(0, singular.length() - 3) + "y";
            } else if (singular.endsWith("es") && singular.length() > 3) {
                singular = singular.substring(0, singular.length() - 2);
            } else if (singular.endsWith("s") && singular.length() > 3) {
                singular = singular.substring(0, singular.length() - 1);
            }
            for (Map.Entry<String, String> entry : IMAGE_MAP.entrySet()) {
                if (singular.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        // Leave blank if no photo match found!
        return "";
    }
}
