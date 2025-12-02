package com.example.expensemanagerapp.domain.model;

import java.io.Serializable;

/**
 * Model class cho Wallet (Ví)
 * ✅ QUAN TRỌNG: Phải implements Serializable để truyền qua Intent
 */
public class Wallet implements Serializable {
    private String id;           // ID ví
    private String name;         // Tên ví
    private String type;         // Loại ví (Tiền mặt, Thẻ tín dụng...)
    private double balance;      // Số dư hiện tại
    private boolean isActive;    // Trạng thái kích hoạt
    private long timestamp;      // Thời gian tạo

    // Constructor rỗng (cần cho Firebase)
    public Wallet() {
    }

    // Constructor đầy đủ
    public Wallet(String id, String name, String type, double balance, boolean isActive, long timestamp) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.balance = balance;
        this.isActive = isActive;
        this.timestamp = timestamp;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}