package com.ostapenko.crm.dto;

import java.util.Date;

public class SaleRow {
    // из sales
    public int saleId;
    public Date saleDate;
    public double saleTotal;

    // из users (может быть null)
    public Integer sellerId;
    public String firstName;
    public String lastName;
    public String login;

    // из sale_items + products
    public int productId;
    public String productName;
    public int quantity;
    public double subtotal;

    public String displaySeller() {
        if (firstName != null && !firstName.isEmpty()) {
            String ln = (lastName == null) ? "" : (" " + lastName);
            return firstName + ln;
        }
        return (login != null && !login.isEmpty())
                ? login
                : (sellerId == null ? "-" : String.valueOf(sellerId));
    }
}
