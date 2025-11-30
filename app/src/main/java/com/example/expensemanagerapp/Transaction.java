package com.example.expensemanagerapp;

import java.io.Serializable;
import java.util.Date; // Import Date

public class Transaction implements Serializable {
    private String id; // ID từ Firestore document
    private String type; // "INCOME" hoặc "EXPENSE"
    private double amount;
    private String category;
    private String note;
    private long timestamp;

    // Constructor mặc định (cần cho Firebase)
    public Transaction() {
    }

    // Constructor 5 tham số (không có id - dùng khi tạo mới transaction)
    public Transaction(String category, double amount, String type, Date date, String note) { // Thay đổi thứ tự và kiểu dữ liệu
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.timestamp = date.getTime(); // Chuyển Date sang timestamp
        this.note = note;
    }

    // ✅ THÊM MỚI: Constructor 6 tham số (có id - dùng khi lấy từ Firebase hoặc truyền qua Intent)
    public Transaction(String id, String type, double amount, String category, String note, long timestamp) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper method to get Date object from timestamp
    public Date getDate() {
        return new Date(timestamp);
    }
}