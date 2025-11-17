package com.example.expensemanagerapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddExpenseActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvAmount;
    private EditText inputNote;
    private StringBuilder currentInput = new StringBuilder();
    private String selectedCategory = "Hàng ngày"; // Giá trị mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        tvAmount = findViewById(R.id.tv_amount);
        inputNote = findViewById(R.id.input_note);

        // Nút điều khiển header
        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnDone = findViewById(R.id.btn_done);

        btnClose.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveExpense());

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
        // Các nút toán tử (+, -, x, /) có thể được thêm logic phức tạp hơn nếu cần
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            String text = ((Button) v).getText().toString();
            // Ngăn chặn nhiều dấu chấm
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

    private void saveExpense() {
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
        // Transaction expense = new Transaction("Expense", amount, selectedCategory, note, System.currentTimeMillis());
        // dbHelper.addTransaction(expense);

        Toast.makeText(this, "Đã lưu chi tiêu: " + amount + " vào mục " + selectedCategory, Toast.LENGTH_LONG).show();
        finish();
    }
}