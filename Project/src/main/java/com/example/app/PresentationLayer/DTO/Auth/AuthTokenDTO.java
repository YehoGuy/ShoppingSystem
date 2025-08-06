package com.example.app.PresentationLayer.DTO.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import java.util.Date;

/**
 
Immutable transport object representing an authentication token
in API requests / responses.*/
public record AuthTokenDTO(
        @NotBlank String token,
        @Future Date expirationTime,
        @NotBlank Integer userId) {

    /* -------- Domain ➜ DTO (for responses) -------- */
    public static AuthTokenDTO fromDomain(com.example.app.DomainLayer.AuthToken t) {
        return new AuthTokenDTO(t.getToken(), t.getExpirationTime(), t.getUserId());
    }

    /* -------- DTO ➜ Domain (rare—mostly tests or deserialisation) -------- */
    public com.example.app.DomainLayer.AuthToken toDomain() {
        return new com.example.app.DomainLayer.AuthToken(token, expirationTime, userId);
    }
}