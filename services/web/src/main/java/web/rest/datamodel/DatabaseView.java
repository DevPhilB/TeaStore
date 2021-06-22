package web.rest.datamodel;

public record DatabaseView(
        String storeIcon,
        String title,
        int numberOfNewCategories,
        int numberOfNewProductsPerCategory,
        int numberOfNewUsers,
        int numberOfMaxOrdersPerUser
) {}
