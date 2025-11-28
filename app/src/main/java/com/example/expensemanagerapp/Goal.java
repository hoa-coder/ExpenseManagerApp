package com.example.expensemanagerapp;

import java.io.Serializable;

/**
 * Model class for a user's savings goal.
 * Compatible with Firebase Firestore.
 */
public class Goal implements Serializable { // Đã thêm implements Serializable
    private String id;
    private String name;
    private double targetAmount;
    private double currentAmount;
    private String startDate; // Stored as "dd/MM/yyyy"
    private String endDate;   // Stored as "dd/MM/yyyy"
    private String icon;
    private String note;
    private long timestamp; // Creation timestamp

    // Required empty constructor for Firestore
    public Goal() {
    }

    public Goal(String id, String name, double targetAmount, double currentAmount, String startDate, String endDate, String icon, String note, long timestamp) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.icon = icon;
        this.note = note;
        this.timestamp = timestamp;
    }

    // Getters and Setters (Required for Firestore serialization)

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

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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