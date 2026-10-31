package com.restaurantrelay;

import java.util.regex.*;

public final class Rules {
    private Rules() {}
    public static boolean canEnable(String source, String brink, String aloha) {
        return validPhone(source) && ((validPhone(brink) && !samePhone(source,brink))
                || (validPhone(aloha) && !samePhone(source,aloha)));
    }
    public static String department(String body) {
        boolean slimChickens = Pattern.compile("(?im)^\\h*SLIM\\h+CHICKENS\\b[^\\r\\n]*$").matcher(body).find();
        boolean powersoulCafe = Pattern.compile("(?imu)^\\h*POWER\\h*SOUL\\h+CAF[EÉ]\\b[^\\r\\n]*$").matcher(body).find();
        boolean lasVegas = Pattern.compile("(?i)\\bLAS\\s+VEGAS\\b").matcher(body).find();
        return slimChickens || (powersoulCafe && lasVegas) ? "BRINK" : "ALOHA";
    }
    public static String destinationKey(String body) { return department(body).equals("BRINK") ? "brink" : "aloha"; }
    public static String phone(String input) {
        String n = input == null ? "" : input.replaceAll("[^0-9]", "");
        if (n.length() == 11 && n.startsWith("1")) n = n.substring(1);
        return n;
    }
    public static boolean validPhone(String input) { return phone(input).matches("[2-9][0-9]{9}"); }
    public static boolean samePhone(String a, String b) { return !phone(a).isEmpty() && phone(a).equals(phone(b)); }
    public static String code(String body) {
        Matcher m = Pattern.compile("(?im)^\\s*Reply\\s+with\\s+(\\d{1,12})\\s*[.!]?\\s*$").matcher(body);
        String code = null;
        while (m.find()) { if (code != null) return null; code = m.group(1); }
        return code;
    }
}
