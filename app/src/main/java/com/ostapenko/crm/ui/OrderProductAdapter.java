// com/ostapenko/crm/ui/OrderProductAdapter.java
package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.Product;

import java.util.ArrayList;
import java.util.List;

public class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.VH> {

    public interface Listener {
        void onProductClick(Product p);
    }

    private final Listener listener;
    private final List<Product> data = new ArrayList<>();
    private TextView tvPrice;

    public OrderProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Product> products) {
        data.clear();
        if (products != null) data.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);
        h.tvName.setText(p.name);

        if (p.price > 0)
            h.tvPrice.setText("₴" + trim(p.price));
        else
            h.tvPrice.setText("₴ —");

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });
    }
    private static String trim(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvPrice;   // 🆕
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvProductName);
            tvPrice = v.findViewById(R.id.tvProductPrice);   // 🆕
        }
    }
}
