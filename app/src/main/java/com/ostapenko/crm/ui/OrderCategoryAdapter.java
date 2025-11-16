// com/ostapenko/crm/ui/OrderCategoryAdapter.java
package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ostapenko.crm.R;

import java.util.ArrayList;
import java.util.List;

public class OrderCategoryAdapter extends RecyclerView.Adapter<OrderCategoryAdapter.VH> {

    public interface Listener {
        void onCategoryClick(String category);
    }

    private final Listener listener;
    private final List<String> data = new ArrayList<>();
    private String selected = null;

    public OrderCategoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<String> categories) {
        data.clear();
        if (categories != null) data.addAll(categories);
        notifyDataSetChanged();
    }

    public void setSelected(String category) {
        selected = category;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String cat = data.get(position);
        h.tvName.setText(cat);

        boolean isSel = cat.equals(selected);
        h.itemView.setAlpha(isSel ? 1f : 0.7f);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(cat);
            setSelected(cat);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvCategoryName);
        }
    }
}
