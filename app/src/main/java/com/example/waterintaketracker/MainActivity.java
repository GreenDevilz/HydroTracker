package com.example.waterintaketracker;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private static final int FACTS_PER_DAY = 5;
    private static final long FACT_ROTATION_INTERVAL = 30000;
    private static final int MAX_UNDO_STACK = 10;
    private static final int PRESS_AMOUNT_ML = 50;
    private static final int PRESS_INTERVAL_MS = 150;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private String[] allFacts;
    private Button btnAddLarge;
    private Button btnAddMedium;
    private Button btnAddSmall;
    private Button btnHistory;
    private Button btnSettings;
    private Button btnUndo;
    private TextView currentIntakeText;
    private Toast currentToast;
    private TextView dailyGoalText;
    private DataManager dataManager;
    private Runnable factRunnable;
    private Runnable hideMessageRunnable;
    private View horizontalProgress;
    private TextView percentageText;
    private Runnable pressRunnable;
    private FrameLayout progressBarContainer;
    private TextView txtEncouragement;
    private WaterBottleView waterBottleView;

    private final List<String> todaysFacts = new ArrayList<>();
    private int currentFactIndex = 0;
    private int dayCounter = 1;
    private final Handler factHandler = new Handler(Looper.getMainLooper());
    private final Handler pressHandler = new Handler(Looper.getMainLooper());
    private boolean isPressing = false;
    private int currentIntake = 0;
    private int dailyGoal = 2500;
    private final Handler messageHandler = new Handler(Looper.getMainLooper());
    private int lastMessageThreshold = -1;
    private boolean isUndoing = false;
    private final Stack<Integer> lastAdditions = new Stack<>();

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data;
                if (result.getResultCode() == RESULT_OK && (data = result.getData()) != null) {
                    int newGoal = data.getIntExtra("DAILY_GOAL", this.dailyGoal);
                    this.dailyGoal = newGoal;
                    this.dataManager.saveDailyGoal(this.dailyGoal);
                    this.dataManager.onGoalChanged(newGoal);
                    this.lastAdditions.clear();
                    this.lastMessageThreshold = -1;
                    updateDisplay();
                    updateUndoButtonState();
                    if (this.currentToast != null) {
                        this.currentToast.cancel();
                    }
                    this.currentToast = Toast.makeText(this, "Goal updated to " + newGoal + "ml\nYou've already had " + this.currentIntake + "ml today", Toast.LENGTH_LONG);
                    this.currentToast.show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.dataManager = new DataManager(this);
        this.dailyGoal = this.dataManager.getDailyGoal();
        this.currentIntake = this.dataManager.getCurrentIntake();

        initializeViews();
        setupClickListeners();
        updateDisplay();
        setupBottlePressListener();
        setupProgressBar();
        updateUndoButtonState();
        updateReminderIcon();
        initializeFacts();
        setupFactClickListener();
        startFactRotation();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateProgressBarVisibility();
        updateBottleVisibility();
        setupBottlePressListener();
        if (this.horizontalProgress != null) {
            this.horizontalProgress.post(() -> {
                int percentage = this.dailyGoal > 0 ? Math.round((this.currentIntake / (float) this.dailyGoal) * 100.0f) : 0;
                updateProgressBar(percentage);
            });
        }
    }

    private void initializeFacts() {
        this.allFacts = new String[]{
                getString(R.string.fact_1), getString(R.string.fact_2), getString(R.string.fact_3),
                getString(R.string.fact_4), getString(R.string.fact_5), getString(R.string.fact_6),
                getString(R.string.fact_7), getString(R.string.fact_8), getString(R.string.fact_9),
                getString(R.string.fact_10), getString(R.string.fact_11), getString(R.string.fact_12)
        };
        calculateDayCounter();
        selectTodaysFacts();
        updateFactDisplay();
    }

    private void calculateDayCounter() {
        long installTime = this.dataManager.getAppInstallDate();
        if (installTime == 0) {
            installTime = System.currentTimeMillis();
            this.dataManager.saveAppInstallDate(installTime);
        }
        long daysSince = (System.currentTimeMillis() - installTime) / 86400000;
        this.dayCounter = ((int) (daysSince % 8)) + 1;
    }

    private void selectTodaysFacts() {
        Random random = new Random();
        Set<Integer> selectedIndices = new HashSet<>();
        this.todaysFacts.clear();
        random.setSeed(((long) this.dayCounter) * 1000);
        
        int availableFacts = this.allFacts.length;
        int numToSelect = Math.min(FACTS_PER_DAY, availableFacts);
        
        while (selectedIndices.size() < numToSelect) {
            int index = random.nextInt(availableFacts);
            if (selectedIndices.add(index)) {
                this.todaysFacts.add(this.allFacts[index]);
            }
        }
        this.currentFactIndex = 0;
    }

    private void setupFactClickListener() {
        View factContainer = findViewById(R.id.factContainer);
        if (factContainer != null) {
            factContainer.setOnClickListener(v -> {
                if (this.todaysFacts.isEmpty()) return;
                this.currentFactIndex = (this.currentFactIndex + 1) % this.todaysFacts.size();
                updateFactDisplay();
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100L).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100L).start()
                );
                stopFactRotation();
                startFactRotation();
            });
        }
    }

    private void startFactRotation() {
        this.factRunnable = new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.todaysFacts.isEmpty()) return;
                MainActivity.this.currentFactIndex = (MainActivity.this.currentFactIndex + 1) % MainActivity.this.todaysFacts.size();
                MainActivity.this.updateFactDisplay();
                MainActivity.this.factHandler.postDelayed(this, FACT_ROTATION_INTERVAL);
            }
        };
        this.factHandler.postDelayed(this.factRunnable, FACT_ROTATION_INTERVAL);
    }

    private void stopFactRotation() {
        if (this.factRunnable != null) {
            this.factHandler.removeCallbacks(this.factRunnable);
        }
    }

    private void showReminderSettingsDialog() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != 0) {
            requestNotificationPermission();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder_settings, (ViewGroup) null);
        final SwitchCompat switchReminders = dialogView.findViewById(R.id.switchReminders);
        final Spinner spinnerInterval = dialogView.findViewById(R.id.spinnerInterval);
        final NumberPicker pickerStartHour = dialogView.findViewById(R.id.pickerStartHour);
        final NumberPicker pickerEndHour = dialogView.findViewById(R.id.pickerEndHour);

        String[] intervals = {"30 minutes", "1 hour", "2 hours", "3 hours", "4 hours"};
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, intervals);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(intervalAdapter);

        switchReminders.setChecked(ReminderScheduler.isReminderEnabled(this));
        int currentInterval = ReminderScheduler.getReminderInterval(this);
        int intervalPosition = 1; // Default 1 hour
        if (currentInterval == 30) intervalPosition = 0;
        else if (currentInterval == 60) intervalPosition = 1;
        else if (currentInterval == 120) intervalPosition = 2;
        else if (currentInterval == 180) intervalPosition = 3;
        else if (currentInterval == 240) intervalPosition = 4;
        spinnerInterval.setSelection(intervalPosition);

        pickerStartHour.setMinValue(0);
        pickerStartHour.setMaxValue(23);
        pickerStartHour.setValue(ReminderScheduler.getStartHour(this));
        pickerStartHour.setWrapSelectorWheel(false);

        pickerEndHour.setMinValue(0);
        pickerEndHour.setMaxValue(23);
        pickerEndHour.setValue(ReminderScheduler.getEndHour(this));
        pickerEndHour.setWrapSelectorWheel(false);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean enabled = switchReminders.isChecked();
                    ReminderScheduler.setReminderEnabled(this, enabled);
                    int[] intervalValues = {30, 60, 120, 180, 240};
                    int selectedInterval = intervalValues[spinnerInterval.getSelectedItemPosition()];
                    ReminderScheduler.setReminderInterval(this, selectedInterval);
                    ReminderScheduler.setStartHour(this, pickerStartHour.getValue());
                    ReminderScheduler.setEndHour(this, pickerEndHour.getValue());
                    updateReminderIcon();
                    Toast.makeText(this, enabled ? "Reminders enabled" : "Reminders disabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }

    private void updateFactDisplay() {
        TextView tvDailyFact = findViewById(R.id.tvDailyFact);
        TextView tvFactCounter = findViewById(R.id.tvFactCounter);
        if (tvDailyFact != null && !this.todaysFacts.isEmpty()) {
            tvDailyFact.setText(this.todaysFacts.get(this.currentFactIndex));
        }
        if (tvFactCounter != null) {
            String counterText = (this.currentFactIndex + 1) + "/" + this.todaysFacts.size();
            tvFactCounter.setText(counterText);
        }
    }

    private void initializeViews() {
        this.waterBottleView = findViewById(R.id.bottleVisual);
        this.percentageText = findViewById(R.id.percentageText);
        this.currentIntakeText = findViewById(R.id.currentIntake);
        this.dailyGoalText = findViewById(R.id.dailyGoal);
        this.txtEncouragement = findViewById(R.id.txtEncouragement);
        this.horizontalProgress = findViewById(R.id.horizontalProgress);
        this.progressBarContainer = findViewById(R.id.progressBarContainer);
        this.btnAddSmall = findViewById(R.id.btnAddSmall);
        this.btnAddMedium = findViewById(R.id.btnAddMedium);
        this.btnAddLarge = findViewById(R.id.btnAddLarge);
        this.btnUndo = findViewById(R.id.btnUndo);
        this.btnSettings = findViewById(R.id.btnSettings);
        this.btnHistory = findViewById(R.id.btnHistory);

        if (this.dailyGoalText != null) {
            this.dailyGoalText.setText(getString(R.string.format_ml, this.dailyGoal));
        }
    }

    private void setupProgressBar() {
        if (this.horizontalProgress == null) return;
        updateProgressBarVisibility();
        updateBottleVisibility();
        this.progressBarContainer.post(() -> {
            int percentage = this.dailyGoal > 0 ? Math.round((this.currentIntake / (float) this.dailyGoal) * 100.0f) : 0;
            updateProgressBar(percentage);
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showReminderSettingsDialog();
            } else {
                Toast.makeText(this, "Notification permission required for reminders", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProgressBar(int percentage) {
        if (this.horizontalProgress == null || this.progressBarContainer == null) return;
        int parentWidth = this.progressBarContainer.getWidth();
        if (parentWidth == 0) return;

        int progressPercent = Math.min(percentage, 100);
        int progressWidth = (int) (parentWidth * (progressPercent / 100.0f));
        ViewGroup.LayoutParams params = this.horizontalProgress.getLayoutParams();
        params.width = progressWidth;
        this.horizontalProgress.setLayoutParams(params);
    }

    private void updateProgressBarVisibility() {
        if (this.progressBarContainer == null) return;
        int orientation = getResources().getConfiguration().orientation;
        this.progressBarContainer.setVisibility(orientation == Configuration.ORIENTATION_LANDSCAPE ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        if (this.btnAddSmall != null) this.btnAddSmall.setOnClickListener(v -> addWater(250));
        if (this.btnAddMedium != null) this.btnAddMedium.setOnClickListener(v -> addWater(500));
        if (this.btnAddLarge != null) this.btnAddLarge.setOnClickListener(v -> addWater(750));
        if (this.btnUndo != null) this.btnUndo.setOnClickListener(v -> undoLastAddition());
        if (this.btnSettings != null) this.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.settingsLauncher.launch(intent);
        });
        if (this.btnHistory != null) this.btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });
        ImageView btnReminder = findViewById(R.id.btnReminder);
        if (btnReminder != null) btnReminder.setOnClickListener(v -> showReminderSettingsDialog());
    }

    private void updateReminderIcon() {
        ImageView btnReminder = findViewById(R.id.btnReminder);
        if (btnReminder == null) return;
        boolean remindersEnabled = ReminderScheduler.isReminderEnabled(this);
        int color = ContextCompat.getColor(this, remindersEnabled ? R.color.blue_600 : R.color.slate_400);
        btnReminder.setColorFilter(color);
    }

    public void addWater(int amount) {
        this.isUndoing = false;
        this.currentIntake += amount;
        this.lastAdditions.push(amount);
        while (this.lastAdditions.size() > MAX_UNDO_STACK) {
            this.lastAdditions.remove(0);
        }
        this.dataManager.saveCurrentIntake(this.currentIntake);
        updateDisplay();
        updateUndoButtonState();
    }

    private void updateDisplay() {
        float ratio = this.dailyGoal > 0 ? this.currentIntake / (float) this.dailyGoal : 0.0f;
        int percentage = Math.round(100.0f * ratio);
        if (this.percentageText != null) {
            this.percentageText.setText(getString(R.string.format_percentage, percentage));
        }
        if (this.currentIntakeText != null) {
            this.currentIntakeText.setText(getString(R.string.format_ml, this.currentIntake));
        }
        if (this.dailyGoalText != null) {
            this.dailyGoalText.setText(getString(R.string.format_ml, this.dailyGoal));
        }
        if (this.waterBottleView != null) {
            float visualLevel = Math.min(ratio, 1.0f);
            this.waterBottleView.setWaterLevel(visualLevel, true);
        }
        updateProgressBar(percentage);
        if (this.currentIntake == 0 && this.lastMessageThreshold != -1) {
            this.lastMessageThreshold = -1;
        }
        showEncouragingMessage(percentage);
    }

    private void undoLastAddition() {
        if (this.lastAdditions.isEmpty()) {
            if (this.currentToast != null) this.currentToast.cancel();
            this.currentToast = Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT);
            this.currentToast.show();
            return;
        }
        this.isUndoing = true;
        int lastAmount = this.lastAdditions.pop();
        this.currentIntake = Math.max(0, this.currentIntake - lastAmount);
        if (this.currentIntake == 0) this.lastMessageThreshold = -1;
        this.dataManager.saveCurrentIntake(this.currentIntake);
        updateDisplay();
        updateUndoButtonState();
        this.isUndoing = false;
        if (this.currentToast != null) this.currentToast.cancel();
        this.currentToast = Toast.makeText(this, "Undid +" + lastAmount + "ml", Toast.LENGTH_SHORT);
        this.currentToast.show();
    }

    private void updateUndoButtonState() {
        if (this.btnUndo != null) {
            boolean hasUndo = !this.lastAdditions.isEmpty();
            this.btnUndo.setAlpha(hasUndo ? 1.0f : 0.5f);
            this.btnUndo.setEnabled(hasUndo);
        }
    }

    private void showEncouragingMessage(int percentage) {
        if (this.isUndoing || this.txtEncouragement == null) return;
        int threshold = -1;
        String message = "";
        if (percentage >= 200) { threshold = 200; message = getString(R.string.message_200); }
        else if (percentage >= 150) { threshold = 150; message = getString(R.string.message_150); }
        else if (percentage >= 100) { threshold = 100; message = getString(R.string.message_100_plus); }
        else if (percentage >= 90) { threshold = 90; message = getString(R.string.message_90); }
        else if (percentage >= 75) { threshold = 75; message = getString(R.string.message_75); }
        else if (percentage >= 50) { threshold = 50; message = getString(R.string.message_50); }
        else if (percentage >= 25) { threshold = 25; message = getString(R.string.message_25); }
        else if (percentage > 0) { threshold = 0; message = getString(R.string.message_start); }

        if (threshold != -1 && threshold != this.lastMessageThreshold) {
            this.lastMessageThreshold = threshold;
            this.txtEncouragement.setText(message);
            this.txtEncouragement.setVisibility(View.VISIBLE);
            Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.message_pop);
            this.txtEncouragement.startAnimation(popAnim);
            if (this.hideMessageRunnable != null) {
                this.messageHandler.removeCallbacks(this.hideMessageRunnable);
            }
            this.hideMessageRunnable = () -> {
                Animation fadeAnim = AnimationUtils.loadAnimation(this, R.anim.message_fade);
                fadeAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) { MainActivity.this.txtEncouragement.setVisibility(View.GONE); }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
                this.txtEncouragement.startAnimation(fadeAnim);
            };
            this.messageHandler.postDelayed(this.hideMessageRunnable, 2500L);
        }
    }

    private void updateBottleVisibility() {
        if (this.waterBottleView == null) return;
        int orientation = getResources().getConfiguration().orientation;
        this.waterBottleView.setVisibility(orientation == Configuration.ORIENTATION_LANDSCAPE ? View.GONE : View.VISIBLE);
    }

    private void setupBottlePressListener() {
        if (this.waterBottleView == null) return;
        this.waterBottleView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performClick();
                    this.isPressing = true;
                    startPressing();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    this.isPressing = false;
                    stopPressing();
                    return true;
                default:
                    return false;
            }
        });
    }

    private void startPressing() {
        if (this.pressRunnable != null) return;
        this.pressRunnable = new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.isPressing) {
                    MainActivity.this.addWater(PRESS_AMOUNT_ML);
                    MainActivity.this.pressHandler.postDelayed(this, PRESS_INTERVAL_MS);
                }
            }
        };
        this.pressHandler.post(this.pressRunnable);
    }

    private void stopPressing() {
        if (this.pressRunnable != null) {
            this.pressHandler.removeCallbacks(this.pressRunnable);
            this.pressRunnable = null;
        }
    }
}
