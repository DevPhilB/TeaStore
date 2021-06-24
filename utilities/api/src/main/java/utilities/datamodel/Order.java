package utilities.datamodel;

public record Order(
        Long id,
        String firstName,
        String lastName,
        String address1,
        String address2,
        String creditCardTyp,
        String creditCardNumber,
        String creditCardExpiryDate
) {}
