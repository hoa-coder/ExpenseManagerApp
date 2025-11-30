package com.example.expensemanagerapp;

/**
 * Class mô hình dữ liệu cho chi tiết giao dịch/danh mục dưới biểu đồ.
 */
public class AnalysisDetail {
    private String category;
    private float percentage;
    private String amount;
    private int iconResId; // Resource ID cho icon

    public AnalysisDetail(String category, float percentage, String amount, int iconResId) {
        this.category = category;
        this.percentage = percentage;
        this.amount = amount;
        this.iconResId = iconResId;
    }

    public String getCategory() {
        return category;
    }

    public float getPercentage() {
        return percentage;
    }

    public String getAmount() {
        return amount;
    }

    public int getIconResId() {
        return iconResId;
    }
}