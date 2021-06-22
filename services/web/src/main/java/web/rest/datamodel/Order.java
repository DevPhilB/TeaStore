package web.rest.datamodel;

public record Order(
        String firstName,
        String lastName,
        String address1,
        String address2,
        String creditCardTyp,
        String creditCardNumber,
        String creditCardExpiryDate
) {}
