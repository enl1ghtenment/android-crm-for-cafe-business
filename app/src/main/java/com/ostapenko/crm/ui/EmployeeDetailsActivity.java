package com.ostapenko.crm.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.db.dao.UserDao;
import com.ostapenko.crm.dto.SaleWithUser;
import com.ostapenko.crm.entity.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EmployeeDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "id";

    public static void start(Context ctx, int userId) {
        Intent i = new Intent(ctx, EmployeeDetailsActivity.class);
        i.putExtra(EXTRA_ID, userId);
        ctx.startActivity(i);
    }

    private TextView tvName, tvLogin, tvRole, tvActive;
    private androidx.recyclerview.widget.RecyclerView rv;
    private SaleShortAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_details);

        int userId = getIntent().getIntExtra(EXTRA_ID, -1);
        if (userId <= 0) { finish(); return; }

        tvName = findViewById(R.id.tvName);
        tvLogin = findViewById(R.id.tvLogin);
        tvRole  = findViewById(R.id.tvRole);
        tvActive= findViewById(R.id.tvActive);

        rv = findViewById(R.id.rvSalesByUser);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SaleShortAdapter();
        rv.setAdapter(adapter);

        var db = AppDatabase.getInstance(getApplicationContext());
        var userDao = db.userDao();
        var saleDao = db.saleDao();

        Executors.newSingleThreadExecutor().execute(() -> {
            User u = userDao.findById(userId);
            if (u == null) {
                runOnUiThread(this::finish);
                return;
            }
            List<SaleWithUser> sales = saleDao.findByUser(userId);
            runOnUiThread(() -> {
                tvName.setText((u.firstName == null ? "" : u.firstName) +
                        (u.lastName == null ? "" : (" " + u.lastName)));
                tvLogin.setText("Логин: " + u.login);
                tvRole.setText("Роль: " + u.role);
                tvActive.setText(u.active ? "Активен" : "Заблокирован");
                adapter.submit(sales);
            });
        });
    }

    static class SaleShortAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<VH> {
        private final java.util.ArrayList<SaleWithUser> data = new java.util.ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        void submit(List<SaleWithUser> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int vt) {
            android.view.View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_sale, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            SaleWithUser s = data.get(pos);
            ((TextView) h.itemView.findViewById(R.id.tvDate)).setText(fmt.format(s.saleDate));
            ((TextView) h.itemView.findViewById(R.id.tvTotal)).setText("₴" + s.total);
        }
        @Override public int getItemCount(){ return data.size(); }
    }
    static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        VH(android.view.View v){ super(v); }
    }
}
