package com.ostapenko.crm.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.IngredientDao;
import com.ostapenko.crm.db.dao.ProductIngredientDao;
import com.ostapenko.crm.entity.Ingredient;
import com.ostapenko.crm.entity.ProductIngredient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeActivity extends AppCompatActivity implements RecipeAdapter.Listener {

    private static final String EXTRA_PRODUCT_ID = "productId";
    public static void start(Context ctx, int productId) {
        Intent i = new Intent(ctx, RecipeActivity.class);
        i.putExtra(EXTRA_PRODUCT_ID, productId);
        ctx.startActivity(i);
    }

    private int productId;
    private RecipeAdapter adapter;
    private ProductIngredientDao recipeDao;
    private IngredientDao ingredientDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final HashMap<Integer, String> ingredientNames = new HashMap<>(); // id -> name

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
        if (productId <= 0) { finish(); return; }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        recipeDao = db.productIngredientDao();
        ingredientDao = db.ingredientDao();

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvRecipe);
        rv.setLayoutManager(new LinearLayoutManager(this));
        // 👇 адаптер уже принимает Listener с onDelete(int rowId)
        adapter = new RecipeAdapter(this);
        rv.setAdapter(adapter);

        findViewById(R.id.fabAddIngredientToRecipe).setOnClickListener(v -> showAddRecipeItemDialog());
        loadData();
    }

    private void loadData() {
        io.execute(() -> {
            // 👇 теперь берём JOIN-представление
            List<com.ostapenko.crm.dto.RecipeItemView> recipe = recipeDao.getRecipeView(productId);
            runOnUiThread(() -> adapter.submit(recipe));
        });
    }

    private void showAddRecipeItemDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_recipe_item, null, false);
        Spinner sp = view.findViewById(R.id.spIngredient);
        EditText etQty = view.findViewById(R.id.etQty);

        io.execute(() -> {
            List<Ingredient> all = ingredientDao.findAll();
            List<String> display = new ArrayList<>();
            for (Ingredient i : all) display.add(i.id + " • " + i.name + " (" + i.unit + ")");
            runOnUiThread(() -> {
                ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, display);
                sp.setAdapter(ad);

                new AlertDialog.Builder(this)
                        .setTitle("Добавить в рецепт")
                        .setView(view)
                        .setPositiveButton("Сохранить", (d, w) -> {
                            int pos = sp.getSelectedItemPosition();
                            if (pos < 0 || pos >= all.size()) {
                                Toast.makeText(this, "Выбери ингредиент", Toast.LENGTH_SHORT).show(); return;
                            }
                            double qty;
                            try { qty = Double.parseDouble(etQty.getText().toString().trim()); }
                            catch (Exception e) { qty = 0; }
                            if (qty <= 0) { Toast.makeText(this, "Количество должно быть > 0", Toast.LENGTH_SHORT).show(); return; }

                            Ingredient chosen = all.get(pos);
                            ProductIngredient row = new ProductIngredient();
                            row.productId = productId;
                            row.ingredientId = chosen.id;
                            row.quantity = qty;

                            io.execute(() -> {
                                recipeDao.insert(row);
                                loadData();
                            });
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        });
    }

    // ==== RecipeAdapter.Listener ====
    @Override public void onDelete(int rowId) {
        io.execute(() -> {
            recipeDao.deleteRow(rowId);
            loadData();
        });
    }
}
