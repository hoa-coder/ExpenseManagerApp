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

public class AddExpenseActivity extends AppCompatActivity implements View.OnClickListener, FirebaseManager.OnCompleteListener {

    private static final int ADD_INCOME_REQUEST = 2;
    private TextView tvAmount;
    private EditText inputNote;
    private LinearLayout calculatorSection;
    private StringBuilder currentInput = new StringBuilder();
    private String selectedCategory = "Hàng ngày";
    private View selectedCategoryView = null;

    private Transaction pendingTransaction = null; // Dùng để lưu transaction trước khi gửi đi

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

        btnClose.setOnClickListener(v -> {
            // Tạo Intent để chuyển về BooksActivity
            // Bạn cần đảm bảo class BooksActivity tồn tại
            Intent intent = new Intent(AddExpenseActivity.this, BooksActivity.class);

            // Cờ FLAG_ACTIVITY_CLEAR_TOP sẽ xóa tất cả các Activity nằm trên BooksActivity
            // và đưa BooksActivity lên đầu, giúp ngăn xếp Activity sạch sẽ hơn.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish(); // Kết thúc AddExpenseActivity
        });

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
        inputNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (calculatorSection != null) {
                    calculatorSection.setVisibility(View.GONE);
                }
            } else {
                hideKeyboard();
                if (calculatorSection != null) {
                    calculatorSection.setVisibility(View.VISIBLE);
                }
            }
        });

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

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void setDefaultCategory() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            findAndSelectDefaultCategory(rootView);
        }
    }

    private void findAndSelectDefaultCategory(View view) {
        if (view instanceof LinearLayout) {
            Object tag = view.getTag();
            if (tag != null && tag.equals("Hàng ngày")) {
                // Manually call onCategoryClick to set initial state
                onCategoryClick(view);
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

    public void onCategoryClick(View view) {
        Object tag = view.getTag();
        if (tag == null) {
            return;
        }

        inputNote.clearFocus();
        hideKeyboard();

        String categoryName = tag.toString();

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

        view.setBackgroundResource(R.drawable.category_selected);
        selectedCategoryView = view;
        selectedCategory = categoryName;

        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            if (layout.getChildCount() >= 2) {
                View child = layout.getChildAt(1);
                if (child instanceof TextView) {
                    // Màu hồng là 0xFFEC407A
                    ((TextView) child).setTextColor(0xFFEC407A);
                    ((TextView) child).setTypeface(null, android.graphics.Typeface.BOLD);
                }
            }
        }

        Toast.makeText(this, "Đã chọn: " + categoryName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        inputNote.clearFocus();
        hideKeyboard();
        
        // --- Logic Calculator an toàn hơn ---
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
                
                // Xử lý số 0 ở đầu
                if (currentInput.length() == 0 && text.equals("0")) {
                    currentInput.append(text);
                } else if (currentInput.toString().equals("0") && !text.equals(".")) {
                    currentInput.setLength(0);
                    currentInput.append(text);
                } else {
                    currentInput.append(text);
                }
            }
        }

        if (currentInput.length() == 0 || currentInput.toString().isEmpty()) {
            tvAmount.setText("0");
            currentInput.setLength(0); // Đảm bảo currentInput rỗng
        } else {
            tvAmount.setText(currentInput.toString());
        }
        // --- Kết thúc Logic Calculator ---
    }

    private void saveExpense() {
        hideKeyboard();

        String amountStr = tvAmount.getText().toString();
        String note = inputNote.getText().toString().trim();

        // Validation an toàn hơn
        if (amountStr.equals("0") || amountStr.isEmpty() || amountStr.equals(".")) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryView == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // Tạo Transaction và lưu vào pendingTransaction
        pendingTransaction = new Transaction(
                null, // ID sẽ được Firestore tạo
                "EXPENSE",
                amount,
                selectedCategory,
                note,
                timestamp
        );

        // Lưu vào Firebase
        FirebaseManager.getInstance().saveTransaction(pendingTransaction, this);
        onSuccess("Giao dịch của bạn đã được lưu");
    }

    @Override
    public void onSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (pendingTransaction != null) {
            // Chuyển hướng trực tiếp về BooksActivity
            Intent intent = new Intent(this, BooksActivity.class);

            // Truyền giao dịch mới
            intent.putExtra("NEW_TRANSACTION", pendingTransaction);

            // Xóa ngăn xếp trên BooksActivity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
        }

        // Kết thúc AddExpenseActivity/AddIncomeActivity
        finish();
    }


    @Override
    public void onFailure(Exception e) {
        String errorMessage = "Lỗi khi lưu giao dịch";
        if (e != null && e.getMessage() != null) {
            errorMessage += ": " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // Reset transaction khi lưu thất bại
        pendingTransaction = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Nếu là giao dịch được thêm thành công từ màn hình Income, chuyển tiếp kết quả về BooksActivity
        if (requestCode == ADD_INCOME_REQUEST && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data); // Chuyển tiếp kết quả về BooksActivity
            finish();
        }
    }
}