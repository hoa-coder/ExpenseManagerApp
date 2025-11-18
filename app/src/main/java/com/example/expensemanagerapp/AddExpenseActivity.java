package com.example.expensemanagerapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ADD_INCOME_REQUEST = 2; // Define request code for Income
    private TextView tvAmount;
    private EditText inputNote;
    private LinearLayout calculatorSection;
    private StringBuilder currentInput = new StringBuilder();
    private String selectedCategory = "Hàng ngày";
    private View selectedCategoryView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        tvAmount = findViewById(R.id.tv_amount);
        inputNote = findViewById(R.id.input_note);
        calculatorSection = findViewById(R.id.calculator_section);

        // Nút điều khiển header
        ImageView btnClose = findViewById(R.id.btn_close);
        LinearLayout tabIncome = findViewById(R.id.tab_income);
        ImageView btnExpense = findViewById(R.id.btn_expense);

        btnClose.setOnClickListener(v -> finish());

        if (tabIncome != null) {
            tabIncome.setOnClickListener(v -> {
                Intent intent = new Intent(AddExpenseActivity.this, AddIncomeActivity.class);
                // Start Income Activity and wait for result (ADD_INCOME_REQUEST)
                startActivityForResult(intent, ADD_INCOME_REQUEST);
            });
        }

        if (btnExpense != null) {
            btnExpense.setOnClickListener(v -> saveExpense());
        }

        // --- Logic ẩn/hiện bàn phím và bộ tính toán (ĐƠN GIẢN HÓA) ---

        // 1. Khi focus vào inputNote
        inputNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Ẩn calculator
                if (calculatorSection != null) {
                    calculatorSection.setVisibility(View.GONE);
                }
            } else {
                // Hiện lại calculator và ẩn bàn phím
                hideKeyboard();
                if (calculatorSection != null) {
                    calculatorSection.setVisibility(View.VISIBLE);
                }
            }
        });

        // 2. Khi click vào tvAmount hoặc container của nó (Bắt đầu nhập số)
        tvAmount.setOnClickListener(v -> {
            inputNote.clearFocus();
            hideKeyboard();
        });

        View inputFieldLayout = (View) tvAmount.getParent();
        if (inputFieldLayout != null) {
            inputFieldLayout.setOnClickListener(v -> {
                inputNote.clearFocus();
                hideKeyboard();
            });
        }

        // --- End Logic ---

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
        findViewById(R.id.btn_delete).setOnClickListener(this);

        // Tìm và set category mặc định
        setDefaultCategory();
    }

    /**
     * ẨN bàn phím
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

    /**
     * Tìm và đánh dấu category mặc định
     */
    private void setDefaultCategory() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            findAndSelectDefaultCategory(rootView);
        }
    }

    /**
     * Đệ quy tìm category "Hàng ngày"
     */
    private void findAndSelectDefaultCategory(View view) {
        if (view instanceof LinearLayout) {
            Object tag = view.getTag();
            if (tag != null && tag.equals("Hàng ngày")) {
                selectedCategoryView = view;
                return;
            }
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findAndSelectDefaultCategory(group.getChildAt(i));
            }
        }
    }

    /**
     * Xử lý khi click vào category
     */
    public void onCategoryClick(View view) {
        Object tag = view.getTag();
        if (tag == null) {
            return;
        }

        // Ẩn bàn phím khi chọn danh mục
        inputNote.clearFocus();
        hideKeyboard();

        String categoryName = tag.toString();

        // Bỏ highlight category cũ
        if (selectedCategoryView != null) {
            selectedCategoryView.setBackgroundResource(0);

            if (selectedCategoryView instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) selectedCategoryView;
                if (layout.getChildCount() >= 2) {
                    View child = layout.getChildAt(1);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(0xFF212121);
                        ((TextView) child).setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            }
        }

        // Highlight category mới
        view.setBackgroundResource(R.drawable.category_selected);
        selectedCategoryView = view;
        selectedCategory = categoryName;

        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            if (layout.getChildCount() >= 2) {
                View child = layout.getChildAt(1);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(0xFFEC407A);
                    ((TextView) child).setTypeface(null, android.graphics.Typeface.BOLD);
                }
            }
        }

        Toast.makeText(this, "Đã chọn: " + categoryName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        // Ẩn bàn phím chữ khi nhấn nút số
        inputNote.clearFocus();
        hideKeyboard();

        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
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

    private void saveExpense() {
        hideKeyboard();

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

        long timestamp = System.currentTimeMillis();

        // --- LƯU MỚI VÀO JSON ---
        Transaction newTransaction = new Transaction("expense", amount, selectedCategory, note, timestamp);
        JsonHelper.addTransaction(this, newTransaction);

        // Chuẩn bị trả về cho BooksActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_transaction", newTransaction);
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "Đã lưu chi tiêu vào file JSON!", Toast.LENGTH_SHORT).show();

        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Nếu là giao dịch được thêm thành công từ màn hình Income, chuyển tiếp kết quả về BooksActivity
        if (requestCode == ADD_INCOME_REQUEST && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data); // Chuyển tiếp kết quả về BooksActivity
            finish(); // Finish AddExpenseActivity, passing the result up to BooksActivity
            return; // Stop execution after relaying
        }
        
        // Nếu giao dịch chi tiêu được lưu thành công (setResult(OK) was called in saveExpense()), or if result relay above finished.
        // We must only finish if the result is OK AND we are not the income relay case (handled above).
        if (resultCode == RESULT_OK && requestCode != ADD_INCOME_REQUEST) {
            finish(); 
        }
    }
}