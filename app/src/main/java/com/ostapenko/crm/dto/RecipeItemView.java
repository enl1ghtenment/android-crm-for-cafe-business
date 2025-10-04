package com.ostapenko.crm.dto;

import androidx.annotation.NonNull;

public class RecipeItemView {
    public int id;
    public int ingredientId;
    public String ingredientName;
    public String unit;
    public double quantity;

    @NonNull @Override public String toString() {
        return ingredientName + " (" + unit + ") — " + quantity;
    }
}
