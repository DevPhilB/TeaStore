package web.rest.datamodel;

public record User (
        Long id,
        String userName,
        String password,
        String realName,
        String email
) {}
