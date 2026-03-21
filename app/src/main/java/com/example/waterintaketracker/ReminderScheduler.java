package com.example.waterintaketracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

public class ReminderScheduler {
    private static final String PREF_REMINDER = "reminder_prefs";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_INTERVAL = "reminder_interval";
    private static final String KEY_REMINDER_START_HOUR = "reminder_start_hour";
    private static final String KEY_REMINDER_END_HOUR = "reminder_end_hour";
    
    private static final int DEFAULT_INTERVAL = 120;
    private static final int DEFAULT_START_HOUR = 8;
    private static final int DEFAULT_END_HOUR = 22;

    public static void scheduleReminders(Context context) {
        if (!isReminderEnabled(context)) {
            cancelAllReminders(context);
            return;
        }

        int interval = getReminderInterval(context);
        int startHour = getStartHour(context);
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleRepeatingReminder(context, calendar.getTimeInMillis(), interval);
    }

    private static void scheduleRepeatingReminder(Context context, long triggerTime, int intervalMinutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("REMINDER");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalMillis = intervalMinutes * 60L * 1000L;
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, intervalMillis, pendingIntent);
        }
    }

    public static void cancelAllReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("REMINDER");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static boolean isReminderEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public static void setReminderEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
        if (enabled) {
            scheduleReminders(context);
        } else {
            cancelAllReminders(context);
        }
    }

    public static int getReminderInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REMINDER_INTERVAL, DEFAULT_INTERVAL);
    }

    public static void setReminderInterval(Context context, int minutes) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_REMINDER_INTERVAL, minutes).apply();
        if (isReminderEnabled(context)) {
            scheduleReminders(context);
        }
    }

    public static int getStartHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REMINDER_START_HOUR, DEFAULT_START_HOUR);
    }

    public static void setStartHour(Context context, int hour) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_REMINDER_START_HOUR, hour).apply();
        if (isReminderEnabled(context)) {
            scheduleReminders(context);
        }
    }

    public static int getEndHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REMINDER_END_HOUR, DEFAULT_END_HOUR);
    }

    public static void setEndHour(Context context, int hour) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_REMINDER, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_REMINDER_END_HOUR, hour).apply();
        if (isReminderEnabled(context)) {
            scheduleReminders(context);
        }
    }
}
