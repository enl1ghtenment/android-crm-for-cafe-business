package com.ostapenko.crm.dto;

import androidx.annotation.NonNull;

// Плоский объект для отображения строки рецепта (JOIN product_ingredients + ingredients)
public class RecipeItemView {
    public int id;             // id записи в product_ingredients
    public int ingredientId;   // id ингредиента
    public String ingredientName;
    public String unit;        // ед. изм. (г/мл/шт)
    public double quantity;    // сколько на 1 порцию

    @NonNull @Override public String toString() {
        return ingredientName + " (" + unit + ") — " + quantity;
    }
}
