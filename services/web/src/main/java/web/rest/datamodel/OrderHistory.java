package web.rest.datamodel;

public record OrderHistory(
        long id,
        String time,
        long price,
        String addressName,
        String address
) {}
