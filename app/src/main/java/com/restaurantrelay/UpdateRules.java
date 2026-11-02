package com.restaurantrelay;

import java.net.URI;
import java.util.Locale;

/** Pure validation shared by the updater and its unit tests. */
public final class UpdateRules {
    private UpdateRules() {}
    public static final String REPOSITORY = "https://github.com/ckent455-wq/SERVICE.app";
    public static String version(String tag) {
        String v=tag.startsWith("v")?tag.substring(1):tag;
        if(!v.matches("[0-9]{1,8}\\.[0-9]{1,8}\\.[0-9]{1,8}")) throw new IllegalArgumentException("Unsupported release version");
        return v;
    }
    public static boolean newer(String candidate,String installed) {
        String[] a=version(candidate).split("\\."),b=version(installed).split("\\.");
        for(int i=0;i<3;i++) { int result=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i])); if(result!=0)return result>0; }
        return false;
    }
    public static String apkName(String tag) { return "SERVICE.app-v"+version(tag)+".apk"; }
    public static boolean releaseAssetUrl(String url,String tag,String filename) {
        return url.equals(REPOSITORY+"/releases/download/"+tag+"/"+filename);
    }
    public static boolean trustedDownloadUrl(String url) {
        try {
            URI uri=URI.create(url); String host=uri.getHost();
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo()==null
                && (uri.getPort()==-1 || uri.getPort()==443) && host!=null
                && (host.equals("github.com") || host.equals("api.github.com") || host.endsWith(".githubusercontent.com"));
        } catch(IllegalArgumentException e) { return false; }
    }
    public static String digest(String value) {
        if(value==null) throw new IllegalArgumentException("Release checksum is missing");
        String hash=value.startsWith("sha256:")?value.substring(7):value;
        if(!hash.matches("(?i)[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid release checksum");
        return hash.toLowerCase(Locale.ROOT);
    }
    public static String checksum(String manifest,String filename) {
        String found=null;
        for(String line:manifest.split("\\R")) {
            String[] fields=line.trim().split("\\s+",2);
            if(fields.length==2 && fields[1].replaceFirst("^\\*", "").equals(filename)) {
                if(found!=null) throw new IllegalArgumentException("Duplicate APK checksum");
                found=digest(fields[0]);
            }
        }
        if(found==null) throw new IllegalArgumentException("No checksum for this APK");
        return found;
    }
    public static boolean matchesIdentity(String packageName,long candidate,long installed,String expectedVersion,String actualVersion) {
        return "com.restaurantrelay".equals(packageName) && candidate>installed && expectedVersion.equals(actualVersion);
    }
}
