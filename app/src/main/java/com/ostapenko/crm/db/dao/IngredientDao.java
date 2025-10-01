package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.Ingredient;
import java.util.List;

@Dao
public interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Ingredient ingredient);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Ingredient> ingredients);

    @Update
    int update(Ingredient ingredient);

    @Delete
    int delete(Ingredient ingredient);

    @Query("SELECT * FROM ingredients ORDER BY name")
    List<Ingredient> findAll();

    @Query("SELECT * FROM ingredients WHERE id = :id")
    Ingredient findById(int id);

    // уменьшить остаток (используем в продаже)
    @Query("UPDATE ingredients SET stock = stock - :delta WHERE id = :ingredientId")
    void decreaseStock(int ingredientId, double delta);

    // подсветка «скоро закончится»
    @Query("SELECT * FROM ingredients WHERE stock <= :threshold ORDER BY stock ASC")
    List<Ingredient> findLowStock(double threshold);
}
