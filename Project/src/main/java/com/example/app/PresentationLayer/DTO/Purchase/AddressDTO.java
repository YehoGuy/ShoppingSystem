package com.example.app.PresentationLayer.DTO.Purchase;


import jakarta.validation.constraints.NotBlank;

/** Immutable representation of a postal address used in JSON payloads. **/
public record AddressDTO(
        @NotBlank String country,
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String houseNumber,
        String apartmentNumber,
        String zipCode) {

    public static AddressDTO fromDomain(com.example.app.DomainLayer.Purchase.Address a) {
        return new AddressDTO(
                a.getCountry(),
                a.getCity(),
                a.getStreet(),
                a.getHouseNumber(),
                a.getApartmentNumber(),
                a.getZipCode());
    }

    /*  DTO → Domain */
    public com.example.app.DomainLayer.Purchase.Address toDomain() {
        return new com.example.app.DomainLayer.Purchase.Address()
                .withCountry(country)
                .withCity(city)
                .withStreet(street)
                .withHouseNumber(houseNumber)
                .withApartmentNumber(apartmentNumber == null ? 0 : Integer.parseInt(apartmentNumber))
                .withZipCode(zipCode);
    }
}