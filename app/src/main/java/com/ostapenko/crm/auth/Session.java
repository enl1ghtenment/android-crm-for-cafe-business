package com.ostapenko.crm.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {
    private static final String PREF = "crm_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "role";
    private final SharedPreferences sp;

    public Session(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void login(int userId, String role) {
        sp.edit().putInt(KEY_USER_ID, userId).putString(KEY_ROLE, role).apply();
    }

    public void logout() {
        sp.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return sp.contains(KEY_USER_ID);
    }

    public String role() {
        return sp.getString(KEY_ROLE, "");
    }

    public int userId() {
        return sp.getInt(KEY_USER_ID, -1);
    }
}
