package com.example.expensemanagerapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity phân tích thu chi với biểu đồ và bộ lọc (OPTIMIZED)
 */
public class AnalysisActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivFilterDate;
    private TabLayout tabLayout;
    private PieChart pieChart;
    private RecyclerView rvCategoryList;
    private ProgressBar progressBar;

    private TextView tvTotalAmount;
    private TextView tvFilterTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private AnalysisCategoryAdapter categoryAdapter;

    private String currentTab = "Chi tiêu";
    private String currentFilter = "Tháng này";
    private Date filterStartDate;
    private Date filterEndDate;

    private DecimalFormat currencyFormatter = new DecimalFormat("#,### đ");

    // ✅ Optimizations
    private ExecutorService executorService;
    private Handler mainHandler;
    private ListenerRegistration transactionListener;
    private boolean isDataLoaded = false;

    // ✅ Cache để tránh tính toán lại
    private Map<String, List<CategoryAnalysis>> cachedAnalysis = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // ✅ Khởi tạo ExecutorService cho background processing
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupTabLayout();
        setupRecyclerView();

        // ✅ Load dữ liệu với Realtime Listener
        loadTransactionsRealtimeFromFirebase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Cleanup
        if (transactionListener != null) {
            transactionListener.remove();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_analysis);
        ivFilterDate = findViewById(R.id.iv_filter_date);
        tabLayout = findViewById(R.id.tab_layout);
        pieChart = findViewById(R.id.pie_chart);
        rvCategoryList = findViewById(R.id.rv_transactions);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvFilterTitle = findViewById(R.id.tv_filter_title);

        // ✅ Thêm ProgressBar (nếu chưa có trong XML)
        progressBar = findViewById(R.id.progress_bar);
        if (progressBar == null) {
            // Tạo ProgressBar động nếu chưa có trong XML
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
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

        // ✅ Tối ưu RecyclerView
        rvCategoryList.setHasFixedSize(true);
        rvCategoryList.setItemViewCacheSize(20);
    }

    /**
     * ✅ Load giao dịch với Realtime Listener (thay vì get())
     */
    private void loadTransactionsRealtimeFromFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // ✅ Hiển thị loading
        showLoading(true);

        // ✅ Remove listener cũ nếu có
        if (transactionListener != null) {
            transactionListener.remove();
        }

        // ✅ Chỉ load dữ liệu trong khoảng thời gian cần thiết (ví dụ: 1 năm gần nhất)
        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);
        long oneYearAgoTimestamp = oneYearAgo.getTimeInMillis();

        // ✅ Sử dụng addSnapshotListener với query được tối ưu
        transactionListener = db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThan("timestamp", oneYearAgoTimestamp) // ✅ Chỉ load 1 năm gần nhất
                .orderBy("timestamp", Query.Direction.DESCENDING) // ✅ Sắp xếp trên server
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        // ✅ Xử lý dữ liệu trong background thread
                        executorService.execute(() -> {
                            List<Transaction> tempList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                try {
                                    Transaction transaction = document.toObject(Transaction.class);
                                    if (transaction != null && transaction.getTimestamp() > 0) {
                                        tempList.add(transaction);
                                    }
                                } catch (Exception ex) {
                                    // Bỏ qua giao dịch lỗi
                                }
                            }

                            // ✅ Cập nhật UI trên main thread
                            mainHandler.post(() -> {
                                allTransactions.clear();
                                allTransactions.addAll(tempList);

                                if (!isDataLoaded) {
                                    isDataLoaded = true;
                                    filterByThisMonth();
                                }

                                updateAnalysis();
                                showLoading(false);
                            });
                        });
                    }
                });
    }

    /**
     * ✅ Hiển thị/ẩn loading
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        pieChart.setVisibility(show ? View.GONE : View.VISIBLE);
        rvCategoryList.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * ✅ Cập nhật phân tích (với cache)
     */
    private void updateAnalysis() {
        // ✅ Tạo cache key
        String cacheKey = currentTab + "_" + currentFilter;

        // ✅ Kiểm tra cache trước
        if (cachedAnalysis.containsKey(cacheKey)) {
            List<CategoryAnalysis> cachedList = cachedAnalysis.get(cacheKey);
            updateUIWithAnalysis(cachedList);
            return;
        }

        // ✅ Xử lý trong background thread
        executorService.execute(() -> {
            // Lọc theo tab
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

            // Nhóm theo category
            Map<String, Double> categoryMap = new HashMap<>();
            for (Transaction t : tabFilteredList) {
                String category = t.getCategory();
                categoryMap.put(category, categoryMap.getOrDefault(category, 0.0) + t.getAmount());
            }

            // Tạo danh sách category analysis
            List<CategoryAnalysis> categoryList = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                double percentage = (total == 0) ? 0 : (entry.getValue() / total) * 100;
                categoryList.add(new CategoryAnalysis(
                        entry.getKey(),
                        entry.getValue(),
                        percentage
                ));
            }

            // Sắp xếp
            categoryList.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

            // ✅ Lưu vào cache
            cachedAnalysis.put(cacheKey, categoryList);

            final double finalTotal = total;
            final Map<String, Double> finalCategoryMap = categoryMap;

            // ✅ Cập nhật UI trên main thread
            mainHandler.post(() -> {
                tvTotalAmount.setText(currencyFormatter.format(finalTotal));
                setupPieChart(finalCategoryMap, finalTotal);
                categoryAdapter.updateData(categoryList);
            });
        });
    }

    /**
     * ✅ Cập nhật UI với dữ liệu đã cache
     */
    private void updateUIWithAnalysis(List<CategoryAnalysis> categoryList) {
        double total = 0;
        Map<String, Double> categoryMap = new HashMap<>();

        for (CategoryAnalysis ca : categoryList) {
            total += ca.getAmount();
            categoryMap.put(ca.getCategory(), ca.getAmount());
        }

        tvTotalAmount.setText(currencyFormatter.format(total));
        setupPieChart(categoryMap, total);
        categoryAdapter.updateData(categoryList);
    }

    /**
     * Cấu hình PieChart (Biểu đồ tròn)
     */
    private void setupPieChart(Map<String, Double> categoryMap, double total) {
        if (categoryMap.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Không có dữ liệu");
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Màu sắc
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(244, 67, 54));
        colors.add(Color.rgb(233, 30, 99));
        colors.add(Color.rgb(156, 39, 176));
        colors.add(Color.rgb(103, 58, 183));
        colors.add(Color.rgb(63, 81, 181));
        colors.add(Color.rgb(33, 150, 243));
        colors.add(Color.rgb(0, 188, 212));
        colors.add(Color.rgb(0, 150, 136));
        colors.add(Color.rgb(76, 175, 80));
        colors.add(Color.rgb(255, 193, 7));
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
        pieChart.animateY(400); // ✅ Giảm thời gian animation từ 1000ms → 400ms
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
        cachedAnalysis.clear(); // ✅ Xóa cache khi đổi filter
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

        cachedAnalysis.clear();
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

                    DatePickerDialog endPicker = new DatePickerDialog(
                            this,
                            (v, y, m, d) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(y, m, d, 23, 59, 59);
                                filterEndDate = endCal.getTime();

                                cachedAnalysis.clear();
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
            if (t.getTimestamp() > 0) {
                Date transDate = t.getDate();
                if (!transDate.before(filterStartDate) && !transDate.after(filterEndDate)) {
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