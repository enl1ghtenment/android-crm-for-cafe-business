package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.Ingredient;
import com.ostapenko.crm.entity.Product;

import java.util.ArrayList;
import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.VH> {

    public interface Listener { void onClick(Ingredient i); }

    private final List<Ingredient> data = new ArrayList<>();
    private final List<Ingredient> all = new ArrayList<>();

    private final Listener listener;

    public IngredientAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Ingredient> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    public void filter(String q) {
        data.clear();
        if (q == null || q.trim().isEmpty()) {
            data.addAll(all);
        } else {
            String s = q.toLowerCase();
            for (Ingredient i : all) {
                String name = i.name == null ? "" : i.name.toLowerCase();
                String unit = i.unit == null ? "" : i.unit.toLowerCase();
                if (name.contains(s) || unit.contains(s)) data.add(i);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Ingredient i = data.get(pos);
        h.tvName.setText(i.name);
        h.tvUnitStock.setText(i.unit + " • " + trim(i.stock));
        h.tvPrice.setText("₴" + trim(i.price) + " / ед.");
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(i);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUnitStock, tvPrice;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvUnitStock = v.findViewById(R.id.tvUnitStock);
            tvPrice = v.findViewById(R.id.tvPrice);
        }
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length()-2);
        return s;
    }
}

