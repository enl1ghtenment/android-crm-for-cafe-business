package com.ostapenko.crm.domain;

import androidx.annotation.WorkerThread;
import androidx.room.Transaction;

import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.*;
import com.ostapenko.crm.entity.*;

import java.util.Date;
import java.util.List;

public class SalesService {

    private final AppDatabase db;
    private final ProductIngredientDao recipeDao;
    private final IngredientDao ingredientDao;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;

    public SalesService(AppDatabase db) {
        this.db = db;
        this.recipeDao = db.productIngredientDao();
        this.ingredientDao = db.ingredientDao();
        this.saleDao = db.saleDao();
        this.saleItemDao = db.saleItemDao();
    }

    /** Сколько порций можно сделать из текущих остатков */
    @WorkerThread
    public int getMaxServings(int productId) {
        Double m = recipeDao.getMaxServings(productId);
        if (m == null) return 0;
        return (int)Math.floor(m);
    }

    /** Продажа qty штук товара productId по total сумме (на уровне UI ты уже знаешь цену) */
    @WorkerThread
    @Transaction
    public void sell(int productId, int qty, double subtotal, double saleTotal) {
        // 1) проверка остатков
        if (getMaxServings(productId) < qty) {
            throw new IllegalStateException("Недостаточно ингредиентов на складе");
        }

        // 2) списываем ингредиенты
        List<ProductIngredient> recipe = recipeDao.getRecipe(productId);
        for (ProductIngredient r : recipe) {
            double consume = r.quantity * qty;
            ingredientDao.decreaseStock(r.ingredientId, consume);
        }

        // 3) сохраняем продажу и позиции
        Sale sale = new Sale();
        sale.saleDate = new Date();
        sale.total = saleTotal;
        int saleId = (int) saleDao.insert(sale);

        SaleItem item = new SaleItem();
        item.saleId = saleId;
        item.productId = productId;
        item.quantity = qty;
        item.subtotal = subtotal;
        saleItemDao.insert(item);
    }
}
