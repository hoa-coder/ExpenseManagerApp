package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AccountTypesActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView btnBack;
    // It's more robust to add specific IDs to each clickable LinearLayout in your XML.
    // However, I will find them within their parent container as a fallback.
    private LinearLayout itemCash, itemDepositCard, itemCreditCard, itemVirtualAccount, itemInvestment, itemReceivable, itemPayable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_types);

        // Khởi tạo nút quay lại
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Tìm các mục trong ScrollView
        // Giả sử LinearLayout con trực tiếp của ScrollView chứa tất cả các mục.
        LinearLayout mainContainer = (LinearLayout) findViewById(R.id.activity_account_types_container); // You need to add this ID to the root LinearLayout inside ScrollView

        // I will assume the order of the items. It is highly recommended to give each item a unique ID.
        // For example: android:id="@+id/item_cash"
        // Since the XML does not have IDs for the clickable rows, I cannot directly find them.
        // I will add a placeholder comment on how to implement it once you add the IDs.

        /*
        // --- VÍ DỤ SAU KHI BẠN THÊM ID VÀO XML ---
        // Trong activity_account_types.xml, thêm ID vào mỗi LinearLayout của từng mục, ví dụ:
        // <LinearLayout android:id="@+id/item_cash" ... >

        itemCash = findViewById(R.id.item_cash);
        itemDepositCard = findViewById(R.id.item_deposit_card);
        itemCreditCard = findViewById(R.id.item_credit_card);
        // ... và các mục khác

        itemCash.setOnClickListener(this);
        itemDepositCard.setOnClickListener(this);
        itemCreditCard.setOnClickListener(this);
        // ...
        */
        
        // Hiển thị thông báo tạm thời vì chưa có ID trong XML
        Toast.makeText(this, "Vui lòng thêm ID cho các mục để kích hoạt sự kiện click.", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onClick(View v) {
        String accountType = "";
        // Sử dụng if-else để xử lý sự kiện click sau khi đã thêm ID
        /*
        if (v.getId() == R.id.item_cash) {
            accountType = "Tiền mặt";
        } else if (v.getId() == R.id.item_deposit_card) {
            accountType = "Thẻ tiền gửi";
        } else if (v.getId() == R.id.item_credit_card) {
            accountType = "Thẻ tín dụng";
        } // ... thêm các else if cho các mục còn lại
        */

        if (!accountType.isEmpty()) {
            // Hiển thị toast để xác nhận lựa chọn
            Toast.makeText(this, "Đã chọn: " + accountType, Toast.LENGTH_SHORT).show();

            // Chuyển đến màn hình tạo tài khoản và gửi kèm loại tài khoản
            // Intent intent = new Intent(AccountTypesActivity.this, CreateAccountActivity.class);
            // intent.putExtra("ACCOUNT_TYPE", accountType);
            // startActivity(intent);
        }
    }
}