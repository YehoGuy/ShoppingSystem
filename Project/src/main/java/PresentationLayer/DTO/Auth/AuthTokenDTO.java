package PresentationLayer.DTO.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import java.util.Date;

/**
 
Immutable transport object representing an authentication token
in API requests / responses.*/
public record AuthTokenDTO(
        @NotBlank String token,
        @Future Date expirationTime) {

    /* -------- Domain ➜ DTO (for responses) -------- */
    public static AuthTokenDTO fromDomain(DomainLayer.AuthToken t) {
        return new AuthTokenDTO(t.getToken(), t.getExpirationTime());
    }

    /* -------- DTO ➜ Domain (rare—mostly tests or deserialisation) -------- */
    public DomainLayer.AuthToken toDomain() {
        return new DomainLayer.AuthToken(token, expirationTime);
    }
}