package com.example.expensemanagerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SplashActivity extends AppCompatActivity {

    // Thời gian hiển thị tối thiểu (1.2 giây = 1200ms)
    private static final int MIN_DISPLAY_TIME = 1200;

    // Khai báo các thành phần UI
    private ImageView logoImageView;
    private TextView appNameTextView;
    private ProgressBar progressBar;
    private ImageView coinDecoration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Theme đã định nghĩa để có background gradient và full screen
        setTheme(R.style.Theme_ExpenseManagerApp_Splash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ánh xạ View
        logoImageView = findViewById(R.id.iv_logo);
        appNameTextView = findViewById(R.id.tv_app_name);
        progressBar = findViewById(R.id.progress_bar);
        coinDecoration = findViewById(R.id.iv_decoration_coin);

        // 1. Áp dụng hiệu ứng động (Animation)
        applyAnimations();

        // 2. Load dữ liệu nền
        loadDataAndNavigate();
    }

    /**
     * Áp dụng các hiệu ứng động cho các thành phần UI
     */
    private void applyAnimations() {
        // Animation cho Logo: Scale + Fade-in
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation);
        logoImageView.startAnimation(logoAnimation);

        // Animation Fade-in chung cho các thành phần khác
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // Áp dụng Fade-in cho Tên ứng dụng
        appNameTextView.startAnimation(fadeInAnimation);
        appNameTextView.setVisibility(View.VISIBLE); // Đảm bảo view được hiển thị

        // Áp dụng Fade-in cho ProgressBar
        // Tạo một Animation mới cho ProgressBar để nó bắt đầu sau một chút (tùy chọn)
        Animation progressFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        // Thiết lập độ trễ ngắn hơn cho ProgressBar để nó xuất hiện sớm hơn chút
        progressFadeIn.setStartOffset(500); 
        progressBar.startAnimation(progressFadeIn);
        progressBar.setVisibility(View.VISIBLE);

        // Áp dụng Fade-in cho icon trang trí
        coinDecoration.startAnimation(fadeInAnimation);
        coinDecoration.setVisibility(View.VISIBLE);
    }

    /**
     * Xử lý load dữ liệu nền và chuyển màn hình
     */
    private void loadDataAndNavigate() {
        // Ghi lại thời điểm bắt đầu load
        final long startTime = SystemClock.uptimeMillis();
        
        // Sử dụng ExecutorService để thực hiện tác vụ nền (yêu cầu bắt buộc)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Tác vụ giả lập load dữ liệu nền
            // Ở đây, có thể thực hiện các thao tác như:
            // - Khởi tạo Database
            // - Load cấu hình
            // - Kiểm tra trạng thái đăng nhập
            try {
                // Giả lập thời gian load dữ liệu thực tế
                // Thay thế bằng logic load dữ liệu thực tế của bạn
                Thread.sleep(500); // Giả lập load 0.5 giây
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Tính toán thời gian đã trôi qua
            long elapsedTime = SystemClock.uptimeMillis() - startTime;
            long waitTime = MIN_DISPLAY_TIME - elapsedTime;

            // Đảm bảo thời gian hiển thị tối thiểu
            if (waitTime > 0) {
                try {
                    // Chờ thêm để đạt MIN_DISPLAY_TIME
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Chuyển về Main Thread để điều hướng
            handler.post(() -> {
                // Chuyển sang MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                
                // Kết thúc SplashActivity để người dùng không thể quay lại
                finish();
                
                // Tùy chọn: Thêm hiệu ứng chuyển màn hình (Activity Transition)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });
    }
}