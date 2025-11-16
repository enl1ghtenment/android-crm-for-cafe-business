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

    /**
     * Создать один заказ с несколькими позициями.
     *
     * @param lines список позиций корзины
     * @param sellerId кто оформляет (может быть null)
     *
     * Каждая позиция корзины:
     *  - productId
     *  - qty (сколько штук)
     *  - pricePerUnit (цена за штуку, которую набил кассир)
     *
     * Мы:
     * 1. проверяем склад (чтобы хватает ингредиентов на все позиции)
     * 2. списываем ингредиенты
     * 3. создаём Sale со статусом "NEW"
     * 4. создаём SaleItem для каждой строки
     */
    @WorkerThread
    @Transaction
    public int sellOrder(List<CartLine> lines, Integer sellerId) {

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Пустой заказ");
        }

        // 1. Проверяем наличие ингредиентов по всем товарам сразу
        //    (если чего-то не хватает -> кидаем исключение ДО списания)
        for (CartLine cl : lines) {
            if (cl.qty <= 0) throw new IllegalArgumentException("Количество должно быть >0");

            int available = getMaxServings(cl.productId);
            if (available < cl.qty) {
                throw new IllegalStateException("Недостаточно ингредиентов для " + cl.productId);
            }
        }

        // 2. Считаем общий total
        double orderTotal = 0.0;
        for (CartLine cl : lines) {
            orderTotal += cl.qty * cl.pricePerUnit;
        }

        // 3. Списываем ингредиенты с учётом количества
        for (CartLine cl : lines) {
            List<ProductIngredient> recipe = recipeDao.getRecipe(cl.productId);
            for (ProductIngredient r : recipe) {
                double consume = r.quantity * cl.qty;
                ingredientDao.decreaseStock(r.ingredientId, consume);
            }
        }

        // 4. Создаём сам чек (Sale) со статусом "NEW"
        Sale sale = new Sale();
        sale.saleDate = new Date();
        sale.total = orderTotal;
        sale.sellerId = sellerId;
        sale.status = "NEW";
        int saleId = (int) saleDao.insert(sale);

        // 5. Добавляем строки
        for (CartLine cl : lines) {
            SaleItem item = new SaleItem();
            item.saleId = saleId;
            item.productId = cl.productId;
            item.quantity = cl.qty;
            item.subtotal = cl.qty * cl.pricePerUnit;
            saleItemDao.insert(item);
        }

        return saleId;
    }

    /**
     * Одна позиция в корзине перед оформлением.
     */
    public static class CartLine {
        public int productId;
        public int qty;
        public double pricePerUnit;
    }
}
