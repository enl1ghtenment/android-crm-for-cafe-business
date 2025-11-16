package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ostapenko.crm.R;
import com.ostapenko.crm.dto.OrderWithItems;

import java.util.ArrayList;
import java.util.List;

public class OrderLinesAdapter extends RecyclerView.Adapter<OrderLinesAdapter.VH> {

    private final List<OrderWithItems.OrderLine> data = new ArrayList<>();

    public void submit(List<OrderWithItems.OrderLine> lines) {
        data.clear();
        if (lines != null) data.addAll(lines);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        OrderWithItems.OrderLine line = data.get(pos);
        String name = (line.productName == null ? "" : line.productName);

        h.tvProduct.setText(name);
        h.tvQtySum.setText(
                "Кол-во: " + line.quantity +
                        " | Сумма: ₴" + trim(line.subtotal)
        );
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvProduct;
        final TextView tvQtySum;
        VH(@NonNull View v) {
            super(v);
            tvProduct = v.findViewById(R.id.tvLineProduct);
            tvQtySum = v.findViewById(R.id.tvLineQtySum);
        }
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
    }
}
