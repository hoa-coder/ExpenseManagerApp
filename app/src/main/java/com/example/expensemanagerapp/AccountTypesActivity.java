package com.example.expensemanagerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountTypesActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView btnBack;
    private LinearLayout itemCash, itemDepositCard, itemCreditCard, itemVirtualAccount, itemInvestment, itemReceivable, itemPayable;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_types);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo nút quay lại
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Ánh xạ các mục và thiết lập sự kiện click
        itemCash = findViewById(R.id.item_cash);
        itemDepositCard = findViewById(R.id.item_deposit_card);
        itemCreditCard = findViewById(R.id.item_credit_card);
        itemVirtualAccount = findViewById(R.id.item_virtual_account);
        itemInvestment = findViewById(R.id.item_investment);
        itemReceivable = findViewById(R.id.item_receivable);
        itemPayable = findViewById(R.id.item_payable);

        itemCash.setOnClickListener(this);
        itemDepositCard.setOnClickListener(this);
        itemCreditCard.setOnClickListener(this);
        itemVirtualAccount.setOnClickListener(this);
        itemInvestment.setOnClickListener(this);
        itemReceivable.setOnClickListener(this);
        itemPayable.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String walletType;

        int id = v.getId();
        if (id == R.id.item_cash) {
            walletType = "Tiền mặt";
        } else if (id == R.id.item_deposit_card) {
            walletType = "Thẻ tiền gửi";
        } else if (id == R.id.item_credit_card) {
            walletType = "Thẻ tín dụng";
        } else if (id == R.id.item_virtual_account) {
            walletType = "Tài khoản ảo";
        } else if (id == R.id.item_investment) {
            walletType = "Đầu tư";
        } else if (id == R.id.item_receivable) {
            walletType = "Phải thu";
        } else if (id == R.id.item_payable) {
            walletType = "Phải trả";
        } else {
            return;
        }

        showCreateWalletDialog(walletType);
    }

    private void showCreateWalletDialog(String walletType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_wallet, null);
        builder.setView(dialogView);

        // Ánh xạ View trong Dialog
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextInputEditText etWalletName = dialogView.findViewById(R.id.et_wallet_name);
        TextInputEditText etInitialBalance = dialogView.findViewById(R.id.et_initial_balance);
        CheckBox cbIsActive = dialogView.findViewById(R.id.cb_is_active);

        tvDialogTitle.setText("Tạo Ví Loại: " + walletType);

        // Thêm nút Lưu/Xác nhận và Hủy
        builder.setPositiveButton("Lưu", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Override Positive Button Click Listener for manual validation
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etWalletName.getText().toString().trim();
            String balanceStr = etInitialBalance.getText().toString().trim();
            boolean isActive = cbIsActive.isChecked();

            // 1. Validate
            if (name.isEmpty()) {
                etWalletName.setError("Tên ví không được để trống");
                etWalletName.requestFocus();
                return;
            }

            if (balanceStr.isEmpty()) {
                etInitialBalance.setError("Số tiền ban đầu không được để trống");
                etInitialBalance.requestFocus();
                return;
            }

            double initialBalance;
            try {
                initialBalance = Double.parseDouble(balanceStr);
                if (initialBalance < 0) {
                    etInitialBalance.setError("Số tiền không được âm");
                    etInitialBalance.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etInitialBalance.setError("Số tiền không hợp lệ");
                etInitialBalance.requestFocus();
                return;
            }

            // 2. Save to Firebase
            saveWalletToFirebase(walletType, name, initialBalance, isActive, alertDialog);
        });
    }

    private void saveWalletToFirebase(String type, String name, double balance, boolean isActive, AlertDialog dialog) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "❌ Lỗi: Người dùng chưa đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }

        String userId = currentUser.getUid();
        // Đường dẫn: users/{userId}/wallets/{walletId}
        CollectionReference walletsRef = db.collection("users").document(userId).collection("wallets");

        // Tạo ID ví mới do Firestore cấp
        String walletId = walletsRef.document().getId();

        Wallet newWallet = new Wallet(
                walletId,
                name,
                type,
                balance,
                isActive,
                System.currentTimeMillis()
        );

        // ✅ Thành công - Đóng dialog
        dialog.dismiss();

        // ✅ Hiển thị thông báo
        Toast.makeText(this, "✅ Đã tạo ví '" + name + "' thành công!", Toast.LENGTH_SHORT).show();

        // ✅ Chuyển sang màn hình quản lý ví
        Intent intent = new Intent(AccountTypesActivity.this, ManageAccountsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Đóng màn hình hiện tại
    }
}