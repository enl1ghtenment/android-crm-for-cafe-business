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
        void onBindForServings(Product p, TextView tvServings); // чтобы подсчитать на фоне
    }

    private final List<Product> data = new ArrayList<>();
    private final Listener listener;

    public ProductAdapter(Listener l) { this.listener = l; }

    public void submit(List<Product> items) {
        data.clear();
        if (items != null) data.addAll(items);
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

        h.btnEditRecipe.setOnClickListener(v -> {
            if (listener != null) listener.onEditRecipe(p);
        });
        h.btnSell.setOnClickListener(v -> {
            if (listener != null) listener.onSell(p);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvServings;
        Button btnEditRecipe, btnSell;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvDesc = v.findViewById(R.id.tvDesc);
            tvServings = v.findViewById(R.id.tvServings);
            btnEditRecipe = v.findViewById(R.id.btnEditRecipe);
            btnSell = v.findViewById(R.id.btnSell);
        }
    }
}
