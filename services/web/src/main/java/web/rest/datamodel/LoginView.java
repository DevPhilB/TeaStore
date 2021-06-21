package web.rest.datamodel;

import java.util.List;

public record LoginView(
    List<Category> categoryList,
    String storeIcon,
    String title,
    boolean isLoggedIn,
    String referer
) {}