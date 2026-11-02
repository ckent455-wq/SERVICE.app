package com.restaurantrelay;
import org.junit.Test;
import static org.junit.Assert.*;
public class DepartmentTest {
    @Test public void commonSlimChickensTyposRouteToBrink() {
        for(String name:new String[]{"Slim's Chickens","Slim’s Chickens","Slimm Chicken","Slim Chickens","Slims Chickens","Slim Chiken","Slim Chikcens","Slmi Chickens","Slim Chckens","Slimm Chicken #102","Slim Pickens","  sLiM   PiCkEnS #102"}) {
            String body="Office/On Call\nAlex 202-555-0142\n"+name+"\nER Service\nReply with 0083";
            assertEquals(name,"BRINK",Rules.department(body));
            assertEquals(name,"brink",Rules.destinationKey(body));
            assertEquals("0083",Rules.code(body));
        }
    }
    @Test public void typoMatchingDoesNotRouteUnrelatedRestaurants() {
        for(String name:new String[]{"Slims Chickensville","Other Restaurant","Slim Kitchen","Chick-fil-A","Swim Kitchens","The Chicken Shop","Other Restaurant\nCustomer mentioned Slimm Chicken","Slim Pickensville","Other Restaurant\nCaller mentioned Slim Pickens"})
            assertEquals(name,"ALOHA",Rules.department(name+"\nReply with 83"));
    }
    @Test public void slimsChickensTypoRoutesToBrink() {
        String body="Office/On Call\nJoe 202-555-0142\nSlims Chickens\nER Service\nCard reader unavailable\nReply with 83";
        assertEquals("BRINK",Rules.department(body));
        assertEquals("brink",Rules.destinationKey(body));
        assertEquals("83",Rules.code(body));
    }
    @Test public void slimsTypoAllowsCaseSpacingAndStoreSuffix() {
        assertEquals("BRINK",Rules.department("Office/On Call\r\n  sLiMs   cHiCkEnS #102\r\nER Service"));
    }
    @Test public void typoSupportDoesNotMatchOtherNamesOrBodyMentions() {
        assertEquals("ALOHA",Rules.department("Slims Chickensville\nReply with 83"));
        assertEquals("ALOHA",Rules.department("Other Restaurant\nCaller mentioned Slims Chickens"));
    }
    @Test public void powersoulLasVegasRoutesToBrink() {
        String body="Office/On Call\nAlex 202-555-0142\nPowersoul Cafe\nER Service\n123 Example St\nLas Vegas NV\nReader unavailable\nReply with 83";
        assertEquals("BRINK",Rules.department(body)); assertEquals("brink",Rules.destinationKey(body)); assertEquals("83",Rules.code(body));
    }
    @Test public void powersoulAcceptsCaseSpacingAccentAndInlineCity() {
        assertEquals("BRINK",Rules.department("  powerSoul   cafe - LAS VEGAS\r\nReply with 42"));
        assertEquals("BRINK",Rules.department("Power Soul Café\nLas   Vegas NV"));
    }
    @Test public void powersoulRuleRequiresRestaurantHeaderAndLasVegas() {
        assertEquals("ALOHA",Rules.department("Powersoul Cafe\nReno NV"));
        assertEquals("ALOHA",Rules.department("Powersoul Cafe\nReply with 83"));
        assertEquals("ALOHA",Rules.department("Other Restaurant\nLas Vegas NV\nCaller mentioned Powersoul Cafe"));
    }
    @Test public void sampleRoutesToBrink() { String body="Office/On Call\nJoe 202-555-0142\nSlim Chickens\nER Service\nCard reader unavailable\nReply with 83"; assertEquals("BRINK",Rules.department(body)); assertEquals("brink",Rules.destinationKey(body)); assertEquals("83",Rules.code(body)); }
    @Test public void acceptsHeaderCaseSpacingAndStoreSuffix() { assertEquals("BRINK",Rules.department("Office/On Call\r\n  sLiM   cHiCkEnS #102\r\nER Service")); }
    @Test public void allOtherRestaurantsUseAloha() { assertEquals("ALOHA",Rules.department("Office/On Call\nOther Restaurant\nReply with 012")); assertEquals("aloha",Rules.destinationKey("Other Restaurant")); }
    @Test public void bodyMentionIsNotAHeader() { assertEquals("ALOHA",Rules.department("Other Restaurant\nCustomer says Slim Chickens uses this reader")); assertEquals("ALOHA",Rules.department("Slim Chickensville")); }
}
