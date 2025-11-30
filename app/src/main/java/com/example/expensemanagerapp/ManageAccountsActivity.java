package com.example.expensemanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity để quản lý các tài khoản (Ví tiền).
 */
public class ManageAccountsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CREATE_WALLET = 100;

    private LinearLayout llAddAccount;
    private LinearLayout accountListContainer;
    private ImageView btnDone;
    private TextView tvHeader;
    private LinearLayout llDeleteButtons;
    private Button btnDeleteSelected;
    private Button btnCancelDelete;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DecimalFormat currencyFormatter = new DecimalFormat("#,### đ");

    private List<Wallet> walletList = new ArrayList<>();
    private ListenerRegistration walletListenerRegistration;

    // ✅ Chế độ xóa nhiều ví
    private boolean isDeleteMode = false;
    private Set<String> selectedWalletIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_accounts);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo Views
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // ✅ Đăng ký listener mỗi khi Activity hiển thị
        loadWalletsFromFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ✅ Hủy listener khi Activity không hiển thị
        if (walletListenerRegistration != null) {
            walletListenerRegistration.remove();
            walletListenerRegistration = null;
        }
    }

    private void initViews() {
        // Nút back
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (isDeleteMode) {
                exitDeleteMode();
            } else {
                finish();
            }
        });

        // Header và nút Done (chuyển thành nút xóa)
        tvHeader = findViewById(R.id.tv_header);
        btnDone = findViewById(R.id.btn_done);
        btnDone.setOnClickListener(v -> {
            if (!isDeleteMode) {
                enterDeleteMode();
            }
        });

        // Nút thêm ví mới
        llAddAccount = findViewById(R.id.ll_add_account);
        llAddAccount.setOnClickListener(v -> {
            Intent intent = new Intent(ManageAccountsActivity.this, AccountTypesActivity.class);
            startActivityForResult(intent, REQUEST_CODE_CREATE_WALLET);
        });

        // Container cho danh sách ví
        accountListContainer = findViewById(R.id.ll_wallet_list_container);

        // ✅ Thêm layout chứa nút xóa (động)
        createDeleteButtonsLayout();
    }

    /**
     * ✅ Tạo layout chứa nút xóa (thêm vào cuối màn hình)
     */
    private void createDeleteButtonsLayout() {
        // Tìm LinearLayout cha chứa ScrollView
        LinearLayout parentLayout = (LinearLayout) accountListContainer.getParent().getParent();

        // Tạo layout chứa nút xóa
        llDeleteButtons = new LinearLayout(this);
        llDeleteButtons.setOrientation(LinearLayout.HORIZONTAL);
        llDeleteButtons.setGravity(android.view.Gravity.CENTER);
        llDeleteButtons.setPadding(16, 16, 16, 16);
        llDeleteButtons.setBackgroundColor(0xFFFFFFFF);
        llDeleteButtons.setVisibility(View.GONE); // Ẩn mặc định

        // Nút Hủy
        btnCancelDelete = new Button(this);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        cancelParams.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
        btnCancelDelete.setLayoutParams(cancelParams);
        btnCancelDelete.setText("Hủy");
        btnCancelDelete.setTextColor(0xFF757575);
        btnCancelDelete.setBackgroundColor(0xFFEEEEEE);
        btnCancelDelete.setOnClickListener(v -> exitDeleteMode());

        // Nút Xóa đã chọn
        btnDeleteSelected = new Button(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        deleteParams.leftMargin = (int) (8 * getResources().getDisplayMetrics().density);
        btnDeleteSelected.setLayoutParams(deleteParams);
        btnDeleteSelected.setText("Xóa đã chọn (0)");
        btnDeleteSelected.setTextColor(0xFFFFFFFF);
        btnDeleteSelected.setBackgroundColor(0xFFE53935);
        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelectedWallets());

        llDeleteButtons.addView(btnCancelDelete);
        llDeleteButtons.addView(btnDeleteSelected);

        // Thêm vào layout cha (trước nút "Thêm ví")
        int addButtonIndex = parentLayout.indexOfChild(llAddAccount);
        parentLayout.addView(llDeleteButtons, addButtonIndex);
    }

    /**
     * ✅ Vào chế độ xóa nhiều ví
     */
    private void enterDeleteMode() {
        isDeleteMode = true;
        selectedWalletIds.clear();

        // Thay đổi UI
        tvHeader.setText("Chọn ví để xóa");
        btnDone.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        llAddAccount.setVisibility(View.GONE);
        llDeleteButtons.setVisibility(View.VISIBLE);

        // Reload danh sách để hiển thị checkbox
        refreshWalletList();
    }

    /**
     * ✅ Thoát chế độ xóa
     */
    private void exitDeleteMode() {
        isDeleteMode = false;
        selectedWalletIds.clear();

        // Khôi phục UI
        tvHeader.setText("Quản lý tài khoản");
        btnDone.setImageResource(android.R.drawable.ic_menu_save);
        llAddAccount.setVisibility(View.VISIBLE);
        llDeleteButtons.setVisibility(View.GONE);

        // Reload danh sách
        refreshWalletList();
    }

    /**
     * ✅ Làm mới danh sách ví
     */
    private void refreshWalletList() {
        if (accountListContainer != null) {
            accountListContainer.removeAllViews();
        }

        for (Wallet wallet : walletList) {
            addWalletCardView(wallet);
        }
    }

    /**
     * ✅ Lắng nghe và cập nhật danh sách ví từ Firebase Firestore (Realtime updates)
     */
    private void loadWalletsFromFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "❌ Vui lòng đăng nhập để xem danh sách ví.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d("ManageAccounts", "Loading wallets for UserID: " + userId); // LOGGING

        // ✅ Gỡ bỏ listener cũ nếu có (tránh duplicate listeners)
        if (walletListenerRegistration != null) {
            walletListenerRegistration.remove();
            Log.d("ManageAccounts", "Removed old wallet listener."); // LOGGING
        }

        // ✅ Đăng ký Snapshot Listener để nhận cập nhật realtime
        walletListenerRegistration = db.collection("users")
                .document(userId)
                .collection("wallets")
                .addSnapshotListener(this, (queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        // Bắt lỗi lắng nghe (ví dụ: mất kết nối)
                        Log.e("ManageAccounts", "Error listening for wallet snapshots: " + e.getMessage()); // LOGGING
                        Toast.makeText(this, "❌ Lỗi lắng nghe danh sách ví: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        Log.d("ManageAccounts", "Snapshot received. Document count: " + queryDocumentSnapshots.size()); // LOGGING
                        walletList.clear();
                        // ✅ Xóa tất cả CardView cũ
                        if (accountListContainer != null) {
                            accountListContainer.removeAllViews();
                        }

                        // ✅ Thêm lại các ví từ Firestore
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                // Thử ánh xạ thủ công để kiểm soát các kiểu dữ liệu khác nhau (long vs Timestamp)
                                Map<String, Object> data = document.getData();
                                if (data != null) {
                                    Wallet wallet = document.toObject(Wallet.class);
                                    long timestamp = 0;
                                    
                                    Object tsField = data.get("timestamp");
                                    if (tsField instanceof Long) {
                                        timestamp = (Long) tsField;
                                    } else if (tsField instanceof Timestamp) {
                                        timestamp = ((Timestamp) tsField).toDate().getTime();
                                    }
                                    
                                    // Gán lại ID và timestamp để đảm bảo tính nhất quán
                                    wallet.setId(document.getId());
                                    wallet.setTimestamp(timestamp);

                                    // CHỈ THÊM VÍ NẾU CÁC TRƯỜNG QUAN TRỌNG KHÔNG NULL
                                    if(wallet.getId() != null && wallet.getName() != null && wallet.getType() != null) {
                                        walletList.add(wallet);
                                        Log.d("ManageAccounts", "Added wallet to list: " + wallet.getName() + " (ID: " + wallet.getId() + ")"); // LOGGING
                                        addWalletCardView(wallet);
                                    } else {
                                        Log.w("ManageAccounts", "Skipping wallet due to missing data. ID: " + document.getId()); // LOGGING
                                    }
                                }
                            } catch (Exception ex) {
                                // BẮT LỖI ÁNH XẠ (MAPPING ERROR) TẠI ĐÂY
                                Log.e("ManageAccounts", "Mapping error for document " + document.getId() + ": " + ex.getMessage()); // LOGGING
                            }
                        }
                        
                        Log.d("ManageAccounts", "Finished processing documents. Total in list: " + walletList.size()); // LOGGING

                        // ✅ Hiển thị thông báo nếu danh sách trống
                        if (walletList.isEmpty() && !isDeleteMode) {
                            // Có thể thêm TextView "Chưa có ví nào" nếu muốn
                        }
                    }
                });
    }

    /**
     * Tạo và thêm CardView ví vào container
     */
    private void addWalletCardView(Wallet wallet) {
        if (accountListContainer == null) return;

        // Tạo CardView
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(8 * getResources().getDisplayMetrics().density);
        cardView.setCardElevation(2 * getResources().getDisplayMetrics().density);

        // LinearLayout bên trong CardView
        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density)
        );
        innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // CheckBox - Thay đổi chức năng theo chế độ
        CheckBox checkBox = new CheckBox(this);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFEC407A));

        if (isDeleteMode) {
            // ✅ Chế độ xóa: checkbox để chọn ví cần xóa
            checkBox.setChecked(selectedWalletIds.contains(wallet.getId()));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedWalletIds.add(wallet.getId());
                } else {
                    selectedWalletIds.remove(wallet.getId());
                }
                updateDeleteButtonText();
            });
        } else {
            // ✅ Chế độ thường: checkbox hiển thị trạng thái active
            checkBox.setChecked(wallet.isActive());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateWalletActiveStatus(wallet, isChecked);
            });
        }
        innerLayout.addView(checkBox);

        // Icon (emoji dạng text)
        TextView iconText = new TextView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)
        );
        iconParams.leftMargin = (int) (8 * getResources().getDisplayMetrics().density);
        iconText.setLayoutParams(iconParams);
        iconText.setText(getWalletIcon(wallet.getType()));
        iconText.setTextSize(28);
        iconText.setGravity(android.view.Gravity.CENTER);
        innerLayout.addView(iconText);

        // Thông tin ví (LinearLayout vertical)
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoParams.leftMargin = (int) (12 * getResources().getDisplayMetrics().density);
        infoLayout.setLayoutParams(infoParams);

        // Tên ví
        TextView nameText = new TextView(this);
        nameText.setText(wallet.getName());
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setTextColor(0xFF212121);
        infoLayout.addView(nameText);

        // Loại ví
        TextView typeText = new TextView(this);
        typeText.setText(wallet.getType());
        typeText.setTextSize(13);
        typeText.setTextColor(0xFF757575);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        typeParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        typeText.setLayoutParams(typeParams);
        infoLayout.addView(typeText);

        // Số dư
        TextView balanceText = new TextView(this);
        balanceText.setText(currencyFormatter.format(wallet.getBalance()));
        balanceText.setTextSize(14);
        balanceText.setTextColor(0xFFEC407A);
        balanceText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        balanceParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        balanceText.setLayoutParams(balanceParams);
        infoLayout.addView(balanceText);

        innerLayout.addView(infoLayout);

        // Icon menu (3 chấm) - Thay đổi thành icon chỉnh sửa
        if (!isDeleteMode) {
            ImageView editIcon = new ImageView(this);
            LinearLayout.LayoutParams editIconParams = new LinearLayout.LayoutParams(
                    (int) (32 * getResources().getDisplayMetrics().density),
                    (int) (32 * getResources().getDisplayMetrics().density)
            );
            editIcon.setLayoutParams(editIconParams);
            editIcon.setImageResource(R.drawable.ic_edit_pencil); // Sử dụng icon chỉnh sửa
            editIcon.setPadding(
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density)
            );
            editIcon.setColorFilter(0xFFEC407A);

            editIcon.setOnClickListener(v -> {
                showEditAccountDialog(wallet); // Gọi dialog chỉnh sửa
            });
            innerLayout.addView(editIcon);
        }

        cardView.addView(innerLayout);

        // Thêm sự kiện click vào card - Chỉ hoạt động trong chế độ xóa
        if (isDeleteMode) {
            cardView.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked());
            });
        } else {
            cardView.setOnClickListener(v -> {
                Toast.makeText(this, "Đã chọn: " + wallet.getName(), Toast.LENGTH_SHORT).show();
            });
        }

        // Thêm vào container
        accountListContainer.addView(cardView);
    }

    /**
     * ✅ Cập nhật text nút xóa
     */
    private void updateDeleteButtonText() {
        int count = selectedWalletIds.size();
        btnDeleteSelected.setText("Xóa đã chọn (" + count + ")");
        btnDeleteSelected.setEnabled(count > 0);
        btnDeleteSelected.setAlpha(count > 0 ? 1.0f : 0.5f);
    }

    /**
     * ✅ Xác nhận xóa các ví đã chọn
     */
    private void confirmDeleteSelectedWallets() {
        if (selectedWalletIds.isEmpty()) {
            Toast.makeText(this, "⚠️ Vui lòng chọn ít nhất một ví để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc muốn xóa " + selectedWalletIds.size() + " ví đã chọn?");
        builder.setPositiveButton("Xóa", (dialog, which) -> deleteSelectedWallets());
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    /**
     * ✅ Xóa các ví đã chọn
     */
    private void deleteSelectedWallets() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        int totalToDelete = selectedWalletIds.size();
        final int[] deletedCount = {0};
        final int[] failedCount = {0};

        for (String walletId : selectedWalletIds) {
            db.collection("users")
                    .document(userId)
                    .collection("wallets")
                    .document(walletId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        checkDeleteCompletion(totalToDelete, deletedCount[0], failedCount[0]);
                    })
                    .addOnFailureListener(e -> {
                        failedCount[0]++;
                        checkDeleteCompletion(totalToDelete, deletedCount[0], failedCount[0]);
                    });
        }
    }

    /**
     * ✅ Kiểm tra hoàn tất xóa
     */
    private void checkDeleteCompletion(int total, int deleted, int failed) {
        if (deleted + failed == total) {
            String message;
            if (failed == 0) {
                message = "✅ Đã xóa " + deleted + " ví thành công!";
            } else {
                message = "⚠️ Đã xóa " + deleted + " ví, " + failed + " ví lỗi";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            exitDeleteMode();
        }
    }

    /**
     * ✅ Cập nhật trạng thái active của ví
     */
    private void updateWalletActiveStatus(Wallet wallet, boolean isActive) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users")
                .document(userId)
                .collection("wallets")
                .document(wallet.getId())
                .update("active", isActive)
                .addOnSuccessListener(aVoid -> {
                    String status = isActive ? "kích hoạt" : "vô hiệu hóa";
                    Toast.makeText(this, "Đã " + status + " ví: " + wallet.getName(), Toast.LENGTH_SHORT).show();
                    // Listener sẽ tự động cập nhật danh sách
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ Hiển thị dialog tùy chọn (Edit/Delete) - Đã cập nhật để gọi showEditAccountDialog
     */
    private void showWalletOptionsDialog(Wallet wallet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tùy chọn ví: " + wallet.getName());
        builder.setItems(new String[]{"✏️ Chỉnh sửa", "🗑️ Xóa"}, (dialog, which) -> {
            if (which == 0) {
                showEditAccountDialog(wallet); // Gọi dialog chỉnh sửa ví
            } else if (which == 1) {
                confirmDeleteWallet(wallet);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    /**
     * ✅ Hiển thị dialog chỉnh sửa ví
     */
    private void showEditAccountDialog(Wallet wallet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_account, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Tham chiếu views trong dialog
        TextInputEditText etAccountName = dialogView.findViewById(R.id.et_account_name);
        TextInputEditText etAmountAdjustment = dialogView.findViewById(R.id.et_amount_adjustment);
        TextView tvCurrentBalance = dialogView.findViewById(R.id.tv_current_balance);
        CheckBox cbIncludeInTotal = dialogView.findViewById(R.id.cb_include_in_total);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        // Điền sẵn dữ liệu hiện tại
        etAccountName.setText(wallet.getName());
        tvCurrentBalance.setText("Số dư hiện tại: " + currencyFormatter.format(wallet.getBalance()));
        cbIncludeInTotal.setChecked(wallet.isActive()); // Sử dụng isActive() cho CheckBox

        // Xử lý nút Hủy
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Xử lý nút Lưu
        btnSave.setOnClickListener(v -> {
            String newName = etAccountName.getText().toString().trim();
            String amountAdjStr = etAmountAdjustment.getText().toString().trim();
            boolean newIsActive = cbIncludeInTotal.isChecked();

            if (newName.isEmpty()) {
                etAccountName.setError("Tên ví không được để trống");
                return;
            }

            double amountAdjustment = 0;
            if (!amountAdjStr.isEmpty()) {
                try {
                    amountAdjustment = Double.parseDouble(amountAdjStr);
                } catch (NumberFormatException e) {
                    etAmountAdjustment.setError("Số tiền không hợp lệ");
                    return;
                }
            }

            double newBalance = wallet.getBalance() + amountAdjustment;

            // Cập nhật ví vào Firebase
            updateWalletInFirebase(wallet.getId(), newName, newBalance, newIsActive);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * ✅ Cập nhật ví vào Firebase Firestore
     */
    private void updateWalletInFirebase(String walletId, String newName, double newBalance, boolean newIsActive) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("balance", newBalance);
        updates.put("active", newIsActive);

        db.collection("users")
                .document(userId)
                .collection("wallets")
                .document(walletId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Đã cập nhật ví thành công!", Toast.LENGTH_SHORT).show();
                    // Listener sẽ tự động tải lại danh sách
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi cập nhật ví: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * ✅ Xác nhận xóa ví đơn lẻ
     */
    private void confirmDeleteWallet(Wallet wallet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc muốn xóa ví '" + wallet.getName() + "'?");
        builder.setPositiveButton("Xóa", (dialog, which) -> deleteWallet(wallet));
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    /**
     * ✅ Xóa ví đơn lẻ khỏi Firebase
     */
    private void deleteWallet(Wallet wallet) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users")
                .document(userId)
                .collection("wallets")
                .document(wallet.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Đã xóa ví: " + wallet.getName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi xóa ví: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Lấy icon theo loại ví
     */
    private String getWalletIcon(String type) {
        switch (type) {
            case "Tiền mặt":
                return "💵";
            case "Thẻ tiền gửi":
                return "💳";
            case "Thẻ tín dụng":
                return "💳";
            case "Tài khoản ảo":
                return "🏦";
            case "Đầu tư":
                return "📈";
            case "Phải thu":
                return "💰";
            case "Phải trả":
                return "💸";
            default:
                return "💼";
        }
    }

    /**
     * ✅ Nhận kết quả từ AccountTypesActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREATE_WALLET && resultCode == RESULT_OK) {
            Toast.makeText(this, "✅ Ví mới đã được thêm! Danh sách đang được làm mới...", Toast.LENGTH_SHORT).show();
            // Buộc tải lại dữ liệu ngay lập tức để giải quyết vấn đề hiển thị chậm/lỗi
            loadWalletsFromFirebase(); 
        }
    }

    @Override
    public void onBackPressed() {
        if (isDeleteMode) {
            exitDeleteMode();
        } else {
            super.onBackPressed();
        }
    }
}