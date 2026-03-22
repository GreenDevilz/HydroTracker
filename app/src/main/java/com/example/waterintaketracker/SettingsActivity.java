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
    private boolean isInitialLoad = true;
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

                // Auto-scroll to restricted mode card
                if (!isInitialLoad) {
                    ScrollView scrollView = findViewById(R.id.scrollView);
                    if (scrollView != null) {
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    }
                }
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

            // After loading gender, add:
            this.checkFever.setChecked(this.dataManager.hasFever());
            this.checkExcessiveSweating.setChecked(this.dataManager.hasExcessiveSweating());
            this.checkHighUrine.setChecked(this.dataManager.hasHighUrine());
            this.checkHighProtein.setChecked(this.dataManager.hasHighProtein());
            this.checkCreatine.setChecked(this.dataManager.hasCreatine());
            this.checkRestricted.setChecked(this.dataManager.hasRestricted());

            this.checkRestricted.setChecked(this.dataManager.hasRestricted());
            isInitialLoad = false;


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
                height, bodyFat, activityDuration,
                isPregnant, isBreastfeeding,
                this.checkFever.isChecked(),
                this.checkExcessiveSweating.isChecked(),
                this.checkHighUrine.isChecked(),
                this.checkHighProtein.isChecked(),
                this.checkCreatine.isChecked(),
                this.checkRestricted.isChecked());

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

    private float getTotalWaterRequirement(float weight, int age, boolean isMale,
                                           Integer height, Float bodyFat,
                                           String activityLevel, Integer activityDuration,
                                           String climate,
                                           boolean excessiveSweating, boolean highUrine, boolean highProtein,
                                           boolean isPregnant, boolean isBreastfeeding, boolean hasFever, boolean creatine) {

        float reeKcal;
        if (bodyFat != null && bodyFat > 0) {
            float leanMass = weight * (1 - bodyFat / 100f);
            reeKcal = 500f + 22f * leanMass;
        } else if (height != null && height > 0) {
            if (isMale) {
                reeKcal = 10f * weight + 6.25f * height - 5f * age + 5f;
            } else {
                reeKcal = 10f * weight + 6.25f * height - 5f * age - 161f;
            }
        } else {
            reeKcal = (isMale ? 24f : 22f) * weight;
        }

        float pal;
        switch (activityLevel) {
            case "Moderate":    pal = 1.6f; break;
            case "Very Active": pal = 1.8f; break;
            case "Athlete":     pal = 2.2f; break;
            default:            pal = 1.4f; break;
        }

        float exerciseKcal = 0f;
        if (activityDuration != null && activityDuration > 0) {
            float met;
            switch (activityLevel) {
                case "Moderate":    met = 5f; break;
                case "Very Active": met = 7f; break;
                case "Athlete":     met = 9f; break;
                default:            met = 3f; break;
            }
            exerciseKcal = met * weight * (activityDuration / 60f);
        }

        float totalWater = (reeKcal * pal + exerciseKcal);

        switch (climate) {
            case "Hot":    totalWater *= 1.3f; break;
            case "Normal": totalWater *= 1.1f; break;
            default:       break;
        }

        if (excessiveSweating) totalWater *= 1.2f;
        if (highUrine) totalWater *= 1.15f;
        if (highProtein) totalWater *= 1.10f;

        if (isPregnant) totalWater += 300f;
        if (isBreastfeeding) totalWater += 700f;
        if (hasFever) totalWater += 500f;
        if (creatine) totalWater += 500f;

        if (age > 65) totalWater *= 1.05f;
        else if (age > 50) totalWater *= 1.02f;

        return Math.max(totalWater, 0f);
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
        boolean isPregnant = this.checkPregnancy.isChecked();
        boolean isBreastfeeding = this.checkBreastfeeding.isChecked();
        boolean hasFever = this.checkFever.isChecked();
        boolean excessiveSweating = this.checkExcessiveSweating.isChecked();
        boolean highUrine = this.checkHighUrine.isChecked();
        boolean highProtein = this.checkHighProtein.isChecked();
        boolean creatine = this.checkCreatine.isChecked();

        float totalWater = getTotalWaterRequirement(weight, age, isMale, height, bodyFat,
                selectedActivity, activityDuration, selectedClimate,
                excessiveSweating, highUrine, highProtein,
                isPregnant, isBreastfeeding, hasFever, creatine);

        float drinkingGoal = totalWater * 0.80f;

        this.calculatedGoal = Math.round(drinkingGoal / 50.0f) * 50;
        this.txtRecommendedGoal.setText(getString(R.string.format_ml, this.calculatedGoal));
        this.resultCard.setVisibility(View.VISIBLE);

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
}