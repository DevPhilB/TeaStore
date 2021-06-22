package web.rest.datamodel;

public record Product (
        long id,
        long categoryId,
        String image,
        String name,
        long listPriceInCents,
        String description,
        String addToCart
) {}
