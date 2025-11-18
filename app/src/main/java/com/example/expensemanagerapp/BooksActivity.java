package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BooksActivity extends AppCompatActivity implements View.OnClickListener {

    private FloatingActionButton fabEdit;
    private LinearLayout bottomNavBooks, bottomNavWallet, bottomNavAnalysis, bottomNavAdd;
    private LinearLayout llAddGoal; // Khai báo View mới

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        // Khởi tạo các View
        fabEdit = findViewById(R.id.fab_edit);
        
        // Khởi tạo mục tiêu
        llAddGoal = findViewById(R.id.ll_add_goal); // Tìm kiếm ID mới

        // Tìm view cha của các mục điều hướng.
        LinearLayout bottomNavigation = findViewById(R.id.bottom_navigation);

        // Giả sử các LinearLayout bên trong bottom_navigation là các mục có thể nhấp vào.
        // Tốt hơn là nên gán ID cụ thể cho các LinearLayout này trong books.xml để code ổn định hơn.
        // Hiện tại, truy cập chúng bằng chỉ mục như một giải pháp thay thế.
        bottomNavBooks = (LinearLayout) bottomNavigation.getChildAt(0);
        bottomNavWallet = (LinearLayout) bottomNavigation.getChildAt(1);
        bottomNavAnalysis = (LinearLayout) bottomNavigation.getChildAt(2);
        bottomNavAdd = (LinearLayout) bottomNavigation.getChildAt(3);
        
        // Cài đặt sự kiện OnClick
        fabEdit.setOnClickListener(this);
        llAddGoal.setOnClickListener(this); // Thêm listener cho mục tiêu
        bottomNavBooks.setOnClickListener(this);
        bottomNavWallet.setOnClickListener(this);
        bottomNavAnalysis.setOnClickListener(this);
        bottomNavAdd.setOnClickListener(this);

        // Tải dữ liệu tài chính
        loadDashboardData();
    }

    private void loadDashboardData() {
        // --- PHẦN GIỮ CHỖ để tải dữ liệu ---
        // Tại đây, bạn sẽ lấy dữ liệu từ cơ sở dữ liệu/ViewModel cho:
        // - Các con số Tổng, Thu nhập, Chi tiêu
        // - Tiến độ ngân sách hàng ngày
        // - Các mục tiêu tiết kiệm
        // - Các giao dịch gần đây
        // Và cập nhật các TextView, ProgressBar, v.v. tương ứng.
        Toast.makeText(this, "Đang tải dữ liệu tài chính...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        // Sử dụng if-else vì các View không có ID trực tiếp
        if (v == fabEdit) {
            // Mở AddExpenseActivity
            Intent intent = new Intent(this, AddExpenseActivity.class);
            startActivity(intent);
        } else if (v == llAddGoal) {
            // Mở CreateSavingsGoalActivity
            Intent intent = new Intent(this, CreateSavingsGoalActivity.class);
            startActivity(intent);
        } else if (v == bottomNavBooks) {
            // Đã ở trên màn hình này
            Toast.makeText(this, "Bạn đang ở màn hình Sổ sách", Toast.LENGTH_SHORT).show();
        } else if (v == bottomNavWallet) {
            // Điều hướng đến ManageAccountsActivity (Ví tiền)
            Intent intent = new Intent(this, ManageAccountsActivity.class);
            startActivity(intent);
        } else if (v == bottomNavAnalysis) {
            // Điều hướng đến AnalysisActivity
            // Intent intent = new Intent(this, AnalysisActivity.class);
            // startActivity(intent);
             Toast.makeText(this, "Điều hướng đến Phân tích", Toast.LENGTH_SHORT).show();
        } else if (v == bottomNavAdd) {
            // Đây có thể là một lối tắt để thêm một cái gì đó
            // Intent intent = new Intent(this, AddTransactionActivity.class); // Hoặc một hộp thoại lựa chọn
            // startActivity(intent);
             Toast.makeText(this, "Điều hướng đến màn hình Thêm", Toast.LENGTH_SHORT).show();
        }
    }
}