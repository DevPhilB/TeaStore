package utilities.datamodel;

import java.util.List;

public record ProfileView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        User user,
        List<OrderHistory> orders
) {}
