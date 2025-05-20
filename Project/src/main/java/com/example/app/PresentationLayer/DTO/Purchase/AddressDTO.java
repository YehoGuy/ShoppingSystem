package com.example.app.PresentationLayer.DTO.Purchase;


public record AddressDTO(
    String country,
    String city,
    String street,
    String houseNumber,
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