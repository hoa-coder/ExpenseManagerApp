package com.example.expensemanagerapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BooksActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ADD_TRANSACTION_REQUEST = 1;

    private FloatingActionButton fabEdit;
    private LinearLayout bottomNavBooks, bottomNavWallet, bottomNavAnalysis, bottomNavAdd;
    private LinearLayout llAddGoal;
    private LinearLayout llTransactions; // This is the main container for groups/transactions
    private TextView tvSeeAllTransactions;

    // Map to store expanded/collapsed state of each date group: Key=DateString (dd/MM/yyyy), Value=isExpanded
    private Map<String, Boolean> expandedDates = new HashMap<>();
    // Map to store references to the containers for transactions of a specific date: Key=DateString, Value=LinearLayout containing transactions
    private Map<String, LinearLayout> transactionContainers = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        fabEdit = findViewById(R.id.fab_edit);
        llAddGoal = findViewById(R.id.ll_add_goal);
        llTransactions = findViewById(R.id.ll_transactions);
        LinearLayout bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // Initialize tvSeeAllTransactions here using the ID from books.xml
        tvSeeAllTransactions = findViewById(R.id.tv_see_all_transactions);

        bottomNavBooks = (LinearLayout) bottomNavigation.getChildAt(0);
        bottomNavWallet = (LinearLayout) bottomNavigation.getChildAt(1);
        bottomNavAnalysis = (LinearLayout) bottomNavigation.getChildAt(2);
        bottomNavAdd = (LinearLayout) bottomNavigation.getChildAt(3);

        fabEdit.setOnClickListener(this);
        llAddGoal.setOnClickListener(this);
        bottomNavBooks.setOnClickListener(this);
        bottomNavWallet.setOnClickListener(this);
        bottomNavAnalysis.setOnClickListener(this);
        bottomNavAdd.setOnClickListener(this);
        
        if (tvSeeAllTransactions != null) {
            tvSeeAllTransactions.setOnClickListener(this);
            tvSeeAllTransactions.setText("Thu gọn"); // Initialize to Collapse All, as loadDashboardData expands everything.
        }

        loadDashboardData();
    }

    private void loadDashboardData() {
        Toast.makeText(this, "Đang tải dữ liệu tài chính...", Toast.LENGTH_SHORT).show();
        llTransactions.removeAllViews(); // Xóa tất cả view cũ (headers và transactions)
        transactionContainers.clear();
        expandedDates.clear();

        List<Transaction> transactions = JsonHelper.loadTransactions(this);

        if (transactions.isEmpty()) {
            Toast.makeText(this, "Chưa có giao dịch nào.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sắp xếp theo timestamp giảm dần (mới nhất lên đầu) - This is for grouping by date order
        transactions.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

        // --- New Grouping Logic with Aggregation ---
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        String currentDay = "";
        double totalIncomeForDay = 0;
        double totalExpenseForDay = 0;

        for (Transaction transaction : transactions) {
            String transactionDay = dayFormat.format(new Date(transaction.getTimestamp()));

            if (!transactionDay.equals(currentDay) && !currentDay.isEmpty()) {
                // Finalize the previous day group
                updateDayGroupSummary(currentDay, totalIncomeForDay, totalExpenseForDay);

                // Reset for new day
                totalIncomeForDay = 0;
                totalExpenseForDay = 0;
            }

            // Aggregate for current day
            if (transaction.getType().equalsIgnoreCase("income")) {
                totalIncomeForDay += transaction.getAmount();
            } else { // expense
                totalExpenseForDay += transaction.getAmount();
            }

            if (!transactionDay.equals(currentDay)) {
                // New Day found, create header (initially expanded) and start tracking
                addDayGroupHeader(transactionDay, true);
                currentDay = transactionDay;
            }

            // Add transaction to its corresponding day container
            addTransactionToDayGroup(transaction, transactionDay);
        }

        // Finalize the last day group
        if (!currentDay.isEmpty()) {
            updateDayGroupSummary(currentDay, totalIncomeForDay, totalExpenseForDay);
        }
    }

    /**
     * Adds a clickable header for a new day group.
     * @param dateString The date to display (e.g., "18/11/2025")
     * @param expandInitially If true, the group will start expanded.
     */
    private void addDayGroupHeader(final String dateString, boolean expandInitially) {
        // 1. Create the container for the transactions of this day
        LinearLayout dailyTransactionContainer = new LinearLayout(this);
        dailyTransactionContainer.setOrientation(LinearLayout.VERTICAL);
        dailyTransactionContainer.setId(View.generateViewId()); // Assign a unique ID
        transactionContainers.put(dateString, dailyTransactionContainer);
        expandedDates.put(dateString, expandInitially);

        // 2. Create the header view using transaction_item layout as a base for consistency
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.transaction_item, llTransactions, false);

        // Hide fields not needed for the header summary
        ImageView ivCategoryIcon = headerView.findViewById(R.id.iv_category_icon);
        TextView tvNote = headerView.findViewById(R.id.tv_note);
        TextView tvAmount = headerView.findViewById(R.id.tv_amount);

        ivCategoryIcon.setVisibility(View.GONE);
        tvNote.setVisibility(View.GONE);

        // Set Day Summary Text
        TextView tvCategoryName = headerView.findViewById(R.id.tv_category_name);
        TextView tvDate = headerView.findViewById(R.id.tv_date);

        tvCategoryName.setText("Ngày: " + dateString);
        tvDate.setText("Nhấn để xem chi tiết");
        tvAmount.setText("Đang tính tổng..."); // Placeholder, will be updated by updateDayGroupSummary

        tvAmount.setTextColor(ContextCompat.getColor(this, android.R.color.black)); // Make header text black
        
        // *** FIX: Giảm kích thước chữ cho header ngày (bao gồm cả Thu/Chi) xuống 11.0f ***
        tvCategoryName.setTextSize(11.0f); 
        tvDate.setTextSize(11.0f);
        tvAmount.setTextSize(11.0f); // Giảm kích thước chữ cho phần tổng Thu/Chi

        // Set click listener for expanding/collapsing
        headerView.setOnClickListener(v -> toggleDayGroup(dateString));
        headerView.setTag("HEADER_" + dateString); // Tag for easy retrieval

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
        DecimalFormat formatter = new DecimalFormat("#,### đ");
        String incomeStr = formatter.format(totalIncome);
        String expenseStr = formatter.format(totalExpense);
        String summaryText = String.format("Thu: +%s | Chi: -%s", incomeStr, expenseStr);

        // Find the header view associated with this date
        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView != null) {
            TextView tvAmount = headerView.findViewById(R.id.tv_amount);
            if (tvAmount != null) {
                tvAmount.setText(summaryText);
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
     * Adds a single transaction view to the correct day's container. (Point 4: Reverse order means append now)
     */
    private void addTransactionToDayGroup(Transaction transaction, String dateString) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View transactionView = inflater.inflate(R.layout.transaction_item, transactionContainers.get(dateString), false);

        ImageView ivCategoryIcon = transactionView.findViewById(R.id.iv_category_icon);
        TextView tvCategoryName = transactionView.findViewById(R.id.tv_category_name);
        TextView tvNote = transactionView.findViewById(R.id.tv_note);
        TextView tvDate = transactionView.findViewById(R.id.tv_date);
        TextView tvAmount = transactionView.findViewById(R.id.tv_amount);

        tvCategoryName.setText(transaction.getCategory());
        tvNote.setText(transaction.getNote());

        // Định dạng lại ngày tháng từ timestamp (chỉ hiển thị giờ/phút trên transaction item)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(transaction.getTimestamp())));

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

        // *** POINT 4: Reverse order means append (oldest first, since loadDashboardData is newest first) ***
        LinearLayout container = transactionContainers.get(dateString);
        
        // If there are existing items in the container (header is not counted as it's added separately, but container.getChildCount() includes it)
        // If count > 1, it means there's at least one transaction already.
        if (container.getChildCount() > 1) {
            View divider = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
            );
            params.setMargins(150, 0, 0, 0);
            divider.setLayoutParams(params);
            divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            
            // Add the divider right before the new item (which will be appended)
            container.addView(divider, container.getChildCount() - 1); 
        }
        
        // Append the new transaction to the end
        container.addView(transactionView);
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

        // Find and update the corresponding header view
        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView != null) {
             TextView tvDate = headerView.findViewById(R.id.tv_date);
             if(tvDate != null) {
                 tvDate.setText(isExpanded ? "Nhấn để thu gọn" : "Nhấn để xem chi tiết");
             }
        }
        
        Toast.makeText(this, isExpanded ? "Đã mở rộng: " + dateString : "Đã thu gọn: " + dateString, Toast.LENGTH_SHORT).show();

    }
    
    private void onSeeAllTransactionsClicked() {
        if (tvSeeAllTransactions.getText().toString().equals("Thu gọn")) {
            // Collapse All
            for (String dateString : expandedDates.keySet()) {
                expandedDates.put(dateString, false);
                LinearLayout container = transactionContainers.get(dateString);
                if (container != null) {
                    container.setVisibility(View.GONE);
                }
                // Update header date text to "Xem chi tiết"
                View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
                if (headerView != null) {
                     TextView tvDate = headerView.findViewById(R.id.tv_date);
                     if(tvDate != null) {
                         tvDate.setText("Nhấn để xem chi tiết");
                     }
                }
            }
            tvSeeAllTransactions.setText("Xem tất cả →");
            Toast.makeText(this, "Đã thu gọn tất cả giao dịch", Toast.LENGTH_SHORT).show();
        } else {
            // Expand All
            for (String dateString : expandedDates.keySet()) {
                expandedDates.put(dateString, true);
                LinearLayout container = transactionContainers.get(dateString);
                if (container != null) {
                    container.setVisibility(View.VISIBLE);
                }
                // Update header date text to "Thu gọn"
                View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
                if (headerView != null) {
                     TextView tvDate = headerView.findViewById(R.id.tv_date);
                     if(tvDate != null) {
                         tvDate.setText("Nhấn để thu gọn");
                     }
                }
            }
            tvSeeAllTransactions.setText("Thu gọn");
            Toast.makeText(this, "Đã mở rộng tất cả giao dịch", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_edit) {
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
        } else if (v == bottomNavAdd) {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST);
        } else if (v == tvSeeAllTransactions) {
            onSeeAllTransactionsClicked();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK && data != null) {
            // --- FIX: Cập nhật ngay lập tức bằng object trả về ---
            Transaction newTransaction = (Transaction) data.getSerializableExtra("NEW_TRANSACTION");
            
            if (newTransaction != null) {
                // Gọi hàm cập nhật UI với dữ liệu mới nhận được
                addNewTransactionToDashboard(newTransaction);
                Toast.makeText(this, "Đã thêm giao dịch thành công!", Toast.LENGTH_SHORT).show();
            } else {
                // Nếu không nhận được object, fallback về tải lại toàn bộ
                loadDashboardData();
                Toast.makeText(this, "Đã thêm giao dịch thành công (Tải lại)", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Adds a single new transaction to the existing view structure immediately.
     */
    private void addNewTransactionToDashboard(Transaction transaction) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String transactionDay = dayFormat.format(new Date(transaction.getTimestamp()));

        // 1. Check if the day header exists or create it
        if (!transactionContainers.containsKey(transactionDay)) {
            // If the day does not exist, we must re-run the grouping logic to insert header and sort correctly.
            loadDashboardData(); 
            return;
        }
        
        // *** FIX for total error: Force reload instead of trying to update instantly with complex ordering/parsing ***
        loadDashboardData();
        
        // 2. If the day exists, add the transaction directly to its container (Append now for Point 4)
        // addTransactionToDayGroup(transaction, transactionDay); // This is now handled by loadDashboardData()
        
        // 3. Update the day summary header instantly
        // updateDayGroupSummaryForNewTransaction(transactionDay, transaction); // This is now handled by loadDashboardData()
    }

    /**
     * Updates the day summary header only for the day where the new transaction was added.
     */
    private void updateDayGroupSummaryForNewTransaction(String dateString, Transaction newTransaction) {
        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView == null) return; 

        TextView tvAmount = headerView.findViewById(R.id.tv_amount);
        if (tvAmount == null) return;

        LinearLayout container = transactionContainers.get(dateString);
        if (container == null) return;

        double currentIncome = 0;
        double currentExpense = 0;

        // Recalculate totals by inspecting every transaction item (and its amount TextView) in the container
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            // Only process LinearLayouts which represent transaction items (dividers are also LinearLayouts but have no amountTv)
            if (child instanceof LinearLayout) { 
                TextView amountTv = child.findViewById(R.id.tv_amount);
                // Check if this LinearLayout has an amount TextView (i.e., it is a transaction item, not a divider)
                if (amountTv != null) {
                    String amountStr = amountTv.getText().toString().replaceAll("[^\\d.-]", ""); // Remove formatting chars like +, -, đ, space
                    
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount > 0) {
                            currentIncome += amount;
                        } else {
                            currentExpense += Math.abs(amount);
                        }
                    } catch (NumberFormatException ignored) {
                        // This can catch dividers if they were accidentally styled as LinearLayouts without proper children structure, 
                        // or if text parsing fails unexpectedly.
                    }
                }
            }
        }
        
        updateDaySummary(dateString, currentIncome, currentExpense);
        
        // If group is expanded, ensure header text for date detail is correct
        if (expandedDates.getOrDefault(dateString, false)) {
            View headerViewDate = llTransactions.findViewWithTag("HEADER_" + dateString);
            if (headerViewDate != null) {
                 TextView tvDate = headerViewDate.findViewById(R.id.tv_date);
                 if(tvDate != null) {
                     tvDate.setText("Nhấn để thu gọn");
                 }
            }
        }
    }
}