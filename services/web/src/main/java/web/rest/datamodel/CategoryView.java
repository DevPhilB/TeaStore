package web.rest.datamodel;

import java.util.List;

public record CategoryView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        List<Product> products,
        int page,
        int productQuantity,
        boolean isLoggedIn
) {}
