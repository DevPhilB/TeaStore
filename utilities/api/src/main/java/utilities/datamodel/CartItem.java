package utilities.datamodel;

public record CartItem (
        Long id,
        String productName,
        String productDescription,
        Integer quantity,
        Long listPrice,
        Long totalCost,
        String removeProduct
) {}
