package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.ostapenko.crm.db.dao.IngredientDao;
import com.ostapenko.crm.entity.Ingredient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ostapenko.crm.auth.Session;

public class InventoryActivity extends AppCompatActivity {

    private IngredientAdapter adapter;
    private IngredientDao ingredientDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Session session = new Session(this);
        if (!"admin".equalsIgnoreCase(session.role())) {
            Toast.makeText(this, "Доступ запрещён: только для администратора", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_inventory);

        ingredientDao = AppDatabase.getInstance(getApplicationContext()).ingredientDao();

        RecyclerView rv = findViewById(R.id.rvIngredients);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IngredientAdapter(this::onIngredientClick); // 👈 клик
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });


        loadData();
    }

    private void loadData() {
        io.execute(() -> {
            List<Ingredient> all = ingredientDao.findAll();
            runOnUiThread(() -> adapter.submit(all));
        });
    }

    private void onIngredientClick(Ingredient ing) {
        String[] actions = {"Приход", "Списание", "Редактировать"};
        new AlertDialog.Builder(this)
                .setTitle(ing.name + " (" + ing.unit + " • " + trim(ing.stock) + ")")
                .setItems(actions, (d, which) -> {
                    if (which == 0) showAdjustDialog(ing, true);
                    else if (which == 1) showAdjustDialog(ing, false);
                    else showEditDialog(ing);
                })
                .show();
    }

    private void showAdjustDialog(Ingredient ing, boolean inbound) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_adjust_stock, null, false);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvHint = view.findViewById(R.id.tvHint);
        EditText etDelta = view.findViewById(R.id.etDelta);

        tvTitle.setText(inbound ? "Приход" : "Списание");
        tvHint.setText(inbound
                ? "Введите сколько добавить к остатку"
                : "Введите сколько списать с остатка");

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Сохранить", (d, w) -> {
                    double delta;
                    try { delta = Double.parseDouble(etDelta.getText().toString().trim()); }
                    catch (Exception e) { delta = 0; }
                    if (delta <= 0) {
                        Toast.makeText(this, "Количество должно быть > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final double val = delta; // для ламбды
                    io.execute(() -> {
                        if (inbound) {
                            ingredientDao.increaseStock(ing.id, val);
                        } else {
                            // защита от ухода в минус:
                            Ingredient fresh = ingredientDao.findById(ing.id);
                            if (fresh != null && fresh.stock >= val) {
                                ingredientDao.decreaseStock(ing.id, val);
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Недостаточно на складе", Toast.LENGTH_LONG).show());
                                return;
                            }
                        }
                        loadData();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditDialog(Ingredient ing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient, null, false);
        EditText etName = view.findViewById(R.id.etName);
        EditText etUnit = view.findViewById(R.id.etUnit);
        EditText etStock = view.findViewById(R.id.etStock);
        EditText etPrice = view.findViewById(R.id.etPrice);

        // префилл
        etName.setText(ing.name);
        etUnit.setText(ing.unit);
        etStock.setText(String.valueOf(ing.stock));
        etPrice.setText(String.valueOf(ing.price));

        new AlertDialog.Builder(this)
                .setTitle("Редактировать ингредиент")
                .setView(view)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String unit = etUnit.getText().toString().trim();
                    double stock = safeParse(etStock.getText().toString().trim());
                    double price = safeParse(etPrice.getText().toString().trim());
                    if (name.isEmpty() || unit.isEmpty()) {
                        Toast.makeText(this, "Заполни имя и ед. изм.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // обновляем существующий объект
                    Ingredient upd = new Ingredient();
                    upd.id = ing.id;
                    upd.name = name;
                    upd.unit = unit;
                    upd.stock = stock;
                    upd.price = price;

                    io.execute(() -> {
                        ingredientDao.update(upd);
                        loadData();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
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

    private static String trim(double d) {
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length()-2);
        return s;
    }
}
