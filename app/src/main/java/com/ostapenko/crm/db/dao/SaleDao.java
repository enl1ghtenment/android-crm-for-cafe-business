package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.Sale;
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
}
