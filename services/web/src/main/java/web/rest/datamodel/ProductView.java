package web.rest.datamodel;

import java.util.List;

public record ProductView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        Product product,
        List<Product> advertisements
) {}
