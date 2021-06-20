package web.rest.datamodel;

import java.util.Map;

public record AboutView (
    Map<String, String> portraits,
    String descartesLogo,
    String storeIcon,
    String title,
    boolean isLoggedIn
) {}
