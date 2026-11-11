package com.bujjet.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Small on-device queue of money-related notifications waiting for the app to read them. */
public class NotifStore {
    private static final String PREFS = "bujjet_notifs", QUEUE = "queue", SEEN = "seen";

    static synchronized boolean add(Context c, JSONObject o) throws JSONException {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray seen = new JSONArray(sp.getString(SEEN, "[]"));
        String id = o.getString("id");
        for (int i = 0; i < seen.length(); i++) if (id.equals(seen.optString(i))) return false;
        seen.put(id);
        while (seen.length() > 400) seen.remove(0);
        JSONArray q = new JSONArray(sp.getString(QUEUE, "[]"));
        q.put(o);
        while (q.length() > 200) q.remove(0);
        sp.edit().putString(QUEUE, q.toString()).putString(SEEN, seen.toString()).apply();
        return true;
    }

    static synchronized JSONArray take(Context c) throws JSONException {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray q = new JSONArray(sp.getString(QUEUE, "[]"));
        sp.edit().putString(QUEUE, "[]").apply();
        return q;
    }
}
