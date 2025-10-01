package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.IngredientDao;
import com.ostapenko.crm.entity.Ingredient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryActivity extends AppCompatActivity {

    private IngredientAdapter adapter;
    private IngredientDao ingredientDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        ingredientDao = AppDatabase.getInstance(getApplicationContext()).ingredientDao();

        RecyclerView rv = findViewById(R.id.rvIngredients);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IngredientAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        loadData();
    }

    private void loadData() {
        io.execute(() -> {
            List<Ingredient> all = ingredientDao.findAll();
            runOnUiThread(() -> adapter.submit(all));
        });
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient, null, false);
        EditText etName = view.findViewById(R.id.etName);
        EditText etUnit = view.findViewById(R.id.etUnit);
        EditText etStock = view.findViewById(R.id.etStock);
        EditText etPrice = view.findViewById(R.id.etPrice);

        new AlertDialog.Builder(this)
                .setTitle("Добавить ингредиент")
                .setView(view)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String unit = etUnit.getText().toString().trim();
                    String sStock = etStock.getText().toString().trim();
                    String sPrice = etPrice.getText().toString().trim();

                    if (name.isEmpty() || unit.isEmpty() || sStock.isEmpty() || sPrice.isEmpty()) {
                        Toast.makeText(this, "Заполни все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double stock = safeParse(sStock);
                    double price = safeParse(sPrice);

                    Ingredient ing = new Ingredient();
                    ing.name = name;
                    ing.unit = unit;
                    ing.stock = stock;
                    ing.price = price;

                    io.execute(() -> {
                        ingredientDao.insert(ing);
                        loadData();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private double safeParse(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0d; }
    }
}
