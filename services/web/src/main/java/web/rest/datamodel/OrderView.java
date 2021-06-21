package web.rest.datamodel;

import java.util.List;

public record OrderView(
    List<Category> categoryList,
    String storeIcon,
    String title,
    boolean isLoggedIn
) {}
