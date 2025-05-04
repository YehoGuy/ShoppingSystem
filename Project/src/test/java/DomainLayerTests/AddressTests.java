package DomainLayerTests;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import DomainLayer.Purchase.Address;

/**
 * Unit tests for {@link Address}.
 * Uses JUnit 5 (Jupiter) and Mockito-core 5.x.
 *
 * ✱  Add to your build:
 *      testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
 *      testImplementation("org.mockito:mockito-core:5.11.0")
 */
@DisplayName("Address – unit tests")
class AddressTests {

    /* ─────────────────────────────────────────
     *  Basic construction & default state
     * ───────────────────────────────────────── */
    @Test
    @DisplayName("Default constructor initialises all fields to \"N/A\"")
    void defaultConstructorInitialisesNA() {
        Address address = new Address();

        assertAll("defaults",
            () -> assertEquals("N/A", address.getCountry(),   "country"),
            () -> assertEquals("N/A", address.getCity(),      "city"),
            () -> assertEquals("N/A", address.getStreet(),    "street"),
            () -> assertEquals("N/A", address.getHouseNumber(),"house"),
            () -> assertEquals("N/A", address.getApartmentNumber(),"apartment"),
            () -> assertEquals("N/A", address.getZipCode(),   "zip")
        );
    }

    /* ─────────────────────────────────────────
     *  Fluent builder behaviour
     * ───────────────────────────────────────── */
    @Nested
    @DisplayName("Builder (withX) methods")
    class BuilderBehaviour {

        private static Stream<TestVector> values() {
            return Stream.of(
                new TestVector("Israel", "Tel-Aviv", "Rothschild",  "10", 3,  "6800000"),
                new TestVector("USA",    "New York", "5th Avenue",  "123",45, "10001"),
                new TestVector("Japan",  "Tokyo",    "Shibuya St.", "7",  12, "150-0002")
            );
        }

        @ParameterizedTest(name = "#{index} – {0}")
        @MethodSource("values")
        @DisplayName("set the expected field values and keep object identity")
        void withMethodsSetValuesAndReturnSelf(TestVector tv) {
            Address address = new Address();

            Address sameInstance = address
                .withCountry(tv.country)
                .withCity(tv.city)
                .withStreet(tv.street)
                .withHouseNumber(tv.houseNo)
                .withApartmentNumber(tv.aptNo)   // pass int directly
                .withZipCode(tv.zip);

            assertSame(address, sameInstance, "each withX should return same instance");

            assertAll("getters",
                () -> assertEquals(tv.country,          address.getCountry()),
                () -> assertEquals(tv.city,             address.getCity()),
                () -> assertEquals(tv.street,           address.getStreet()),
                () -> assertEquals(tv.houseNo,          address.getHouseNumber()),
                () -> assertEquals(String.valueOf(tv.aptNo), address.getApartmentNumber()),
                () -> assertEquals(tv.zip,              address.getZipCode())
            );
        }

        /** Simple container for parameterised data */
        private record TestVector(
            String country, String city, String street,
            String houseNo, int aptNo, String zip
        ) {
            @Override public String toString() { return country + '/' + city; }
        }
    }


    /* ─────────────────────────────────────────
     *  toString() contains every field
     * ───────────────────────────────────────── */
    @Test
    @DisplayName("toString outputs every field")
    void toStringContainsAllFields() {
        Address address = new Address()
                .withCountry("IL")
                .withCity("TLV")
                .withStreet("Herzl")
                .withHouseNumber("1")
                .withApartmentNumber(2)
                .withZipCode("999");

        String s = address.toString();
        assertAll("toString",
            () -> assertTrue(s.contains("IL")),
            () -> assertTrue(s.contains("TLV")),
            () -> assertTrue(s.contains("Herzl")),
            () -> assertTrue(s.contains("1")),
            () -> assertTrue(s.contains("2")),
            () -> assertTrue(s.contains("999"))
        );
    }

    /* ─────────────────────────────────────────
     *  equals() – full contract
     * ───────────────────────────────────────── */
    @Nested
    @DisplayName("equals contract")
    class EqualsContract {

        Address base   = new Address()
                .withCountry("IL")
                .withCity("Haifa")
                .withStreet("Ha-Namal")
                .withHouseNumber("15")
                .withApartmentNumber(8)
                .withZipCode("3303125");

        Address same   = new Address()
                .withCountry("IL")
                .withCity("Haifa")
                .withStreet("Ha-Namal")
                .withHouseNumber("15")
                .withApartmentNumber(8)
                .withZipCode("3303125");

        Address diff   = new Address()
                .withCountry("IL")
                .withCity("Tel-Aviv")
                .withStreet("Herzl")
                .withHouseNumber("1")
                .withApartmentNumber(1)
                .withZipCode("61234");

        @Test @DisplayName("reflexive")
        void reflexive() { assertEquals(base, base); }

        @Test @DisplayName("symmetric")
        void symmetric() {
            assertEquals(base, same);
            assertEquals(same, base);
        }

        @Test @DisplayName("transitive")
        void transitive() {
            Address copy = new Address()
                .withCountry("IL").withCity("Haifa").withStreet("Ha-Namal")
                .withHouseNumber("15").withApartmentNumber(8).withZipCode("3303125");

            assertEquals(base, same);
            assertEquals(same, copy);
            assertEquals(base, copy);
        }

        @Test @DisplayName("consistent")
        void consistent() {
            for (int i = 0; i < 5; i++) {
                assertEquals(base, same);
                assertNotEquals(base, diff);
            }
        }

        @Test @DisplayName("null returns false")
        void nullSafe() { assertNotEquals(base, null); }
    }

    /* ─────────────────────────────────────────
     *  Mockito integration demo (spy)
     * ───────────────────────────────────────── */
    @Test
    @DisplayName("Mockito spy – verify builder invocations")
    void spyBuilderCalls() {
        Address spy = spy(new Address());

        spy.withCountry("IL").withCity("Beer-Sheva");

        /* Verify the builder methods were actually invoked with given arguments */
        verify(spy).withCountry("IL");
        verify(spy).withCity("Beer-Sheva");

        /* Underlying state changed as expected */
        assertEquals("Beer-Sheva", spy.getCity());
    }
}
