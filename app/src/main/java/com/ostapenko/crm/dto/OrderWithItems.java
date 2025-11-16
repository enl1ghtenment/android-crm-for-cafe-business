package com.ostapenko.crm.dto;

import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderWithItems {
    public int saleId;
    public Date saleDate;
    public double total;
    public String status;

    public Integer sellerId;
    public String firstName;
    public String lastName;
    public String login;

    @Ignore
    public final List<OrderLine> lines = new ArrayList<>();

    public static class OrderLine {
        public int productId;
        public String productName;
        public int quantity;
        public double subtotal;
    }
}
