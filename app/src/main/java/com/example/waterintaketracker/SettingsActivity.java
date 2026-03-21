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
    private RadioButton radioMale;
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
        loadSavedData();
        ReminderScheduler.scheduleReminders(this);
    }

    private void initializeViews() {
        this.inputWeight = findViewById(R.id.inputWeight);
        this.inputAge = findViewById(R.id.inputAge);
        this.radioMale = findViewById(R.id.radioMale);
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
            this.inputWeight.setText(String.valueOf((int) this.dataManager.getUserWeight()));
            this.inputAge.setText(String.valueOf(this.dataManager.getUserAge()));
            if ("Female".equals(this.dataManager.getUserGender())) {
                RadioButton radioFemale = findViewById(R.id.radioFemale);
                if (radioFemale != null) radioFemale.setChecked(true);
            } else {
                this.radioMale.setChecked(true);
            }
            
            String savedActivity = this.dataManager.getActivityLevel();
            String[] activityValues = {"Sedentary", "Light", "Moderate", "Very Active", "Athlete"};
            for (int i = 0; i < activityValues.length; i++) {
                if (activityValues[i].equals(savedActivity)) {
                    this.spinnerActivity.setSelection(i);
                    this.selectedActivity = savedActivity;
                    break;
                }
            }
            
            String savedClimate = this.dataManager.getClimate();
            String[] climateValues = {"Cool", "Moderate", "Hot"};
            for (int i = 0; i < climateValues.length; i++) {
                if (climateValues[i].equals(savedClimate)) {
                    this.spinnerClimate.setSelection(i);
                    this.selectedClimate = savedClimate;
                    break;
                }
            }
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
                if (validateInputs()) {
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

    private void saveProfile() {
        float weight = Float.parseFloat(this.inputWeight.getText().toString());
        int age = Integer.parseInt(this.inputAge.getText().toString());
        String gender = this.radioMale.isChecked() ? "Male" : "Female";
        this.dataManager.saveUserProfile(weight, age, gender, this.selectedActivity, this.selectedClimate);
        this.dataManager.saveDailyGoal(this.calculatedGoal);
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
        if (validateInputs()) {
            saveProfile();
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

        float baseGoal = getBaseGoal(weight, age);
        this.calculatedGoal = Math.round(baseGoal / 50.0f) * 50;
        this.txtRecommendedGoal.setText(getString(R.string.format_ml, this.calculatedGoal));
        this.resultCard.setVisibility(View.VISIBLE);

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private float getBaseGoal(float weight, int age) {
        boolean isMale = this.radioMale.isChecked();
        boolean isPregnant = this.checkPregnancy.isChecked();
        boolean isBreastfeeding = this.checkBreastfeeding.isChecked();
        boolean hasFever = this.checkFever.isChecked();
        boolean excessiveSweating = this.checkExcessiveSweating.isChecked();
        boolean highUrine = this.checkHighUrine.isChecked();
        boolean highProtein = this.checkHighProtein.isChecked();
        boolean creatine = this.checkCreatine.isChecked();

        float multiplier = 1.0f;
        float base = (isMale ? 35.0f : 31.0f) * weight;

        if (age > 65) multiplier *= 0.85f;
        else if (age > 50) multiplier *= 0.9f;
        else if (age > 30) multiplier *= 0.95f;

        switch (this.selectedActivity) {
            case "Light": multiplier *= 1.2f; break;
            case "Moderate": multiplier *= 1.4f; break;
            case "Very Active": multiplier *= 1.6f; break;
            case "Athlete": multiplier *= 2.0f; break;
        }

        switch (this.selectedClimate) {
            case "Cool": break;
            case "Hot": multiplier *= 1.3f; break;
            default: multiplier *= 1.1f; break;
        }

        if (excessiveSweating) multiplier *= 1.2f;
        if (highUrine) multiplier *= 1.15f;
        if (highProtein) multiplier *= 1.1f;

        float goal = base * multiplier;
        if (isPregnant) goal += 300.0f;
        if (isBreastfeeding) goal += 700.0f;
        if (hasFever) goal += 500.0f;
        if (creatine) goal += 500.0f;

        return goal;
    }
}
