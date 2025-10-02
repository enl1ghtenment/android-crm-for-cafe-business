package com.ostapenko.crm.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = @Index(value = {"login"}, unique = true)
)
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String login;        // уникальный
    public String passwordHash; // SHA-256
    public String role;         // "admin" | "employee"
    public boolean active;

    // 👇 новые поля (необязательные)
    public String firstName;    // может быть null
    public String lastName;     // может быть null
}
