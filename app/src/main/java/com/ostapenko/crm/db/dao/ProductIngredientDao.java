package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.ProductIngredient;
import java.util.List;

@Dao
public interface ProductIngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProductIngredient pi);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ProductIngredient> list);

    @Query("DELETE FROM product_ingredients WHERE productId = :productId")
    void deleteByProduct(int productId);

    @Query("SELECT * FROM product_ingredients WHERE productId = :productId")
    List<ProductIngredient> getRecipe(int productId);

    // Сколько порций можно сделать из текущих остатков:
    // берем min(stock / quantity) по всем ингредиентам рецепта
    @Query("SELECT MIN(i.stock / pi.quantity) " +
            "FROM product_ingredients pi " +
            "JOIN ingredients i ON i.id = pi.ingredientId " +
            "WHERE pi.productId = :productId")
    Double getMaxServings(int productId);
}
