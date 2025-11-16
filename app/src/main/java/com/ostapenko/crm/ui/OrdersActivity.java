package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.dto.OrderWithItems;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdersActivity extends AppCompatActivity implements OrdersAdapter.Listener {

    private SaleDao saleDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private OrdersAdapter ordersAdapter;
    private androidx.recyclerview.widget.RecyclerView rvOrders;

    private String currentStatus = "NEW"; // стартуем с активных

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        MaterialToolbar tb = findViewById(R.id.toolbarOrders);
        setSupportActionBar(tb);

        saleDao = AppDatabase.getInstance(getApplicationContext()).saleDao();

        rvOrders = findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter(this);
        rvOrders.setAdapter(ordersAdapter);

        MaterialButtonToggleGroup group = findViewById(R.id.groupStatus);
        View btnActive = findViewById(R.id.btnActive);
        View btnDone   = findViewById(R.id.btnDone);

        // По умолчанию выбираем "Активные"
        group.check(R.id.btnActive);

        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnActive) {
                currentStatus = "NEW";
            } else if (checkedId == R.id.btnDone) {
                currentStatus = "DONE";
            }
            loadOrders();
        });

        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadOrders());

        // Первичная загрузка
        loadOrders();
    }

    private void loadOrders() {
        io.execute(() -> {
            try {
                List<OrderWithItems> list = saleDao.findOrdersByStatus(currentStatus);
                runOnUiThread(() -> ordersAdapter.submit(list));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Ошибка загрузки: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    @Override
    public void onShowItems(int saleId, String titleForDialog) {
        io.execute(() -> {
            try {
                List<OrderWithItems.OrderLine> lines = saleDao.findLinesForOrder(saleId);
                runOnUiThread(() -> showLinesDialog(titleForDialog, lines));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Ошибка позиций: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    @Override
    public void onMarkDone(int saleId) {
        io.execute(() -> {
            try {
                saleDao.markDone(saleId);
                // после апдейта перезагружаем список
                List<OrderWithItems> list = saleDao.findOrdersByStatus(currentStatus);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Заказ выдан", Toast.LENGTH_SHORT).show();
                    ordersAdapter.submit(list);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Не удалось обновить статус: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void showLinesDialog(String title, List<OrderWithItems.OrderLine> lines) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_order_lines, null, false);

        androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rvOrderLines);
        rv.setLayoutManager(new LinearLayoutManager(this));

        OrderLinesAdapter linesAdapter = new OrderLinesAdapter();
        rv.setAdapter(linesAdapter);
        linesAdapter.submit(lines);

        ((android.widget.TextView) dialogView.findViewById(R.id.tvDialogTitle))
                .setText(title);

        new AlertDialog.Builder(this, R.style.ThemeOverlay_CRM_Dialog) // твой диалоговый стиль
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }
}
