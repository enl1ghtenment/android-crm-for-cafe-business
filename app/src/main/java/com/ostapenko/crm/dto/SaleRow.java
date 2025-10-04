package com.ostapenko.crm.dto;

import java.util.Date;

public class SaleRow {
    public int saleId;
    public Date saleDate;
    public double saleTotal;

    public Integer sellerId;
    public String firstName;
    public String lastName;
    public String login;

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
