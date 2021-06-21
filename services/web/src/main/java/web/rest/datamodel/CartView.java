package web.rest.datamodel;

import java.util.List;
import java.util.Map;

public record CartView (
    String storeIcon,
    String title,
    List<Category> categoryList,
    List<OrderItem> orderItems,
    Map<Long, Product> products,
    boolean isLoggedIn,
    List<Product> advertisements,
    List<String> productImages
) {}
