package com.restaurantrelay;
import org.junit.Test;
import static org.junit.Assert.*;
public class RulesTest {
    @Test public void extractsCodeWithoutUsingCallerNumber() { assertEquals("0083",Rules.code("Alex 202-555-0142\nRestaurant\nReply with 0083")); }
    @Test public void rejectsAmbiguousOrMalformedInstructions() { assertNull(Rules.code("Reply with 83\nReply with 42")); assertNull(Rules.code("Please call 83")); assertNull(Rules.code("Reply with 83 then call")); }
    @Test public void matchesUsCountryCodeAndFormatting() { assertTrue(Rules.samePhone("+1 (202) 555-0142","2025550142")); assertFalse(Rules.samePhone("", "")); assertFalse(Rules.samePhone("2025550142","2025550143")); }
    @Test public void validatesDestinations() { assertTrue(Rules.validPhone("202-555-0142")); assertFalse(Rules.validPhone("911")); }
}
