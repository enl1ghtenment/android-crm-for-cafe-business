package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ostapenko.crm.R;
import com.ostapenko.crm.dto.OrderWithItems;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    public interface Listener {
        void onShowItems(int saleId, String titleForDialog);
        void onMarkDone(int saleId);
    }

    private final Listener listener;
    private final List<OrderWithItems> data = new ArrayList<>();
    private final SimpleDateFormat df =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public OrdersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<OrderWithItems> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        OrderWithItems o = data.get(pos);

        // дата
        Date d = o.saleDate;
        h.tvOrderDate.setText(d == null ? "-" : df.format(d));

        // сумма
        h.tvOrderTotal.setText("₴" + trim(o.total));

        // продавец
        String seller;
        if (o.firstName != null && !o.firstName.isEmpty()) {
            seller = o.firstName + (o.lastName == null ? "" : " " + o.lastName);
        } else if (o.login != null && !o.login.isEmpty()) {
            seller = o.login;
        } else {
            seller = (o.sellerId == null ? "-" : String.valueOf(o.sellerId));
        }
        h.tvOrderSeller.setText("Продавец: " + seller);

        // статус
        h.tvOrderStatus.setText("Статус: " + o.status);

        // кнопка "Позиции"
        h.btnShowItems.setOnClickListener(v -> {
            String title = "Чек #" + o.saleId + " (" +
                    (d == null ? "-" : df.format(d)) + ")";
            listener.onShowItems(o.saleId, title);
        });

        // кнопка "Выдано"
        if ("DONE".equalsIgnoreCase(o.status)) {
            h.btnMarkDone.setVisibility(View.GONE);
        } else {
            h.btnMarkDone.setVisibility(View.VISIBLE);
            h.btnMarkDone.setOnClickListener(v -> listener.onMarkDone(o.saleId));
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvOrderDate;
        final TextView tvOrderTotal;
        final TextView tvOrderSeller;
        final TextView tvOrderStatus;
        final MaterialButton btnShowItems;
        final MaterialButton btnMarkDone;

        VH(@NonNull View v) {
            super(v);
            tvOrderDate   = v.findViewById(R.id.tvOrderDate);
            tvOrderTotal  = v.findViewById(R.id.tvOrderTotal);
            tvOrderSeller = v.findViewById(R.id.tvOrderSeller);
            tvOrderStatus = v.findViewById(R.id.tvOrderStatus);
            btnShowItems  = v.findViewById(R.id.btnShowItems);
            btnMarkDone   = v.findViewById(R.id.btnMarkDone);
        }
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
    }
}
