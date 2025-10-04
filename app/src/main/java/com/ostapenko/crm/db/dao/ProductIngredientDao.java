package com.ostapenko.crm.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ostapenko.crm.dto.RecipeItemView;
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

    @Query("SELECT pi.id AS id, " +
            "       pi.ingredientId AS ingredientId, " +
            "       i.name AS ingredientName, " +
            "       i.unit AS unit, " +
            "       pi.quantity AS quantity " +
            "FROM product_ingredients pi " +
            "JOIN ingredients i ON i.id = pi.ingredientId " +
            "WHERE pi.productId = :productId " +
            "ORDER BY i.name")
    List<RecipeItemView> getRecipeView(int productId);

    @Query("DELETE FROM product_ingredients WHERE id = :rowId")
    void deleteRow(int rowId);

    @Query("SELECT MIN(i.stock / pi.quantity) " +
            "FROM product_ingredients pi " +
            "JOIN ingredients i ON i.id = pi.ingredientId " +
            "WHERE pi.productId = :productId")
    Double getMaxServings(int productId);

    @Query("SELECT i.id AS ingredientId, " +
            "       i.name AS ingredientName, " +
            "       i.unit AS unit, " +
            "       i.price AS pricePerUnit, " +
            "       pi.quantity AS quantityPerItem " +
            "FROM product_ingredients pi " +
            "JOIN ingredients i ON i.id = pi.ingredientId " +
            "WHERE pi.productId = :productId " +
            "ORDER BY i.name")
    java.util.List<com.ostapenko.crm.dto.CostRow> getCostRows(int productId);

}
