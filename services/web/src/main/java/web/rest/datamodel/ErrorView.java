package web.rest.datamodel;

import java.util.List;

public record ErrorView(
    List<Category> categoryList,
    String storeIcon,
    String errorImage,
    String title,
    boolean isLoggedIn
) {}
