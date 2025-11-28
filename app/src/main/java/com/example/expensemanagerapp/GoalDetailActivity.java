package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Activity để xem chi tiết, chỉnh sửa, xóa và thêm/rút tiền mục tiêu tiết kiệm.
 */
public class GoalDetailActivity extends AppCompatActivity implements FirebaseManager.OnCompleteListener {

    public static final String EXTRA_GOAL = "extra_goal";
    public static final int REQUEST_CODE_EDIT_GOAL = 101;
    private static final String TAG = "GoalDetailActivity";

    private Goal currentGoal;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    
    // Views for Display Card
    private TextView tvIconEmoji;
    private TextView tvGoalNameDisplay;
    private TextView tvCurrentTarget;
    private ProgressBar progressBar;
    private TextView tvPercentage;

    // Views for Edit Fields
    private EditText etGoalName;
    private EditText etTargetAmount;
    private EditText etCurrentAmount;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private EditText etNote;
    
    // Action Buttons
    private ImageView btnClose;
    private ImageView btnSave;
    private Button btnAddFund;
    private Button btnWithdraw;
    private Button btnDeleteGoal;

    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    private DecimalFormat currencyFormatter = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_detail);

        // 1. Nhận dữ liệu mục tiêu
        currentGoal = (Goal) getIntent().getSerializableExtra(EXTRA_GOAL);

        if (currentGoal == null) {
            Toast.makeText(this, "Không tìm thấy dữ liệu mục tiêu.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadGoalData();
        setupListeners();
    }

    private void initViews() {
        // Display Views
        tvIconEmoji = findViewById(R.id.tv_icon_emoji);
        tvGoalNameDisplay = findViewById(R.id.tv_goal_name_display);
        tvCurrentTarget = findViewById(R.id.tv_current_target);
        progressBar = findViewById(R.id.progress_bar);
        tvPercentage = findViewById(R.id.tv_percentage);

        // Edit Views
        etGoalName = findViewById(R.id.et_goal_name);
        etTargetAmount = findViewById(R.id.et_target_amount);
        etCurrentAmount = findViewById(R.id.et_current_amount);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        etNote = findViewById(R.id.et_note);
        
        // Action Buttons
        btnClose = findViewById(R.id.btn_close);
        btnSave = findViewById(R.id.btn_save);
        btnAddFund = findViewById(R.id.btn_add_fund);
        btnWithdraw = findViewById(R.id.btn_withdraw);
        btnDeleteGoal = findViewById(R.id.btn_delete_goal);
    }

    /**
     * Điền dữ liệu mục tiêu vào các trường chỉnh sửa.
     */
    private void loadGoalData() {
        if (currentGoal == null) return;
        
        // Cập nhật thẻ hiển thị
        tvIconEmoji.setText(currentGoal.getIcon());
        tvGoalNameDisplay.setText(currentGoal.getName());
        updateProgressDisplay(currentGoal.getCurrentAmount(), currentGoal.getTargetAmount());

        // Điền vào trường chỉnh sửa
        etGoalName.setText(currentGoal.getName());
        etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount()));
        etCurrentAmount.setText(String.valueOf(currentGoal.getCurrentAmount()));
        etNote.setText(currentGoal.getNote());
        
        // Thiết lập ngày tháng
        tvStartDate.setText("Ngày bắt đầu: " + currentGoal.getStartDate());
        tvEndDate.setText("Ngày kết thúc: " + currentGoal.getEndDate());
        
        // Khởi tạo Calendar từ chuỗi ngày tháng (dùng cho DatePicker)
        try {
            startDateCalendar.setTime(dateFormatter.parse(currentGoal.getStartDate()));
            endDateCalendar.setTime(dateFormatter.parse(currentGoal.getEndDate()));
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi parse ngày tháng: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật hiển thị tiến độ (Thẻ Progress Card)
     */
    private void updateProgressDisplay(double current, double target) {
        String currentStr = currencyFormatter.format(current);
        String targetStr = currencyFormatter.format(target);
        tvCurrentTarget.setText(String.format("%s / %s", currentStr, targetStr));
        
        int percentage = (int) ((target == 0) ? 0 : (current / target) * 100);
        
        progressBar.setProgress(percentage);
        tvPercentage.setText(String.format("%d%% hoàn thành", percentage));
    }


    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveGoalChanges());
        btnDeleteGoal.setOnClickListener(v -> deleteGoalConfirmation());
        
        // Lắng nghe sự kiện click cho trường ngày tháng (cho phép chọn lại)
        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate, startDateCalendar));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate, endDateCalendar));
        
        // Xử lý Thêm tiền/Rút tiền
        btnAddFund.setOnClickListener(v -> showFundDialog(true));
        btnWithdraw.setOnClickListener(v -> showFundDialog(false));
    }
    
    /**
     * Hiển thị DatePicker cho việc chọn ngày.
     */
    private void showDatePicker(TextView textView, Calendar calendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String dateString = dateFormatter.format(calendar.getTime());
                    if (textView == tvStartDate) {
                        textView.setText("Ngày bắt đầu: " + dateString);
                    } else {
                        textView.setText("Ngày kết thúc: " + dateString);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    /**
     * Hiển thị dialog để thêm hoặc rút tiền.
     */
    private void showFundDialog(boolean isAdding) {
        String title = isAdding ? "Thêm tiền vào mục tiêu" : "Rút tiền khỏi mục tiêu";
        
        final EditText input = new EditText(this);
        input.setHint("Nhập số tiền");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(isAdding ? "Thêm" : "Rút", (dialog, which) -> {
                    String amountStr = input.getText().toString();
                    if (!TextUtils.isEmpty(amountStr)) {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            if (amount > 0) {
                                updateGoalFund(isAdding, amount);
                            } else {
                                Toast.makeText(this, "Số tiền phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Số tiền không hợp lệ.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Cập nhật số tiền hiện tại của mục tiêu và lưu vào Firebase.
     */
    private void updateGoalFund(boolean isAdding, double amount) {
        double newCurrentAmount = currentGoal.getCurrentAmount();
        
        if (isAdding) {
            newCurrentAmount += amount;
        } else {
            if (currentGoal.getCurrentAmount() < amount) {
                Toast.makeText(this, "Số tiền rút lớn hơn số tiền hiện có.", Toast.LENGTH_SHORT).show();
                return;
            }
            newCurrentAmount -= amount;
        }
        
        currentGoal.setCurrentAmount(newCurrentAmount);
        
        // Cập nhật UI ngay lập tức
        etCurrentAmount.setText(String.valueOf(newCurrentAmount));
        updateProgressDisplay(newCurrentAmount, currentGoal.getTargetAmount());

        // Lưu mục tiêu đã cập nhật
        FirebaseManager.getInstance().updateGoal(currentGoal, new GoalUpdateListener("fund"));
    }

    /**
     * Xử lý lưu các thay đổi về chi tiết mục tiêu.
     */
    private void saveGoalChanges() {
        String name = etGoalName.getText().toString().trim();
        String targetStr = etTargetAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        
        if (name.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đủ Tên và Số tiền Mục tiêu.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double targetAmount = Double.parseDouble(targetStr);
            double currentAmount = Double.parseDouble(etCurrentAmount.getText().toString());
            
            if (targetAmount <= 0) {
                 Toast.makeText(this, "Số tiền mục tiêu phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                 return;
            }
            if (currentAmount > targetAmount) {
                 Toast.makeText(this, "Số tiền hiện tại không thể lớn hơn mục tiêu.", Toast.LENGTH_SHORT).show();
                 return;
            }
            if (endDateCalendar.before(startDateCalendar)) {
                Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật đối tượng hiện tại
            currentGoal.setName(name);
            currentGoal.setTargetAmount(targetAmount);
            currentGoal.setCurrentAmount(currentAmount); // Trường hợp người dùng sửa tay
            currentGoal.setNote(note);
            currentGoal.setStartDate(dateFormatter.format(startDateCalendar.getTime()));
            currentGoal.setEndDate(dateFormatter.format(endDateCalendar.getTime()));
            
            // Cập nhật UI hiển thị
            tvGoalNameDisplay.setText(name);
            updateProgressDisplay(currentGoal.getCurrentAmount(), targetAmount);
            
            // Lưu vào Firebase
            FirebaseManager.getInstance().updateGoal(currentGoal, new GoalUpdateListener("save"));

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hiển thị xác nhận xóa mục tiêu.
     */
    private void deleteGoalConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Mục tiêu")
                .setMessage("Bạn có chắc chắn muốn xóa mục tiêu \"" + currentGoal.getName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseManager.getInstance().deleteGoal(currentGoal.getId(), new GoalUpdateListener("delete"));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    /**
     * Lớp Listener nội bộ để xử lý kết quả Firebase.
     */
    private class GoalUpdateListener implements FirebaseManager.OnCompleteListener {
        private final String action;

        public GoalUpdateListener(String action) {
            this.action = action;
        }

        @Override
        public void onSuccess(String message) {
            Toast.makeText(GoalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            
            if (action.equals("delete") || action.equals("save") || action.equals("fund")) {
                // Đặt kết quả cho BooksActivity và thoát
                setResult(RESULT_OK); 
                finish();
            }
        }

        @Override
        public void onFailure(Exception e) {
            Toast.makeText(GoalDetailActivity.this, "Thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // Unused method from interface (implemented by internal class GoalUpdateListener)
    @Override
    public void onSuccess(String message) {}

    @Override
    public void onFailure(Exception e) {}
}