package com.example.waterintaketracker;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "water_reminder_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "REMINDER".equals(intent.getAction())) {
            showNotification(context);
        } else if (intent != null && "SNOOZE".equals(intent.getAction())) {
            // Snooze for 30 minutes
            scheduleSnooze(context);
        }
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Create notification channel (only once)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Water Intake Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Channel for water intake reminders");
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);

        // Get custom message settings
        boolean useCustom = ReminderScheduler.useCustomMessage(context);
        String customMsg = ReminderScheduler.getCustomMessage(context);
        String message;

        if (useCustom && !customMsg.isEmpty()) {
            message = customMsg;
            // Add emoji to custom messages if not already there
            if (!message.startsWith("💧") && !message.startsWith("💪")) {
                message = "💧 " + message;
            }
        } else {
            message = context.getString(R.string.reminder_default);
        }

        String title = context.getString(R.string.reminder_notification_title);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Snooze intent
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                snoozeIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water_drop)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_revert, "Snooze 30m", snoozePendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\n💪 Stay hydrated!"));

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void scheduleSnooze(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent reminderIntent = new Intent(context, NotificationReceiver.class);
        reminderIntent.setAction("REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 30 * 60 * 1000; // 30 minutes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
