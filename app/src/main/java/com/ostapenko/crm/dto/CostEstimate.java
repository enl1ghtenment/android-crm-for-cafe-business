package com.ostapenko.crm.dto;

import java.util.ArrayList;
import java.util.List;

public class CostEstimate {
    public int productId;
    public int qty; // сколько штук считаем
    public double totalCost; // суммарная себестоимость
    public int maxServingsAvailable; // сколько максимум можно сделать из текущих остатков (для инфо)
    public final List<CostLine> lines = new ArrayList<>();

    public static class CostLine {
        public int ingredientId;
        public String ingredientName;
        public String unit;
        public double pricePerUnit;
        public double quantityForOrder; // сколько всего понадобится на qty штук
        public double lineCost;         // стоимость по ингредиенту = quantityForOrder * pricePerUnit
    }
}
