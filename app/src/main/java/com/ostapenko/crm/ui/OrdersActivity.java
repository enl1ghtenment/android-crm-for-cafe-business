// com/ostapenko/crm/ui/OrdersActivity.java
package com.ostapenko.crm.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.ostapenko.crm.auth.Session;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.ProductDao;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.db.dao.SaleItemDao;
import com.ostapenko.crm.dto.OrderWithItems;
import com.ostapenko.crm.entity.Product;
import com.ostapenko.crm.entity.Sale;
import com.ostapenko.crm.entity.SaleItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdersActivity extends AppCompatActivity
        implements OrdersAdapter.Listener,
        OrderCategoryAdapter.Listener,
        OrderProductAdapter.Listener {

    private SaleDao saleDao;
    private SaleItemDao saleItemDao;
    private ProductDao productDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Session session;

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

    // внутренняя модель для диалога
    private static class OrderLineUi {
        int productId;
        String name;
        int qty;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        MaterialToolbar tb = findViewById(R.id.toolbarOrders);
        setSupportActionBar(tb);

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        saleDao = db.saleDao();
        saleItemDao = db.saleItemDao();
        productDao = db.productDao();
        session = new Session(this);

        // --- табы Новый заказ / История ---
        MaterialButtonToggleGroup groupTabs = findViewById(R.id.groupOrdersTabs);
        View layoutOrderBuilder = findViewById(R.id.layoutOrderBuilder);
        View layoutHistory = findViewById(R.id.layoutHistory);

        groupTabs.check(R.id.btnTabMakeOrder); // по умолчанию — конструктор

        groupTabs.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnTabMakeOrder) {
                layoutOrderBuilder.setVisibility(View.VISIBLE);
                layoutHistory.setVisibility(View.GONE);
            } else if (checkedId == R.id.btnTabHistory) {
                layoutOrderBuilder.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.VISIBLE);
                loadOrders(); // обновить список
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
        tvCurrentOrder.setOnClickListener(v -> showCurrentOrderDialog());

        // загружаем категории
        loadCategories();

        // --- История заказов ---
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

    @Override
    public void onCategoryClick(String category) {
        loadProductsForCategory(category);
    }

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

    // ======== Диалог редактирования текущего заказа =========

    private void showCurrentOrderDialog() {
        if (currentOrder.isEmpty()) {
            Toast.makeText(this, "Заказ пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            List<OrderLineUi> lines = new LinkedList<>();
            for (Map.Entry<Integer, Integer> e : currentOrder.entrySet()) {
                int productId = e.getKey();
                int qty = e.getValue();
                Product p = productDao.findById(productId);

                OrderLineUi ui = new OrderLineUi();
                ui.productId = productId;
                ui.qty = qty;
                ui.name = (p != null && p.name != null && !p.name.isEmpty())
                        ? p.name
                        : ("Товар #" + productId);
                lines.add(ui);
            }
            runOnUiThread(() -> buildCurrentOrderDialog(lines));
        });
    }

    private void buildCurrentOrderDialog(List<OrderLineUi> lines) {
        // контекст с темой диалога
        Context dialogCtx = new ContextThemeWrapper(this, R.style.ThemeOverlay_CRM_Dialog);
        int colorOnSurface = resolveAttrColor(dialogCtx, R.attr.colorOnSurface);

        // корневой layout
        android.widget.LinearLayout root = new android.widget.LinearLayout(dialogCtx);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        // ===== Заголовок + крестик =====
        android.widget.LinearLayout header = new android.widget.LinearLayout(dialogCtx);
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvTitle = new TextView(dialogCtx);
        tvTitle.setText("Текущий заказ");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(colorOnSurface);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams lpTitle =
                new android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
        tvTitle.setLayoutParams(lpTitle);

        TextView tvCloseIcon = new TextView(dialogCtx);
        tvCloseIcon.setText("✕");
        tvCloseIcon.setTextSize(18);
        tvCloseIcon.setTextColor(colorOnSurface);
        tvCloseIcon.setPadding(dp(8), 0, 0, 0);

        header.addView(tvTitle);
        header.addView(tvCloseIcon);
        root.addView(header);

        // подсказка
        TextView tvHint = new TextView(dialogCtx);
        tvHint.setText("Плюс/минус меняют количество. «Удалить» убирает позицию из заказа.");
        tvHint.setTextSize(13);
        tvHint.setTextColor(colorOnSurface);
        tvHint.setPadding(0, dp(4), 0, dp(12));
        root.addView(tvHint);

        // контейнер строк
        android.widget.LinearLayout linesContainer = new android.widget.LinearLayout(dialogCtx);
        linesContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.addView(linesContainer);

        for (OrderLineUi line : lines) {
            final OrderLineUi ln = line;

            android.widget.LinearLayout row = new android.widget.LinearLayout(dialogCtx);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));
            row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // название
            TextView tvName = new TextView(dialogCtx);
            tvName.setTextColor(colorOnSurface);
            tvName.setTextSize(15);
            tvName.setText(ln.name);
            android.widget.LinearLayout.LayoutParams lpName =
                    new android.widget.LinearLayout.LayoutParams(
                            0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                    );
            tvName.setLayoutParams(lpName);

            // блок [-] qty [+]
            android.widget.LinearLayout qtyBlock = new android.widget.LinearLayout(dialogCtx);
            qtyBlock.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            qtyBlock.setGravity(Gravity.CENTER_VERTICAL);

            int btnSize = dp(40);

            Button btnMinus = new Button(dialogCtx);
            btnMinus.setText("−");
            android.widget.LinearLayout.LayoutParams lpMinus =
                    new android.widget.LinearLayout.LayoutParams(
                            btnSize,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            btnMinus.setLayoutParams(lpMinus);

            TextView tvQty = new TextView(dialogCtx);
            tvQty.setTextColor(colorOnSurface);
            tvQty.setTextSize(15);
            tvQty.setPadding(dp(8), 0, dp(8), 0);
            tvQty.setText(String.valueOf(ln.qty));

            Button btnPlus = new Button(dialogCtx);
            btnPlus.setText("+");
            android.widget.LinearLayout.LayoutParams lpPlus =
                    new android.widget.LinearLayout.LayoutParams(
                            btnSize,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            btnPlus.setLayoutParams(lpPlus);

            qtyBlock.addView(btnMinus);
            qtyBlock.addView(tvQty);
            qtyBlock.addView(btnPlus);

            // кнопка Удалить
            Button btnDelete = new Button(dialogCtx);
            btnDelete.setText("Удалить");

            // логика +/-/Удалить
            btnMinus.setOnClickListener(v -> {
                int q = ln.qty - 1;
                if (q <= 0) {
                    currentOrder.remove(ln.productId);
                    linesContainer.removeView(row);
                    Toast.makeText(this, "Позиция удалена", Toast.LENGTH_SHORT).show();
                } else {
                    ln.qty = q;
                    currentOrder.put(ln.productId, q);
                    tvQty.setText(String.valueOf(q));
                }
                updateCurrentOrderLabel();
            });

            btnPlus.setOnClickListener(v -> {
                int q = ln.qty + 1;
                ln.qty = q;
                currentOrder.put(ln.productId, q);
                tvQty.setText(String.valueOf(q));
                updateCurrentOrderLabel();
            });

            btnDelete.setOnClickListener(v -> {
                currentOrder.remove(ln.productId);
                linesContainer.removeView(row);
                updateCurrentOrderLabel();
                Toast.makeText(this, "Позиция удалена", Toast.LENGTH_SHORT).show();
            });

            row.addView(tvName);
            row.addView(qtyBlock);
            row.addView(btnDelete);

            linesContainer.addView(row);
        }

        // разделитель
        View divider = new View(dialogCtx);
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                dp(12)
        ));
        root.addView(divider);

        // ===== нижние кнопки: Создать / Очистить =====
        android.widget.LinearLayout bottom = new android.widget.LinearLayout(dialogCtx);
        bottom.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView btnCreate = new TextView(dialogCtx);
        btnCreate.setText("Создать заказ");
        styleOutlinedAction(btnCreate, colorOnSurface);

        TextView btnClear = new TextView(dialogCtx);
        btnClear.setText("Очистить все");
        styleOutlinedAction(btnClear, colorOnSurface);

        android.widget.LinearLayout.LayoutParams lpActionLeft =
                new android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
        lpActionLeft.setMargins(0, 0, dp(8), 0);
        btnCreate.setLayoutParams(lpActionLeft);

        android.widget.LinearLayout.LayoutParams lpActionRight =
                new android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
        lpActionRight.setMargins(dp(8), 0, 0, 0);
        btnClear.setLayoutParams(lpActionRight);

        bottom.addView(btnCreate);
        bottom.addView(btnClear);
        root.addView(bottom);

        AlertDialog dialog = new AlertDialog.Builder(dialogCtx, R.style.ThemeOverlay_CRM_Dialog)
                .setView(root)
                .create();

        // крестик
        tvCloseIcon.setOnClickListener(v -> dialog.dismiss());

        // Создать заказ → пишем в БД
        btnCreate.setOnClickListener(v -> {
            if (currentOrder.isEmpty()) {
                Toast.makeText(this, "Заказ пуст", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            // снапшот на момент клика, чтобы не ловить гонки
            Map<Integer, Integer> snapshot = new LinkedHashMap<>(currentOrder);

            io.execute(() -> {
                try {
                    createOrderInDb(snapshot);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Заказ создан", Toast.LENGTH_SHORT).show();
                        currentOrder.clear();
                        updateCurrentOrderLabel();
                        dialog.dismiss();

                        // если открыт список активных — обновим
                        if ("NEW".equals(currentStatus)) {
                            loadOrders();
                        }
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> Toast.makeText(
                            this,
                            "Ошибка создания заказа: " + ex.getMessage(),
                            Toast.LENGTH_LONG
                    ).show());
                }
            });
        });

        btnClear.setOnClickListener(v -> {
            currentOrder.clear();
            updateCurrentOrderLabel();
            dialog.dismiss();
            Toast.makeText(this, "Заказ очищен", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /**
     * Фактическая запись заказа в БД:
     *  - создаём Sale со статусом NEW;
     *  - для него создаём SaleItem по каждой позиции.
     */
    private void createOrderInDb(Map<Integer, Integer> items) {
        if (items.isEmpty()) return;

        Sale sale = new Sale();
        sale.status = "NEW";                    // поле точно есть (миграция 6_7)
        try {
            // если в Sale есть sellerId (Integer / int) — проставится
            sale.sellerId = session.userId();
        } catch (Throwable ignored) {
            // если поля нет — просто проигнорируется, поправишь под свою модель
        }

        long saleIdLong = saleDao.insert(sale);   // стандартный @Insert
        int saleId = (int) saleIdLong;

        for (Map.Entry<Integer, Integer> e : items.entrySet()) {
            int productId = e.getKey();
            int qty = e.getValue();

            if (qty <= 0) continue;

            SaleItem item = new SaleItem();
            item.saleId = saleId;        // подгони тип, если long
            item.productId = productId;
            item.quantity = qty;              // если поле называется иначе — поправь

            // цену/сумму можно не ставить (0) — это чисто "кухонный" заказ
            saleItemDao.insert(item);
        }
    }

    // ============ ИСТОРИЯ =============

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

    // ==== util ====
    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private int resolveAttrColor(Context ctx, int attr) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    /** Обрисовка нижних TextView как кнопок с обводкой и скруглениями */
    private void styleOutlinedAction(TextView tv, int strokeColor) {
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(android.graphics.Color.TRANSPARENT);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), strokeColor);

        tv.setBackground(bg);
        tv.setTextColor(strokeColor);
    }
}
