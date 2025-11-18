package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        LinearLayout tabIncome = findViewById(R.id.tab_income); // Tìm tab Thu nhập

        btnClose.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveExpense());
        
        // Kiểm tra tabIncome có tồn tại trước khi gán listener
        if (tabIncome != null) {
            tabIncome.setOnClickListener(v -> {
                Intent intent = new Intent(AddExpenseActivity.this, AddIncomeActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình hiện tại để không bị chồng chéo
            });
        } else {
            // Có thể thêm log hoặc toast để debug nếu tab bị thiếu
             Toast.makeText(this, "Lỗi: Không tìm thấy tab Thu Nhập (tab_income)", Toast.LENGTH_LONG).show();
        }

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
        // Chỉ xử lý các nút calculator, không xử lý các nút khác
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            // Đảm bảo chỉ các nút calculator (là Button) mới được xử lý
            if (v instanceof Button) {
                String text = ((Button) v).getText().toString();
                // Ngăn chặn nhiều dấu chấm
                if (text.equals(".") && currentInput.toString().contains(".")) {
                    return;
                }
                currentInput.append(text);
            }
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

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }


        // --- PHẦN GIỮ CHỖ CHO LOGIC LƯU VÀO DATABASE ---
        // Ví dụ:
        // Transaction expense = new Transaction("Expense", amount, selectedCategory, note, System.currentTimeMillis());
        // dbHelper.addTransaction(expense);

        Toast.makeText(this, "Đã lưu chi tiêu: " + amount + " vào mục " + selectedCategory, Toast.LENGTH_LONG).show();
        finish();
    }
}