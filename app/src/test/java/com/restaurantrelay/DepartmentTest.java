package com.restaurantrelay;
import org.junit.Test;
import static org.junit.Assert.*;
public class DepartmentTest {
    @Test public void sampleRoutesToBrink() { String body="Office/On Call\nJoe 202-555-0142\nSlim Chickens\nER Service\nCard reader unavailable\nReply with 83"; assertEquals("BRINK",Rules.department(body)); assertEquals("brink",Rules.destinationKey(body)); assertEquals("83",Rules.code(body)); }
    @Test public void acceptsHeaderCaseSpacingAndStoreSuffix() { assertEquals("BRINK",Rules.department("Office/On Call\r\n  sLiM   cHiCkEnS #102\r\nER Service")); }
    @Test public void allOtherRestaurantsUseAloha() { assertEquals("ALOHA",Rules.department("Office/On Call\nOther Restaurant\nReply with 012")); assertEquals("aloha",Rules.destinationKey("Other Restaurant")); }
    @Test public void bodyMentionIsNotAHeader() { assertEquals("ALOHA",Rules.department("Other Restaurant\nCustomer says Slim Chickens uses this reader")); assertEquals("ALOHA",Rules.department("Slim Chickensville")); }
}
