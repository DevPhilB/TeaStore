package web.rest.datamodel;

public record DatabaseView(
        String storeIcon,
        String title,
        Integer numberOfNewCategories,
        Integer numberOfNewProductsPerCategory,
        Integer numberOfNewUsers,
        Integer numberOfMaxOrdersPerUser
) {}
