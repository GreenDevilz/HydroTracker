package com.example.waterintaketracker;

import java.util.Arrays;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.CheckBox;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private static final int FACTS_PER_DAY = 5;
    private static final long FACT_ROTATION_INTERVAL = 30000;
    private static final int PRESS_AMOUNT_ML = 50;
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
    private UserManager userManager;
    private Runnable factRunnable;
    private Runnable hideMessageRunnable;
    private View horizontalProgress;
    private TextView percentageText;
    private FrameLayout progressBarContainer;
    private TextView txtEncouragement;
    private WaterBottleView waterBottleView;

    private final List<String> todaysFacts = new ArrayList<>();
    private int currentFactIndex = 0;
    private int dayCounter = 1;
    private final Handler factHandler = new Handler(Looper.getMainLooper());
    private int currentIntake = 0;
    private int dailyGoal = 2500;
    private final Handler messageHandler = new Handler(Looper.getMainLooper());
    private int lastMessageThreshold = -1;
    private boolean isUndoing = false;
    private final Stack<Integer> lastAdditions = new Stack<>();

    private static final String KEY_UNDO_STACK = "undo_stack";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Integer> undoList = new ArrayList<>(lastAdditions);
        outState.putIntegerArrayList(KEY_UNDO_STACK, undoList);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Integer> undoList = savedInstanceState.getIntegerArrayList(KEY_UNDO_STACK);
        if (undoList != null) {
            lastAdditions.clear();
            lastAdditions.addAll(undoList);
            updateUndoButtonState();
        }
    }

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshData();
                    if (this.currentToast != null) {
                        this.currentToast.cancel();
                    }
                    String updateMsg = getString(R.string.goal_updated_toast, this.dailyGoal, this.currentIntake);
                    this.currentToast = Toast.makeText(this, updateMsg, Toast.LENGTH_LONG);
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
        this.userManager = new UserManager(this);
        
        initializeViews();
        refreshData();
        setupClickListeners();
        setupBottlePressListener();
        setupProgressBar();
        initializeFacts();
        setupFactClickListener();
        startFactRotation();
    }

    private void refreshData() {
        this.dailyGoal = this.dataManager.getDailyGoal();
        this.currentIntake = this.dataManager.getCurrentIntake();
        updateDisplay();
        updateUndoButtonState();
        updateReminderIcon();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
                getString(R.string.fact_10), getString(R.string.fact_11), getString(R.string.fact_12),
                getString(R.string.fact_13), getString(R.string.fact_14), getString(R.string.fact_15),
                getString(R.string.fact_16), getString(R.string.fact_17), getString(R.string.fact_18),
                getString(R.string.fact_19), getString(R.string.fact_20), getString(R.string.fact_21),
                getString(R.string.fact_22), getString(R.string.fact_23), getString(R.string.fact_24),
                getString(R.string.fact_25), getString(R.string.fact_26), getString(R.string.fact_27),
                getString(R.string.fact_28), getString(R.string.fact_30), getString(R.string.fact_31),
                getString(R.string.fact_32), getString(R.string.fact_33), getString(R.string.fact_34),
                getString(R.string.fact_35), getString(R.string.fact_36), getString(R.string.fact_37),
                getString(R.string.fact_38), getString(R.string.fact_39), getString(R.string.fact_40)
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

    private List<String> remainingFacts = new ArrayList<>();

    private void selectTodaysFacts() {
        if (remainingFacts.isEmpty()) {
            remainingFacts = new ArrayList<>(Arrays.asList(this.allFacts));
        }
        int numToSelect = Math.min(FACTS_PER_DAY, this.allFacts.length);
        if (remainingFacts.size() < numToSelect) {
            remainingFacts = new ArrayList<>(Arrays.asList(this.allFacts));
        }
        Random random = new Random();
        Set<Integer> selectedIndices = new HashSet<>();
        this.todaysFacts.clear();
        random.setSeed(((long) this.dayCounter) * 1000);
        while (selectedIndices.size() < numToSelect) {
            int index = random.nextInt(remainingFacts.size());
            if (selectedIndices.add(index)) {
                this.todaysFacts.add(remainingFacts.get(index));
            }
        }
        List<Integer> sortedIndices = new ArrayList<>(selectedIndices);
        sortedIndices.sort((a, b) -> b - a);
        for (int index : sortedIndices) {
            remainingFacts.remove(index);
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

    private void showUserPanel() {
        View panelView = getLayoutInflater().inflate(R.layout.dialog_user_panel, null);
        TextView panelTitle = panelView.findViewById(R.id.panelTitle);
        View layoutUsername = panelView.findViewById(R.id.layoutUsername);
        View layoutPassword = panelView.findViewById(R.id.layoutPassword);
        TextInputEditText inputUsername = panelView.findViewById(R.id.inputUsername);
        TextInputEditText inputPassword = panelView.findViewById(R.id.inputPassword);
        Button btnSignIn = panelView.findViewById(R.id.btnSignIn);
        Button btnSignUp = panelView.findViewById(R.id.btnSignUp);
        Button btnLogout = panelView.findViewById(R.id.btnLogout);

        View userInfoSection = panelView.findViewById(R.id.userInfoSection);
        TextView txtStreak = panelView.findViewById(R.id.txtStreak);
        TextView txtAverage = panelView.findViewById(R.id.txtAverage);
        TextView txtGoal = panelView.findViewById(R.id.txtGoal);

        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            panelTitle.setText(getString(R.string.logged_in_as, currentUser));
            layoutUsername.setVisibility(View.GONE);
            layoutPassword.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnSignUp.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);

            userInfoSection.setVisibility(View.VISIBLE);
            txtStreak.setText(getString(R.string.format_days, dataManager.getCurrentStreak()));
            txtAverage.setText(getString(R.string.format_ml, dataManager.getAverageIntake()));
            txtGoal.setText(getString(R.string.format_ml, dataManager.getDailyGoal()));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(panelView)
                .create();

        btnSignIn.setOnClickListener(v -> {
            Editable userEditable = inputUsername.getText();
            Editable passEditable = inputPassword.getText();
            if (userEditable == null || passEditable == null) return;
            
            String u = userEditable.toString().trim();
            String p = passEditable.toString();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all), Toast.LENGTH_SHORT).show();
                return;
            }
            if (userManager.login(u, p, this)) {
                Toast.makeText(this, getString(R.string.logged_in_toast, u), Toast.LENGTH_SHORT).show();
                refreshData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
            }
        });

        btnSignUp.setOnClickListener(v -> {
            Editable userEditable = inputUsername.getText();
            Editable passEditable = inputPassword.getText();
            if (userEditable == null || passEditable == null) return;

            String u = userEditable.toString().trim();
            String p = passEditable.toString();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all), Toast.LENGTH_SHORT).show();
                return;
            }
            if (userManager.signUp(u, p, this)) {
                Toast.makeText(this, getString(R.string.account_created, u), Toast.LENGTH_SHORT).show();
                refreshData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, getString(R.string.username_exists), Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            userManager.logout(this);
            Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
            refreshData();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showReminderSettingsDialog() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != 0) {
            requestNotificationPermission();
        } else {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder_settings, null);
            SwitchCompat switchReminders = dialogView.findViewById(R.id.switchReminders);
            Spinner spinnerInterval = dialogView.findViewById(R.id.spinnerInterval);
            NumberPicker pickerStart = dialogView.findViewById(R.id.pickerStartHour);
            NumberPicker pickerEnd = dialogView.findViewById(R.id.pickerEndHour);
            CheckBox checkCustom = dialogView.findViewById(R.id.checkUseCustomMessage);
            EditText inputMessage = dialogView.findViewById(R.id.inputCustomMessage);
            TextView txtPreview = dialogView.findViewById(R.id.txtMessagePreview);

            // Initialize values
            switchReminders.setChecked(ReminderScheduler.isReminderEnabled(this));
            
            String[] intervals = {"30", "60", "90", "120", "180", "240"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, intervals);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerInterval.setAdapter(adapter);
            
            int currentInterval = ReminderScheduler.getReminderInterval(this);
            for (int i = 0; i < intervals.length; i++) {
                if (Integer.parseInt(intervals[i]) == currentInterval) {
                    spinnerInterval.setSelection(i);
                    break;
                }
            }

            pickerStart.setMinValue(0);
            pickerStart.setMaxValue(23);
            pickerStart.setValue(ReminderScheduler.getStartHour(this));

            pickerEnd.setMinValue(0);
            pickerEnd.setMaxValue(23);
            pickerEnd.setValue(ReminderScheduler.getEndHour(this));

            checkCustom.setChecked(ReminderScheduler.useCustomMessage(this));
            inputMessage.setText(ReminderScheduler.getCustomMessage(this));
            inputMessage.setEnabled(true); // Modifiable no matter if toggled or not

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    updateReminderPreview(checkCustom.isChecked(), s.toString(), txtPreview);
                }
            };
            inputMessage.addTextChangedListener(watcher);
            
            // Set initial preview
            updateReminderPreview(checkCustom.isChecked(), inputMessage.getText().toString(), txtPreview);

            checkCustom.setOnCheckedChangeListener((buttonView, isChecked) -> updateReminderPreview(isChecked, inputMessage.getText().toString(), txtPreview));

            new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    ReminderScheduler.setReminderEnabled(this, switchReminders.isChecked());
                    ReminderScheduler.setReminderInterval(this, Integer.parseInt(intervals[spinnerInterval.getSelectedItemPosition()]));
                    ReminderScheduler.setStartHour(this, pickerStart.getValue());
                    ReminderScheduler.setEndHour(this, pickerEnd.getValue());
                    ReminderScheduler.setUseCustomMessage(this, checkCustom.isChecked());
                    ReminderScheduler.setCustomMessage(this, inputMessage.getText().toString());
                    updateReminderIcon();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        }
    }

    private void updateReminderPreview(boolean useCustom, String text, TextView preview) {
        if (!useCustom || text.trim().isEmpty()) {
            preview.setText(getString(R.string.reminder_default));
        } else {
            String trimmed = text.trim();
            if (trimmed.startsWith("💧")) {
                preview.setText(trimmed);
            } else {
                preview.setText(getString(R.string.reminder_custom_format, trimmed));
            }
        }
    }

    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_CODE);
    }

    private void initializeViews() {
        this.currentIntakeText = findViewById(R.id.currentIntake);
        this.dailyGoalText = findViewById(R.id.dailyGoal);
        this.percentageText = findViewById(R.id.percentageText);
        this.txtEncouragement = findViewById(R.id.txtEncouragement);
        this.btnAddSmall = findViewById(R.id.btnAddSmall);
        this.btnAddMedium = findViewById(R.id.btnAddMedium);
        this.btnAddLarge = findViewById(R.id.btnAddLarge);
        this.btnUndo = findViewById(R.id.btnUndo);
        this.btnHistory = findViewById(R.id.btnHistory);
        this.btnSettings = findViewById(R.id.btnSettings);
        this.waterBottleView = findViewById(R.id.bottleVisual);
        this.progressBarContainer = findViewById(R.id.progressBarContainer);
    }

    private void setupClickListeners() {
        this.btnAddSmall.setOnClickListener(v -> addWater(250));
        this.btnAddMedium.setOnClickListener(v -> addWater(500));
        this.btnAddLarge.setOnClickListener(v -> addWater(750));
        this.btnUndo.setOnClickListener(v -> undoLastAddition());
        this.btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });
        this.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });
        findViewById(R.id.btnProfile).setOnClickListener(v -> showUserPanel());
        findViewById(R.id.btnReminder).setOnClickListener(v -> showReminderSettingsDialog());
    }

    private void setupBottlePressListener() {
        if (this.waterBottleView == null) return;
        this.waterBottleView.setOnBottleTouchListener(() -> addWater(PRESS_AMOUNT_ML));
    }

    private void addWater(int amount) {
        this.currentIntake += amount;
        this.dataManager.saveCurrentIntake(this.currentIntake);
        if (!this.isUndoing) {
            this.lastAdditions.push(amount);
            updateUndoButtonState();
        }
        updateDisplay();
        checkProgressMessages();
    }

    private void undoLastAddition() {
        if (!this.lastAdditions.isEmpty()) {
            this.isUndoing = true;
            int lastAmount = this.lastAdditions.pop();
            this.currentIntake = Math.max(0, this.currentIntake - lastAmount);
            this.dataManager.saveCurrentIntake(this.currentIntake);
            updateDisplay();
            updateUndoButtonState();
            this.isUndoing = false;
        }
    }

    private void updateUndoButtonState() {
        if (this.btnUndo != null) {
            this.btnUndo.setEnabled(!this.lastAdditions.isEmpty());
            this.btnUndo.setAlpha(this.lastAdditions.isEmpty() ? 0.5f : 1.0f);
        }
    }

    private void updateDisplay() {
        if (this.currentIntakeText != null) {
            this.currentIntakeText.setText(String.valueOf(this.currentIntake));
        }
        if (this.dailyGoalText != null) {
            this.dailyGoalText.setText(String.valueOf(this.dailyGoal));
        }
        int percentage = this.dailyGoal > 0 ? Math.round((this.currentIntake / (float) this.dailyGoal) * 100.0f) : 0;
        if (this.percentageText != null) {
            this.percentageText.setText(getString(R.string.format_percentage, percentage));
        }
        if (this.waterBottleView != null) {
            this.waterBottleView.setWaterLevel(percentage / 100.0f, true);
        }
        updateProgressBar(percentage);
    }

    private void updateProgressBar(int percentage) {
        if (this.horizontalProgress != null) {
            ViewGroup.LayoutParams params = this.horizontalProgress.getLayoutParams();
            int maxWidth = ((View) this.horizontalProgress.getParent()).getWidth();
            params.width = (int) (maxWidth * (Math.min(percentage, 100) / 100.0f));
            this.horizontalProgress.setLayoutParams(params);
        }
    }

    private void setupProgressBar() {
        this.horizontalProgress = findViewById(R.id.horizontalProgress);
        updateProgressBarVisibility();
    }

    private void updateProgressBarVisibility() {
        if (this.progressBarContainer != null) {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            this.progressBarContainer.setVisibility(isLandscape ? View.VISIBLE : View.GONE);
        }
    }

    private void updateBottleVisibility() {
        if (this.waterBottleView != null) {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            this.waterBottleView.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        }
    }

    private void updateFactDisplay() {
        TextView factText = findViewById(R.id.tvDailyFact);
        if (factText != null && !this.todaysFacts.isEmpty()) {
            Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOut.setDuration(500L);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    factText.setText(MainActivity.this.todaysFacts.get(MainActivity.this.currentFactIndex));
                    Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in);
                    fadeIn.setDuration(500L);
                    factText.startAnimation(fadeIn);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            factText.startAnimation(fadeOut);
        }
    }

    private void checkProgressMessages() {
        int percentage = this.dailyGoal > 0 ? (this.currentIntake * 100) / this.dailyGoal : 0;
        int threshold = -1;
        String message = "";

        if (percentage >= 200) { threshold = 200; message = getString(R.string.message_200); }
        else if (percentage >= 150) { threshold = 150; message = getString(R.string.message_150); }
        else if (percentage >= 100) { threshold = 100; message = getString(R.string.message_100_plus); }
        else if (percentage >= 90) { threshold = 90; message = getString(R.string.message_90); }
        else if (percentage >= 75) { threshold = 75; message = getString(R.string.message_75); }
        else if (percentage >= 50) { threshold = 50; message = getString(R.string.message_50); }
        else if (percentage >= 25) { threshold = 25; message = getString(R.string.message_25); }

        if (threshold != -1 && threshold != this.lastMessageThreshold && !this.isUndoing) {
            this.lastMessageThreshold = threshold;
            showEncouragement(message);
        }
    }

    private void showEncouragement(String message) {
        if (this.txtEncouragement != null) {
            this.txtEncouragement.setText(message);
            this.txtEncouragement.setVisibility(View.VISIBLE);
            this.txtEncouragement.setAlpha(0.0f);
            this.txtEncouragement.animate().alpha(1.0f).setDuration(500L).start();

            if (this.hideMessageRunnable != null) {
                this.messageHandler.removeCallbacks(this.hideMessageRunnable);
            }
            this.hideMessageRunnable = () -> MainActivity.this.txtEncouragement.animate().alpha(0.0f).setDuration(500L).withEndAction(() -> MainActivity.this.txtEncouragement.setVisibility(View.GONE)).start();
            this.messageHandler.postDelayed(this.hideMessageRunnable, 5000L);
        }
    }

    private void updateReminderIcon() {
        ImageView imgReminders = findViewById(R.id.btnReminder);
        if (imgReminders != null) {
            boolean enabled = ReminderScheduler.isReminderEnabled(this);
            imgReminders.setColorFilter(ContextCompat.getColor(this, enabled ? R.color.blue_600 : R.color.slate_400));
        }
    }
}
