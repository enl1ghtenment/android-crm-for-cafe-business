package com.ostapenko.crm.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String description;

    public String category;

    public String imageResName;

    public double price;

}
