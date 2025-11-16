// com/ostapenko/crm/ui/OrdersActivity.java
package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.ProductDao;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.dto.OrderWithItems;
import com.ostapenko.crm.entity.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdersActivity extends AppCompatActivity
        implements OrdersAdapter.Listener,
        OrderCategoryAdapter.Listener,
        OrderProductAdapter.Listener {

    private SaleDao saleDao;
    private ProductDao productDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // История
    private OrdersAdapter ordersAdapter;
    private androidx.recyclerview.widget.RecyclerView rvOrders;
    private String currentStatus = "NEW";

    // Конструктор
    private OrderCategoryAdapter categoryAdapter;
    private OrderProductAdapter productAdapter;
    private TextView tvCurrentOrder;

    // currentOrder: productId -> qty
    private final LinkedHashMap<Integer, Integer> currentOrder = new LinkedHashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        MaterialToolbar tb = findViewById(R.id.toolbarOrders);
        setSupportActionBar(tb);

        var db = AppDatabase.getInstance(getApplicationContext());
        saleDao = db.saleDao();
        productDao = db.productDao();

        // --- табы Новый заказ / История ---
        MaterialButtonToggleGroup groupTabs = findViewById(R.id.groupOrdersTabs);
        View layoutOrderBuilder = findViewById(R.id.layoutOrderBuilder);
        View layoutHistory      = findViewById(R.id.layoutHistory);

        groupTabs.check(R.id.btnTabMakeOrder); // по умолчанию — конструктор

        groupTabs.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnTabMakeOrder) {
                layoutOrderBuilder.setVisibility(View.VISIBLE);
                layoutHistory.setVisibility(View.GONE);
            } else if (checkedId == R.id.btnTabHistory) {
                layoutOrderBuilder.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.VISIBLE);
                loadOrders(); // на всякий
            }
        });

        // --- Конструктор: категории ---
        androidx.recyclerview.widget.RecyclerView rvCats = findViewById(R.id.rvOrderCategories);
        rvCats.setLayoutManager(new GridLayoutManager(this, 2));
        categoryAdapter = new OrderCategoryAdapter(this);
        rvCats.setAdapter(categoryAdapter);

        // --- Конструктор: товары ---
        androidx.recyclerview.widget.RecyclerView rvProds = findViewById(R.id.rvOrderProducts);
        rvProds.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new OrderProductAdapter(this);
        rvProds.setAdapter(productAdapter);

        tvCurrentOrder = findViewById(R.id.tvCurrentOrder);
        tvCurrentOrder.setOnClickListener(v ->
                Toast.makeText(this, "Редактирование заказа сделаем следующим шагом", Toast.LENGTH_SHORT).show()
        );

        // загружаем категории
        loadCategories();

        // --- История: список заказов, как было ---
        rvOrders = findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter(this);
        rvOrders.setAdapter(ordersAdapter);

        MaterialButtonToggleGroup groupStatus = findViewById(R.id.groupStatus);
        groupStatus.check(R.id.btnActive);

        groupStatus.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnActive) {
                currentStatus = "NEW";
            } else if (checkedId == R.id.btnDone) {
                currentStatus = "DONE";
            }
            loadOrders();
        });

        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadOrders());
    }

    // ============ КОНСТРУКТОР: категории/товары =============

    private void loadCategories() {
        io.execute(() -> {
            try {
                List<String> cats = productDao.findDistinctCategories();
                runOnUiThread(() -> {
                    categoryAdapter.submit(cats);
                    if (cats != null && !cats.isEmpty()) {
                        categoryAdapter.setSelected(cats.get(0));
                        loadProductsForCategory(cats.get(0));
                    } else {
                        productAdapter.submit(new ArrayList<>());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Ошибка категорий: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void loadProductsForCategory(String category) {
        io.execute(() -> {
            try {
                List<Product> prods = productDao.findByCategory(category);
                runOnUiThread(() -> productAdapter.submit(prods));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Ошибка товаров: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    // из OrderCategoryAdapter.Listener
    @Override
    public void onCategoryClick(String category) {
        loadProductsForCategory(category);
    }

    // из OrderProductAdapter.Listener
    @Override
    public void onProductClick(Product p) {
        int cur = currentOrder.containsKey(p.id) ? currentOrder.get(p.id) : 0;
        currentOrder.put(p.id, cur + 1);
        updateCurrentOrderLabel();
        Toast.makeText(this, p.name + " добавлен в заказ", Toast.LENGTH_SHORT).show();
    }

    private void updateCurrentOrderLabel() {
        if (currentOrder.isEmpty()) {
            tvCurrentOrder.setText("Заказ пуст");
            return;
        }
        int lines = currentOrder.size();
        int totalQty = 0;
        for (int q : currentOrder.values()) totalQty += q;

        tvCurrentOrder.setText(String.format(
                Locale.getDefault(),
                "Текущий заказ: позиций %d, всего %d шт. (нажми, чтобы изменить)",
                lines, totalQty
        ));
    }

    // ============ ИСТОРИЯ: как было =============

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

        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText(title);

        new AlertDialog.Builder(this, R.style.ThemeOverlay_CRM_Dialog)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }
}
