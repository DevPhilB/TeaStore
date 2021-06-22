package web.rest.datamodel;

public record ErrorView(
    String storeIcon,
    String title,
    String errorImage,
    String backToShop,
    boolean isLoggedIn
) {}
