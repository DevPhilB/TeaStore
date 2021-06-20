package web.rest.datamodel;

public record User (
    long id,
    String userName,
    String password,
    String realName,
    String email
) {}
