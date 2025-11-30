package com.example.expensemanagerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public void updateData(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false); // Cần tạo layout item_transaction.xml
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvCategory.setText(transaction.getCategory());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
        holder.tvType.setText(transaction.getType());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(transaction.getDate())); // Sử dụng getDate() mới
        holder.tvNote.setText(transaction.getNote());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvType, tvDate, tvNote;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_transaction_category);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvType = itemView.findViewById(R.id.tv_transaction_type);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvNote = itemView.findViewById(R.id.tv_transaction_note);
        }
    }
}
