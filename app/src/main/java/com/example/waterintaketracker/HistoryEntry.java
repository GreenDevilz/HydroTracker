package com.example.waterintaketracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class HistoryEntry {
    private final String date;
    private final int goal;
    private final boolean goalMet;
    private final int intake;

    public HistoryEntry(String date, int intake, int goal) {
        this.date = date;
        this.intake = intake;
        this.goal = goal;
        this.goalMet = intake >= goal;
    }

    public String getDate() {
        return this.date;
    }

    public int getIntake() {
        return this.intake;
    }

    public int getGoal() {
        return this.goal;
    }

    public boolean isGoalMet() {
        return this.goalMet;
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateObj = sdf.parse(this.date);
            if (dateObj == null) {
                return this.date;
            }
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            return displayFormat.format(dateObj);
        } catch (Exception e) {
            return this.date;
        }
    }

    public int getPercentage() {
        if (this.goal <= 0) {
            return 0;
        }
        return Math.min(100, (int) ((this.intake / this.goal) * 100.0f));
    }

    public String toString() {
        return "HistoryEntry{date='" + this.date + "', intake=" + this.intake + ", goal=" + this.goal + ", goalMet=" + this.goalMet + '}';
    }
}
