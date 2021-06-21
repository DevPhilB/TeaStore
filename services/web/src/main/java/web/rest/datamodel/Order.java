package web.rest.datamodel;

public record Order(
    long id,
    long userId,
    String time,
    long totalPriceInCents,
    String addressName,
    String address1,
    String address2,
    String creditCardCompany,
    String creditCardNumber,
    String creditCardExpiryDate
) {}
