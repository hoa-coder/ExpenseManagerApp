package com.example.expensemanagerapp;

import android.content.Context;
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

import androidx.annotation.Nullable;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BooksActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ADD_TRANSACTION_REQUEST = 1;
    private static final String TAG = "BooksActivity";

    private FloatingActionButton fabEdit;
    private LinearLayout bottomNavBooks, bottomNavWallet, bottomNavAnalysis, bottomNavMe;
    private LinearLayout llAddGoal;
    private LinearLayout llGoalsGrid;
    private LinearLayout llTransactions;
    private TextView tvSeeAllTransactions;

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

        if (tvSeeAllTransactions != null) {
            tvSeeAllTransactions.setOnClickListener(this);
            tvSeeAllTransactions.setText("Thu gọn");
        }

        loadDashboardData();
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
        loadDashboardData();
        loadSavingsGoals();
    }

    /**
     * Tải dữ liệu giao dịch từ Firebase Firestore.
     */
    private void loadDashboardData() {
        // Kiểm tra null của FirebaseManager.getInstance() để tránh NullPointerException khi khởi tạo quá sớm
        FirebaseManager manager = FirebaseManager.getInstance();
        if (manager == null) return; 
        
        CollectionReference transactionsRef = manager.getUserCollectionRef(FirebaseManager.TRANSACTIONS_COLLECTION);
        if (transactionsRef == null) {
            // Chỉ hiển thị toast nếu người dùng đã cố gắng truy cập mà chưa đăng nhập
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                 Toast.makeText(this, "Không thể tải dữ liệu: Lỗi khởi tạo Firebase.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        Toast.makeText(this, "Đang tải giao dịch từ Firebase...", Toast.LENGTH_SHORT).show();

        transactionsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Transaction: " + e.getMessage());
                        }
                    }

                    // Xử lý và hiển thị dữ liệu đã tải
                    processAndDisplayTransactions(transactions);

                    if (transactions.isEmpty()) {
                         Toast.makeText(this, "Chưa có giao dịch nào.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải giao dịch: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading transactions", e);
                });
    }

    /**
     * Xử lý nhóm và hiển thị các giao dịch.
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

        // 2. Create the header view using transaction_item layout as a base for consistency
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.transaction_item, llTransactions, false);

        // Hide fields not needed for the header summary
        ImageView ivCategoryIcon = headerView.findViewById(R.id.iv_category_icon);
        TextView tvNote = headerView.findViewById(R.id.tv_note);
        TextView tvAmount = headerView.findViewById(R.id.tv_amount);

        if (ivCategoryIcon != null) ivCategoryIcon.setVisibility(View.GONE);
        if (tvNote != null) tvNote.setVisibility(View.GONE);


        // Set Day Summary Text
        TextView tvCategoryName = headerView.findViewById(R.id.tv_category_name);
        TextView tvDate = headerView.findViewById(R.id.tv_date);

        if (tvCategoryName != null) tvCategoryName.setText("Ngày: " + dateString);
        if (tvDate != null) tvDate.setText("Nhấn để xem chi tiết");
        if (tvAmount != null) {
            tvAmount.setText("Đang tính tổng..."); // Placeholder, will be updated by updateDayGroupSummary
            tvAmount.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            tvAmount.setTextSize(11.0f);
        }

        if (tvCategoryName != null) tvCategoryName.setTextSize(11.0f);
        if (tvDate != null) tvDate.setTextSize(11.0f);

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
     * Adds a single transaction view to the correct day's container.
     */
    private void addTransactionToDayGroup(Transaction transaction, String dateString) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout container = transactionContainers.get(dateString);
        if (container == null) return; // Should not happen if grouping logic is correct

        View transactionView = inflater.inflate(R.layout.transaction_item, container, false);

        ImageView ivCategoryIcon = transactionView.findViewById(R.id.iv_category_icon);
        TextView tvCategoryName = transactionView.findViewById(R.id.tv_category_name);
        TextView tvNote = transactionView.findViewById(R.id.tv_note);
        TextView tvDate = transactionView.findViewById(R.id.tv_date);
        TextView tvAmount = transactionView.findViewById(R.id.tv_amount);

        if (tvCategoryName != null) tvCategoryName.setText(transaction.getCategory());
        if (tvNote != null) tvNote.setText(transaction.getNote());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (tvDate != null) tvDate.setText(sdf.format(new Date(transaction.getTimestamp())));

        DecimalFormat formatter = new DecimalFormat("#,### đ");
        String formattedAmount = formatter.format(transaction.getAmount());

        if (tvAmount != null && ivCategoryIcon != null) {
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
        }

        // Add divider before adding the new item (if it's not the first transaction item)
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
            );
            params.setMargins(150, 0, 0, 0);
            divider.setLayoutParams(params);
            divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));

            container.addView(divider);
        }

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

        View headerView = llTransactions.findViewWithTag("HEADER_" + dateString);
        if (headerView != null) {
            TextView tvDate = headerView.findViewById(R.id.tv_date);
            if(tvDate != null) {
                tvDate.setText(isExpanded ? "Nhấn để thu gọn" : "Nhấn để xem chi tiết");
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
            if (headerView != null) {
                TextView tvDate = headerView.findViewById(R.id.tv_date);
                if(tvDate != null) {
                    tvDate.setText(expand ? "Nhấn để thu gọn" : "Nhấn để xem chi tiết");
                }
            }
        }
        tvSeeAllTransactions.setText(newText);
        Toast.makeText(this, currentlyExpanded ? "Đã thu gọn tất cả giao dịch" : "Đã mở rộng tất cả giao dịch", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        // Sử dụng v.getId() an toàn hơn để tránh NullPointerException khi so sánh view objects
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
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
            loadDashboardData();
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

        goalsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Goal> goals = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Goal goal = document.toObject(Goal.class);
                            goal.setId(document.getId());
                            goals.add(goal);
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

        // Thêm click listener để xem chi tiết
        layout.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Chi tiết: " + name + " - " + percentage + "% (ID: " + goalId + ")",
                    Toast.LENGTH_SHORT).show();
            // TODO: Mở activity chi tiết mục tiêu
        });

        return layout;
    }
}