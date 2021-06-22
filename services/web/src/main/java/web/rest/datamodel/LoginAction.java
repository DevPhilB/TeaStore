package web.rest.datamodel;

public record LoginAction(
        String name,
        String username,
        String password
) {}

