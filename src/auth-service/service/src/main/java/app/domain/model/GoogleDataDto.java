package app.domain.model;

public record GoogleDataDto (
        String name,
        String surname,
        String email,
        String googleId
) {}
