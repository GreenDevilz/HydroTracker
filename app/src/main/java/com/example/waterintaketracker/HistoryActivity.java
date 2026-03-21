package com.example.waterintaketracker;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private LinearLayout chartContainer;
    private DataManager dataManager;
    private ListView historyListView;
    private TextView txtAverage;
    private TextView txtStreak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        this.dataManager = new DataManager(this);
        initializeViews();
        setupBackButton();
        setupClearButton();
        loadStats();
        drawChart();
        loadHistoryList();
    }

    private void initializeViews() {
        this.txtStreak = findViewById(R.id.txtStreak);
        this.txtAverage = findViewById(R.id.txtAverage);
        this.chartContainer = findViewById(R.id.chartContainer);
        this.historyListView = findViewById(R.id.historyListView);
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupClearButton() {
        ImageButton btnClear = findViewById(R.id.btnClearHistory);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> showClearConfirmationDialog());
        }
    }

    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> clearHistory())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearHistory() {
        this.dataManager.clearHistory();
        loadStats();
        drawChart();
        loadHistoryList();
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
    }

    private void loadStats() {
        int streak = this.dataManager.getCurrentStreak();
        int average = this.dataManager.getAverageIntake();
        if (this.txtStreak != null) {
            this.txtStreak.setText(String.valueOf(streak));
        }
        if (this.txtAverage != null) {
            this.txtAverage.setText(getString(R.string.format_ml, average));
        }
    }

    private void drawChart() {
        if (this.chartContainer == null) return;
        List<HistoryEntry> last7 = this.dataManager.getLast7Days();
        this.chartContainer.removeAllViews();

        if (last7.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.no_history_data);
            emptyText.setTextColor(ContextCompat.getColor(this, R.color.slate_500));
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 50, 0, 50);
            this.chartContainer.addView(emptyText);
            return;
        }

        int maxIntake = 0;
        for (HistoryEntry entry : last7) {
            if (entry.getIntake() > maxIntake) maxIntake = entry.getIntake();
        }
        if (maxIntake == 0) maxIntake = 1;

        for (HistoryEntry entry : last7) {
            LinearLayout barContainer = new LinearLayout(this);
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.setPadding(8, 0, 8, 0);
            barContainer.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);

            TextView dateText = new TextView(this);
            String day = entry.getFormattedDate();
            if (day.length() > 3) day = day.substring(0, 3);
            dateText.setText(day);
            dateText.setTextSize(12);
            dateText.setTextColor(ContextCompat.getColor(this, R.color.blue_900));
            dateText.setGravity(android.view.Gravity.CENTER);

            int maxHeightPx = 300; // Increased height for better visibility
            int barHeight = (int) ((entry.getIntake() / (float) maxIntake) * maxHeightPx);
            if (barHeight < 10) barHeight = 10;

            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, barHeight);
            params.setMargins(0, 8, 0, 8);
            bar.setLayoutParams(params);
            bar.setBackgroundColor(entry.isGoalMet() ? 
                    ContextCompat.getColor(this, android.R.color.holo_green_dark) : 
                    ContextCompat.getColor(this, android.R.color.holo_blue_dark));

            TextView intakeText = new TextView(this);
            intakeText.setText(String.format(Locale.getDefault(), "%.1fL", entry.getIntake() / 1000.0f));
            intakeText.setTextSize(10);
            intakeText.setTextColor(ContextCompat.getColor(this, R.color.blue_900));
            intakeText.setGravity(android.view.Gravity.CENTER);

            barContainer.addView(intakeText);
            barContainer.addView(bar);
            barContainer.addView(dateText);
            this.chartContainer.addView(barContainer);
        }
    }

    private void loadHistoryList() {
        if (this.historyListView == null) return;
        List<HistoryEntry> history = new ArrayList<>(this.dataManager.getHistory());
        Collections.reverse(history);
        HistoryAdapter historyAdapter = new HistoryAdapter(this, history);
        this.historyListView.setAdapter(historyAdapter);
    }
}
