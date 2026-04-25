package com.example.waterintaketracker;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private DataManager dataManager;
    private UserManager userManager;
    
    private TextView txtCurrentUsername;
    private TextView txtProfileStreak;
    private TextView txtProfileAverage;
    private TextView txtProfileGoal;
    private TextView txtTotalAllTime;
    private LinearLayout accountsListContainer;
    private View authCard;
    private View statsGrid;
    private TextView titleOtherAccounts;
    private View cardOtherAccounts;
    private TextInputEditText inputUsername, inputPassword;
    private ShapeableImageView imgProfilePicture;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveProfilePicture(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
            int systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars);
            return insets;
        });

        dataManager = new DataManager(this);
        userManager = new UserManager(this);

        initializeViews();
        setupListeners();
        updateUI();
    }

    private void initializeViews() {
        txtCurrentUsername = findViewById(R.id.txtCurrentUsername);
        txtProfileStreak = findViewById(R.id.txtProfileStreak);
        txtProfileAverage = findViewById(R.id.txtProfileAverage);
        txtProfileGoal = findViewById(R.id.txtProfileGoal);
        txtTotalAllTime = findViewById(R.id.txtTotalAllTime);
        accountsListContainer = findViewById(R.id.accountsListContainer);
        authCard = findViewById(R.id.authCard);
        statsGrid = findViewById(R.id.statsGrid);
        titleOtherAccounts = findViewById(R.id.titleOtherAccounts);
        cardOtherAccounts = findViewById(R.id.cardOtherAccounts);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            userManager.logout(this);
            Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
            updateUI();
        });

        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteConfirmation());

        findViewById(R.id.btnSignIn).setOnClickListener(v -> handleSignIn());
        findViewById(R.id.btnSignUp).setOnClickListener(v -> handleSignUp());
        
        findViewById(R.id.btnEditProfilePic).setOnClickListener(v -> {
            if (userManager.getCurrentUser() == null) {
                Toast.makeText(this, "Please sign in to set a profile picture", Toast.LENGTH_SHORT).show();
                return;
            }
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    private void updateUI() {
        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            txtCurrentUsername.setText(currentUser);
            authCard.setVisibility(View.GONE);
            findViewById(R.id.accountActions).setVisibility(View.VISIBLE);
            
            // Show stats when logged in
            statsGrid.setVisibility(View.VISIBLE);
            
            // Rename to "Other Registered Accounts" and show the list
            titleOtherAccounts.setText(R.string.other_accounts_stats);
            titleOtherAccounts.setVisibility(View.VISIBLE);
            cardOtherAccounts.setVisibility(View.VISIBLE);
            
            txtProfileStreak.setText(String.valueOf(dataManager.getCurrentStreak()));
            txtProfileAverage.setText(formatVolume(dataManager.getAverageIntake()));
            txtProfileGoal.setText(formatVolume(dataManager.getDailyGoal()));
            txtTotalAllTime.setText(formatVolume(dataManager.getTotalIntakeAllTime()));
            
            loadProfilePicture();
        } else {
            txtCurrentUsername.setText(R.string.guest_user);
            authCard.setVisibility(View.VISIBLE);
            findViewById(R.id.accountActions).setVisibility(View.GONE);
            
            // HIDE stats in Guest mode
            statsGrid.setVisibility(View.GONE);
            
            // Keep "All Registered Accounts" visible for guests
            titleOtherAccounts.setText(R.string.all_accounts_stats);
            titleOtherAccounts.setVisibility(View.VISIBLE);
            cardOtherAccounts.setVisibility(View.VISIBLE);
            
            imgProfilePicture.setImageResource(R.drawable.ic_user_profile);
            imgProfilePicture.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_600)));
        }
        
        loadOtherAccounts();
    }

    private String formatVolume(long amountMl) {
        if (amountMl >= 10000) {
            return getString(R.string.format_liters, amountMl / 1000.0f);
        } else {
            return getString(R.string.format_ml, (int) amountMl);
        }
    }

    private void saveProfilePicture(Uri uri) {
        String username = userManager.getCurrentUser();
        if (username == null) return;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            File file = new File(getFilesDir(), "profile_" + username + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            
            loadProfilePicture();
            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ProfileActivity", "Failed to save profile picture", e);
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePicture() {
        String username = userManager.getCurrentUser();
        if (username == null) return;

        File file = new File(getFilesDir(), "profile_" + username + ".jpg");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            imgProfilePicture.setImageBitmap(bitmap);
            imgProfilePicture.setImageTintList(null); // Remove the tint if we have a real photo
        } else {
            imgProfilePicture.setImageResource(R.drawable.ic_user_profile);
        }
    }

    private void handleSignIn() {
        String u = (inputUsername.getText() != null) ? inputUsername.getText().toString().trim() : "";
        String p = (inputPassword.getText() != null) ? inputPassword.getText().toString() : "";
        if (u.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, R.string.please_fill_all, Toast.LENGTH_SHORT).show();
            return;
        }
        if (userManager.login(u, p, this)) {
            Toast.makeText(this, getString(R.string.logged_in_toast, u), Toast.LENGTH_SHORT).show();
            updateUI();
        } else {
            // Check if user exists to offer reset
            if (userManager.getAllUsernames().contains(u)) {
                showResetAccountDialog(u, p);
            } else {
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showResetAccountDialog(String username, String newPassword) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_account_title)
                .setMessage(getString(R.string.reset_account_message, username))
                .setPositiveButton(R.string.btn_reset, (dialog, which) -> {
                    userManager.deleteAccount(username, this);
                    if (userManager.signUp(username, newPassword, this)) {
                        Toast.makeText(this, getString(R.string.account_reset_success, username), Toast.LENGTH_SHORT).show();
                    }
                    updateUI();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void handleSignUp() {
        String u = (inputUsername.getText() != null) ? inputUsername.getText().toString().trim() : "";
        String p = (inputPassword.getText() != null) ? inputPassword.getText().toString() : "";
        if (u.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, R.string.please_fill_all, Toast.LENGTH_SHORT).show();
            return;
        }
        if (userManager.signUp(u, p, this)) {
            Toast.makeText(this, getString(R.string.account_created, u), Toast.LENGTH_SHORT).show();
            updateUI();
        } else {
            Toast.makeText(this, R.string.username_exists, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    userManager.deleteAccount(userManager.getCurrentUser(), this);
                    updateUI();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadOtherAccounts() {
        accountsListContainer.removeAllViews();
        List<String> usernames = userManager.getAllUsernames();
        
        // When signed in, hide current user from the list. 
        // When guest, show everyone.
        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            usernames.remove(currentUser);
        }

        if (usernames.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(R.string.no_accounts_found);
            tv.setPadding(32, 32, 32, 32);
            accountsListContainer.addView(tv);
            return;
        }

        for (String name : usernames) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, accountsListContainer, false);
            TextView text1 = itemView.findViewById(android.R.id.text1);
            TextView text2 = itemView.findViewById(android.R.id.text2);
            
            long total = userManager.getUserTotalIntake(name);
            text1.setText(name);
            text1.setTextColor(ContextCompat.getColor(this, R.color.blue_900));
            text1.setTextSize(16);
            
            text2.setText(getString(R.string.total_all_time, formatVolume(total)));
            text2.setTextColor(ContextCompat.getColor(this, R.color.slate_500));
            
            itemView.setPadding(16, 16, 16, 16);
            accountsListContainer.addView(itemView);
        }
    }
}
