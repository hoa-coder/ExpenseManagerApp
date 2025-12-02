package com.example.expensemanagerapp;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity quên mật khẩu - Gửi email reset password qua Firebase
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";

    private FirebaseAuth mAuth;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private Button btnSendResetEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        initViews();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);

        btnSendResetEmail.setOnClickListener(v -> sendPasswordResetEmail());

        // Nút Back (nếu có)
        if (findViewById(R.id.btn_back) != null) {
            findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        }
    }

    private void sendPasswordResetEmail() {
        String email = etEmail.getText().toString().trim();

        // ✅ Clear error trước
        tilEmail.setError(null);

        // ✅ Validation
        if (email.isEmpty()) {
            tilEmail.setError("Email không được để trống");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // ✅ Log để debug
        Log.d(TAG, "========================================");
        Log.d(TAG, "Bắt đầu gửi email reset password");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Firebase Auth: " + (mAuth != null ? "OK" : "NULL"));
        Log.d(TAG, "========================================");

        // ✅ Hiển thị loading
        showLoading(true);

        // ✅ Gọi Firebase sendPasswordResetEmail
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // ✅ THÀNH CÔNG - Firebase đã gửi email
                        Log.d(TAG, "✅ Email reset password đã được gửi thành công!");
                        Log.d(TAG, "Email đích: " + email);
                        Log.d(TAG, "Sender: noreply@expensemanagerapp-54d95.firebaseapp.com");

                        showSuccessMessage(email);

                        // Tự động quay lại màn đăng nhập sau 3 giây
                        new android.os.Handler().postDelayed(() -> {
                            finish();
                        }, 3000);

                    } else {
                        // ❌ THẤT BẠI - Có lỗi xảy ra
                        Exception exception = task.getException();

                        Log.e(TAG, "❌ Gửi email thất bại!");
                        if (exception != null) {
                            Log.e(TAG, "Exception: " + exception.getClass().getSimpleName());
                            Log.e(TAG, "Message: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        handleSendEmailError(exception);
                    }
                })
                .addOnFailureListener(e -> {
                    // ❌ Lỗi network hoặc Firebase
                    showLoading(false);
                    Log.e(TAG, "❌ onFailure: " + e.getMessage(), e);
                    Toast.makeText(this,
                            "Lỗi kết nối. Vui lòng kiểm tra internet và thử lại.",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * ✅ Hiển thị thông báo thành công
     */
    private void showSuccessMessage(String email) {
        String message = "✅ Email khôi phục mật khẩu đã được gửi!\n\n" +
                "📧 Email: " + email + "\n\n" +
                "Vui lòng kiểm tra:\n" +
                "• Hộp thư đến (Inbox)\n" +
                "• Thư rác (Spam/Junk)\n\n" +
                "⚠️ Lưu ý:\n" +
                "• Email có thể mất 1-5 phút để đến\n" +
                "• Sender: noreply@expensemanagerapp-54d95.firebaseapp.com\n" +
                "• Link reset có hiệu lực trong 1 giờ";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * ✅ Xử lý lỗi khi gửi email
     */
    private void handleSendEmailError(Exception exception) {
        String errorMessage;

        if (exception instanceof FirebaseAuthInvalidUserException) {
            // ⚠️ LƯU Ý: Firebase thường KHÔNG trả lỗi này để bảo mật
            // Nhưng vẫn xử lý cho chắc
            errorMessage = "Không tìm thấy tài khoản với email này.\n" +
                    "Vui lòng kiểm tra lại địa chỉ email.";
            tilEmail.setError("Email chưa được đăng ký");

        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            errorMessage = "Email không hợp lệ. Vui lòng kiểm tra lại.";
            tilEmail.setError("Email không đúng định dạng");

        } else if (exception != null && exception.getMessage() != null) {
            // Lỗi khác: network, Firebase server...
            String msg = exception.getMessage();

            // Xử lý một số lỗi phổ biến
            if (msg.contains("network")) {
                errorMessage = "Lỗi kết nối mạng. Vui lòng kiểm tra internet và thử lại.";
            } else if (msg.contains("timeout")) {
                errorMessage = "Timeout. Vui lòng thử lại sau.";
            } else {
                errorMessage = "Lỗi: " + msg;
            }

        } else {
            errorMessage = "Có lỗi xảy ra. Vui lòng thử lại sau.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * ✅ Hiển thị/ẩn trạng thái loading
     */
    private void showLoading(boolean isLoading) {
        btnSendResetEmail.setEnabled(!isLoading);

        if (isLoading) {
            btnSendResetEmail.setText("Đang gửi email...");
            etEmail.setEnabled(false);
        } else {
            btnSendResetEmail.setText("Gửi email khôi phục");
            etEmail.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ForgotPasswordActivity destroyed");
    }
}