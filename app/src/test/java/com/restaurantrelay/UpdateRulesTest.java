package com.restaurantrelay;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateRulesTest {
    private static final String HASH="a".repeat(64);
    @Test public void comparesVersionsNumericallyAndRejectsDowngrades() {
        assertTrue(UpdateRules.newer("v1.0.10","1.0.9")); assertTrue(UpdateRules.newer("2.0.0","1.99.99"));
        assertFalse(UpdateRules.newer("1.0.5","1.0.5")); assertFalse(UpdateRules.newer("1.0.4","1.0.5"));
    }
    @Test public void rejectsPrereleaseOrMalformedVersion() {
        for(String value:new String[]{"v1.0.6-beta","latest","1.0","1.0.0/evil","999999999.0.0"}) {
            assertThrows(IllegalArgumentException.class,()->UpdateRules.version(value));
        }
    }
    @Test public void selectsOnlyExpectedRepositoryAsset() {
        String name=UpdateRules.apkName("v1.0.6"); assertEquals("SERVICE.app-v1.0.6.apk",name);
        assertTrue(UpdateRules.releaseAssetUrl(UpdateRules.REPOSITORY+"/releases/download/v1.0.6/"+name,"v1.0.6",name));
        assertFalse(UpdateRules.releaseAssetUrl("https://example.org/"+name,"v1.0.6",name));
        assertFalse(UpdateRules.releaseAssetUrl(UpdateRules.REPOSITORY+"/releases/download/v1.0.4/"+name,"v1.0.6",name));
    }
    @Test public void redirectsMustStayOnHttpsGithubInfrastructure() {
        assertTrue(UpdateRules.trustedDownloadUrl("https://release-assets.githubusercontent.com/file?signature=123"));
        for(String url:new String[]{"http://github.com/a","https://github.com.evil.org/a","https://github.com@evil.org/a","file:///a","https://example.org/a","https://github.com:8080/a"}) assertFalse(url,UpdateRules.trustedDownloadUrl(url));
    }
    @Test public void readsOnlyTheMatchingApkChecksum() {
        assertEquals(HASH,UpdateRules.checksum("b".repeat(64)+"  other.apk\n"+HASH+"  SERVICE.app-v1.0.6.apk\n","SERVICE.app-v1.0.6.apk"));
        assertEquals(HASH,UpdateRules.digest("sha256:"+HASH.toUpperCase()));
        assertThrows(IllegalArgumentException.class,()->UpdateRules.checksum(HASH+"  other.apk","target.apk"));
        assertThrows(IllegalArgumentException.class,()->UpdateRules.checksum(HASH+"  a.apk\n"+HASH+"  a.apk","a.apk"));
        assertThrows(IllegalArgumentException.class,()->UpdateRules.digest("abc"));
    }
    @Test public void requiresSamePackageNewerCodeAndExpectedVersion() {
        assertTrue(UpdateRules.matchesIdentity("com.restaurantrelay",11,10,"1.0.6","1.0.6"));
        assertFalse(UpdateRules.matchesIdentity("com.other",11,10,"1.0.6","1.0.6"));
        assertFalse(UpdateRules.matchesIdentity("com.restaurantrelay",10,10,"1.0.6","1.0.6"));
        assertFalse(UpdateRules.matchesIdentity("com.restaurantrelay",9,10,"1.0.6","1.0.6"));
        assertFalse(UpdateRules.matchesIdentity("com.restaurantrelay",11,10,"1.0.6","1.0.7"));
    }
}
