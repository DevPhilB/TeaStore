package utilities.datamodel;

public record Product (
        Long id,
        Long categoryId,
        String image,
        String name,
        Long listPriceInCents,
        String description,
        String addToCart
) {}
