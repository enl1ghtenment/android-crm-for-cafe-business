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

    @WorkerThread
    public int getMaxServings(int productId) {
        Double m = recipeDao.getMaxServings(productId);
        if (m == null) return 0;
        return (int)Math.floor(m);
    }

    @WorkerThread
    @Transaction
    public void sell(int productId, int qty, double subtotal, double saleTotal, Integer sellerId) {
        if (getMaxServings(productId) < qty) {
            throw new IllegalStateException("Недостаточно ингредиентов на складе");
        }
        List<ProductIngredient> recipe = recipeDao.getRecipe(productId);
        for (ProductIngredient r : recipe) {
            double consume = r.quantity * qty;
            ingredientDao.decreaseStock(r.ingredientId, consume);
        }
        Sale sale = new Sale();
        sale.saleDate = new Date();
        sale.total = saleTotal;
        sale.sellerId = sellerId;
        int saleId = (int) saleDao.insert(sale);

        SaleItem item = new SaleItem();
        item.saleId = saleId;
        item.productId = productId;
        item.quantity = qty;
        item.subtotal = subtotal;
        saleItemDao.insert(item);
    }


    @WorkerThread
    public com.ostapenko.crm.dto.CostEstimate estimateCost(int productId, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        Double m = recipeDao.getMaxServings(productId);
        int max = (m == null) ? 0 : (int) Math.floor(m);

        java.util.List<com.ostapenko.crm.dto.CostRow> rows = recipeDao.getCostRows(productId);

        com.ostapenko.crm.dto.CostEstimate result = new com.ostapenko.crm.dto.CostEstimate();
        result.productId = productId;
        result.qty = qty;
        result.maxServingsAvailable = max;

        double total = 0.0;
        for (com.ostapenko.crm.dto.CostRow r : rows) {
            com.ostapenko.crm.dto.CostEstimate.CostLine line = new com.ostapenko.crm.dto.CostEstimate.CostLine();
            line.ingredientId = r.ingredientId;
            line.ingredientName = r.ingredientName;
            line.unit = r.unit;
            line.pricePerUnit = r.pricePerUnit;
            line.quantityForOrder = r.quantityPerItem * qty;
            line.lineCost = line.quantityForOrder * r.pricePerUnit;
            total += line.lineCost;
            result.lines.add(line);
        }
        result.totalCost = total;
        return result;
    }

}
