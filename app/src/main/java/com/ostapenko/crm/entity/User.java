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

    public String login;
    public String passwordHash;
    public String role;
    public boolean active;

    public String firstName;
    public String lastName;
}
