package utilities.datamodel;

import java.util.List;

public record OrderView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        Order order,
        String confirm
) {}
