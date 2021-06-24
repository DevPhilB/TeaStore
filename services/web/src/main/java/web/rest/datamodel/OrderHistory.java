package web.rest.datamodel;

public record OrderHistory(
        Long id,
        String time,
        Long price,
        String addressName,
        String address
) {}
