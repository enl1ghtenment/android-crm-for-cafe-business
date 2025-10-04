package com.ostapenko.crm.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.ostapenko.crm.entity.SaleItem;
import java.util.List;

@Dao
public interface SaleItemDao {

    @Insert
    long insert(SaleItem item);

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    List<SaleItem> findBySale(int saleId);

    @Query("SELECT COUNT(*) FROM sale_items WHERE productId = :productId")
    int countByProduct(int productId);
}
