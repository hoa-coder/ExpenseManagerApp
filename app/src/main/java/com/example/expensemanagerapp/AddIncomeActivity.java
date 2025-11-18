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

/**
 * Activity to handle the addition of new income transactions.
 */
public class AddIncomeActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText amountEt, noteEt;
    private TextView categoryTv;
    private Button saveBtn;
    private TextView tvAmount; // Thêm cho logic calculator
    private EditText inputNote; // Thêm cho logic calculator
    private StringBuilder currentInput = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income); 
        
        // Khởi tạo Views
        // Giả định: activity_add_income.xml sử dụng các ID của calculator và header giống activity_add_expense.
        tvAmount = findViewById(R.id.tv_amount);
        inputNote = findViewById(R.id.input_note);
        
        // Nút điều khiển header
        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnDone = findViewById(R.id.btn_done);
        LinearLayout tabExpense = findViewById(R.id.tab_expense); // Tìm tab Chi tiêu

        btnClose.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveIncome());
        
        // Gán sự kiện click cho tab Chi tiêu (Tab Chi tiêu nằm trong layout activity_add_income)
        tabExpense.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, AddExpenseActivity.class);
            startActivity(intent);
            finish(); // Đóng màn hình hiện tại để không bị chồng chéo
        });

        // Các nút số (Calculator logic)
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
        findViewById(R.id.btn_delete).setOnClickListener(this);
        
        // Cập nhật lại các ID cũ nếu bạn muốn giữ lại (tôi đã thay thế bằng logic calculator)
        // amountEt = findViewById(R.id.amount_et); // Nếu ID này tồn tại
        // noteEt = findViewById(R.id.note_et); // Nếu ID này tồn tại
        // categoryTv = findViewById(R.id.category_tv); // Nếu ID này tồn tại
        // saveBtn = findViewById(R.id.save_btn); // Nếu ID này tồn tại
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            // Kiểm tra xem view có phải là Button không
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

    /**
     * Handles the logic for validating and saving the income transaction.
     */
    private void saveIncome() {
        String amountStr = tvAmount.getText().toString();
        String note = inputNote.getText().toString().trim();
        String selectedCategory = "Lương"; // Giá trị mặc định

        if (amountStr.equals("0") || amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Đã lưu thu nhập: " + amount + " vào mục " + selectedCategory, Toast.LENGTH_LONG).show();
        finish();
    }
}