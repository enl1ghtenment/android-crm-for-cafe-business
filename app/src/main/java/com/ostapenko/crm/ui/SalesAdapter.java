package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.Sale;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.VH> {
    private final List<Sale> data = new ArrayList<>();
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private final List<Sale> all = new ArrayList<>();


    public void submit(List<Sale> items) {
        all.clear(); data.clear();
        if (items != null) { all.addAll(items); data.addAll(items); }
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Sale s = data.get(position);
        h.tvDate.setText(df.format(s.saleDate));
        h.tvTotal.setText("₴" + trim(s.total));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTotal;
        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvTotal = v.findViewById(R.id.tvTotal);
        }
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length()-2);
        return s;
    }

    public void filter(String q) {
        data.clear();
        if (q==null || q.trim().isEmpty()) data.addAll(all);
        else {
            String s = q.toLowerCase();
            for (Sale sale : all) {
                String dateStr = df.format(sale.saleDate).toLowerCase();
                String totalStr = String.valueOf(sale.total);
                if (dateStr.contains(s) || totalStr.contains(s)) data.add(sale);
            }
        }
        notifyDataSetChanged();
    }

}
