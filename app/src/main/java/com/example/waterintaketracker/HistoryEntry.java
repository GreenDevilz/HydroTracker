package com.example.waterintaketracker;

import androidx.annotation.NonNull;

public class HistoryEntry {
    private final String date;
    private final int goal;
    private final int intake;

    public HistoryEntry(String date, int intake, int goal) {
        this.date = date;
        this.intake = intake;
        this.goal = goal;
    }

    public String getFormattedDate() {
        return this.date;
    }

    public int getIntake() {
        return this.intake;
    }

    public int getGoal() {
        return this.goal;
    }

    public int getPercentage() {
        if (this.goal == 0) return 0;
        return Math.min(100, (int) ((this.intake / (float) this.goal) * 100.0f));
    }

    public boolean isGoalMet() {
        return this.intake >= this.goal;
    }

    @NonNull
    @Override
    public String toString() {
        return "HistoryEntry{date='" + this.date + "', intake=" + this.intake + ", goal=" + this.goal + "}";
    }
}
