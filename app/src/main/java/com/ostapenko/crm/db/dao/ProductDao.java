package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.Product;
import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Product product);

    @Update
    int update(Product product);

    @Delete
    int delete(Product product);

    @Query("SELECT * FROM products ORDER BY name")
    List<Product> findAll();

    @Query("SELECT * FROM products WHERE id = :id")
    Product findById(int id);
}
