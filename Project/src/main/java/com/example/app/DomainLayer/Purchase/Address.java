package com.example.app.DomainLayer.Purchase;

/**
 * The {@code Address} class represents a physical real-world address in the shopping system.
 * 
 * <p>This class is implemented using the Builder Design Pattern, allowing for flexible
 * and incremental construction of an {@code Address} object. Each field can be set
 * individually using the provided "with" methods, enabling a fluent and readable API.
 * 
 * <p>Key features of the {@code Address} class:
 * <ul>
 *   <li>Encapsulates details of an address, including country, city, street, house number, apartment number, and zip code.</li>
 *   <li>Provides default values for all fields when no specific values are provided.</li>
 *   <li>Supports fluent-style method chaining for constructing an address.</li>
 *   <li>Overrides {@code toString} and {@code equals} methods for meaningful string representation and comparison.</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Address address = new Address()
 *     .withCountry("USA")
 *     .withCity("New York")
 *     .withStreet("5th Avenue")
 *     .withHouseNumber("123")
 *     .withApartmentNumber("45B")
 *     .withZipCode("10001");
 * }</pre>
 * 
 */
public class Address {
    
    protected String Country;
    protected String City;
    protected String Street;
    protected String HouseNumber;
    protected String ApartmentNumber;
    protected String ZipCode;



    /**
     * Constructs a new {@code Address} with default values for all fields.
     * 
     * <p>All fields are initialized to "N/A" to indicate that no specific values
     * have been provided.
     */
    public Address() {
        this.Country = "N/A";
        this.City = "N/A";
        this.Street = "N/A";
        this.HouseNumber = "N/A";
        this.ApartmentNumber = "N/A";
        this.ZipCode = "N/A";
    }

    /**
     * Sets the country of the address.
     * 
     * @param country the country to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withCountry(String country) {
        this.Country = country;
        return this;
    }

    /**
     * Sets the city of the address.
     * 
     * @param city the city to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withCity(String city) {
        this.City = city;
        return this;
    }

    /**
     * Sets the street of the address.
     * 
     * @param street the street to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withStreet(String street) {
        this.Street = street;
        return this;
    }

    /**
     * Sets the house number of the address.
     * 
     * @param houseNumber the house number to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withHouseNumber(String houseNumber) {
        this.HouseNumber = houseNumber;
        return this;
    }

    /**
     * Sets the apartment number of the address.
     * 
     * @param apartmentNumber the apartment number to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withApartmentNumber(int apartmentNumber) {
        this.ApartmentNumber = ""+apartmentNumber;
        return this;
    }

    /**
     * Sets the zip code of the address.
     * 
     * @param zipCode the zip code to set.
     * @return the current {@code Address} instance for method chaining.
     */
    public Address withZipCode(String zipCode) {
        this.ZipCode = zipCode;
        return this;
    }

    /**
     * Returns the street of the address.
     * 
     * @return the street.
     */
    public String getStreet() {
        return Street;
    }

    /**
     * Returns the house number of the address.
     * 
     * @return the house number.
     */
    public String getHouseNumber() {
        return HouseNumber;
    }

    /**
     * Returns the apartment number of the address.
     * 
     * @return the apartment number.
     */
    public String getApartmentNumber() {
        return ApartmentNumber;
    }

    /**
     * Returns the zip code of the address.
     * 
     * @return the zip code.
     */
    public String getZipCode() {
        return ZipCode;
    }

    /**
     * Returns the city of the address.
     * 
     * @return the city.
     */
    public String getCity() {
        return City;
    }

    /**
     * Returns the country of the address.
     * 
     * @return the country.
     */
    public String getCountry() {
        return Country;
    }

    /**
     * Returns a string representation of the address.
     * 
     * <p>The string includes all fields of the address in a readable format.
     * 
     * @return a string representation of the address.
     */
    @Override
    public String toString() {
        return "Address{" +
                "Country='" + Country + '\'' +
                ", City='" + City + '\'' +
                ", Street='" + Street + '\'' +
                ", HouseNumber='" + HouseNumber + '\'' +
                ", ApartmentNumber='" + ApartmentNumber + '\'' +
                ", ZipCode='" + ZipCode + '\'' +
                '}';
    }

    /**
     * Compares this address to another object for equality.
     * 
     * <p>Two addresses are considered equal if all their fields match.
     * 
     * @param o the object to compare to.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;

        Address address = (Address) o;

        if (!Country.equals(address.Country)) return false;
        if (!City.equals(address.City)) return false;
        if (!Street.equals(address.Street)) return false;
        if (!HouseNumber.equals(address.HouseNumber)) return false;
        if (!ApartmentNumber.equals(address.ApartmentNumber)) return false;
        return ZipCode.equals(address.ZipCode);
    }
}