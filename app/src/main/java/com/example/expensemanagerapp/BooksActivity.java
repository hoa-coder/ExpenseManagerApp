package com.example.expensemanagerapp;

import android.content.Context;
import android.content.DialogInterface; // New: For AlertDialog buttons
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.NumberPicker; // Used for custom picker

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // New: For Custom Dialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BooksActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ADD_TRANSACTION_REQUEST = 1;
    private static final int EDIT_GOAL_REQUEST = 2; // Request code mới
    private static final String TAG = "BooksActivity";

    private FloatingActionButton fabEdit;
    private LinearLayout bottomNavBooks, bottomNavWallet, bottomNavAnalysis, bottomNavMe;
    private LinearLayout llAddGoal;
    private LinearLayout llGoalsGrid;
    private LinearLayout llTransactions;
    private TextView tvSeeAllTransactions;

    // Các Views và State mới cho Thống kê theo tháng
    private TextView tvSelectedMonthYear;
    private TextView tvTotalAmount;
    private TextView tvIncomeAmount;
    private TextView tvExpenseAmount;
    private LinearLayout monthYearPickerLayout;
    private ImageView ivToggleVisibility; 

    private Calendar currentCalendar; // State for selected month/year
    
    // State for amount visibility and stored financial data
    private boolean isAmountVisible = true;
    private double currentTotalIncome = 0;
    private double currentTotalExpense = 0;

    // Lưu trữ danh sách mục tiêu đã tải để truy cập nhanh khi người dùng click
    private Map<String, Goal> loadedGoalsMap = new HashMap<>();

    private Map<String, Boolean> expandedDates = new HashMap<>();
    private Map<String, LinearLayout> transactionContainers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        // Kiểm tra xem người dùng đã đăng nhập chưa
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Nếu chưa đăng nhập, chuyển về màn hình đăng nhập
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Khởi tạo Views
        fabEdit = findViewById(R.id.fab_edit);
        llAddGoal = findViewById(R.id.ll_add_goal);
        llGoalsGrid = findViewById(R.id.ll_goals_grid);
        llTransactions = findViewById(R.id.ll_transactions);
        tvSeeAllTransactions = findViewById(R.id.tv_see_all_transactions);

        // Khởi tạo Views mới cho thống kê
        tvSelectedMonthYear = findViewById(R.id.tv_selected_month_year);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvIncomeAmount = findViewById(R.id.tv_income_amount);
        tvExpenseAmount = findViewById(R.id.tv_expense_amount);
        monthYearPickerLayout = findViewById(R.id.month_year_picker_layout);
        ivToggleVisibility = findViewById(R.id.iv_toggle_visibility); 

        // Khởi tạo State (mặc định là tháng hiện tại)
        currentCalendar = Calendar.getInstance();
        
        // Khởi tạo Bottom Navigation Views
        LinearLayout bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            // Lấy các tab theo Index
            bottomNavBooks = (LinearLayout) bottomNavigation.getChildAt(0);
            bottomNavWallet = (LinearLayout) bottomNavigation.getChildAt(1);
            bottomNavAnalysis = (LinearLayout) bottomNavigation.getChildAt(2);
            // Tab "Tôi" được lấy theo ID do nó có ID riêng trong layout
            bottomNavMe = findViewById(R.id.bottom_navigation_me);
        }

        // Set Listeners an toàn hơn
        if (fabEdit != null) fabEdit.setOnClickListener(this);
        if (llAddGoal != null) llAddGoal.setOnClickListener(this);
        if (bottomNavBooks != null) bottomNavBooks.setOnClickListener(this);
        if (bottomNavWallet != null) bottomNavWallet.setOnClickListener(this);
        if (bottomNavAnalysis != null) bottomNavAnalysis.setOnClickListener(this);
        if (bottomNavMe != null) bottomNavMe.setOnClickListener(this);
        if (monthYearPickerLayout != null) monthYearPickerLayout.setOnClickListener(this); // Thêm listener cho Date Picker
        if (ivToggleVisibility != null) ivToggleVisibility.setOnClickListener(this); // New listener for visibility toggle
        if (tvSeeAllTransactions != null) {
            tvSeeAllTransactions.setOnClickListener(this);
            tvSeeAllTransactions.setText("Thu gọn");
        }

        // Tải dữ liệu ban đầu
        updateDateUI();
        loadMonthlyData(); // Tải dữ liệu cho tháng hiện tại
        loadSavingsGoals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại đăng nhập sau khi quay lại (trường hợp user logout xong back)
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
             startActivity(new Intent(this, LoginActivity.class));
             finish();
             return;
        }
        loadMonthlyData(); // Tải lại dữ liệu cho tháng đang chọn
        loadSavingsGoals();
    }

    /**
     * Cập nhật giao diện hiển thị tháng/năm đang chọn.
     */
    private void updateDateUI() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        tvSelectedMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));
    }

    /**
     * Hiển thị Custom Dialog chọn tháng/năm (sử dụng 2 NumberPicker).
     */
    private void showMonthYearPicker() {
        // 1. Tạo Custom View chứa 2 NumberPicker
        LinearLayout pickerLayout = new LinearLayout(this);
        pickerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        pickerLayout.setOrientation(LinearLayout.HORIZONTAL);
        pickerLayout.setGravity(android.view.Gravity.CENTER);
        pickerLayout.setPadding(32, 32, 32, 32);

        // 2. Month Picker
        final NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0); // Calendar.JANUARY
        monthPicker.setMaxValue(11); // Calendar.DECEMBER
        String[] displayedMonths = new String[12];
        for (int i = 0; i < 12; i++) {
            // Hiển thị tháng từ 1 đến 12
            displayedMonths[i] = String.valueOf(i + 1); 
        }
        monthPicker.setDisplayedValues(displayedMonths);
        monthPicker.setValue(currentCalendar.get(Calendar.MONTH));
        
        // Thêm margin để ngăn cách
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        monthParams.setMarginEnd(32);
        monthPicker.setLayoutParams(monthParams);
        
        pickerLayout.addView(monthPicker);

        // 3. Year Picker
        final NumberPicker yearPicker = new NumberPicker(this);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        yearPicker.setMinValue(1900); // Yêu cầu: 1900
        yearPicker.setMaxValue(2099); // Yêu cầu: 2099
        yearPicker.setValue(currentYear);

        LinearLayout.LayoutParams yearParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        yearPicker.setLayoutParams(yearParams);
        
        pickerLayout.addView(yearPicker);
        
        // 4. Tạo và hiển thị AlertDialog
        new AlertDialog.Builder(this)
            .setTitle("Chọn Tháng và Năm")
            .setView(pickerLayout)
            .setPositiveButton("Chọn", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int selectedMonth = monthPicker.getValue(); // 0-indexed month
                    int selectedYear = yearPicker.getValue();

                    // Cập nhật state
                    currentCalendar.set(Calendar.YEAR, selectedYear);
                    currentCalendar.set(Calendar.MONTH, selectedMonth);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, 1); // Giữ ngày cố định

                    updateDateUI();
                    loadMonthlyData(); // Tải lại dữ liệu
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }


    /**
     * Tải dữ liệu giao dịch từ Firebase Firestore cho tháng/năm đang chọn.
     */
    private void loadMonthlyData() {
        FirebaseManager manager = FirebaseManager.getInstance();
        if (manager == null) return; 
        
        CollectionReference transactionsRef = manager.getUserCollectionRef(FirebaseManager.TRANSACTIONS_COLLECTION);
        if (transactionsRef == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                 Toast.makeText(this, "Không thể tải dữ liệu: Lỗi khởi tạo Firebase.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // 1. Tính toán mốc thời gian (timestamp) đầu và cuối tháng
        Calendar startCal = (Calendar) currentCalendar.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startTime = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) currentCalendar.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long endTime = endCal.getTimeInMillis();

        // 2. Query Firebase Firestore
        transactionsRef
                // Lọc theo khoảng thời gian
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> monthlyTransactions = new ArrayList<>();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            monthlyTransactions.add(transaction);
                            
                            // Tính tổng cho tháng đang chọn
                            if (transaction.getType().equalsIgnoreCase("income")) {
                                totalIncome += transaction.getAmount();
                            } else {
                                totalExpense += transaction.getAmount();
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Transaction: " + e.getMessage());
                        }
                    }

                    // Store totals
                    currentTotalIncome = totalIncome;
                    currentTotalExpense = totalExpense;

                    // Cập nhật tổng quan tài chính cho tháng
                    updateFinancialOverview(currentTotalIncome, currentTotalExpense);

                    // Hiển thị danh sách giao dịch (chỉ hiển thị các giao dịch đã lọc)
                    processAndDisplayTransactions(monthlyTransactions);

                    if (monthlyTransactions.isEmpty()) {
                         // Toast.makeText(this, "Chưa có giao dịch nào trong tháng này.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải giao dịch: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading transactions", e);
                });
    }

    /**
     * Toggles the visibility state of financial amounts.
     */
    private void toggleAmountVisibility() {
        isAmountVisible = !isAmountVisible;
        
        // Cập nhật icon
        if (ivToggleVisibility != null) {
            int iconResource = isAmountVisible 
                ? android.R.drawable.ic_menu_view // Icon mắt mở
                : android.R.drawable.ic_lock_silent_mode; // Icon mắt đóng/im lặng
            
            ivToggleVisibility.setImageResource(iconResource);
            // Thiết lập lại màu tint như trong XML (màu xám)
            ivToggleVisibility.setColorFilter(Color.parseColor("#616161"), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        // Cập nhật UI ngay lập tức
        updateFinancialOverview(currentTotalIncome, currentTotalExpense);
    }

    /**
     * Cập nhật hiển thị tổng quan tài chính (Tổng số, Thu nhập, Chi tiêu) cho tháng đang chọn.
     */
    private void updateFinancialOverview(double totalIncome, double totalExpense) {
        
        // 1. Kiểm tra trạng thái ẩn/hiện
        if (!isAmountVisible) {
            String hiddenText = "******* đ"; // Dùng ký tự ẩn

            // Cập nhật TextViews với văn bản ẩn
            if (tvTotalAmount != null) tvTotalAmount.setText(hiddenText);
            if (tvIncomeAmount != null) tvIncomeAmount.setText(hiddenText);
            if (tvExpenseAmount != null) tvExpenseAmount.setText(hiddenText);
            
            // Giữ màu sắc hiển thị đúng trạng thái (dương/âm) ngay cả khi ẩn
            int colorGreen = ContextCompat.getColor(this, android.R.color.holo_green_dark);
            int colorRed = ContextCompat.getColor(this, android.R.color.holo_red_dark);
            
            double totalAmount = totalIncome - totalExpense;
            if (tvTotalAmount != null) {
                if (totalAmount > 0) {
                    tvTotalAmount.setTextColor(colorGreen);
                } else if (totalAmount < 0) {
                    tvTotalAmount.setTextColor(colorRed);
                } else {
                    tvTotalAmount.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
            }
            if (tvIncomeAmount != null) tvIncomeAmount.setTextColor(colorGreen);
            if (tvExpenseAmount != null) tvExpenseAmount.setTextColor(colorRed);

            return;
        }

        // --- Logic Hiển thị bình thường ---
        double totalAmount = totalIncome - totalExpense;

        // Sử dụng đơn vị "đ" (đồng)
        DecimalFormat formatter = new DecimalFormat("#,### đ"); 
        
        // 1. Cập nhật Tổng số (Total)
        if (tvTotalAmount != null) {
            tvTotalAmount.setText(formatter.format(Math.abs(totalAmount)));
            
            // Cài đặt lại màu sắc dựa trên yêu cầu
            int colorGreen = ContextCompat.getColor(this, android.R.color.holo_green_dark);
            int colorRed = ContextCompat.getColor(this, android.R.color.holo_red_dark);
            
            if (totalAmount > 0) {
                tvTotalAmount.setTextColor(colorGreen);
            } else if (totalAmount < 0) {
                tvTotalAmount.setTextColor(colorRed);
            } else {
                tvTotalAmount.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            }
        }
        
        // 2. Cập nhật Thu nhập (Income)
        if (tvIncomeAmount != null) {
            tvIncomeAmount.setText(formatter.format(totalIncome));
            tvIncomeAmount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }

        // 3. Cập nhật Chi tiêu (Expense)
        if (tvExpenseAmount != null) {
            tvExpenseAmount.setText(formatter.format(totalExpense));
            tvExpenseAmount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }


    /**
     * Xử lý nhóm và hiển thị các giao dịch.
     * @param transactions Danh sách giao dịch đã được lọc cho tháng đang chọn.
     */
    private void processAndDisplayTransactions(List<Transaction> transactions) {
        if (llTransactions == null) return; // Kiểm tra null an toàn
        llTransactions.removeAllViews();
        transactionContainers.clear();
        expandedDates.clear();

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        String currentDay = "";
        double totalIncomeForDay = 0;
        double totalExpenseForDay = 0;

        for (Transaction transaction : transactions) {
            String transactionDay = dayFormat.format(new Date(transaction.getTimestamp()));

            if (!transactionDay.equals(currentDay) && !currentDay.isEmpty()) {
                updateDayGroupSummary(currentDay, totalIncomeForDay, totalExpenseForDay);
                totalIncomeForDay = 0;
                totalExpenseForDay = 0;
            }

            if (transaction.getType().equalsIgnoreCase("income")) {
                totalIncomeForDay += transaction.getAmount();
            } else {
                totalExpenseForDay += transaction.getAmount();
            }

            if (!transactionDay.equals(currentDay)) {
                addDayGroupHeader(transactionDay, true);
                currentDay = transactionDay;
            }

            addTransactionToDayGroup(transaction, transactionDay);
        }

        if (!currentDay.isEmpty()) {
            updateDayGroupSummary(currentDay, totalIncomeForDay, totalExpenseForDay);
        }
    }


    /**
     * Adds a clickable header for a new day group.
     */
    private void addDayGroupHeader(final String dateString, boolean expandInitially) {
        if (llTransactions == null) return;
        // 1. Create the container for the transactions of this day
        LinearLayout dailyTransactionContainer = new LinearLayout(this);
        dailyTransactionContainer.setOrientation(LinearLayout.VERTICAL);
        dailyTransactionContainer.setId(View.generateViewId()); // Assign a unique ID
        transactionContainers.put(dateString, dailyTransactionContainer);
        expandedDates.put(dateString, expandInitially);

        // 2. Create the header view using code
        // Do không có layout transaction_item trong context, ta sẽ tạo View bằng code để đảm bảo
        // tính tương thích với cấu trúc hiển thị giao dịch đã có trong layout books.xml

        LinearLayout headerView = new LinearLayout(this);
        headerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        headerView.setOrientation(LinearLayout.HORIZONTAL);
        headerView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerView.setPadding(12, 12, 12, 12);
        // Đã sửa lỗi: Thay thế R.drawable.ripple_effect bằng màu trắng, dựa vào onClickListener để có hiệu ứng chạm
        headerView.setBackgroundColor(Color.WHITE); 
        headerView.setTag("HEADER_" + dateString); // Tag for easy retrieval

        // Left section (Date and description)
        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        leftLayout.setOrientation(LinearLayout.VERTICAL);
        leftLayout.setPadding(0, 0, 12, 0);

        TextView tvCategoryName = new TextView(this);
        tvCategoryName.setText("Ngày: " + dateString);
        tvCategoryName.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvCategoryName.setTextSize(11.0f);
        tvCategoryName.setTypeface(null, android.graphics.Typeface.BOLD);
        leftLayout.addView(tvCategoryName);

        TextView tvDate = new TextView(this);
        tvDate.setText("Nhấn để xem chi tiết");
        tvDate.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        tvDate.setTextSize(11.0f);
        leftLayout.addView(tvDate);
        
        headerView.addView(leftLayout);
        
        // Right section (Amount Summary)
        TextView tvAmount = new TextView(this);
        tvAmount.setText("Đang tính tổng..."); // Placeholder, will be updated by updateDayGroupSummary
        tvAmount.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvAmount.setTextSize(11.0f);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        headerView.addView(tvAmount);

        // Set click listener for expanding/collapsing
        headerView.setOnClickListener(v -> toggleDayGroup(dateString));

        // Add Header View and Transaction Container to the main list
        llTransactions.addView(headerView);
        llTransactions.addView(dailyTransactionContainer);

        // Set initial visibility
        dailyTransactionContainer.setVisibility(expandInitially ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates the summary text on the header view for a specific date.
     */
    private void updateDaySummary(String dateString, double totalIncome, double totalExpense) {
        // Daily summaries are not affected by the top-level visibility toggle
        DecimalFormat formatter = new DecimalFormat("#,### đ");
        String incomeStr = formatter.format(totalIncome);
        String expenseStr = formatter.format(totalExpense);
        String summaryText = String.format("Thu: +%s | Chi: -%s", incomeStr, expenseStr);

        // Find the header view associated with this date
        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView instanceof LinearLayout) {
            LinearLayout headerLayout = (LinearLayout) headerView;
            // Trong cấu trúc mới, TextView hiển thị số tiền là view thứ 2 trong headerView
            if (headerLayout.getChildCount() > 1) {
                View amountView = headerLayout.getChildAt(1);
                if (amountView instanceof TextView) {
                    ((TextView) amountView).setText(summaryText);
                }
            }
        }
    }

    /**
     * Wrapper to call updateDaySummary.
     */
    private void updateDayGroupSummary(String dateString, double totalIncome, double totalExpense) {
        updateDaySummary(dateString, totalIncome, totalExpense);
    }


    /**
     * Adds a single transaction view to the correct day's container.
     */
    private void addTransactionToDayGroup(Transaction transaction, String dateString) {
        // Ta sử dụng lại cấu trúc transaction item từ books.xml, không sử dụng layout transaction_item.xml
        LinearLayout container = transactionContainers.get(dateString);
        if (container == null) return; 

        // Tạo layout cho giao dịch
        // **LƯU Ý:** Vì không có file `transaction_item.xml`, chúng ta tạo lại cấu trúc bằng code.

        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(12, 12, 12, 12);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        // Đã sửa lỗi: Thay thế R.drawable.ripple_effect bằng màu trắng
        itemLayout.setBackgroundColor(Color.WHITE); 

        // Icon
        ImageView ivCategoryIcon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (40 * getResources().getDisplayMetrics().density), 
                (int) (40 * getResources().getDisplayMetrics().density));
        ivCategoryIcon.setLayoutParams(iconParams);
        itemLayout.addView(ivCategoryIcon);

        // Content
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density), 
                0, 0, 0);

        TextView tvCategoryName = new TextView(this);
        tvCategoryName.setText(transaction.getCategory());
        tvCategoryName.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvCategoryName.setTextSize(14.0f);
        tvCategoryName.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(tvCategoryName);

        TextView tvNote = new TextView(this);
        tvNote.setText(transaction.getNote());
        tvNote.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        tvNote.setTextSize(12.0f);
        contentLayout.addView(tvNote);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        TextView tvDate = new TextView(this);
        tvDate.setText(sdf.format(new Date(transaction.getTimestamp())));
        tvDate.setTextColor(Color.parseColor("#999999"));
        tvDate.setTextSize(10.0f);
        contentLayout.addView(tvDate);

        itemLayout.addView(contentLayout);

        // Amount
        TextView tvAmount = new TextView(this);
        tvAmount.setTextSize(15.0f);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        itemLayout.addView(tvAmount);

        DecimalFormat formatter = new DecimalFormat("#,### đ");
        String formattedAmount = formatter.format(transaction.getAmount());

        if (transaction.getType().equalsIgnoreCase("income")) {
            tvAmount.setText("+" + formattedAmount);
            tvAmount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            ivCategoryIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else { // expense
            tvAmount.setText("-" + formattedAmount);
            tvAmount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_crop);
            ivCategoryIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        // Add divider before adding the new item (if it's not the first transaction item)
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
            );
            // Căn lề cho Divider giống như trong layout books.xml
            params.setMargins((int) (52 * getResources().getDisplayMetrics().density), 0, 0, 0);
            divider.setLayoutParams(params);
            divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));

            container.addView(divider);
        }

        container.addView(itemLayout);
    }

    /**
     * Toggles the visibility of a day group and updates the header.
     */
    private void toggleDayGroup(String dateString) {
        boolean isExpanded = expandedDates.getOrDefault(dateString, true);
        isExpanded = !isExpanded;
        expandedDates.put(dateString, isExpanded);

        LinearLayout container = transactionContainers.get(dateString);
        if (container != null) {
            container.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }

        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView instanceof LinearLayout) {
            LinearLayout headerLayout = (LinearLayout) headerView;
            // Trong cấu trúc mới, TextView hiển thị "Nhấn để xem chi tiết" là view thứ hai của LeftLayout (Child 0)
            if (headerLayout.getChildCount() > 0 && headerLayout.getChildAt(0) instanceof LinearLayout) {
                 LinearLayout leftLayout = (LinearLayout) headerLayout.getChildAt(0);
                 if (leftLayout.getChildCount() > 1 && leftLayout.getChildAt(1) instanceof TextView) {
                    TextView tvDate = (TextView) leftLayout.getChildAt(1);
                    tvDate.setText(isExpanded ? "Nhấn để thu gọn" : "Nhấn để xem chi tiết");
                 }
            }
        }
    }

    private void onSeeAllTransactionsClicked() {
        if (tvSeeAllTransactions == null) return;
        boolean currentlyExpanded = tvSeeAllTransactions.getText().toString().equals("Thu gọn");
        String newText = currentlyExpanded ? "Xem tất cả →" : "Thu gọn";

        for (String dateString : expandedDates.keySet()) {
            boolean expand = !currentlyExpanded;
            expandedDates.put(dateString, expand);
            LinearLayout container = transactionContainers.get(dateString);
            if (container != null) {
                container.setVisibility(expand ? View.VISIBLE : View.GONE);
            }

            View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
            if (headerView instanceof LinearLayout) {
                LinearLayout headerLayout = (LinearLayout) headerView;
                if (headerLayout.getChildCount() > 0 && headerLayout.getChildAt(0) instanceof LinearLayout) {
                    LinearLayout leftLayout = (LinearLayout) headerLayout.getChildAt(0);
                    if (leftLayout.getChildCount() > 1 && leftLayout.getChildAt(1) instanceof TextView) {
                       TextView tvDate = (TextView) leftLayout.getChildAt(1);
                       tvDate.setText(expand ? "Nhấn để thu gọn" : "Nhấn để xem chi tiết");
                    }
               }
            }
        }
        tvSeeAllTransactions.setText(newText);
        Toast.makeText(this, currentlyExpanded ? "Đã thu gọn tất cả giao dịch" : "Đã mở rộng tất cả giao dịch", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab_edit) {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST);
        } else if (v == llAddGoal) {
            Intent intent = new Intent(this, CreateSavingsGoalActivity.class);
            startActivity(intent);
        } else if (v == bottomNavBooks) {
            Toast.makeText(this, "Bạn đang ở màn hình Sổ sách", Toast.LENGTH_SHORT).show();
        } else if (v == bottomNavWallet) {
            Intent intent = new Intent(this, ManageAccountsActivity.class);
            startActivity(intent);
        } else if (v == bottomNavAnalysis) {
            Toast.makeText(this, "Điều hướng đến Phân tích", Toast.LENGTH_SHORT).show();
        } else if (v == bottomNavMe) {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        } else if (v == tvSeeAllTransactions) {
            onSeeAllTransactionsClicked();
        } else if (id == R.id.month_year_picker_layout) {
            showMonthYearPicker();
        } else if (id == R.id.iv_toggle_visibility) { // New click handler
            toggleAmountVisibility();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ADD_TRANSACTION_REQUEST) {
                loadMonthlyData(); // Tải lại dữ liệu tháng sau khi thêm giao dịch mới
            } else if (requestCode == EDIT_GOAL_REQUEST) {
                loadSavingsGoals(); // Tải lại danh sách mục tiêu sau khi chỉnh sửa/xóa
            }
        }
    }


    /**
     * Tải và hiển thị các mục tiêu tiết kiệm từ Firebase Firestore
     */
    private void loadSavingsGoals() {
        if (llGoalsGrid == null) return;

        FirebaseManager manager = FirebaseManager.getInstance();
        if (manager == null) return;
        
        CollectionReference goalsRef = manager.getUserCollectionRef(FirebaseManager.GOALS_COLLECTION);
        if (goalsRef == null) return;
        
        // Xóa map cũ và tạo lại map mới
        loadedGoalsMap.clear();

        goalsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Goal> goals = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Goal goal = document.toObject(Goal.class);
                            goal.setId(document.getId());
                            goals.add(goal);
                            loadedGoalsMap.put(goal.getId(), goal); // Lưu vào Map
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Goal: " + e.getMessage());
                        }
                    }

                    displayGoals(goals);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải mục tiêu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading goals", e);
                });
    }

    /**
     * Hiển thị danh sách các mục tiêu đã tải.
     */
    private void displayGoals(List<Goal> goals) {
        if (llGoalsGrid == null) return;
        // Xóa tất cả các view cũ (trừ nút Add, giả định nút Add là cuối cùng)
        int childCount = llGoalsGrid.getChildCount();
        if (childCount > 1) {
            llGoalsGrid.removeViews(0, childCount - 1);
        }

        for (Goal goal : goals) {
            double target = goal.getTargetAmount();
            double current = goal.getCurrentAmount();
            int percentage = (int)((target == 0) ? 0 : (current / target) * 100);

            // Tạo view cho mỗi mục tiêu
            LinearLayout goalItem = createGoalItemView(goal.getName(), goal.getIcon(), percentage, goal.getId());

            // Thêm vào grid (trước nút Add)
            int addButtonIndex = llGoalsGrid.getChildCount() - 1;
            llGoalsGrid.addView(goalItem, addButtonIndex);
        }
    }

    /**
     * Tạo view cho một mục tiêu tiết kiệm
     */
    private LinearLayout createGoalItemView(String name, String icon, int percentage, final String goalId) {
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                (int) (80 * getResources().getDisplayMetrics().density)
        );
        params.weight = 1.0f;
        params.setMargins(4, 0, 4, 0);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(Color.WHITE);
        layout.setPadding(8, 8, 8, 8);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(32);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvIcon);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(12);
        tvName.setTextColor(Color.parseColor("#212121"));
        tvName.setGravity(android.view.Gravity.CENTER);
        tvName.setMaxLines(1);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, 4, 0, 0);
        tvName.setLayoutParams(nameParams);
        layout.addView(tvName);

        TextView tvPercentage = new TextView(this);
        tvPercentage.setText(percentage + "%");
        tvPercentage.setTextSize(10);
        tvPercentage.setTextColor(Color.parseColor("#757575"));
        tvPercentage.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvPercentage);

        // THAY THẾ LOGIC: Mở GoalDetailActivity và truyền object Goal
        layout.setOnClickListener(v -> {
            Goal selectedGoal = loadedGoalsMap.get(goalId);
            if (selectedGoal != null) {
                Intent intent = new Intent(this, GoalDetailActivity.class);
                intent.putExtra(GoalDetailActivity.EXTRA_GOAL, selectedGoal);
                startActivityForResult(intent, EDIT_GOAL_REQUEST); // Sử dụng startActivityForResult
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy chi tiết mục tiêu.", Toast.LENGTH_SHORT).show();
            }
        });

        return layout;
    }
}