package com.example.app.PresentationLayer.DTO.Purchase;
import jakarta.validation.constraints.NotBlank;

public record AddressDTO(
    @NotBlank String country,
    @NotBlank String city,
    @NotBlank String street,
    @NotBlank String houseNumber,
    String apartmentNumber,
    String zipCode
) {

    /* -------- Domain → DTO -------- */
    public static AddressDTO fromDomain(com.example.app.DomainLayer.Purchase.Address a) {
        return new AddressDTO(
            a.getCountry(),
            a.getCity(),
            a.getStreet(),
            a.getHouseNumber(),
            a.getApartmentNumber(),
            a.getZipCode()
        );
    }
}