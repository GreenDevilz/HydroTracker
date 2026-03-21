package com.example.waterintaketracker;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataManager {
    private static final String PREF_NAME = "HydroTrackPrefs";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final String KEY_CURRENT_INTAKE = "current_intake";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_HISTORY = "history_data";
    private static final String KEY_APP_INSTALL_DATE = "app_install_date";
    private static final String KEY_USER_WEIGHT = "user_weight";
    private static final String KEY_USER_AGE = "user_age";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_ACTIVITY_LEVEL = "activity_level";
    private static final String KEY_CLIMATE = "climate";
    private static final int MAX_HISTORY_DAYS = 30;

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;
    private final Gson gson = new Gson();

    public DataManager(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = this.preferences.edit();
    }

    public void saveDailyGoal(int goal) {
        this.editor.putInt(KEY_DAILY_GOAL, goal).apply();
    }

    public int getDailyGoal() {
        return this.preferences.getInt(KEY_DAILY_GOAL, 2500);
    }

    public int getCurrentIntake() {
        String today = getTodayDate();
        String lastDate = this.preferences.getString(KEY_LAST_DATE, "");
        if (today.equals(lastDate)) {
            return this.preferences.getInt(KEY_CURRENT_INTAKE, 0);
        }
        return 0;
    }

    public void saveCurrentIntake(int intake) {
        String today = getTodayDate();
        String lastDate = this.preferences.getString(KEY_LAST_DATE, "");
        int currentGoal = getDailyGoal();

        if (!today.equals(lastDate)) {
            int yesterdayIntake = this.preferences.getInt(KEY_CURRENT_INTAKE, 0);
            int yesterdayGoal = this.preferences.getInt(KEY_DAILY_GOAL, 2500);
            if (yesterdayIntake > 0 && !lastDate.isEmpty()) {
                saveSpecificDayToHistory(lastDate, yesterdayIntake, yesterdayGoal);
            }
            this.editor.putInt(KEY_CURRENT_INTAKE, intake);
            this.editor.putString(KEY_LAST_DATE, today);
        } else {
            this.editor.putInt(KEY_CURRENT_INTAKE, intake);
            updateTodayEntry(intake, currentGoal);
        }
        this.editor.apply();
    }

    private void updateTodayEntry(int intake, int goal) {
        String today = getTodayDate();
        List<HistoryEntry> history = getHistory();
        boolean found = false;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getDate().equals(today)) {
                history.set(i, new HistoryEntry(today, intake, goal));
                found = true;
                break;
            }
        }
        if (!found) {
            history.add(new HistoryEntry(today, intake, goal));
        }
        saveHistory(history);
    }

    public void onGoalChanged(int newGoal) {
        String today = getTodayDate();
        int currentIntake = getCurrentIntake();
        updateTodayEntry(currentIntake, newGoal);
        this.editor.putInt(KEY_DAILY_GOAL, newGoal).apply();
    }

    private void saveSpecificDayToHistory(String date, int intake, int goal) {
        List<HistoryEntry> history = getHistory();
        boolean found = false;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getDate().equals(date)) {
                history.set(i, new HistoryEntry(date, intake, goal));
                found = true;
                break;
            }
        }
        if (!found) {
            history.add(new HistoryEntry(date, intake, goal));
        }
        while (history.size() > MAX_HISTORY_DAYS) {
            history.remove(0);
        }
        saveHistory(history);
    }

    private void saveHistory(List<HistoryEntry> history) {
        String json = this.gson.toJson(history);
        this.editor.putString(KEY_HISTORY, json).apply();
    }

    public List<HistoryEntry> getHistory() {
        String json = this.preferences.getString(KEY_HISTORY, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<HistoryEntry>>() {}.getType();
        return this.gson.fromJson(json, type);
    }

    public List<HistoryEntry> getLast7Days() {
        List<HistoryEntry> all = getHistory();
        int size = all.size();
        if (size <= 7) return all;
        return all.subList(size - 7, size);
    }

    public int getCurrentStreak() {
        List<HistoryEntry> history = getHistory();
        String today = getTodayDate();
        int streak = 0;
        
        int currentIntake = getCurrentIntake();
        int currentGoal = getDailyGoal();
        boolean todayMet = currentIntake >= currentGoal;

        if (todayMet) {
            streak = 1;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryEntry entry = history.get(i);
            if (!entry.getDate().equals(today)) {
                if (entry.isGoalMet()) {
                    streak++;
                } else {
                    break;
                }
            }
        }
        return streak;
    }

    public int getAverageIntake() {
        List<HistoryEntry> last7 = getLast7Days();
        if (last7.isEmpty()) return 0;
        int total = 0;
        for (HistoryEntry entry : last7) {
            total += entry.getIntake();
        }
        return total / last7.size();
    }

    public void saveAppInstallDate(long date) {
        this.editor.putLong(KEY_APP_INSTALL_DATE, date).apply();
    }

    public long getAppInstallDate() {
        return this.preferences.getLong(KEY_APP_INSTALL_DATE, 0L);
    }

    public void saveUserProfile(float weight, int age, String gender, String activityLevel, String climate) {
        this.editor.putFloat(KEY_USER_WEIGHT, weight);
        this.editor.putInt(KEY_USER_AGE, age);
        this.editor.putString(KEY_USER_GENDER, gender);
        this.editor.putString(KEY_ACTIVITY_LEVEL, activityLevel);
        this.editor.putString(KEY_CLIMATE, climate);
        this.editor.apply();
    }

    public float getUserWeight() { return this.preferences.getFloat(KEY_USER_WEIGHT, 70.0f); }
    public int getUserAge() { return this.preferences.getInt(KEY_USER_AGE, 30); }
    public String getUserGender() { return this.preferences.getString(KEY_USER_GENDER, "Male"); }
    public String getActivityLevel() { return this.preferences.getString(KEY_ACTIVITY_LEVEL, "Moderate"); }
    public String getClimate() { return this.preferences.getString(KEY_CLIMATE, "Moderate"); }
    public boolean hasUserProfile() { return this.preferences.contains(KEY_USER_WEIGHT); }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void clearHistory() {
        this.editor.remove(KEY_HISTORY).apply();
    }
}
