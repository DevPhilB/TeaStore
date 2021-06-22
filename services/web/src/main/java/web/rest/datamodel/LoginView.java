package web.rest.datamodel;

import java.util.List;

public record LoginView(
        String storeIcon,
        String title,
        List<Category> categoryList,
        String description,
        String username,
        String password,
        String signIn,
        boolean isLoggedIn,
        String referer
) {}