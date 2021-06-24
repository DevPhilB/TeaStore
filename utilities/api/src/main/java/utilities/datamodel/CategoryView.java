package utilities.datamodel;

import java.util.List;

public record CategoryView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        List<Product> products,
        Integer page,
        Integer productQuantity
) {}
