package utilities.datamodel;

import java.util.List;

public record SessionData (
        Long userId,
        String sessionId,
        String token,
        Order order,
        List<CartItem> cartItemList,
        String message
) {}
