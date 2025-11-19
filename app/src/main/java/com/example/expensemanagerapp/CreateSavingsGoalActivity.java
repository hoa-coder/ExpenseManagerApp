package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
 * Activity để tạo mới mục tiêu tiết kiệm
 */
public class CreateSavingsGoalActivity extends AppCompatActivity {

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

    private static final String PREFS_NAME = "SavingsGoalsPrefs";
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
                    tvStartDate.setTextColor(getResources().getColor(R.color.pink));
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
                    tvEndDate.setTextColor(getResources().getColor(R.color.pink));
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
     * Lưu mục tiêu tiết kiệm
     */
    private void saveSavingsGoal() {
        // Lấy dữ liệu từ form
        String goalName = etGoalName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String currentAmountStr = etCurrentAmount.getText().toString().trim();
        String startDate = tvStartDate.getText().toString();
        String endDate = tvEndDate.getText().toString();
        String note = etNote.getText().toString().trim();

        // Validation
        if (goalName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên mục tiêu", Toast.LENGTH_SHORT).show();
            etGoalName.requestFocus();
            return;
        }

        if (targetAmountStr.isEmpty() || targetAmountStr.equals("0")) {
            Toast.makeText(this, "Vui lòng nhập số tiền mục tiêu", Toast.LENGTH_SHORT).show();
            etTargetAmount.requestFocus();
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
            if (!currentAmountStr.isEmpty() && !currentAmountStr.equals("0")) {
                currentAmount = Double.parseDouble(currentAmountStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền hiện tại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.equals("Chọn ngày")) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.equals("Chọn ngày")) {
            Toast.makeText(this, "Vui lòng chọn ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra ngày kết thúc phải sau ngày bắt đầu
        if (endDateCalendar.before(startDateCalendar)) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu vào SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // Tạo key duy nhất dựa trên timestamp
        long timestamp = System.currentTimeMillis();
        String goalKey = "goal_" + timestamp;

        editor.putString(goalKey + "_name", goalName);
        editor.putString(goalKey + "_target", String.valueOf(targetAmount));
        editor.putString(goalKey + "_current", String.valueOf(currentAmount));
        editor.putString(goalKey + "_start_date", startDate);
        editor.putString(goalKey + "_end_date", endDate);
        editor.putString(goalKey + "_icon", selectedIcon);
        editor.putString(goalKey + "_note", note);
        editor.putLong(goalKey + "_timestamp", timestamp);

        // Lưu danh sách các key
        String existingKeys = sharedPref.getString("goal_keys", "");
        if (!existingKeys.isEmpty()) {
            existingKeys += ",";
        }
        existingKeys += goalKey;
        editor.putString("goal_keys", existingKeys);

        editor.apply();

        // Tính phần trăm hoàn thành
        double percentage = (currentAmount / targetAmount) * 100;

        Toast.makeText(this,
                "Đã lưu mục tiêu:\n" +
                        "Tên: " + goalName + "\n" +
                        "Mục tiêu: " + formatCurrency(targetAmount) + " VND\n" +
                        "Hiện tại: " + formatCurrency(currentAmount) + " VND\n" +
                        "Hoàn thành: " + String.format("%.1f", percentage) + "%\n" +
                        "Icon: " + selectedIcon,
                Toast.LENGTH_LONG).show();

        finish();
    }

    /**
     * Format số tiền
     */
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f", amount);
    }
}