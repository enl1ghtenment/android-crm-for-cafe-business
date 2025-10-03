package com.ostapenko.crm.dto;

import java.util.Date;

public class SaleWithUser {
    public int id;
    public Date saleDate;
    public double total;

    public Integer sellerId;      // может быть null
    public String firstName;
    public String lastName;
    public String login;

    public String displayName() {
        if (firstName != null && !firstName.isEmpty()) {
            String ln = (lastName == null) ? "" : (" " + lastName);
            return firstName + ln;
        }
        return (login != null && !login.isEmpty()) ? login : (sellerId == null ? "-" : String.valueOf(sellerId));
    }
}
