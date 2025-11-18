package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity để quản lý các tài khoản (Ví tiền).
 * Cần tạo layout tương ứng: R.layout.activity_manage_accounts.
 */
public class ManageAccountsActivity extends AppCompatActivity {

    private LinearLayout llAddAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lưu ý: Cần đảm bảo file layout activity_manage_accounts.xml tồn tại
        // Nếu không có, ứng dụng sẽ bị crash.
        setContentView(R.layout.activity_manage_accounts);
        
        // Khởi tạo View
        llAddAccount = findViewById(R.id.ll_add_account);

        // Gán sự kiện click
        llAddAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mở màn hình AccountTypesActivity
                Intent intent = new Intent(ManageAccountsActivity.this, AccountTypesActivity.class);
                startActivity(intent);
            }
        });

        // Thêm sự kiện cho nút back nếu cần
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}