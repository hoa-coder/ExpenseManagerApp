package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity để tạo mới mục tiêu tiết kiệm, lưu vào Firebase Firestore.
 */
public class CreateSavingsGoalActivity extends AppCompatActivity implements FirebaseManager.OnCompleteListener {

    private EditText etGoalName;
    private EditText etTargetAmount;
    private EditText etCurrentAmount;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView tvIconEmoji;
    private EditText etNote;

    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String selectedIcon = "🚗"; // Icon mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_savings_goal);

        // Khởi tạo views
        initViews();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        etGoalName = findViewById(R.id.et_goal_name);
        etTargetAmount = findViewById(R.id.et_target_amount);
        etCurrentAmount = findViewById(R.id.et_current_amount);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvIconEmoji = findViewById(R.id.tv_icon_emoji);
        etNote = findViewById(R.id.et_note);

        // Set icon mặc định
        if (tvIconEmoji != null) {
            tvIconEmoji.setText(selectedIcon);
        }

        // Đếm ký tự ghi chú
        TextView tvNoteCounter = findViewById(R.id.tv_note_counter);
        if (etNote != null && tvNoteCounter != null) {
            etNote.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tvNoteCounter.setText(s.length() + "/200");
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void setupListeners() {
        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Nút lưu
        ImageView btnDone = findViewById(R.id.btn_done);
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> saveSavingsGoal());
        }

        // Chọn ngày bắt đầu
        LinearLayout layoutStartDate = findViewById(R.id.layout_start_date);
        if (layoutStartDate != null) {
            layoutStartDate.setOnClickListener(v -> showStartDatePicker());
        }

        // Chọn ngày kết thúc
        LinearLayout layoutEndDate = findViewById(R.id.layout_end_date);
        if (layoutEndDate != null) {
            layoutEndDate.setOnClickListener(v -> showEndDatePicker());
        }

        // Chọn biểu tượng
        LinearLayout layoutIcon = findViewById(R.id.layout_icon);
        if (layoutIcon != null) {
            layoutIcon.setOnClickListener(v -> showIconPicker());
        }

        // Chọn màu sắc
        LinearLayout layoutColor = findViewById(R.id.layout_color);
        if (layoutColor != null) {
            layoutColor.setOnClickListener(v -> showColorPicker());
        }
    }

    /**
     * Hiển thị DatePicker cho ngày bắt đầu
     */
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startDateCalendar.set(year, month, dayOfMonth);
                    tvStartDate.setText(dateFormatter.format(startDateCalendar.getTime()));
                    // Giả sử có R.color.pink hoặc dùng màu cố định
                    try {
                        tvStartDate.setTextColor(getResources().getColor(R.color.pink));
                    } catch (android.content.res.Resources.NotFoundException e) {
                        tvStartDate.setTextColor(0xFFFF6B9D); // Màu hồng từ books.xml
                    }
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Hiển thị DatePicker cho ngày kết thúc
     */
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endDateCalendar.set(year, month, dayOfMonth);
                    tvEndDate.setText(dateFormatter.format(endDateCalendar.getTime()));
                    // Giả sử có R.color.pink hoặc dùng màu cố định
                    try {
                        tvEndDate.setTextColor(getResources().getColor(R.color.pink));
                    } catch (android.content.res.Resources.NotFoundException e) {
                        tvEndDate.setTextColor(0xFFFF6B9D); // Màu hồng từ books.xml
                    }
                },
                endDateCalendar.get(Calendar.YEAR),
                endDateCalendar.get(Calendar.MONTH),
                endDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Hiển thị dialog chọn icon
     */
    private void showIconPicker() {
        // Danh sách emoji phổ biến
        final String[] icons = {"🚗", "💰", "🏠", "✈️", "📱", "💻", "🎓", "💍", "🎮", "🎸",
                "📷", "⌚", "👗", "👟", "🎂", "🍕", "☕", "🏖️", "🎭", "🎨"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn biểu tượng");
        builder.setItems(icons, (dialog, which) -> {
            selectedIcon = icons[which];
            tvIconEmoji.setText(selectedIcon);
        });
        builder.show();
    }

    /**
     * Hiển thị dialog chọn màu sắc
     */
    private void showColorPicker() {
        Toast.makeText(this, "Chức năng chọn màu đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    /**
     * Lưu mục tiêu tiết kiệm vào Firebase Firestore
     */
    private void saveSavingsGoal() {
        // Lấy dữ liệu từ form
        String goalName = etGoalName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String currentAmountStr = etCurrentAmount.getText().toString().trim();
        String startDate = tvStartDate.getText().toString();
        String endDate = tvEndDate.getText().toString();
        String note = etNote.getText().toString().trim();

        // Validation (giữ nguyên logic validation)
        if (goalName.isEmpty() || targetAmountStr.isEmpty() || targetAmountStr.equals("0") || startDate.equals("Chọn ngày") || endDate.equals("Chọn ngày")) {
            Toast.makeText(this, "Vui lòng điền đủ các thông tin cần thiết.", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount = 0;
        double currentAmount = 0;

        try {
            targetAmount = Double.parseDouble(targetAmountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền mục tiêu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!currentAmountStr.isEmpty()) {
                currentAmount = Double.parseDouble(currentAmountStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền hiện tại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDateCalendar.before(startDateCalendar)) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng Goal
        Goal newGoal = new Goal(
                null, // ID sẽ được Firestore tạo
                goalName,
                targetAmount,
                currentAmount,
                startDate,
                endDate,
                selectedIcon,
                note,
                System.currentTimeMillis()
        );

        // Lưu vào Firebase Firestore
        FirebaseManager.getInstance().saveGoal(newGoal, this);
        onSuccess("Thêm mục tiêu thành công");
    }

    @Override
    public void onSuccess(String message) {
        // Thay đổi thông báo Toast để rõ ràng hơn
        String toastMessage = "Thêm mục tiêu thành công.";
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

        // Chuyển về màn hình chính (MainActivity) và xóa hết các activity trên stack
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFailure(Exception e) {
        Toast.makeText(this, "Lỗi khi lưu mục tiêu: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    /**
     * Format số tiền
     */
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f", amount);
    }
}