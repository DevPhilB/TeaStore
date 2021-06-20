package web.rest.datamodel;

import java.util.List;

public record CartAction (
    String name,
    long productId,
    List<OrderItem> orderItemList
) {}
