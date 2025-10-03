package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.Sale;
import com.ostapenko.crm.dto.SaleWithUser;

import java.util.Date;
import java.util.List;

@Dao
public interface SaleDao {

    @Insert
    long insert(Sale sale);

    @Query("SELECT * FROM sales WHERE date(saleDate/1000, 'unixepoch') = date(:day/1000, 'unixepoch') ORDER BY saleDate DESC")
    List<Sale> findByDay(Date day);

    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :from AND :to ORDER BY saleDate DESC")
    List<Sale> findBetween(Date from, Date to);

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate BETWEEN :from AND :to")
    double sumBetween(Date from, Date to);

    @Query("SELECT s.id, s.saleDate, s.total, s.sellerId, u.firstName, u.lastName, u.login " +
            "FROM sales s " +
            "LEFT JOIN users u ON u.id = s.sellerId " +
            "WHERE s.saleDate BETWEEN :from AND :to " +
            "ORDER BY s.saleDate DESC")
    List<SaleWithUser> findBetweenWithUser(Date from, Date to);

    @Query("SELECT s.id, s.saleDate, s.total, s.sellerId, u.firstName, u.lastName, u.login " +
            "FROM sales s " +
            "LEFT JOIN users u ON u.id = s.sellerId " +
            "WHERE s.sellerId = :userId " +
            "ORDER BY s.saleDate DESC")
    List<SaleWithUser> findByUser(int userId);
}
