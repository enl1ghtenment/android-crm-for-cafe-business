package com.ostapenko.crm.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_ingredients")
public class ProductIngredient {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int productId;
    public int ingredientId;

    public double quantity;
}
