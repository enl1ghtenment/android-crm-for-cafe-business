package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ostapenko.crm.R;
import com.ostapenko.crm.dto.SaleRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.VH> {

    public interface Listener {
        void onSaleClick(int saleId, java.util.Date saleDate);
    }

    private final List<SaleRow> data = new ArrayList<>();
    private final List<SaleRow> all = new ArrayList<>();
    private final SimpleDateFormat df =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private final Listener listener;

    public SalesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<SaleRow> items) {
        all.clear();
        data.clear();
        if (items != null) {
            all.addAll(items);
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sale, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SaleRow r = data.get(position);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaleClick(r.saleId, r.saleDate);
            }
        });
        Date sd = r.saleDate;
        String dateText = (sd == null) ? "-" : df.format(sd);
        h.tvDate.setText(dateText);
        h.tvTotal.setText("₴" + trim(r.subtotal));
        String productName = (r.productName == null ? "" : r.productName);
        h.tvProduct.setText(productName + " × " + r.quantity);

        String seller;
        if (r.firstName != null && !r.firstName.isEmpty()) {
            seller = r.firstName + (r.lastName == null ? "" : " " + r.lastName);
        } else if (r.login != null && !r.login.isEmpty()) {
            seller = r.login;
        } else {
            seller = (r.sellerId == null ? "-" : String.valueOf(r.sellerId));
        }
        h.tvSeller.setText("Продавец: " + seller);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvTotal;
        final TextView tvProduct;
        final TextView tvSeller;
        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvTotal = v.findViewById(R.id.tvTotal);
            tvProduct = v.findViewById(R.id.tvProduct);
            tvSeller = v.findViewById(R.id.tvSeller);
        }
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0")
                ? s.substring(0, s.length() - 2)
                : s;
    }

    public void filter(String q) {
        data.clear();
        if (q == null || q.trim().isEmpty()) {
            data.addAll(all);
        } else {
            String s = q.toLowerCase(Locale.getDefault());
            for (SaleRow r : all) {

                String dateStr = (r.saleDate == null)
                        ? "-"
                        : df.format(r.saleDate).toLowerCase(Locale.getDefault());

                String lineSumStr = String.valueOf(r.subtotal)
                        .toLowerCase(Locale.getDefault());

                String product = (r.productName == null ? "" : r.productName)
                        .toLowerCase(Locale.getDefault());

                String sellerFull =
                        ((r.firstName == null ? "" : r.firstName) + " " +
                                (r.lastName == null ? "" : r.lastName))
                                .trim()
                                .toLowerCase(Locale.getDefault());

                String loginStr = (r.login == null ? "" : r.login)
                        .toLowerCase(Locale.getDefault());

                String saleTotalStr = String.valueOf(r.saleTotal)
                        .toLowerCase(Locale.getDefault());

                if (dateStr.contains(s)
                        || lineSumStr.contains(s)
                        || saleTotalStr.contains(s)
                        || product.contains(s)
                        || sellerFull.contains(s)
                        || loginStr.contains(s)) {
                    data.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }
}
