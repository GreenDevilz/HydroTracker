package com.example.waterintaketracker;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManager {
    private static final String PREF_USERS = "HydroTrackUsers";
    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String MAIN_PREFS = "HydroTrackPrefs";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private Map<String, UserData> users;
    private String currentUser;

    public UserManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_USERS, Context.MODE_PRIVATE);
        loadUsers();
        this.currentUser = this.prefs.getString(KEY_CURRENT_USER, null);
    }

    private void loadUsers() {
        String json = this.prefs.getString(KEY_USER_LIST, "{}");
        Type type = new TypeToken<Map<String, UserData>>(){}.getType();
        this.users = this.gson.fromJson(json, type);
        if (this.users == null) this.users = new HashMap<>();
    }

    private void saveUsers() {
        String json = this.gson.toJson(this.users);
        this.prefs.edit().putString(KEY_USER_LIST, json).apply();
    }

    public boolean signUp(String username, String password, Context context) {
        if (this.users.containsKey(username)) return false;
        
        UserData newUser = new UserData(username, password);
        // Copy current local data to the new user if they just signed up
        newUser.localData = captureCurrentData(context);
        
        this.users.put(username, newUser);
        saveUsers();
        return login(username, password, context);
    }

    public boolean login(String username, String password, Context context) {
        UserData user = this.users.get(username);
        if (user != null && user.password.equals(password)) {
            // Save data for current user before switching
            saveCurrentUserData(context);
            
            this.currentUser = username;
            this.prefs.edit().putString(KEY_CURRENT_USER, username).apply();
            applyUserData(user, context);
            return true;
        }
        return false;
    }

    public void logout(Context context) {
        saveCurrentUserData(context);
        this.currentUser = null;
        this.prefs.edit().remove(KEY_CURRENT_USER).apply();
        
        // Clear current app data on logout
        context.getApplicationContext().getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public void deleteAccount(String username, Context context) {
        if (username.equals(this.currentUser)) {
            logout(context);
        }
        this.users.remove(username);
        saveUsers();
    }

    public List<String> getAllUsernames() {
        return new ArrayList<>(this.users.keySet());
    }

    private void saveCurrentUserData(Context context) {
        if (this.currentUser != null) {
            UserData user = this.users.get(this.currentUser);
            if (user != null) {
                user.localData = captureCurrentData(context);
                saveUsers();
            }
        }
    }

    public String getCurrentUser() {
        return this.currentUser;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureCurrentData(Context context) {
        SharedPreferences mainPrefs = context.getApplicationContext().getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE);
        return (Map<String, Object>) mainPrefs.getAll();
    }

    private void applyUserData(UserData user, Context context) {
        SharedPreferences mainPrefs = context.getApplicationContext().getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mainPrefs.edit();
        editor.clear();
        for (Map.Entry<String, Object> entry : user.localData.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) val);
            else if (val instanceof Float) editor.putFloat(entry.getKey(), (Float) val);
            else if (val instanceof Integer) editor.putInt(entry.getKey(), (Integer) val);
            else if (val instanceof Long) editor.putLong(entry.getKey(), (Long) val);
            else if (val instanceof String) editor.putString(entry.getKey(), (String) val);
        }
        editor.apply();
    }

    public long getUserTotalIntake(String username) {
        UserData user = this.users.get(username);
        if (user == null || user.localData == null) return 0;
        
        Object historyJson = user.localData.get("history_data");
        if (historyJson instanceof String) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<HistoryEntry>>() {}.getType();
            List<HistoryEntry> history = gson.fromJson((String) historyJson, type);
            if (history != null) {
                long total = 0;
                for (HistoryEntry entry : history) {
                    total += entry.getIntake();
                }
                return total;
            }
        }
        return 0;
    }

    private static class UserData {
        String username;
        String password;
        Map<String, Object> localData;

        UserData(String u, String p) {
            this.username = u;
            this.password = p;
            this.localData = new HashMap<>();
        }
    }
}
