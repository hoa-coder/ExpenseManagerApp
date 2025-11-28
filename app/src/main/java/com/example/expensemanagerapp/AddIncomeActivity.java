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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Activity to handle the addition of new income transactions, saving to Firebase.
 */
public class AddIncomeActivity extends AppCompatActivity implements View.OnClickListener, FirebaseManager.OnCompleteListener {

    private TextView tvAmount;
    private EditText inputNote;
    private StringBuilder currentInput = new StringBuilder();

    // Biến lưu danh mục đã chọn
    private String selectedCategory = "Tiền công"; // Mặc định
    private LinearLayout selectedCategoryLayout = null;

    // LỖI #1: Tạo Transaction 2 lần gây mất đồng bộ dữ liệu
    // Giải pháp: Lưu transaction vào biến để tái sử dụng
    private Transaction pendingTransaction = null;

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

        btnClose.setOnClickListener(v -> {
            // Khởi tạo Intent để chuyển về BooksActivity
            // *Lưu ý: BooksActivity phải tồn tại trong project của bạn*
            Intent intent = new Intent(AddIncomeActivity.this, BooksActivity.class);

            // Thêm cờ để đảm bảo rằng nếu BooksActivity đã ở trên stack, nó sẽ được đưa lên đầu
            // và xóa các Activity khác ở phía trên nó (giúp tránh việc quay lại màn hình AddIncome)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish(); // Kết thúc AddIncomeActivity
        });

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
        android.widget.GridLayout gridLayout = findViewById(R.id.income_category_grid);

        // LỖI #2: Không kiểm tra null cho gridLayout
        // Nếu layout không tồn tại trong XML sẽ gây crash NullPointerException
        if (gridLayout == null) {
            return;
        }

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout categoryLayout = (LinearLayout) child;

                if (categoryLayout.getChildCount() >= 2) {
                    View secondChild = categoryLayout.getChildAt(1);
                    if (secondChild instanceof TextView) {
                        TextView categoryName = (TextView) secondChild;

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

    /**
     * Chọn danh mục thu nhập
     */
    private void selectCategory(LinearLayout categoryLayout, String categoryName) {
        // LỖI #3: Không kiểm tra tham số null
        // Nếu truyền null vào sẽ gây crash khi gọi categoryLayout.setBackgroundResource()
        if (categoryLayout == null || categoryName == null) {
            return;
        }

        if (selectedCategoryLayout != null) {
            selectedCategoryLayout.setBackgroundResource(0);

            if (selectedCategoryLayout.getChildCount() >= 2) {
                View child = selectedCategoryLayout.getChildAt(1);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                    ((TextView) child).setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        }

        selectedCategoryLayout = categoryLayout;
        selectedCategory = categoryName;
        categoryLayout.setBackgroundResource(R.drawable.category_selected);

        if (categoryLayout.getChildCount() >= 2) {
            View child = categoryLayout.getChildAt(1);
            if (child instanceof TextView) {
                // Giả định màu hồng là R.color.pink. Nếu không có, sẽ dùng màu cố định 0xFFEC407A
                int color = 0xFFEC407A;
                try {
                    color = ContextCompat.getColor(this, R.color.pink);
                } catch (android.content.res.Resources.NotFoundException ignored) {}

                ((TextView) child).setTextColor(color);
                ((TextView) child).setTypeface(null, android.graphics.Typeface.BOLD);
            }
        }

        Toast.makeText(this, "Đã chọn: " + categoryName, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_delete) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        } else {
            if (v instanceof Button) {
                String text = ((Button) v).getText().toString();
                // LỖI #4: Cho phép nhập nhiều dấu chấm (1.2.3 không hợp lệ)
                // Kiểm tra đã có dấu chấm chưa trước khi thêm
                if (text.equals(".") && currentInput.toString().contains(".")) {
                    return;
                }
                // LỖI #5: Cho phép nhập nhiều số 0 ở đầu (001, 002...)
                // Xử lý: Nếu đang là "0" và nhập số khác thì thay thế
                if (text.equals("0") && currentInput.length() == 0) {
                    currentInput.append(text);
                } else if (currentInput.toString().equals("0") && !text.equals(".")) {
                    currentInput.setLength(0);
                    currentInput.append(text);
                } else {
                    currentInput.append(text);
                }
            }
        }

        // LỖI #6: Không xử lý tốt trường hợp rỗng sau khi xóa hết
        if (currentInput.length() == 0 || currentInput.toString().isEmpty()) {
            tvAmount.setText("0");
        } else {
            tvAmount.setText(currentInput.toString());
        }
    }

    private void saveIncome() {
        hideKeyboard();

        String amountStr = tvAmount.getText().toString();
        String note = inputNote.getText().toString().trim();

        // LỖI #7: Không kiểm tra trường hợp chỉ nhập dấu chấm "."
        // Cần kiểm tra cả ".", không chỉ "0" và rỗng
        if (amountStr.equals("0") || amountStr.isEmpty() || amountStr.equals(".")) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            // LỖI #8: Không kiểm tra số âm hoặc bằng 0
            // User có thể nhập -100 hoặc parse ra 0 từ các trường hợp đặc biệt
            if (amount <= 0) {
                Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // LỖI #9: Tạo Transaction ở đây và lại tạo lần nữa trong onSuccess()
        // Vấn đề: timestamp khác nhau, dữ liệu không khớp giữa Firebase và UI
        // Giải pháp: Tạo 1 lần rồi lưu vào biến pendingTransaction để tái sử dụng
        pendingTransaction = new Transaction(
                null, // ID sẽ được Firestore tạo
                "INCOME", // Sử dụng chữ hoa cho Firestore
                amount,
                selectedCategory,
                note,
                timestamp
        );

        // Lưu vào Firebase
        FirebaseManager.getInstance().saveTransaction(pendingTransaction, this);
        // Không gọi finish() ở đây, mà chờ callback của Firebase
        onSuccess("Giao dịch đã được lưu");
    }

    @Override
    public void onSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (pendingTransaction != null) {
            // Thay vì setResult/finish(), chúng ta khởi tạo Intent để chuyển hướng trực tiếp
            Intent intent = new Intent(AddIncomeActivity.this, BooksActivity.class);

            // Gửi giao dịch mới qua Intent
            intent.putExtra("NEW_TRANSACTION", pendingTransaction);

            // Đảm bảo ngăn xếp hoạt động được làm sạch và BooksActivity được đưa lên đầu
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
        }

        // Dù đã chuyển hướng, việc gọi finish() vẫn là cần thiết để đóng AddIncomeActivity
        finish();
    }

    @Override
    public void onFailure(Exception e) {
        // LỖI #11: Không kiểm tra null cho exception và message
        // Nếu e hoặc e.getMessage() là null sẽ gây crash
        String errorMessage = "Lỗi khi lưu giao dịch";
        if (e != null && e.getMessage() != null) {
            errorMessage += ": " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // Reset transaction khi lưu thất bại để tránh dữ liệu cũ
        pendingTransaction = null;
    }
}