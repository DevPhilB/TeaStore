package web.rest.datamodel;

import java.util.List;
import java.util.Map;

public record CartView (
    String storeIcon,
    String title,
    List<Category> categoryList,
    List<CartItem> cartItems,
    List<Product> advertisements,
    List<String> productImages,
    String updateCart,
    String proceedToCheckout
) {}
