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
    // Thêm cờ để biết nếu cần reload BooksActivity
    private boolean dataDeleted = false; 

    private ImageView ivAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvUid;
    private Button btnLogout;
    private ImageView btnBack;
    private TextView tvSecuritySettings;
    private TextView tvDataManagement;

    private FirebaseAuth mAuth;
    private FirebaseManager firebaseManager;

    // Define a custom functional interface for our confirmation click listener
    @FunctionalInterface
    interface ConfirmationAction {
        void run();
    }

    // Define a custom functional interface for our generic confirmation dialog logic
    @FunctionalInterface
    interface ConfirmationDialogClickListener {
        void onClick(View v, String title, String message, ConfirmationAction deleteAction);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance(); // Initialize FirebaseManager

        initViews();
        loadUserInfo();
    }
    
    @Override
    public void finish() {
        if (dataDeleted) {
            // Đặt kết quả OK nếu có dữ liệu bị xóa để BooksActivity có thể reload
            setResult(RESULT_OK); 
        }
        super.finish();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);
        tvSecuritySettings = findViewById(R.id.tv_security_settings);
        tvDataManagement = findViewById(R.id.tv_data_management);

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnLogout.setOnClickListener(v -> logout());

        tvSecuritySettings.setOnClickListener(v -> showChangePasswordDialog());

        tvDataManagement.setOnClickListener(v -> showDataManagementDialog());
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

    /**
     * Hiển thị dialog Quản lý dữ liệu và xử lý logic xóa dữ liệu.
     */
    private void showDataManagementDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_data_management, null);
        dialogBuilder.setView(dialogView);

        TextView btnDeleteIncome = dialogView.findViewById(R.id.btn_delete_income);
        TextView btnDeleteExpense = dialogView.findViewById(R.id.btn_delete_expense);
        TextView btnDeleteAllTransactions = dialogView.findViewById(R.id.btn_delete_all_transactions);
        TextView btnDeleteGoals = dialogView.findViewById(R.id.btn_delete_goals);
        TextView btnDeleteWallets = dialogView.findViewById(R.id.btn_delete_wallets);
        TextView btnDeleteAllData = dialogView.findViewById(R.id.btn_delete_all_data);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_data_management);

        AlertDialog dataManagementDialog = dialogBuilder.create();
        dataManagementDialog.show();

        // Helper for OnCompleteListener - now also dismisses the data management dialog on success
        FirebaseManager.OnCompleteListener deleteCompleteListener = new FirebaseManager.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> { // Đảm bảo chạy trên luồng UI
                    Toast.makeText(AccountActivity.this, message, Toast.LENGTH_SHORT).show();
                    dataDeleted = true; // Đặt cờ thành công
                    dataManagementDialog.dismiss(); // Dismiss the data management dialog after successful deletion
                    finish(); // Kết thúc AccountActivity để BooksActivity reload
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> { // Đảm bảo chạy trên luồng UI
                    Toast.makeText(AccountActivity.this, "Lỗi: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Delete operation failed", e);
                });
            }
        };

        // Generic confirmation dialog logic
        ConfirmationDialogClickListener confirmAndDeleteListener = (v, title, message, deleteAction) -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        // Hiển thị thông báo "Đang xóa..." ngay lập tức
                        Toast.makeText(AccountActivity.this, "Đang xóa...", Toast.LENGTH_SHORT).show();
                        
                        // === GỌI HÀM TẢI LẠI THEO YÊU CẦU CỦA BẠN (MÔ PHỎNG) ===
                        // LƯU Ý: Những hàm này không có chức năng tải lại UI, 
                        // mà chỉ là giả lập theo yêu cầu.
                        loadTransactionsData(); 
                        loadSavingsGoals();
                        // =======================================================
                        
                        deleteAction.run(); // Execute the actual delete operation
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        };

        btnDeleteIncome.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa các khoản thu", "Bạn có chắc chắn muốn xóa TẤT CẢ các khoản thu? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllTransactionsByType("INCOME", deleteCompleteListener));
        });

        btnDeleteExpense.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa các khoản chi", "Bạn có chắc chắn muốn xóa TẤT CẢ các khoản chi? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllTransactionsByType("EXPENSE", deleteCompleteListener));
        });

        btnDeleteAllTransactions.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa tất cả giao dịch", "Bạn có chắc chắn muốn xóa TẤT CẢ các khoản thu và chi? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllTransactions(deleteCompleteListener));
        });

        btnDeleteGoals.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa tất cả mục tiêu", "Bạn có chắc chắn muốn xóa TẤT CẢ các mục tiêu tiết kiệm? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllGoals(deleteCompleteListener));
        });

        btnDeleteWallets.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa tất cả ví", "Bạn có chắc chắn muốn xóa TẤT CẢ các ví và số dư liên quan? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllWallets(deleteCompleteListener));
        });

        btnDeleteAllData.setOnClickListener(v -> {
            confirmAndDeleteListener.onClick(v, "Xóa toàn bộ dữ liệu", "Bạn có chắc chắn muốn xóa TOÀN BỘ dữ liệu của bạn (giao dịch, mục tiêu, ví)? Hành động này không thể hoàn tác.",
                    () -> firebaseManager.deleteAllUserData(deleteCompleteListener));
        });

        btnCancel.setOnClickListener(v -> dataManagementDialog.dismiss());
    }
    
    // === CÁC PHƯƠNG THỨC MÔ PHỎNG THEO YÊU CẦU CỦA BẠN ===
    // Các hàm này không có chức năng tải dữ liệu thực tế trên màn hình AccountActivity
    private void loadTransactionsData() {
        Log.d(TAG, "Mô phỏng: loadTransactionsData() được gọi trong AccountActivity.");
    }
    
    private void loadSavingsGoals() {
        Log.d(TAG, "Mô phỏng: loadSavingsGoals() được gọi trong AccountActivity.");
    }
    // ========================================================
}