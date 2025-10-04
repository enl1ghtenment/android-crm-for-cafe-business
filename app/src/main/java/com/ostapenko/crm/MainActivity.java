package com.ostapenko.crm;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.IngredientDao;
import com.ostapenko.crm.db.dao.ProductDao;
import com.ostapenko.crm.db.dao.ProductIngredientDao;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.db.dao.SaleItemDao;
import com.ostapenko.crm.domain.SalesService;
import com.ostapenko.crm.entity.Ingredient;
import com.ostapenko.crm.entity.Product;
import com.ostapenko.crm.entity.ProductIngredient;

import java.util.Date;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        Executors.newSingleThreadExecutor().execute(this::runSmokeTest);
    }

    private void runSmokeTest() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            IngredientDao ingredientDao = db.ingredientDao();
            ProductDao productDao = db.productDao();
            ProductIngredientDao recipeDao = db.productIngredientDao();
            SaleDao saleDao = db.saleDao();
            SaleItemDao saleItemDao = db.saleItemDao();
            Ingredient lime = new Ingredient();
            lime.name = "Лайм"; lime.unit = "шт"; lime.stock = 20; lime.price = 10.0;
            lime.id = (int) ingredientDao.insert(lime);
            Ingredient ice = new Ingredient();
            ice.name = "Лёд"; ice.unit = "г"; ice.stock = 5000; ice.price = 0.02;
            ice.id = (int) ingredientDao.insert(ice);
            Product mojito = new Product();
            mojito.name = "Мохито"; mojito.description = "Классический";
            mojito.id = (int) productDao.insert(mojito);
            ProductIngredient r1 = new ProductIngredient();
            r1.productId = mojito.id; r1.ingredientId = lime.id; r1.quantity = 1;
            recipeDao.insert(r1);
            ProductIngredient r2 = new ProductIngredient();
            r2.productId = mojito.id; r2.ingredientId = ice.id; r2.quantity = 200;
            recipeDao.insert(r2);
            Double maxServings = recipeDao.getMaxServings(mojito.id);
            int maxBefore = (int) Math.floor(maxServings != null ? maxServings : 0);
            SalesService sales = new SalesService(db);
            sales.sell(mojito.id, 3, /*subtotal*/ 3 * 120.0, /*saleTotal*/ 3 * 120.0, /*sellerId*/ null);
            Double maxServingsAfter = recipeDao.getMaxServings(mojito.id);
            int maxAfter = (int) Math.floor(maxServingsAfter != null ? maxServingsAfter : 0);
            double todaySum = saleDao.sumBetween(
                    new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000),
                    new Date()
            );
            String msg = "SMOKE OK\n" +
                    "Порций до продажи: " + maxBefore + "\n" +
                    "Порций после продажи (–3): " + maxAfter + "\n" +
                    "Выручка за период: " + todaySum;

            Log.d("SMOKE", msg);
            runOnUiThread(() -> tvResult.setText(msg));
        } catch (Exception e) {
            Log.e("SMOKE", "Ошибка", e);
            runOnUiThread(() -> tvResult.setText("SMOKE FAIL: " + e.getMessage()));
        }
    }
}
