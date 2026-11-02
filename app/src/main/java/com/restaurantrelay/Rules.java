package com.restaurantrelay;

import java.util.regex.*;

public final class Rules {
    private Rules() {}
    public static boolean canEnable(String source, String brink, String aloha) {
        return validPhone(source) && ((validPhone(brink) && !samePhone(source,brink))
                || (validPhone(aloha) && !samePhone(source,aloha)));
    }
    public static String department(String body) {
        boolean slimChickens = isSlimChickensHeader(body);
        boolean powersoulCafe = Pattern.compile("(?imu)^\\h*POWER\\h*SOUL\\h+CAF[EÉ]\\b[^\\r\\n]*$").matcher(body).find();
        boolean lasVegas = Pattern.compile("(?i)\\bLAS\\s+VEGAS\\b").matcher(body).find();
        return slimChickens || (powersoulCafe && lasVegas) ? "BRINK" : "ALOHA";
    }
    public static String destinationKey(String body) { return department(body).equals("BRINK") ? "brink" : "aloha"; }
    private static boolean isSlimChickensHeader(String body) {
        if (Pattern.compile("(?im)^\\h*SLIM\\h+PICKENS\\b[^\\r\\n]*$").matcher(body).find()) return true;
        for (String line : body.split("\\R")) {
            String[] words = line.trim().toLowerCase(java.util.Locale.ROOT).split("\\h+");
            if (words.length < 2) continue;
            String slim = words[0].replaceAll("[^a-z]", "");
            String chicken = words[1].replaceAll("[^a-z]", "");
            // Keep short-word matching tight; allow common omissions/transpositions in chicken(s).
            if (Math.abs(slim.length()-4)<=1 && Math.abs(chicken.length()-8)<=2
                    && distance(slim,"slim")<=1
                    && (distance(chicken,"chicken")<=(chicken.startsWith("c")?2:1)
                    || distance(chicken,"chickens")<=(chicken.startsWith("c")?2:1))) return true;
        }
        return false;
    }
    private static int distance(String a, String b) {
        int[][] d=new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++) d[i][0]=i;
        for(int j=0;j<=b.length();j++) d[0][j]=j;
        for(int i=1;i<=a.length();i++) for(int j=1;j<=b.length();j++) {
            d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
            if(i>1 && j>1 && a.charAt(i-1)==b.charAt(j-2) && a.charAt(i-2)==b.charAt(j-1))
                d[i][j]=Math.min(d[i][j],d[i-2][j-2]+1);
        }
        return d[a.length()][b.length()];
    }
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
