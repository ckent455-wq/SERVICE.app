package com.restaurantrelay;
import org.junit.Test;
import static org.junit.Assert.*;
public class PartialSetupTest {
    @Test public void brinkAloneCanEnable() { assertTrue(Rules.canEnable("2025550100","2025550101","")); }
    @Test public void alohaAloneCanEnable() { assertTrue(Rules.canEnable("2025550100","","2025550102")); }
    @Test public void missingOrLoopingDestinationsCannotEnable() { assertFalse(Rules.canEnable("2025550100","","")); assertFalse(Rules.canEnable("2025550100","2025550100","")); }
    @Test public void unconfiguredAlohaDoesNotChangeBrinkRouting() { assertEquals("brink",Rules.destinationKey("Slim Chickens\nReply with 83")); assertEquals("aloha",Rules.destinationKey("Other Restaurant\nReply with 84")); assertFalse(Rules.validPhone("")); }
}
