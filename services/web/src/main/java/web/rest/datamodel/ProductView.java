package web.rest.datamodel;

import java.util.List;

public record ProductView(
    List<Category> categoryList,
    String title,
    boolean isLoggedIn,
    Product product,
    List<Product> advertisements,
    List<String> productImages,
    String productImage,
    String storeIcon
) {}
