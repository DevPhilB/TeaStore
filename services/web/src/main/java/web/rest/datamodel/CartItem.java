package web.rest.datamodel;

public record CartItem (
        long itemId,
        String productName,
        String productDescription,
        int quantity,
        long listPrice,
        long totalCost,
        String removeProduct
) {}
