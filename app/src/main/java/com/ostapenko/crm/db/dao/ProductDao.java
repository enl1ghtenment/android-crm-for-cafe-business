package com.ostapenko.crm.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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

    // ====== ДЛЯ ВКЛАДКИ «ЗАКАЗЫ» ======

    /** Список уникальных категорий (NULL/'' -> 'Прочее') */
    @Query("SELECT DISTINCT " +
            "COALESCE(NULLIF(category, ''), 'Прочее') AS category " +
            "FROM products " +
            "ORDER BY category")
    List<String> findDistinctCategories();

    /** Все товары внутри выбранной категории (NULL/'' -> 'Прочее') */
    @Query("SELECT * FROM products " +
            "WHERE COALESCE(NULLIF(category, ''), 'Прочее') = :category " +
            "ORDER BY name")
    List<Product> findByCategory(String category);
}
