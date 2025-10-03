package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.dto.RecipeItemView;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.VH> {

    public interface Listener { void onDelete(int rowId); }

    private final List<RecipeItemView> data = new ArrayList<>();
    private final Listener listener;
    private final boolean readOnly;               // 👈 NEW

    public RecipeAdapter(Listener l, boolean readOnly) {
        this.listener = l;
        this.readOnly = readOnly;                 // 👈 NEW
    }

    public void submit(List<RecipeItemView> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        RecipeItemView r = data.get(pos);
        h.tvIngredientName.setText(r.ingredientName + " (" + r.unit + ")");
        h.tvQty.setText(String.valueOf(r.quantity));

        // 👇 скрываем кнопку удаления в readOnly-режиме
        if (readOnly) {
            h.btnDelete.setVisibility(View.GONE);
            h.btnDelete.setOnClickListener(null);
        } else {
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(r.id);
            });
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIngredientName, tvQty;
        Button btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvIngredientName = v.findViewById(R.id.tvIngredientName);
            tvQty           = v.findViewById(R.id.tvQty);
            btnDelete       = v.findViewById(R.id.btnDelete);
        }
    }
}
