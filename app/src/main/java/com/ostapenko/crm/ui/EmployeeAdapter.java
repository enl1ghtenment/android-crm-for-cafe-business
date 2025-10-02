package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.User;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.VH> {

    public interface Listener { void onClick(User u); }

    private final List<User> all = new ArrayList<>();
    private final List<User> data = new ArrayList<>();
    private final Listener listener;

    public EmployeeAdapter(Listener l) { this.listener = l; }

    public void submit(List<User> items) {
        all.clear(); data.clear();
        if (items != null) { all.addAll(items); data.addAll(items); }
        notifyDataSetChanged();
    }

    public void filter(String q) {
        data.clear();
        if (q == null || q.trim().isEmpty()) {
            data.addAll(all);
        } else {
            String s = q.toLowerCase();
            for (User u : all) {
                String name = ((u.firstName == null ? "" : u.firstName) + " " +
                        (u.lastName == null ? "" : u.lastName)).trim().toLowerCase();
                String login = u.login == null ? "" : u.login.toLowerCase();
                if (name.contains(s) || login.contains(s)) data.add(u);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_employee, p, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = data.get(pos);
        String full = ((u.firstName == null ? "" : u.firstName) + " " + (u.lastName == null ? "" : u.lastName)).trim();
        h.tvName.setText(full.isEmpty() ? "(без имени)" : full);
        h.tvLoginRole.setText(u.login + " • " + u.role);
        h.tvActive.setText(u.active ? "Активен" : "Заблокирован");
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(u); });
    }
    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvLoginRole, tvActive;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvLoginRole = v.findViewById(R.id.tvLoginRole);
            tvActive = v.findViewById(R.id.tvActive);
        }
    }
}
