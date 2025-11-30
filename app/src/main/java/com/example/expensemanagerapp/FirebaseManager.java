package com.example.expensemanagerapp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ✅ Singleton Firebase Manager - Optimized Version
 * Quản lý tất cả các tương tác với Firebase (Auth & Firestore)
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final ExecutorService executorService;

    // Collection names
    private static final String USERS_COLLECTION = "users";
    public static final String TRANSACTIONS_COLLECTION = "transactions";
    public static final String GOALS_COLLECTION = "goals";
    public static final String WALLETS_COLLECTION = "wallets";
    public static final String CATEGORIES_COLLECTION = "categories";

    // ✅ Cache user ID để tránh gọi getCurrentUser() nhiều lần
    private String cachedUserId = null;

    // ✅ Listener registry để cleanup
    private final Map<String, ListenerRegistration> listenerRegistry = new HashMap<>();

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ Cấu hình Firestore để tối ưu
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Enable offline persistence
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);

        // ✅ ExecutorService cho background tasks
        executorService = Executors.newFixedThreadPool(3);

        // ✅ Cache user ID khi auth state thay đổi
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            cachedUserId = (user != null) ? user.getUid() : null;

            // Clear all listeners khi user logout
            if (cachedUserId == null) {
                removeAllListeners();
            }
        });
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * ✅ Lấy ID của người dùng hiện tại (cached)
     */
    @Nullable
    public String getUserId() {
        if (cachedUserId == null) {
            FirebaseUser user = auth.getCurrentUser();
            cachedUserId = (user != null) ? user.getUid() : null;
        }
        return cachedUserId;
    }

    /**
     * ✅ Kiểm tra user đã đăng nhập chưa
     */
    public boolean isUserLoggedIn() {
        return getUserId() != null;
    }

    /**
     * ✅ Lấy Firebase User object
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * ✅ Đăng xuất và clear cache
     */
    public void signOut() {
        auth.signOut();
        cachedUserId = null;
        removeAllListeners();
    }

    // ==================== COLLECTION REFERENCES ====================

    /**
     * ✅ Lấy Collection Reference cho user hiện tại
     */
    @Nullable
    public CollectionReference getUserCollectionRef(@NonNull String collectionName) {
        String uid = getUserId();
        if (uid == null) {
            Log.w(TAG, "getUserCollectionRef: User not logged in");
            return null;
        }
        return db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(collectionName);
    }

    /**
     * ✅ Lấy Document Reference
     */
    @Nullable
    public DocumentReference getDocumentRef(@NonNull String collectionName, @NonNull String documentId) {
        CollectionReference ref = getUserCollectionRef(collectionName);
        if (ref == null) return null;
        return ref.document(documentId);
    }

    // ==================== TRANSACTIONS ====================

    /**
     * ✅ Lưu transaction mới
     */
    public void saveTransaction(@NonNull Transaction transaction, @NonNull OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(TRANSACTIONS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.add(transaction)
                .addOnSuccessListener(documentReference -> {
                    transaction.setId(documentReference.getId());
                    listener.onSuccess("Transaction saved: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveTransaction failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Cập nhật transaction
     */
    public void updateTransaction(@NonNull Transaction transaction, @NonNull OnCompleteListener listener) {
        if (transaction.getId() == null) {
            listener.onFailure(new Exception("Transaction ID is null"));
            return;
        }

        DocumentReference ref = getDocumentRef(TRANSACTIONS_COLLECTION, transaction.getId());
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.set(transaction)
                .addOnSuccessListener(aVoid -> listener.onSuccess("Transaction updated"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateTransaction failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Xóa transaction
     */
    public void deleteTransaction(@NonNull String transactionId, @NonNull OnCompleteListener listener) {
        DocumentReference ref = getDocumentRef(TRANSACTIONS_COLLECTION, transactionId);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess("Transaction deleted"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteTransaction failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Load transactions với listener (realtime)
     */
    public ListenerRegistration loadTransactionsRealtime(@NonNull OnDataLoadListener<Transaction> listener) {
        CollectionReference ref = getUserCollectionRef(TRANSACTIONS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return null;
        }

        return ref.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "loadTransactionsRealtime failed", error);
                        listener.onFailure(error);
                        return;
                    }

                    if (snapshots != null) {
                        List<Transaction> transactions = snapshots.toObjects(Transaction.class);
                        listener.onSuccess(transactions);
                    }
                });
    }

    // ==================== GOALS ====================

    /**
     * ✅ Lưu mục tiêu mới
     */
    public void saveGoal(@NonNull Goal goal, @NonNull OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(GOALS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.add(goal)
                .addOnSuccessListener(documentReference -> {
                    goal.setId(documentReference.getId());
                    listener.onSuccess("Goal saved: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveGoal failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Cập nhật mục tiêu
     */
    public void updateGoal(@NonNull Goal goal, @NonNull OnCompleteListener listener) {
        if (goal.getId() == null) {
            listener.onFailure(new Exception("Goal ID is null"));
            return;
        }

        DocumentReference ref = getDocumentRef(GOALS_COLLECTION, goal.getId());
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.set(goal)
                .addOnSuccessListener(aVoid -> listener.onSuccess("Goal updated"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateGoal failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Xóa mục tiêu
     */
    public void deleteGoal(@NonNull String goalId, @NonNull OnCompleteListener listener) {
        DocumentReference ref = getDocumentRef(GOALS_COLLECTION, goalId);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        ref.delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess("Goal deleted"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteGoal failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Load goals với listener (realtime)
     */
    public ListenerRegistration loadGoalsRealtime(@NonNull OnDataLoadListener<Goal> listener) {
        CollectionReference ref = getUserCollectionRef(GOALS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return null;
        }

        return ref.orderBy("targetDate", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "loadGoalsRealtime failed", error);
                        listener.onFailure(error);
                        return;
                    }

                    if (snapshots != null) {
                        List<Goal> goals = snapshots.toObjects(Goal.class);
                        listener.onSuccess(goals);
                    }
                });
    }

    // ==================== WALLETS ====================

    /**
     * ✅ Lưu ví mới
     */
    public void saveWallet(@NonNull Wallet wallet, @NonNull OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(WALLETS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        String walletId = ref.document().getId();
        wallet.setId(walletId);

        ref.document(walletId).set(wallet)
                .addOnSuccessListener(aVoid -> listener.onSuccess("Wallet saved: " + walletId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveWallet failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Load wallets với listener (realtime)
     */
    public ListenerRegistration loadWalletsRealtime(@NonNull OnDataLoadListener<Wallet> listener) {
        CollectionReference ref = getUserCollectionRef(WALLETS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return null;
        }

        return ref.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "loadWalletsRealtime failed", error);
                        listener.onFailure(error);
                        return;
                    }

                    if (snapshots != null) {
                        List<Wallet> wallets = snapshots.toObjects(Wallet.class);
                        listener.onSuccess(wallets);
                    }
                });
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * ✅ Xóa nhiều documents cùng lúc (batch delete)
     */
    public void batchDelete(@NonNull String collectionName, @NonNull List<String> documentIds,
                            @NonNull OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(collectionName);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        WriteBatch batch = db.batch();
        for (String docId : documentIds) {
            batch.delete(ref.document(docId));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onSuccess("Batch delete successful: " + documentIds.size() + " items"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "batchDelete failed", e);
                    listener.onFailure(e);
                });
    }

    /**
     * ✅ Batch update
     */
    public void batchUpdate(@NonNull String collectionName, @NonNull Map<String, Object> updates,
                            @NonNull List<String> documentIds, @NonNull OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(collectionName);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        WriteBatch batch = db.batch();
        for (String docId : documentIds) {
            batch.update(ref.document(docId), updates);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onSuccess("Batch update successful"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "batchUpdate failed", e);
                    listener.onFailure(e);
                });
    }

    // ==================== LISTENER MANAGEMENT ====================

    /**
     * ✅ Đăng ký listener với key để quản lý
     */
    public void registerListener(@NonNull String key, @NonNull ListenerRegistration registration) {
        // Remove old listener with same key
        removeListener(key);
        listenerRegistry.put(key, registration);
    }

    /**
     * ✅ Remove listener theo key
     */
    public void removeListener(@NonNull String key) {
        ListenerRegistration registration = listenerRegistry.remove(key);
        if (registration != null) {
            registration.remove();
        }
    }

    /**
     * ✅ Remove tất cả listeners
     */
    public void removeAllListeners() {
        for (ListenerRegistration registration : listenerRegistry.values()) {
            registration.remove();
        }
        listenerRegistry.clear();
    }

    // ==================== UTILITIES ====================

    /**
     * ✅ Check kết nối Firestore
     */
    public Task<Void> checkConnection() {
        return db.collection("_health_check").document("test").get()
                .continueWith(task -> null);
    }

    /**
     * ✅ Clear cache
     */
    public Task<Void> clearCache() {
        return db.clearPersistence();
    }

    /**
     * ✅ Cleanup resources
     */
    public void cleanup() {
        removeAllListeners();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ==================== INTERFACES ====================

    /**
     * Callback cho các thao tác đơn giản
     */
    public interface OnCompleteListener {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    /**
     * ✅ Callback cho load data với generic type
     */
    public interface OnDataLoadListener<T> {
        void onSuccess(List<T> data);
        void onFailure(Exception e);
    }

    /**
     * ✅ Callback cho load single document
     */
    public interface OnDocumentLoadListener<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }
}