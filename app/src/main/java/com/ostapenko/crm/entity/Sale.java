package com.ostapenko.crm.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "sales")
public class Sale {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Date saleDate;
    public double total;
    public Integer sellerId;
}
