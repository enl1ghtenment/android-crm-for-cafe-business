package com.ostapenko.crm.dto;

import java.util.ArrayList;
import java.util.List;

public class CostEstimate {
    public int productId;
    public int qty;
    public double totalCost;
    public int maxServingsAvailable;
    public final List<CostLine> lines = new ArrayList<>();

    public static class CostLine {
        public int ingredientId;
        public String ingredientName;
        public String unit;
        public double pricePerUnit;
        public double quantityForOrder;
        public double lineCost;
    }
}
