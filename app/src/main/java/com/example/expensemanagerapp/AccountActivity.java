package com.example.expensemanagerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Giả định có thư viện Glide hoặc Picasso để load ảnh từ URL. Do không có,
// tôi sẽ chỉ hiển thị placeholder và log URL ảnh (nếu có).

/**
 * Màn hình hiển thị thông tin tài khoản và xử lý đăng xuất.
 */
public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";

    private ImageView ivAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvUid;
    private Button btnLogout;
    private ImageView btnBack;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadUserInfo();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> {
            // Quay về màn hình trước (BooksActivity) mà không reload
            finish();
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Tên hiển thị
            String displayName = user.getDisplayName();
            tvDisplayName.setText(displayName != null && !displayName.isEmpty() ? displayName : "Chưa đặt tên");

            // Email
            tvEmail.setText(user.getEmail());

            // UID
            tvUid.setText("UID: " + user.getUid());

            // Avatar (URL) - Giả định load ảnh nếu có thư viện
            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                // TODO: Sử dụng Glide/Picasso để load ảnh vào ivAvatar nếu đã thêm dependency
                Log.d(TAG, "Avatar URL: " + photoUrl.toString());
            }

            // Có thể thêm logic load thêm thông tin từ Firestore nếu cần
        } else {
            // Trường hợp lỗi (không nên xảy ra nếu được gọi từ BooksActivity)
            Toast.makeText(this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Xử lý Đăng xuất: signOut, xóa navigation stack và chuyển về LoginActivity.
     */
    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Đã đăng xuất thành công.", Toast.LENGTH_SHORT).show();

        // Điều hướng về LoginActivity và xóa tất cả Activity cũ
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        // Flags để xóa stack: CLEAR_TASK đảm bảo Activity mới là gốc, NEW_TASK là cờ bắt buộc đi kèm
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // Kết thúc AccountActivity hiện tại
        finish();
    }
}