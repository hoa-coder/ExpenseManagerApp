package com.example.expensemanagerapp;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonHelper {
    private static final String TAG = "JsonHelper";
    private static final String FILE_NAME = "transactions.json";

    private static File getJsonFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static void addTransaction(Context context, Transaction transaction) {
        List<Transaction> transactions = loadTransactions(context);
        transactions.add(0, transaction);

        try (FileWriter writer = new FileWriter(getJsonFile(context))) {
            Gson gson = new Gson();
            gson.toJson(transactions, writer);
            Log.d(TAG, "Transaction added and saved successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving transaction to JSON file", e);
        }
    }

    public static List<Transaction> loadTransactions(Context context) {
        File file = getJsonFile(context);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Transaction>>(){}.getType();
            List<Transaction> transactions = gson.fromJson(reader, listType);
            
            if (transactions == null) {
                return new ArrayList<>();
            }
            return transactions;
        } catch (IOException e) {
            Log.e(TAG, "Error loading transactions from JSON file", e);
            return new ArrayList<>();
        }
    }
}