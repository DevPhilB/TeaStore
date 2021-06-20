package web.rest.datamodel;

public record Product (
    long id,
    long categoryId,
    String name,
    String description,
    long listPriceInCents
) {}
