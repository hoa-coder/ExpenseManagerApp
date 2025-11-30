package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp; // Import Timestamp

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity phân tích thu chi với biểu đồ và bộ lọc
 */
public class AnalysisActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivFilterDate;
    private TabLayout tabLayout;
    private PieChart pieChart;
    private RecyclerView rvCategoryList;

    private TextView tvTotalAmount;
    private TextView tvFilterTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private AnalysisCategoryAdapter categoryAdapter;

    private String currentTab = "Chi tiêu"; // Mặc định
    private String currentFilter = "Tháng này";
    private Date filterStartDate;
    private Date filterEndDate;

    private DecimalFormat currencyFormatter = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupTabLayout();
        setupRecyclerView();

        // Load dữ liệu từ Firebase
        loadTransactionsFromFirebase();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_analysis);
        ivFilterDate = findViewById(R.id.iv_filter_date);
        tabLayout = findViewById(R.id.tab_layout);
        pieChart = findViewById(R.id.pie_chart);
        rvCategoryList = findViewById(R.id.rv_transactions);

        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvFilterTitle = findViewById(R.id.tv_filter_title);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        // Bộ lọc ngày
        ivFilterDate.setOnClickListener(v -> showFilterBottomSheet());
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString();
                updateAnalysis();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new AnalysisCategoryAdapter(new ArrayList<>());
        rvCategoryList.setAdapter(categoryAdapter);
    }

    /**
     * Load giao dịch từ Firebase
     */
    private void loadTransactionsFromFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTransactions.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Cố gắng ánh xạ thủ công để kiểm soát lỗi timestamp
                            Transaction transaction = document.toObject(Transaction.class);
                            
                            // Nếu toObject thành công và timestamp là long (như trong POJO)
                            if (transaction != null && transaction.getTimestamp() > 0) {
                                allTransactions.add(transaction);
                                continue;
                            }

                            // Nếu mapping thất bại hoặc timestamp là 0, thử kiểm tra Timestamp object (nếu bạn đã đổi sang Timestamp)
                            // HOẶC, nếu toObject thất bại, chúng ta sẽ bắt nó ở đây.
                            // Vì bạn đã định nghĩa nó là long, ta sẽ thử lấy dữ liệu map và kiểm tra
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                Long tsLong = (Long) data.get("timestamp");
                                if (tsLong != null && tsLong > 0) {
                                    Transaction manualTransaction = new Transaction(
                                            document.getId(),
                                            (String) data.get("type"),
                                            (Double) data.get("amount"),
                                            (String) data.get("category"),
                                            (String) data.get("note"),
                                            tsLong
                                    );
                                    allTransactions.add(manualTransaction);
                                } 
                                // BỎ QUA các giao dịch có timestamp không hợp lệ/bằng 0
                            }

                        } catch (Exception e) {
                            // Bắt lỗi chung nếu có lỗi xảy ra trong quá trình mapping
                            // Điều này ngăn crash ứng dụng
                            // Log.e("AnalysisActivity", "Error mapping transaction: " + document.getId(), e);
                        }
                    }

                    // Mặc định lọc theo "Tháng này"
                    filterByThisMonth();
                    updateAnalysis();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cập nhật phân tích: biểu đồ + danh sách
     */
    private void updateAnalysis() {
        // Lọc theo tab hiện tại
        List<Transaction> tabFilteredList = new ArrayList<>();
        for (Transaction t : filteredTransactions) {
            if (currentTab.equals("Chi tiêu") && t.getType().equals("EXPENSE")) {
                tabFilteredList.add(t);
            } else if (currentTab.equals("Thu nhập") && t.getType().equals("INCOME")) {
                tabFilteredList.add(t);
            } else if (currentTab.equals("Tất cả")) {
                tabFilteredList.add(t);
            }
        }

        // Tính tổng
        double total = 0;
        for (Transaction t : tabFilteredList) {
            total += t.getAmount();
        }

        tvTotalAmount.setText(currencyFormatter.format(total));

        // Nhóm theo category
        Map<String, Double> categoryMap = new HashMap<>();
        for (Transaction t : tabFilteredList) {
            String category = t.getCategory();
            categoryMap.put(category, categoryMap.getOrDefault(category, 0.0) + t.getAmount());
        }

        // Cập nhật PieChart
        setupPieChart(categoryMap, total);

        // Cập nhật RecyclerView
        List<CategoryAnalysis> categoryList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            double percentage = (total == 0) ? 0 : (entry.getValue() / total) * 100;
            categoryList.add(new CategoryAnalysis(
                    entry.getKey(),
                    entry.getValue(),
                    percentage
            ));
        }

        // Sắp xếp theo số tiền giảm dần
        categoryList.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        categoryAdapter.updateData(categoryList);
    }

    /**
     * Cấu hình PieChart (Biểu đồ tròn)
     */
    private void setupPieChart(Map<String, Double> categoryMap, double total) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Màu sắc đa dạng
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(244, 67, 54));   // Đỏ
        colors.add(Color.rgb(233, 30, 99));   // Hồng đậm
        colors.add(Color.rgb(156, 39, 176));  // Tím
        colors.add(Color.rgb(103, 58, 183));  // Tím đậm
        colors.add(Color.rgb(63, 81, 181));   // Indigo
        colors.add(Color.rgb(33, 150, 243));  // Xanh dương
        colors.add(Color.rgb(0, 188, 212));   // Cyan
        colors.add(Color.rgb(0, 150, 136));   // Teal
        colors.add(Color.rgb(76, 175, 80));   // Xanh lá
        colors.add(Color.rgb(255, 193, 7));   // Vàng

        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(currentTab + "\n" + currencyFormatter.format(total));
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.BLACK);
        pieChart.getLegend().setEnabled(false);
        pieChart.setVisibility(View.VISIBLE);
        pieChart.animateY(1000);
    }

    /**
     * Hiển thị BottomSheet bộ lọc
     */
    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filter_date, null);
        dialog.setContentView(view);

        LinearLayout llAll = view.findViewById(R.id.ll_filter_all);
        LinearLayout llToday = view.findViewById(R.id.ll_filter_today);
        LinearLayout llYesterday = view.findViewById(R.id.ll_filter_yesterday);
        LinearLayout llThisWeek = view.findViewById(R.id.ll_filter_this_week);
        LinearLayout llLastWeek = view.findViewById(R.id.ll_filter_last_week);
        LinearLayout llThisMonth = view.findViewById(R.id.ll_filter_this_month);
        LinearLayout llLastMonth = view.findViewById(R.id.ll_filter_last_month);
        LinearLayout llThisYear = view.findViewById(R.id.ll_filter_this_year);
        LinearLayout llCustom = view.findViewById(R.id.ll_filter_custom);

        llAll.setOnClickListener(v -> { filterByAll(); dialog.dismiss(); });
        llToday.setOnClickListener(v -> { filterByToday(); dialog.dismiss(); });
        llYesterday.setOnClickListener(v -> { filterByYesterday(); dialog.dismiss(); });
        llThisWeek.setOnClickListener(v -> { filterByThisWeek(); dialog.dismiss(); });
        llLastWeek.setOnClickListener(v -> { filterByLastWeek(); dialog.dismiss(); });
        llThisMonth.setOnClickListener(v -> { filterByThisMonth(); dialog.dismiss(); });
        llLastMonth.setOnClickListener(v -> { filterByLastMonth(); dialog.dismiss(); });
        llThisYear.setOnClickListener(v -> { filterByThisYear(); dialog.dismiss(); });
        llCustom.setOnClickListener(v -> { showCustomDatePicker(); dialog.dismiss(); });

        dialog.show();
    }

    // ==================== CÁC HÀM LỌC ====================

    private void filterByAll() {
        currentFilter = "Tất cả";
        filteredTransactions = new ArrayList<>(allTransactions);
        tvFilterTitle.setText("Tất cả");
        updateAnalysis();
    }

    private void filterByToday() {
        currentFilter = "Hôm nay";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Hôm nay");
    }

    private void filterByYesterday() {
        currentFilter = "Hôm qua";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Hôm qua");
    }

    private void filterByThisWeek() {
        currentFilter = "Tuần này";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.SECOND, -1);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Tuần này");
    }

    private void filterByLastWeek() {
        currentFilter = "Tuần trước";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.SECOND, -1);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Tuần trước");
    }

    private void filterByThisMonth() {
        currentFilter = "Tháng này";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Tháng này");
    }

    private void filterByLastMonth() {
        currentFilter = "Tháng trước";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Tháng trước");
    }

    private void filterByThisYear() {
        currentFilter = "Năm này";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        filterStartDate = cal.getTime();

        cal.add(Calendar.YEAR, 1);
        cal.add(Calendar.SECOND, -1);
        filterEndDate = cal.getTime();

        applyDateFilter();
        tvFilterTitle.setText("Năm này");
    }

    private void showCustomDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month, day, 0, 0, 0);
                    filterStartDate = startCal.getTime();

                    // Chọn ngày kết thúc
                    DatePickerDialog endPicker = new DatePickerDialog(
                            this,
                            (v, y, m, d) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(y, m, d, 23, 59, 59);
                                filterEndDate = endCal.getTime();

                                applyDateFilter();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                tvFilterTitle.setText(sdf.format(filterStartDate) + " - " + sdf.format(filterEndDate));
                            },
                            year, month, day
                    );
                    endPicker.setTitle("Chọn ngày kết thúc");
                    endPicker.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        startPicker.setTitle("Chọn ngày bắt đầu");
        startPicker.show();
    }

    private void applyDateFilter() {
        filteredTransactions.clear();
        for (Transaction t : allTransactions) {
            // Sửa lỗi tiềm ẩn: Kiểm tra timestamp > 0 và sử dụng phương thức getDate()
            if (t.getTimestamp() > 0) {
                Date transDate = t.getDate(); 
                if (transDate.after(filterStartDate) && transDate.before(filterEndDate)) {
                    filteredTransactions.add(t);
                }
            }
        }
        updateAnalysis();
    }

    /**
     * Inner class: CategoryAnalysis
     */
    public static class CategoryAnalysis {
        private String category;
        private double amount;
        private double percentage;

        public CategoryAnalysis(String category, double amount, double percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public double getPercentage() { return percentage; }
    }
}