package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtNewUsername, edtNewPassword, edtConfirmPassword;
    private Button btnRegisterSubmit, btnGoToLogin;
    private FirebaseAuth firebaseAuth;

    // Các hằng số Regex
    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.([a-zA-Z]{2,})$";
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}$";

    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);
    private static final Pattern PASSWORD_REGEX = Pattern.compile(PASSWORD_PATTERN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListener();
    }

    /**
     * Khởi tạo các view từ layout
     */
    private void initializeViews() {
        edtNewUsername = findViewById(R.id.edtNewUsername);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
    }

    /**
     * Thiết lập sự kiện click cho các nút
     */
    private void setupClickListener() {
        btnRegisterSubmit.setOnClickListener(v -> handleRegister());
        btnGoToLogin.setOnClickListener(v -> navigateToLogin());
    }

    /**
     * Xử lý logic đăng ký
     */
    private void handleRegister() {
        String email = getInputText(edtNewUsername);
        String password = getInputText(edtNewPassword);
        String confirmPassword = getInputText(edtConfirmPassword);

        // Validation
        if (!validateInputs(email, password, confirmPassword)) {
            return;
        }

        // Vô hiệu hóa nút để tránh click lặp lại
        setRegisterButtonEnabled(false);

        // Đăng ký với Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            showToast("Đăng ký thành công!");
                            navigateToLogin();
                        } else {
                            handleRegistrationError(task);
                            setRegisterButtonEnabled(true);
                        }
                    }
                });
    }

    /**
     * Lấy giá trị text từ EditText (đã trim)
     */
    private String getInputText(EditText editText) {
        return editText.getText().toString().trim();
    }

    /**
     * Kiểm tra toàn bộ input
     */
    private boolean validateInputs(String email, String password, String confirmPassword) {
        // Kiểm tra trường rỗng
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Vui lòng nhập đầy đủ thông tin");
            return false;
        }

        // Kiểm tra định dạng email
        if (!isValidEmail(email)) {
            showToast("Vui lòng nhập đúng định dạng Email");
            return false;
        }

        // Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirmPassword)) {
            showToast("Mật khẩu xác nhận không khớp");
            return false;
        }

        // Kiểm tra độ an toàn mật khẩu
        if (!isStrongPassword(password)) {
            showToast("Mật khẩu phải dài ít nhất 6 ký tự, bao gồm ít nhất một chữ hoa, " +
                    "một chữ thường, một số và một ký tự đặc biệt");
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra định dạng email
     */
    private boolean isValidEmail(String email) {
        return EMAIL_REGEX.matcher(email).matches();
    }

    /**
     * Kiểm tra độ an toàn của mật khẩu
     */
    private boolean isStrongPassword(String password) {
        return PASSWORD_REGEX.matcher(password).matches();
    }

    /**
     * Xử lý lỗi đăng ký từ Firebase
     */
    private void handleRegistrationError(Task<AuthResult> task) {
        String errorMessage = "Đăng ký thất bại.";
        if (task.getException() != null) {
            String exception = task.getException().getMessage();
            if (exception != null) {
                if (exception.contains("email")) {
                    errorMessage = "Email này đã được sử dụng";
                } else if (exception.contains("password")) {
                    errorMessage = "Mật khẩu không hợp lệ";
                } else {
                    errorMessage = "Đăng ký thất bại: " + exception;
                }
            }
        }
        showToast(errorMessage);
    }

    /**
     * Hiển thị Toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Điều hướng sang màn hình đăng nhập
     */
    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * Bật/Tắt nút đăng ký
     */
    private void setRegisterButtonEnabled(boolean enabled) {
        btnRegisterSubmit.setEnabled(enabled);
    }
}