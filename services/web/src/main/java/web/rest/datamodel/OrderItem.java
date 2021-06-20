package web.rest.datamodel;

public record OrderItem (
  long id,
  long productId,
  long orderId,
  int quantity,
  long unitPriceInCents
) {}
