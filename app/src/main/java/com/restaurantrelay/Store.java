package com.restaurantrelay;

import android.content.*;
import org.json.*;

final class Store {
    static final String ANSWERING_SERVICE = "8333550861";
    static synchronized android.content.SharedPreferences prefs(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("relay", Context.MODE_PRIVATE);
        if(!p.getBoolean("departmentsConfigured",false)) {
            p.edit().putBoolean("enabled",false).putBoolean("departmentsConfigured",true).commit();
        }
        return p;
    }
    static synchronized JSONArray history(Context c) {
        try { return new JSONArray(prefs(c).getString("history", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    static synchronized void save(Context c, JSONObject row) {
        JSONArray old = history(c), next = new JSONArray(); next.put(row);
        for (int i=0; i<old.length() && next.length()<100; i++) {
            JSONObject item=old.optJSONObject(i);
            if (item!=null && !item.optString("id").equals(row.optString("id"))) next.put(item);
        }
        prefs(c).edit().putString("history", next.toString()).commit();
    }
    static synchronized JSONObject find(Context c, String id) {
        JSONArray rows=history(c);
        for(int i=0;i<rows.length();i++) if(rows.optJSONObject(i).optString("id").equals(id)) return rows.optJSONObject(i);
        return null;
    }
    static void put(JSONObject row, String key, Object value) { try { row.put(key,value); } catch(JSONException e) { throw new IllegalStateException(e); } }
}
