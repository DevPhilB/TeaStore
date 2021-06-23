package web.rest.datamodel;

public record CartItem (
        long id,
        String productName,
        String productDescription,
        int quantity,
        long listPrice,
        long totalCost,
        String removeProduct
) {}
