package com.example.expensemanagerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Màn hình hiển thị thông tin tài khoản và xử lý đăng xuất.
 */
public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";

    private ImageView ivAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvUid;
    private Button btnLogout;
    private ImageView btnBack;
    private TextView tvSecuritySettings; // New field for security settings

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadUserInfo();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);
        tvSecuritySettings = findViewById(R.id.tv_security_settings); // Initialize new view

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnLogout.setOnClickListener(v -> logout());

        // New click listener for "Cài đặt bảo mật"
        tvSecuritySettings.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            tvDisplayName.setText(displayName != null && !displayName.isEmpty() ? displayName : "Chưa đặt tên");

            tvEmail.setText(user.getEmail());

            tvUid.setText("UID: " + user.getUid());

            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                // TODO: Sử dụng Glide/Picasso để load ảnh vào ivAvatar nếu đã thêm dependency
                Log.d(TAG, "Avatar URL: " + photoUrl.toString());
            }
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Xử lý Đăng xuất: signOut, xóa navigation stack và chuyển về LoginActivity.
     */
    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Đã đăng xuất thành công.", Toast.LENGTH_SHORT).show();

        // Điều hướng về LoginActivity và xóa tất cả Activity cũ
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc AccountActivity hiện tại
        finish();
    }

    /**
     * Hiển thị dialog Đổi Mật Khẩu và xử lý logic.
     */
    private void showChangePasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Lỗi: Không thể đổi mật khẩu. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        dialogBuilder.setView(dialogView);

        // Views
        TextInputLayout tilCurrentPassword = dialogView.findViewById(R.id.til_current_password);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.til_new_password);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextInputLayout tilConfirmNewPassword = dialogView.findViewById(R.id.til_confirm_new_password);
        TextInputEditText etConfirmNewPassword = dialogView.findViewById(R.id.et_confirm_new_password);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnChange = dialogView.findViewById(R.id.btn_change);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        // Handle Cancel button
        btnCancel.setOnClickListener(v -> alertDialog.dismiss());

        // Handle Change Password button
        btnChange.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();

            // Clear previous errors
            tilCurrentPassword.setError(null);
            tilNewPassword.setError(null);
            tilConfirmNewPassword.setError(null);

            boolean isValid = true;

            if (currentPassword.isEmpty()) {
                tilCurrentPassword.setError("Mật khẩu hiện tại không được để trống.");
                isValid = false;
            }
            if (newPassword.isEmpty()) {
                tilNewPassword.setError("Mật khẩu mới không được để trống.");
                isValid = false;
            } else if (newPassword.length() < 6) {
                tilNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự.");
                isValid = false;
            }
            if (confirmNewPassword.isEmpty()) {
                tilConfirmNewPassword.setError("Xác nhận mật khẩu mới không được để trống.");
                isValid = false;
            }
            if (!newPassword.equals(confirmNewPassword)) {
                tilNewPassword.setError("Mật khẩu mới và xác nhận mật khẩu không khớp.");
                tilConfirmNewPassword.setError("Mật khẩu mới và xác nhận mật khẩu không khớp.");
                isValid = false;
            }

            if (isValid) {
                // 1. Re-authenticate user
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 2. Update password
                            user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        // 3. Success: Show message, logout, and navigate to Login
                                        Toast.makeText(AccountActivity.this, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                                        alertDialog.dismiss();
                                        logout();
                                    } else {
                                        // Handle update password failure (e.g., weak password, although we validated it)
                                        Log.e(TAG, "Update password failed", updateTask.getException());
                                        Toast.makeText(AccountActivity.this, "Lỗi: Không thể đổi mật khẩu. " + updateTask.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                        } else {
                            // Handle re-authentication failure (i.e., incorrect current password)
                            Log.e(TAG, "Re-authentication failed", task.getException());
                            tilCurrentPassword.setError("Mật khẩu hiện tại không đúng.");
                        }
                    });
            }
        });
    }
}