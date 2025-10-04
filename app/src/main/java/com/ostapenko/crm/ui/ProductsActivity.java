package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.ProductDao;
import com.ostapenko.crm.db.dao.ProductIngredientDao;
import com.ostapenko.crm.domain.SalesService;
import com.ostapenko.crm.entity.Product;
import java.util.List;
import java.util.concurrent.ExecutorService;
import com.ostapenko.crm.auth.Session;
import java.util.concurrent.Executors;
import com.ostapenko.crm.db.dao.SaleItemDao;

public class ProductsActivity extends AppCompatActivity implements ProductAdapter.Listener {

    private ProductAdapter adapter;
    private ProductDao productDao;
    private ProductIngredientDao recipeDao;
    private SalesService salesService;
    private SaleItemDao saleItemDao;
    private boolean isAdmin;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Session session;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        session = new Session(this);
        isAdmin = "admin".equalsIgnoreCase(session.role());

        EditText etSearch = findViewById(R.id.etSearchProducts);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        productDao = db.productDao();
        recipeDao = db.productIngredientDao();
        saleItemDao = db.saleItemDao();
        salesService = new SalesService(db);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(this, isAdmin);
        rv.setAdapter(adapter);

        findViewById(R.id.fabAddProduct).setOnClickListener(v -> showAddProductDialog());
        loadData();
    }

    private void loadData() {
        io.execute(() -> {
            List<Product> all = productDao.findAll();
            runOnUiThread(() -> adapter.submit(all));
        });
    }

    private void showAddProductDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null, false);
        EditText etName = view.findViewById(R.id.etName);
        EditText etDesc = view.findViewById(R.id.etDesc);

        new AlertDialog.Builder(this)
                .setTitle("Добавить товар")
                .setView(view)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Название пустое", Toast.LENGTH_SHORT).show(); return; }
                    Product p = new Product();
                    p.name = name; p.description = desc;
                    io.execute(() -> {
                        productDao.insert(p);
                        loadData();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ==== ProductAdapter.Listener ====
    @Override public void onEditRecipe(Product p) {
        RecipeActivity.start(this, p.id);
    }

    @Override public void onSell(Product p) {
        showSellDialog(p);
    }

    @Override public void onBindForServings(Product p, TextView tvServings) {
        io.execute(() -> {
            Double m = recipeDao.getMaxServings(p.id);
            int max = (int)Math.floor(m == null ? 0 : m);
            runOnUiThread(() -> tvServings.setText("Можно приготовить: " + max));
        });
    }

    private void showSellDialog(Product p) {
        View view = getLayoutInflater().inflate(R.layout.dialog_sell, null, false);
        EditText etQty = view.findViewById(R.id.etQty);
        EditText etPrice = view.findViewById(R.id.etPrice);

        new AlertDialog.Builder(this)
                .setTitle("Продать: " + p.name)
                .setView(view)
                .setPositiveButton("OK", (d, w) -> {
                    int parsedQty;
                    double parsedPrice;
                    try { parsedQty = Integer.parseInt(etQty.getText().toString().trim()); } catch (Exception e) { parsedQty = 0; }
                    try { parsedPrice = Double.parseDouble(etPrice.getText().toString().trim()); } catch (Exception e) { parsedPrice = 0; }

                    if (parsedQty <= 0 || parsedPrice <= 0) {
                        Toast.makeText(this, "Неверные значения", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int qtyVal = parsedQty;
                    final double priceVal = parsedPrice;
                    final double subtotalVal = qtyVal * priceVal;

                    io.execute(() -> {
                        try {
                            salesService.sell(p.id, qtyVal, subtotalVal, subtotalVal, session.userId());

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Продано: " + qtyVal + " × " + p.name, Toast.LENGTH_SHORT).show();
                                loadData();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onCost(Product p) {
        View view = getLayoutInflater().inflate(R.layout.dialog_estimate_qty, null, false);
        EditText etQty = view.findViewById(R.id.etQty);

        new AlertDialog.Builder(this)
                .setTitle("Себестоимость: " + p.name)
                .setView(view)
                .setPositiveButton("Рассчитать", (d, w) -> {
                    int qty;
                    try { qty = Integer.parseInt(etQty.getText().toString().trim()); } catch (Exception e) { qty = 0; }
                    if (qty <= 0) {
                        Toast.makeText(this, "Укажи количество > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final int qtyVal = qty;
                    io.execute(() -> {
                        try {
                            com.ostapenko.crm.dto.CostEstimate est = salesService.estimateCost(p.id, qtyVal);
                            runOnUiThread(() -> showCostResultDialog(p.name, est));
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showCostResultDialog(String productName, com.ostapenko.crm.dto.CostEstimate est) {
        StringBuilder sb = new StringBuilder();
        sb.append("Товар: ").append(productName).append("\n");
        sb.append("Количество: ").append(est.qty).append(" шт.\n\n");
        sb.append("Ингредиенты:\n");
        for (com.ostapenko.crm.dto.CostEstimate.CostLine line : est.lines) {
            sb.append("• ")
                    .append(line.ingredientName).append(" (").append(line.unit).append("): ")
                    .append(trim(line.quantityForOrder)).append(" × ₴").append(trim(line.pricePerUnit))
                    .append(" = ₴").append(trim(line.lineCost)).append("\n");
        }
        sb.append("\nИтого себестоимость: ₴").append(trim(est.totalCost)).append("\n");
        sb.append("Доступно из остатков сейчас: ").append(est.maxServingsAvailable).append(" шт.");

        new AlertDialog.Builder(this)
                .setTitle("Себестоимость")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        return s;
    }

    @Override public void onDelete(Product p) {
        if (!isAdmin) {
            Toast.makeText(this, "Удаление доступно только администратору", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Удалить товар?")
                .setMessage("«" + p.name + "» и его рецепт будут удалены. Продажи не трогаем.")
                .setPositiveButton("Удалить", (d, w) -> doDeleteProduct(p))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void doDeleteProduct(Product p) {
        io.execute(() -> {
            try {
                int used = saleItemDao.countByProduct(p.id);
                if (used > 0) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Нельзя удалить: есть " + used + " продаж(и) этого товара",
                            Toast.LENGTH_LONG).show());
                    return;
                }
                recipeDao.deleteByProduct(p.id);
                productDao.delete(p);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Товар удалён", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
