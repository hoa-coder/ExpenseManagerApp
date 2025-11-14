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

public class LoginActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin, btnRegister;
    // DatabaseHelper db; // Bỏ DatabaseHelper cục bộ

    private FirebaseAuth firebaseAuth; // Khai báo đối tượng Firebase Authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            String email = edtUsername.getText().toString().trim(); // Coi username là email cho Firebase
            String pass = edtPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                // Đăng nhập người dùng với Firebase Authentication
                firebaseAuth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Đăng nhập thành công
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    // Đăng nhập thất bại
                                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}
