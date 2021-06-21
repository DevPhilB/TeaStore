package web.rest.datamodel;

import java.util.List;

public record ProfileView(
    String storeIcon,
    List<Category> categoryList,
    String title,
    User user,
    List<Order> orders,
    boolean isLoggedIn
) {}
