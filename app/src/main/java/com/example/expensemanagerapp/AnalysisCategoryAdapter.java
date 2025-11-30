package com.example.expensemanagerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Adapter hiển thị danh sách phân tích theo category
 */
public class AnalysisCategoryAdapter extends RecyclerView.Adapter<AnalysisCategoryAdapter.ViewHolder> {

    private List<AnalysisActivity.CategoryAnalysis> categoryList;
    private DecimalFormat currencyFormatter = new DecimalFormat("#,### đ");

    public AnalysisCategoryAdapter(List<AnalysisActivity.CategoryAnalysis> categoryList) {
        this.categoryList = categoryList;
    }

    public void updateData(List<AnalysisActivity.CategoryAnalysis> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_analysis, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnalysisActivity.CategoryAnalysis item = categoryList.get(position);

        holder.tvCategory.setText(item.getCategory());
        holder.tvAmount.setText(currencyFormatter.format(item.getAmount()));
        holder.tvPercentage.setText(String.format("%.1f%%", item.getPercentage()));
        holder.progressBar.setProgress((int) item.getPercentage());
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvAmount;
        TextView tvPercentage;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}