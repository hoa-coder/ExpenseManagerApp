package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class CreateSavingsGoalActivity extends AppCompatActivity {

    private ImageView btnBack, btnDone;
    private EditText etGoalName, etTargetAmount, etCurrentAmount, etNote;
    private TextView tvStartDate, tvEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_savings_goal);

        // Initialize Views
        btnBack = findViewById(R.id.btn_back);
        btnDone = findViewById(R.id.btn_done);
        etGoalName = findViewById(R.id.et_goal_name);
        etTargetAmount = findViewById(R.id.et_target_amount);
        etCurrentAmount = findViewById(R.id.et_current_amount);
        etNote = findViewById(R.id.et_note);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);

        // Setup Listeners
        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> saveSavingsGoal());

        tvStartDate.setOnClickListener(v -> showDatePickerDialog(tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePickerDialog(tvEndDate));
    }

    private void showDatePickerDialog(final TextView dateTextView) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    dateTextView.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveSavingsGoal() {
        String goalName = etGoalName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String currentAmountStr = etCurrentAmount.getText().toString().trim();
        String startDate = tvStartDate.getText().toString().trim();
        String endDate = tvEndDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (goalName.isEmpty() || targetAmountStr.isEmpty() || startDate.equals("Chọn ngày") || endDate.equals("Chọn ngày")) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- PLACEHOLDER FOR DATABASE LOGIC ---
        // Here you would typically create a SavingsGoal object and
        // save it to your database.
        // For example:
        // SavingsGoal goal = new SavingsGoal(goalName, Double.parseDouble(targetAmountStr), Double.parseDouble(currentAmountStr), startDate, endDate, note);
        // dbHelper.addSavingsGoal(goal);

        Toast.makeText(this, "Đã lưu mục tiêu: " + goalName, Toast.LENGTH_SHORT).show();
        finish();
    }
}