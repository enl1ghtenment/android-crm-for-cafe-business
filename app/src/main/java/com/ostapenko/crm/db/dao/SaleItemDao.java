package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.SaleItem;
import java.util.List;

@Dao
public interface SaleItemDao {

    @Insert
    long insert(SaleItem item);

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    List<SaleItem> findBySale(int saleId);
}
