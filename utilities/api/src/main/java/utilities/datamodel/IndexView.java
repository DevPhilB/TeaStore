package utilities.datamodel;

import java.util.List;

public record IndexView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        String largeStoreIcon
) {}
