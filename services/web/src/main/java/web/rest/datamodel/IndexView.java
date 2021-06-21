package web.rest.datamodel;

import java.util.List;

public record IndexView(
    List<Category> categoryList,
    String title,
    boolean isLoggedIn,
    String storeIcon
) {}
