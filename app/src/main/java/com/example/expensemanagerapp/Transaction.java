package com.example.expensemanagerapp;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String type; // "INCOME" or "EXPENSE"
    private double amount;
    private String category;
    private String note;
    private long timestamp;

    // Constructors
    public Transaction() {
    }

    public Transaction(String type, double amount, String category, String note, long timestamp) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.timestamp = timestamp;
    }

    // Getters and Setters
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
}
