package com.example.expensemanagerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Activity to handle the addition of new income transactions.
 */
public class AddIncomeActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvAmount;
    private EditText inputNote;
    private StringBuilder currentInput = new StringBuilder();

    // Kept these constants for consistency, though they are now unused in this file
    private static final String PREFS_NAME = "MyIncomePrefs";
    private static final String KEY_LAST_INCOME_AMOUNT = "lastIncomeAmount";
    private static final String KEY_LAST_INCOME_NOTE = "lastIncomeNote";
    private static final String KEY_LAST_INCOME_CATEGORY = "lastIncomeCategory";

    // Biến lưu danh mục đã chọn
    private String selectedCategory = "Tiền công"; // Mặc định
    private LinearLayout selectedCategoryLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        // Khởi tạo Views
        tvAmount = findViewById(R.id.tv_amount);
        inputNote = findViewById(R.id.input_note);

        // Nút điều khiển header
        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnIncome = findViewById(R.id.btn_income);
        LinearLayout tabExpense = findViewById(R.id.tab_expense);

        btnClose.setOnClickListener(v -> finish());

        // Nút lưu thu nhập
        if (btnIncome != null) {
            btnIncome.setOnClickListener(v -> saveIncome());
        }

        // Chuyển sang màn hình Chi tiêu
        if (tabExpense != null) {
            tabExpense.setOnClickListener(v -> {
                Intent intent = new Intent(AddIncomeActivity.this, AddExpenseActivity.class);
                startActivity(intent);
                finish();
            });
        }

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

        // Thiết lập click listener cho các danh mục thu nhập
        setupCategoryListeners();
    }

    /**
     * Thiết lập sự kiện click cho các danh mục thu nhập
     */
    private void setupCategoryListeners() {
        // Tìm GridLayout chứa các danh mục
        android.widget.GridLayout gridLayout = findViewById(R.id.income_category_grid);

        if (gridLayout != null) {
            // Duyệt qua tất cả các LinearLayout con (mỗi danh mục)
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                View child = gridLayout.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout categoryLayout = (LinearLayout) child;

                    // Lấy TextView chứa tên danh mục (child thứ 2)
                    if (categoryLayout.getChildCount() >= 2) {
                        View secondChild = categoryLayout.getChildAt(1);
                        if (secondChild instanceof TextView) {
                            TextView categoryName = (TextView) secondChild;

                            // Thiết lập click listener
                            categoryLayout.setOnClickListener(v -> {
                                selectCategory(categoryLayout, categoryName.getText().toString());
                            });

                            // Đánh dấu danh mục đầu tiên là được chọn
                            if (i == 0) {
                                selectCategory(categoryLayout, categoryName.getText().toString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Chọn danh mục thu nhập
     */
    private void selectCategory(LinearLayout categoryLayout, String categoryName) {
        // Bỏ chọn danh mục cũ
        if (selectedCategoryLayout != null) {
            selectedCategoryLayout.setBackgroundResource(0); // Bỏ background

            // Đổi màu text về màu mặc định (dựa trên giả định màu sắc trong layout)
            if (selectedCategoryLayout.getChildCount() >= 2) {
                View child = selectedCategoryLayout.getChildAt(1);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                    ((TextView) child).setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        }

        // Chọn danh mục mới
        selectedCategoryLayout = categoryLayout;
        selectedCategory = categoryName;
        categoryLayout.setBackgroundResource(R.drawable.category_selected);

        // Đổi màu text của danh mục được chọn
        if (categoryLayout.getChildCount() >= 2) {
            View child = categoryLayout.getChildAt(1);
            if (child instanceof TextView) {
                // Màu hồng thường là màu highlight
                ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.pink));
                ((TextView) child).setTypeface(null, android.graphics.Typeface.BOLD);
            }
        }

        Toast.makeText(this, "Đã chọn: " + categoryName, Toast.LENGTH_SHORT).show();
    }

    /**
     * Thu nhỏ bàn phím ảo.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onClick(View v) {
        // Chỉ xử lý các nút calculator
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            // Đảm bảo chỉ các nút calculator (là Button) mới được xử lý
            if (v instanceof Button) {
                String text = ((Button) v).getText().toString();
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
        hideKeyboard(); // Thu nhỏ bàn phím

        String amountStr = tvAmount.getText().toString();
        String note = inputNote.getText().toString().trim();

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

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- LƯU MỚI VÀO JSON ---
        long timestamp = System.currentTimeMillis();
        // Transaction("type", amount, category, note, timestamp)
        Transaction newTransaction = new Transaction("income", amount, selectedCategory, note, timestamp);
        JsonHelper.addTransaction(this, newTransaction);
        
        // --- FIX: Gửi Transaction object trực tiếp về cho Activity gọi để cập nhật ngay lập tức ---
        Intent resultIntent = new Intent();
        resultIntent.putExtra("NEW_TRANSACTION", newTransaction);

        Toast.makeText(this,
                "Đã lưu thu nhập vào lịch sử giao dịch:\n" +
                        "Số tiền: " + amount + " VND\n" +
                        "Danh mục: " + selectedCategory + "\n" +
                        "Ghi chú: " + (note.isEmpty() ? "(Không có)" : note),
                Toast.LENGTH_LONG).show();

        setResult(RESULT_OK, resultIntent); // Signal to BooksActivity to reload data
        finish();
    }
}