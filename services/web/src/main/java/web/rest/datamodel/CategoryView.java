package web.rest.datamodel;

import java.util.List;
import java.util.Map;

public record CategoryView(
    Map<String, String> productImages,
    String storeIcon,
    List<Category> categoryList,
    String title,
    String category,
    boolean isLoggedIn,
    long categoryId,
    int productQuantity,
    int page
) {}
