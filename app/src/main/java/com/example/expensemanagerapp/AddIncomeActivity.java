package com.example.expensemanagerapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddIncomeActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvAmount;
    private EditText inputNote;
    private StringBuilder currentInput = new StringBuilder();
    private String selectedCategory = "Lương"; // Giá trị mặc định cho thu nhập

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Giả sử layout tên là activity_add_income.xml và có cấu trúc tương tự add_expense
        setContentView(R.layout.activity_add_income);

        // Các ID dưới đây là giả định. Bạn cần đảm bảo chúng khớp với file XML của bạn.
        tvAmount = findViewById(R.id.tv_amount);
        inputNote = findViewById(R.id.input_note);

        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnDone = findViewById(R.id.btn_done);

        btnClose.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveIncome());

        // Các nút số
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_dot).setOnClickListener(this);

        // Nút chức năng
        findViewById(R.id.btn_delete).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            String text = ((Button) v).getText().toString();
            if (text.equals(".") && currentInput.toString().contains(".")) {
                return;
            }
            currentInput.append(text);
        }

        if (currentInput.length() == 0) {
            tvAmount.setText("0");
        } else {
            tvAmount.setText(currentInput.toString());
        }
    }

    private void saveIncome() {
        String amountStr = tvAmount.getText().toString();
        String note = inputNote.getText().toString().trim();

        if (amountStr.equals("0") || amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn một danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // --- PHẦN GIỮ CHỖ CHO LOGIC LƯU VÀO DATABASE ---
        // Ví dụ:
        // Transaction income = new Transaction("Income", amount, selectedCategory, note, System.currentTimeMillis());
        // dbHelper.addTransaction(income);

        Toast.makeText(this, "Đã lưu thu nhập: " + amount + " vào mục " + selectedCategory, Toast.LENGTH_LONG).show();
        finish();
    }
}