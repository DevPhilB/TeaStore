package web.rest.datamodel;

public record Order(
        long id,
        String firstName,
        String lastName,
        String address1,
        String address2,
        String creditCardTyp,
        String creditCardNumber,
        String creditCardExpiryDate
) {}
