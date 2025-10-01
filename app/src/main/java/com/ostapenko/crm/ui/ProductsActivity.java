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
import java.util.concurrent.Executors;

public class ProductsActivity extends AppCompatActivity implements ProductAdapter.Listener {

    private ProductAdapter adapter;
    private ProductDao productDao;
    private ProductIngredientDao recipeDao;
    private SalesService salesService;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        productDao = db.productDao();
        recipeDao = db.productIngredientDao();
        salesService = new SalesService(db);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(this);
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

                    // 👇 финальные значения для использования в ламбде
                    final int qtyVal = parsedQty;
                    final double priceVal = parsedPrice;
                    final double subtotalVal = qtyVal * priceVal;

                    io.execute(() -> {
                        try {
                            salesService.sell(p.id, qtyVal, subtotalVal, subtotalVal);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Продано: " + qtyVal + " × " + p.name, Toast.LENGTH_SHORT).show();
                                loadData(); // обновим «Можно приготовить»
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
