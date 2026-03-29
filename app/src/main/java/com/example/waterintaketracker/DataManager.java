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

    private static final String KEY_USER_FEVER = "user_fever";
    private static final String KEY_USER_EXCESSIVE_SWEATING = "user_excessive_sweating";
    private static final String KEY_USER_HIGH_URINE = "user_high_urine";
    private static final String KEY_USER_HIGH_PROTEIN = "user_high_protein";
    private static final String KEY_USER_CREATINE = "user_creatine";
    private static final String KEY_USER_RESTRICTED = "user_restricted";
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
    private static final String KEY_USER_HEIGHT = "user_height";
    private static final String KEY_USER_BODY_FAT = "user_body_fat";
    private static final String KEY_USER_ACTIVITY_DURATION = "user_activity_duration";
    private static final String KEY_USER_PREGNANT = "user_pregnant";
    private static final String KEY_USER_BREASTFEEDING = "user_breastfeeding";
    private static final int MAX_HISTORY_DAYS = 30;

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;
    private final Gson gson = new Gson();

    public DataManager(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = this.preferences.edit();
    }

    // Daily goal methods
    public void saveDailyGoal(int goal) {
        this.editor.putInt(KEY_DAILY_GOAL, goal).apply();
    }

    public int getDailyGoal() {
        return this.preferences.getInt(KEY_DAILY_GOAL, 2500);
    }

    // Current intake methods
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
            if (history.get(i).getFormattedDate().equals(today)) {
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
        updateTodayEntry(getCurrentIntake(), newGoal);
        this.editor.putInt(KEY_DAILY_GOAL, newGoal).apply();
    }

    // History methods
    private void saveSpecificDayToHistory(String date, int intake, int goal) {
        List<HistoryEntry> history = getHistory();
        boolean found = false;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getFormattedDate().equals(date)) {
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
            if (!entry.getFormattedDate().equals(today)) {
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

    // App install date methods
    public void saveAppInstallDate(long date) {
        this.editor.putLong(KEY_APP_INSTALL_DATE, date).apply();
    }

    public long getAppInstallDate() {
        return this.preferences.getLong(KEY_APP_INSTALL_DATE, 0L);
    }

    // User profile save method (all settings)
    public void saveUserProfile(float weight, int age, String gender, String activityLevel, String climate,
                                int height, float bodyFat, int activityDuration, boolean isPregnant, boolean isBreastfeeding, boolean hasFever, boolean excessiveSweating,
                                boolean highUrine, boolean highProtein, boolean creatine,
                                boolean restricted) {
        this.editor.putFloat(KEY_USER_WEIGHT, weight);
        this.editor.putInt(KEY_USER_AGE, age);
        this.editor.putString(KEY_USER_GENDER, gender);
        this.editor.putString(KEY_ACTIVITY_LEVEL, activityLevel);
        this.editor.putString(KEY_CLIMATE, climate);
        this.editor.putInt(KEY_USER_HEIGHT, height);
        this.editor.putFloat(KEY_USER_BODY_FAT, bodyFat);
        this.editor.putInt(KEY_USER_ACTIVITY_DURATION, activityDuration);
        this.editor.putBoolean(KEY_USER_FEVER, hasFever);
        this.editor.putBoolean(KEY_USER_EXCESSIVE_SWEATING, excessiveSweating);
        this.editor.putBoolean(KEY_USER_HIGH_URINE, highUrine);
        this.editor.putBoolean(KEY_USER_HIGH_PROTEIN, highProtein);
        this.editor.putBoolean(KEY_USER_CREATINE, creatine);
        this.editor.putBoolean(KEY_USER_RESTRICTED, restricted);

        // Only save pregnancy/breastfeeding states for females
        if (gender.equals("Female")) {
            this.editor.putBoolean(KEY_USER_PREGNANT, isPregnant);
            this.editor.putBoolean(KEY_USER_BREASTFEEDING, isBreastfeeding);
        } else {
            this.editor.putBoolean(KEY_USER_PREGNANT, false);
            this.editor.putBoolean(KEY_USER_BREASTFEEDING, false);
        }
        this.editor.apply();
    }

    // User profile getter methods

    public boolean hasFever() { return this.preferences.getBoolean(KEY_USER_FEVER, false); }
    public boolean hasExcessiveSweating() { return this.preferences.getBoolean(KEY_USER_EXCESSIVE_SWEATING, false); }
    public boolean hasHighUrine() { return this.preferences.getBoolean(KEY_USER_HIGH_URINE, false); }
    public boolean hasHighProtein() { return this.preferences.getBoolean(KEY_USER_HIGH_PROTEIN, false); }
    public boolean hasCreatine() { return this.preferences.getBoolean(KEY_USER_CREATINE, false); }
    public boolean hasRestricted() { return this.preferences.getBoolean(KEY_USER_RESTRICTED, false); }
    public float getUserWeight() {
        return this.preferences.getFloat(KEY_USER_WEIGHT, 70.0f);
    }

    public int getUserAge() {
        return this.preferences.getInt(KEY_USER_AGE, 30);
    }

    public String getUserGender() {
        return this.preferences.getString(KEY_USER_GENDER, "Male");
    }

    public String getActivityLevel() {
        return this.preferences.getString(KEY_ACTIVITY_LEVEL, "Moderate");
    }

    public String getClimate() {
        return this.preferences.getString(KEY_CLIMATE, "Moderate");
    }

    public int getUserHeight() {
        return this.preferences.getInt(KEY_USER_HEIGHT, 0);
    }

    public float getUserBodyFat() {
        return this.preferences.getFloat(KEY_USER_BODY_FAT, 0f);
    }

    public int getUserActivityDuration() {
        return this.preferences.getInt(KEY_USER_ACTIVITY_DURATION, 0);
    }

    // Pregnancy and breastfeeding getter methods
    public boolean isUserPregnant() {
        return this.preferences.getBoolean(KEY_USER_PREGNANT, false);
    }

    public boolean isUserBreastfeeding() {
        return this.preferences.getBoolean(KEY_USER_BREASTFEEDING, false);
    }

    public boolean hasUserProfile() {
        return this.preferences.contains(KEY_USER_WEIGHT);
    }

    // Helper method
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void clearHistory() {
        this.editor.remove(KEY_HISTORY).apply();
    }
}
