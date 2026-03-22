package com.example.waterintaketracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private Button btnCalculate;
    private Button btnSetCustomGoal;
    private CheckBox checkBreastfeeding;
    private CheckBox checkCreatine;
    private CheckBox checkExcessiveSweating;
    private CheckBox checkFever;
    private CheckBox checkHighProtein;
    private CheckBox checkHighUrine;
    private CheckBox checkPregnancy;
    private CheckBox checkRestricted;
    private DataManager dataManager;
    private EditText inputAge;
    private EditText inputCustomGoal;
    private EditText inputWeight;
    private EditText inputHeight;
    private EditText inputBodyFat;
    private EditText inputActivityDuration;
    private RadioButton radioMale;
    private RadioButton radioFemale;
    private RadioGroup radioGroupGender;
    private LinearLayout restrictedModeCard;
    private LinearLayout resultCard;
    private Spinner spinnerActivity;
    private Spinner spinnerClimate;
    private TextView txtRecommendedGoal;
    private int calculatedGoal = 2500;
    private String selectedActivity = "Moderate";
    private String selectedClimate = "Moderate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.dataManager = new DataManager(this);
        initializeViews();
        setupSpinners();
        setupClickListeners();
        setupGenderValidation();
        loadSavedData();
        ReminderScheduler.scheduleReminders(this);
    }

    private void initializeViews() {
        this.inputWeight = findViewById(R.id.inputWeight);
        this.inputAge = findViewById(R.id.inputAge);
        this.inputHeight = findViewById(R.id.inputHeight);
        this.inputBodyFat = findViewById(R.id.inputBodyFat);
        this.inputActivityDuration = findViewById(R.id.inputActivityDuration);
        this.radioMale = findViewById(R.id.radioMale);
        this.radioFemale = findViewById(R.id.radioFemale);
        this.radioGroupGender = findViewById(R.id.radioGroupGender);
        this.spinnerActivity = findViewById(R.id.spinnerActivity);
        this.spinnerClimate = findViewById(R.id.spinnerClimate);
        this.checkPregnancy = findViewById(R.id.checkPregnancy);
        this.checkBreastfeeding = findViewById(R.id.checkBreastfeeding);
        this.checkFever = findViewById(R.id.checkFever);
        this.checkExcessiveSweating = findViewById(R.id.checkExcessiveSweating);
        this.checkHighUrine = findViewById(R.id.checkHighUrine);
        this.checkHighProtein = findViewById(R.id.checkHighProtein);
        this.checkCreatine = findViewById(R.id.checkCreatine);
        this.checkRestricted = findViewById(R.id.checkRestricted);
        this.restrictedModeCard = findViewById(R.id.restrictedModeCard);
        this.inputCustomGoal = findViewById(R.id.inputCustomGoal);
        this.btnSetCustomGoal = findViewById(R.id.btnSetCustomGoal);
        this.btnCalculate = findViewById(R.id.btnCalculate);
        this.resultCard = findViewById(R.id.resultCard);
        this.txtRecommendedGoal = findViewById(R.id.txtRecommendedGoal);

        setupRestrictedModeListener();
    }

    private void setupGenderValidation() {
        // Listen for gender changes to clear invalid checkboxes
        this.radioGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMale = (checkedId == R.id.radioMale);
            if (isMale) {
                // Clear pregnancy and breastfeeding if male is selected
                if (this.checkPregnancy.isChecked()) {
                    this.checkPregnancy.setChecked(false);
                    showGenderError("Pregnancy is only applicable for females");
                }
                if (this.checkBreastfeeding.isChecked()) {
                    this.checkBreastfeeding.setChecked(false);
                    showGenderError("Breastfeeding is only applicable for females");
                }
            }
        });

        // Validate when trying to check pregnancy or breastfeeding
        this.checkPregnancy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && this.radioMale.isChecked()) {
                this.checkPregnancy.setChecked(false);
                showGenderError("Pregnancy option is only available for female users");
            }
        });

        this.checkBreastfeeding.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && this.radioMale.isChecked()) {
                this.checkBreastfeeding.setChecked(false);
                showGenderError("Breastfeeding option is only available for female users");
            }
        });
    }

    private void showGenderError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setupRestrictedModeListener() {
        this.checkRestricted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                this.restrictedModeCard.setVisibility(View.VISIBLE);
                this.btnCalculate.setVisibility(View.GONE);
                this.resultCard.setVisibility(View.GONE);
            } else {
                this.restrictedModeCard.setVisibility(View.GONE);
                this.btnCalculate.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadSavedData() {
        if (this.dataManager.hasUserProfile()) {
            // Load basic profile data
            this.inputWeight.setText(String.valueOf((int) this.dataManager.getUserWeight()));
            this.inputAge.setText(String.valueOf(this.dataManager.getUserAge()));

            // Load optional physical metrics
            if (this.dataManager.getUserHeight() > 0) {
                this.inputHeight.setText(String.valueOf(this.dataManager.getUserHeight()));
            }
            if (this.dataManager.getUserBodyFat() > 0) {
                this.inputBodyFat.setText(String.valueOf(this.dataManager.getUserBodyFat()));
            }
            if (this.dataManager.getUserActivityDuration() > 0) {
                this.inputActivityDuration.setText(String.valueOf(this.dataManager.getUserActivityDuration()));
            }

            // Load gender and gender-specific conditions
            String savedGender = this.dataManager.getUserGender();
            if ("Female".equals(savedGender)) {
                this.radioFemale.setChecked(true);
                // Load pregnancy and breastfeeding states for female users
                this.checkPregnancy.setChecked(this.dataManager.isUserPregnant());
                this.checkBreastfeeding.setChecked(this.dataManager.isUserBreastfeeding());
            } else {
                this.radioMale.setChecked(true);
                // Explicitly clear pregnancy/breastfeeding for male users
                this.checkPregnancy.setChecked(false);
                this.checkBreastfeeding.setChecked(false);
            }

            // Load activity level
            String savedActivity = this.dataManager.getActivityLevel();
            String[] activityValues = {"Sedentary", "Light", "Moderate", "Very Active", "Athlete"};
            for (int i = 0; i < activityValues.length; i++) {
                if (activityValues[i].equals(savedActivity)) {
                    this.spinnerActivity.setSelection(i);
                    this.selectedActivity = savedActivity;
                    break;
                }
            }

            // Load climate
            String savedClimate = this.dataManager.getClimate();
            String[] climateValues = {"Cool", "Moderate", "Hot"};
            for (int i = 0; i < climateValues.length; i++) {
                if (climateValues[i].equals(savedClimate)) {
                    this.spinnerClimate.setSelection(i);
                    this.selectedClimate = savedClimate;
                    break;
                }
            }

            // Load other health conditions (these are generic and apply to everyone)
            // Note: You may want to save these in DataManager as well if you want them to persist
            // For now, they remain unchecked by default when loading settings
        }
    }

    private void setupSpinners() {
        String[] activityLevels = {"Sedentary (office job)", "Light (1-3x/week)", "Moderate (3-5x/week)", "Very Active (daily)", "Athlete (2x/day)"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activityLevels);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerActivity.setAdapter(activityAdapter);
        this.spinnerActivity.setSelection(2);
        this.spinnerActivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = {"Sedentary", "Light", "Moderate", "Very Active", "Athlete"};
                if (position >= 0 && position < values.length) {
                    SettingsActivity.this.selectedActivity = values[position];
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] climates = {"Cool / Air conditioned", "Moderate", "Hot / Humid"};
        ArrayAdapter<String> climateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, climates);
        climateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerClimate.setAdapter(climateAdapter);
        this.spinnerClimate.setSelection(1);
        this.spinnerClimate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = {"Cool", "Moderate", "Hot"};
                if (position >= 0 && position < values.length) {
                    SettingsActivity.this.selectedClimate = values[position];
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        this.btnCalculate.setOnClickListener(v -> calculateHydration());
        this.btnSetCustomGoal.setOnClickListener(v -> setCustomGoal());

        Button btnApply = findViewById(R.id.btnApply);
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (validateInputs() && validateGenderConditions()) {
                    saveProfile();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("DAILY_GOAL", this.calculatedGoal);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(inputWeight.getText())) {
            inputWeight.setError("Weight required");
            return false;
        }
        if (TextUtils.isEmpty(inputAge.getText())) {
            inputAge.setError("Age required");
            return false;
        }
        return true;
    }

    private boolean validateGenderConditions() {
        boolean isMale = this.radioMale.isChecked();

        if (isMale && (this.checkPregnancy.isChecked() || this.checkBreastfeeding.isChecked())) {
            String errorMsg = "Pregnancy and breastfeeding options are only applicable for female users";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

            // Uncheck invalid options
            if (this.checkPregnancy.isChecked()) {
                this.checkPregnancy.setChecked(false);
            }
            if (this.checkBreastfeeding.isChecked()) {
                this.checkBreastfeeding.setChecked(false);
            }
            return false;
        }

        return true;
    }

    private void saveProfile() {
        float weight = Float.parseFloat(this.inputWeight.getText().toString());
        int age = Integer.parseInt(this.inputAge.getText().toString());
        String gender = this.radioMale.isChecked() ? "Male" : "Female";
        int height = 0;
        float bodyFat = 0f;
        int activityDuration = 0;
        if (!TextUtils.isEmpty(this.inputHeight.getText())) {
            height = Integer.parseInt(this.inputHeight.getText().toString());
        }
        if (!TextUtils.isEmpty(this.inputBodyFat.getText())) {
            bodyFat = Float.parseFloat(this.inputBodyFat.getText().toString());
        }
        if (!TextUtils.isEmpty(this.inputActivityDuration.getText())) {
            activityDuration = Integer.parseInt(this.inputActivityDuration.getText().toString());
        }

        // Only save pregnancy/breastfeeding status if female
        boolean isPregnant = false;
        boolean isBreastfeeding = false;
        if (gender.equals("Female")) {
            isPregnant = this.checkPregnancy.isChecked();
            isBreastfeeding = this.checkBreastfeeding.isChecked();
        }

        // Check if the goal has changed from previously saved goal
        int oldGoal = this.dataManager.getDailyGoal();
        int newGoal = this.calculatedGoal;

        // Save the profile
        this.dataManager.saveUserProfile(weight, age, gender, this.selectedActivity, this.selectedClimate,
                height, bodyFat, activityDuration, isPregnant, isBreastfeeding);

        // If the goal has changed, update history entries to reflect the new goal
        if (oldGoal != newGoal) {
            this.dataManager.onGoalChanged(newGoal);
        } else {
            this.dataManager.saveDailyGoal(newGoal);
        }
    }

    private void setCustomGoal() {
        String customGoalStr = this.inputCustomGoal.getText().toString();
        if (TextUtils.isEmpty(customGoalStr)) {
            this.inputCustomGoal.setError("Please enter a custom goal");
            return;
        }
        this.calculatedGoal = Integer.parseInt(customGoalStr);
        if (this.calculatedGoal < 500 || this.calculatedGoal > 10000) {
            this.inputCustomGoal.setError("Please enter a realistic goal (500-10000 ml)");
            return;
        }
        if (validateInputs() && validateGenderConditions()) {
            saveProfile(); // This now handles the goal change properly
            Intent resultIntent = new Intent();
            resultIntent.putExtra("DAILY_GOAL", this.calculatedGoal);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private void calculateHydration() {
        if (!validateInputs()) return;

        float weight = Float.parseFloat(this.inputWeight.getText().toString());
        int age = Integer.parseInt(this.inputAge.getText().toString());

        if (weight < 20.0f || weight > 300.0f) {
            this.inputWeight.setError("Please enter a realistic weight (20-300 kg)");
            return;
        }
        if (age < 1 || age > 120) {
            this.inputAge.setError("Please enter a realistic age (1-120 years)");
            return;
        }

        // Validate gender-specific conditions
        if (!validateGenderConditions()) {
            return;
        }

        // Optional fields
        Integer height = null;
        if (!TextUtils.isEmpty(this.inputHeight.getText())) {
            height = Integer.parseInt(this.inputHeight.getText().toString());
            if (height < 50 || height > 300) {
                this.inputHeight.setError("Height must be between 50 and 300 cm");
                return;
            }
        }
        Float bodyFat = null;
        if (!TextUtils.isEmpty(this.inputBodyFat.getText())) {
            bodyFat = Float.parseFloat(this.inputBodyFat.getText().toString());
            if (bodyFat < 0 || bodyFat > 60) {
                this.inputBodyFat.setError("Body fat % must be between 0 and 60");
                return;
            }
        }
        Integer activityDuration = null;
        if (!TextUtils.isEmpty(this.inputActivityDuration.getText())) {
            activityDuration = Integer.parseInt(this.inputActivityDuration.getText().toString());
            if (activityDuration < 0 || activityDuration > 600) {
                this.inputActivityDuration.setError("Duration must be 0–600 minutes");
                return;
            }
        }

        boolean isMale = this.radioMale.isChecked();
        boolean isPregnant = false;
        boolean isBreastfeeding = false;

        // Only consider pregnancy/breastfeeding if female
        if (!isMale) {
            isPregnant = this.checkPregnancy.isChecked();
            isBreastfeeding = this.checkBreastfeeding.isChecked();
        }

        boolean hasFever = this.checkFever.isChecked();
        boolean excessiveSweating = this.checkExcessiveSweating.isChecked();
        boolean highUrine = this.checkHighUrine.isChecked();
        boolean highProtein = this.checkHighProtein.isChecked();
        boolean creatine = this.checkCreatine.isChecked();

        float totalWater = getTotalWaterRequirement(weight, age, isMale, height, bodyFat,
                selectedActivity, activityDuration, selectedClimate,
                excessiveSweating, highUrine, highProtein,
                isPregnant, isBreastfeeding, hasFever, creatine);

        // Account for food water (internal adjustment)
        float foodWaterFactor = 0.2f; // assume 20% from food
        float drinkingGoal = totalWater * (1 - foodWaterFactor);

        this.calculatedGoal = Math.round(drinkingGoal / 50.0f) * 50;
        this.txtRecommendedGoal.setText(getString(R.string.format_ml, this.calculatedGoal));
        this.resultCard.setVisibility(View.VISIBLE);

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private float getTotalWaterRequirement(float weight, int age, boolean isMale,
                                           Integer height, Float bodyFat,
                                           String activityLevel, Integer activityDuration,
                                           String climate,
                                           boolean excessiveSweating, boolean highUrine, boolean highProtein,
                                           boolean isPregnant, boolean isBreastfeeding, boolean hasFever, boolean creatine) {
        // Base water from all sources (ml)
        float baseWater;

        // Use body fat if provided -> lean body mass based
        if (bodyFat != null && bodyFat > 0) {
            float leanMass = weight * (1 - bodyFat / 100f);
            // 40 ml per kg lean mass (typical range 40-45)
            baseWater = leanMass * 40f;
        }
        // Else use height if provided -> body surface area based
        else if (height != null && height > 0) {
            // Mosteller BSA formula: sqrt( (weight * height) / 3600 )
            double bsa = Math.sqrt((weight * height) / 3600.0);
            // 1500 ml per m² BSA (typical range 1500-2000)
            baseWater = (float) (bsa * 1500f);
        }
        // Else fall back to weight-based
        else {
            baseWater = (isMale ? 35f : 31f) * weight;
        }

        // Age adjustment
        if (age > 65) baseWater *= 0.85f;
        else if (age > 50) baseWater *= 0.9f;
        else if (age > 30) baseWater *= 0.95f;

        // Activity level multiplier
        float activityMultiplier = 1.0f;
        switch (activityLevel) {
            case "Light": activityMultiplier = 1.2f; break;
            case "Moderate": activityMultiplier = 1.4f; break;
            case "Very Active": activityMultiplier = 1.6f; break;
            case "Athlete": activityMultiplier = 2.0f; break;
        }
        baseWater *= activityMultiplier;

        // Activity duration extra (ml per minute of moderate activity)
        if (activityDuration != null && activityDuration > 0) {
            baseWater += activityDuration * 10f; // 10 ml per minute
        }

        // Climate adjustment
        switch (climate) {
            case "Cool": break;
            case "Hot": baseWater *= 1.3f; break;
            default: baseWater *= 1.1f; break;
        }

        // Health conditions
        if (excessiveSweating) baseWater *= 1.2f;
        if (highUrine) baseWater *= 1.15f;
        if (highProtein) baseWater *= 1.1f;

        // Special conditions (additive)
        if (isPregnant) baseWater += 300f;
        if (isBreastfeeding) baseWater += 700f;
        if (hasFever) baseWater += 500f;
        if (creatine) baseWater += 500f;

        return baseWater;
    }
}