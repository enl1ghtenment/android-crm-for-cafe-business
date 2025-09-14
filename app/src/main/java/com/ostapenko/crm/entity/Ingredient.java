package com.ostapenko.crm.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ingredients")
public class Ingredient {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String unit;
    public double stock;
    public double price;
}
