package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Purchase.Address;

class AddressTests {

    private Address address;

    @BeforeEach
    void setUp() {
        address = new Address();
    }

    @Test
    void testDefaultConstructor() {
        assertEquals("N/A", address.getCountry());
        assertEquals("N/A", address.getCity());
        assertEquals("N/A", address.getStreet());
        assertEquals("N/A", address.getHouseNumber());
        assertEquals("N/A", address.getApartmentNumber());
        assertEquals("N/A", address.getZipCode());
    }

    @Test
    void testFluentSetters() {
        address.withCountry("USA")
               .withCity("New York")
               .withStreet("5th Avenue")
               .withHouseNumber("123")
               .withApartmentNumber("45B")
               .withZipCode("10001");

        assertEquals("USA", address.getCountry());
        assertEquals("New York", address.getCity());
        assertEquals("5th Avenue", address.getStreet());
        assertEquals("123", address.getHouseNumber());
        assertEquals("45B", address.getApartmentNumber());
        assertEquals("10001", address.getZipCode());
    }

    @Test
    void testEqualsTrue() {
        Address addr1 = new Address().withCountry("USA");
        Address addr2 = new Address().withCountry("USA");
        assertEquals(addr1, addr2, "Addresses with same fields should be equal");
    }

    @Test
    void testEqualsFalse() {
        Address addr1 = new Address().withCountry("USA");
        Address addr2 = new Address().withCountry("Canada");
        assertNotEquals(addr1, addr2, "Addresses with different countries should not be equal");
    }

    @Test
    void testToString() {
        address.withCountry("USA")
               .withCity("New York")
               .withStreet("5th Avenue")
               .withHouseNumber("123")
               .withApartmentNumber("45B")
               .withZipCode("10001");

        String expected = "Address{Country='USA', City='New York', Street='5th Avenue', "
                        + "HouseNumber='123', ApartmentNumber='45B', ZipCode='10001'}";
        assertEquals(expected, address.toString());
    }
}
