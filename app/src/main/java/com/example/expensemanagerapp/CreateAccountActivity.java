package com.example.expensemanagerapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    private TextView tvAccountType;
    private EditText etAccountName, etInitialBalance, etNote;
    private ImageView btnBack, btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Giả sử các ID trong file XML của bạn là như sau:
        tvAccountType = findViewById(R.id.tv_account_type);
        etAccountName = findViewById(R.id.et_account_name);
        etInitialBalance = findViewById(R.id.et_initial_balance);
        etNote = findViewById(R.id.et_note);
        btnBack = findViewById(R.id.btn_back);
        btnDone = findViewById(R.id.btn_done);

        // Lấy loại tài khoản được truyền từ Activity trước (nếu có)
        String accountType = getIntent().getStringExtra("ACCOUNT_TYPE");
        if (accountType != null && !accountType.isEmpty()) {
            tvAccountType.setText(accountType);
        }

        // Cài đặt sự kiện click
        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveAccount());
    }

    private void saveAccount() {
        String accountType = tvAccountType.getText().toString();
        String accountName = etAccountName.getText().toString().trim();
        String initialBalanceStr = etInitialBalance.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (accountName.isEmpty()) {
            etAccountName.setError("Tên tài khoản không được để trống");
            etAccountName.requestFocus();
            return;
        }

        if (initialBalanceStr.isEmpty()) {
            etInitialBalance.setError("Số dư ban đầu không được để trống");
            etInitialBalance.requestFocus();
            return;
        }

        double initialBalance;
        try {
            initialBalance = Double.parseDouble(initialBalanceStr);
        } catch (NumberFormatException e) {
            etInitialBalance.setError("Số dư không hợp lệ");
            etInitialBalance.requestFocus();
            return;
        }

        // --- PHẦN GIỮ CHỖ CHO LOGIC LƯU VÀO DATABASE ---
        // Ví dụ:
        // Wallet newWallet = new Wallet(accountName, accountType, initialBalance, note);
        // dbHelper.addAccount(newWallet);

        Toast.makeText(this, "Đã tạo ví '" + accountName + "' thành công!", Toast.LENGTH_LONG).show();
        finish();
    }
}