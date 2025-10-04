package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.VH> {

    public interface Listener { void onClick(Ingredient i); }

    private final List<Ingredient> all = new ArrayList<>();
    private final List<Ingredient> data = new ArrayList<>();
    private final Listener listener;
    private String currentQuery = "";

    public IngredientAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Ingredient> items) {
        all.clear();
        if (items != null) all.addAll(items);
        applyFilter(); // пере-применяем текущий запрос
    }

    public void filter(String q) {
        currentQuery = (q == null) ? "" : q.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    private void applyFilter() {
        data.clear();
        if (currentQuery.isEmpty()) {
            data.addAll(all);
        } else {
            for (Ingredient i : all) {
                String name = i.name == null ? "" : i.name.toLowerCase(Locale.getDefault());
                String unit = i.unit == null ? "" : i.unit.toLowerCase(Locale.getDefault());
                String stock = String.valueOf(i.stock).toLowerCase(Locale.getDefault());
                String price = String.valueOf(i.price).toLowerCase(Locale.getDefault());

                if (name.contains(currentQuery) ||
                        unit.contains(currentQuery) ||
                        stock.contains(currentQuery) ||
                        price.contains(currentQuery)) {
                    data.add(i);
                }
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
        h.tvName.setText(i.name == null ? "(без названия)" : i.name);
        h.tvUnitStock.setText((i.unit == null ? "" : i.unit) + " • " + trim(i.stock));
        h.tvPrice.setText("₴" + trim(i.price) + " / ед.");
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(i); });
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
