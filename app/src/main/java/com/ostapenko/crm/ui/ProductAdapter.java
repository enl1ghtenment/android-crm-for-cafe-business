package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface Listener {
        void onEditRecipe(Product p);
        void onSell(Product p);
        void onBindForServings(Product p, TextView tvServings);
        void onCost(Product p);
        void onDelete(Product p);
    }

    private final List<Product> data = new ArrayList<>();
    private final Listener listener;
    private final boolean canDelete;

    private final List<Product> all = new ArrayList<>();

    public ProductAdapter(Listener l, boolean canDelete) {
        this.listener = l;
        this.canDelete = canDelete;
    }

    public void submit(List<Product> items) {
        all.clear(); data.clear();
        if (items != null) { all.addAll(items); data.addAll(items); }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Product p = data.get(pos);
        h.tvName.setText(p.name);
        h.tvDesc.setText(p.description == null ? "" : p.description);
        h.tvServings.setText("Считаю…");
        if (listener != null) listener.onBindForServings(p, h.tvServings);

        h.btnEditRecipe.setOnClickListener(v -> { if (listener != null) listener.onEditRecipe(p); });
        h.btnCost.setOnClickListener(v -> { if (listener != null) listener.onCost(p); });
        h.btnSell.setOnClickListener(v -> { if (listener != null) listener.onSell(p); });

        if (canDelete) {
            h.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onDelete(p);
                return true;
            });
        } else {
            h.itemView.setOnLongClickListener(null);
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvServings;
        Button btnEditRecipe, btnCost, btnSell;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvDesc = v.findViewById(R.id.tvDesc);
            tvServings = v.findViewById(R.id.tvServings);
            btnEditRecipe = v.findViewById(R.id.btnEditRecipe);
            btnCost = v.findViewById(R.id.btnCost);
            btnSell = v.findViewById(R.id.btnSell);
        }
    }

    public void filter(String q) {
        data.clear();
        if (q == null || q.trim().isEmpty()) data.addAll(all);
        else {
            String s = q.toLowerCase();
            for (Product p : all) {
                String n = p.name == null ? "" : p.name.toLowerCase();
                String d = p.description == null ? "" : p.description.toLowerCase();
                if (n.contains(s) || d.contains(s)) data.add(p);
            }
        }
        notifyDataSetChanged();
    }

}
