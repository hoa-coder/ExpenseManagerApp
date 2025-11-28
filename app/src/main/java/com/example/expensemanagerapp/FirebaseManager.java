package com.example.expensemanagerapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private static final String USERS_COLLECTION = "users";
    public static final String TRANSACTIONS_COLLECTION = "transactions";
    public static final String GOALS_COLLECTION = "goals";


    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Lấy ID của người dùng hiện tại.
     * @return UID của người dùng hoặc null nếu chưa đăng nhập.
     */
    public String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    /**
     * Lấy đường dẫn Collection cho một loại dữ liệu cụ thể (transactions, goals,...)
     * dành cho người dùng hiện tại.
     * Path: users/{uid}/{collectionName}/...
     * @param collectionName Tên của sub-collection (ví dụ: "transactions")
     * @return CollectionReference hoặc null nếu người dùng chưa đăng nhập.
     */
    public CollectionReference getUserCollectionRef(String collectionName) {
        String uid = getUserId();
        if (uid != null) {
            return db.collection(USERS_COLLECTION)
                     .document(uid)
                     .collection(collectionName);
        }
        return null;
    }

    // Example method for saving a Transaction
    public void saveTransaction(Transaction transaction, OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(TRANSACTIONS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in."));
            return;
        }

        // Add a transaction, let Firestore generate the ID
        ref.add(transaction)
           .addOnSuccessListener(documentReference -> {
               // Update the transaction object with the new Firestore ID
               transaction.setId(documentReference.getId());
               listener.onSuccess("Giao dịch đã được lưu với ID: " + documentReference.getId());
           })
           .addOnFailureListener(e -> {
               listener.onFailure(e);
           });
    }
    
    /**
     * Lưu một mục tiêu tiết kiệm vào Firestore.
     */
    public void saveGoal(Goal goal, OnCompleteListener listener) {
        CollectionReference ref = getUserCollectionRef(GOALS_COLLECTION);
        if (ref == null) {
            listener.onFailure(new Exception("User not logged in."));
            return;
        }

        ref.add(goal)
           .addOnSuccessListener(documentReference -> {
               goal.setId(documentReference.getId());
               listener.onSuccess("Mục tiêu đã được lưu với ID: " + documentReference.getId());
           })
           .addOnFailureListener(e -> {
               listener.onFailure(e);
           });
    }


    // Interface for callbacks
    public interface OnCompleteListener {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}