package com.ostapenko.crm.db.dao;

import androidx.annotation.WorkerThread;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.ostapenko.crm.dto.SaleRow;
import com.ostapenko.crm.dto.SaleWithUser;
import com.ostapenko.crm.dto.OrderWithItems;
import com.ostapenko.crm.entity.Sale;

import java.util.Date;
import java.util.List;

@Dao
public interface SaleDao {

    // --- базовые CRUD ---
    @Insert
    long insert(Sale sale);

    @Update
    int update(Sale sale);

    // --- аналитика для экрана "Продажи" ---
    @Query("SELECT * FROM sales " +
            "WHERE date(saleDate/1000, 'unixepoch') = date(:day/1000, 'unixepoch') " +
            "ORDER BY saleDate DESC")
    List<Sale> findByDay(Date day);

    @Query("SELECT * FROM sales " +
            "WHERE saleDate BETWEEN :from AND :to " +
            "ORDER BY saleDate DESC")
    List<Sale> findBetween(Date from, Date to);

    @Query("SELECT COALESCE(SUM(total), 0) " +
            "FROM sales " +
            "WHERE saleDate BETWEEN :from AND :to")
    double sumBetween(Date from, Date to);

    @Query("SELECT s.id, s.saleDate, s.total, s.sellerId, " +
            "       u.firstName, u.lastName, u.login " +
            "FROM sales s " +
            "LEFT JOIN users u ON u.id = s.sellerId " +
            "WHERE s.saleDate BETWEEN :from AND :to " +
            "ORDER BY s.saleDate DESC")
    List<SaleWithUser> findBetweenWithUser(Date from, Date to);

    @Query("SELECT s.id, s.saleDate, s.total, s.sellerId, " +
            "       u.firstName, u.lastName, u.login " +
            "FROM sales s " +
            "LEFT JOIN users u ON u.id = s.sellerId " +
            "WHERE s.sellerId = :userId " +
            "ORDER BY s.saleDate DESC")
    List<SaleWithUser> findByUser(int userId);

    @Query(
            "SELECT " +
                    "  s.id         AS saleId, " +
                    "  s.saleDate   AS saleDate, " +
                    "  s.total      AS saleTotal, " +
                    "  u.id         AS sellerId, " +
                    "  u.firstName  AS firstName, " +
                    "  u.lastName   AS lastName, " +
                    "  u.login      AS login, " +
                    "  si.productId AS productId, " +
                    "  p.name       AS productName, " +
                    "  si.quantity  AS quantity, " +
                    "  si.subtotal  AS subtotal " +
                    "FROM sales s " +
                    "LEFT JOIN users u  ON u.id = s.sellerId " +
                    "JOIN sale_items si ON si.saleId = s.id " +
                    "JOIN products p    ON p.id = si.productId " +
                    "WHERE s.saleDate BETWEEN :from AND :to " +
                    "ORDER BY s.saleDate DESC, s.id DESC, si.id ASC"
    )
    List<SaleRow> findRowsBetween(Date from, Date to);

    // --- для экрана "Заказы" (кухня / активные / история) ---

    // Заголовки заказов по статусу (NEW / DONE)
    @Query(
            "SELECT s.id AS saleId, " +
                    "       s.saleDate AS saleDate, " +
                    "       s.total AS total, " +
                    "       s.status AS status, " +
                    "       u.id AS sellerId, " +
                    "       u.firstName AS firstName, " +
                    "       u.lastName AS lastName, " +
                    "       u.login AS login " +
                    "FROM sales s " +
                    "LEFT JOIN users u ON u.id = s.sellerId " +
                    "WHERE s.status = :status " +
                    "ORDER BY s.saleDate DESC"
    )
    List<OrderWithItems> findOrdersByStatus(String status);

    // Позиции конкретного заказа
    @Query(
            "SELECT si.productId AS productId, " +
                    "       p.name AS productName, " +
                    "       si.quantity AS quantity, " +
                    "       si.subtotal AS subtotal " +
                    "FROM sale_items si " +
                    "JOIN products p ON p.id = si.productId " +
                    "WHERE si.saleId = :saleId " +
                    "ORDER BY si.id ASC"
    )
    List<OrderWithItems.OrderLine> findLinesForOrder(int saleId);

    // Пометить заказ как выданный
    @Query("UPDATE sales SET status = 'DONE' WHERE id = :saleId")
    void markDone(int saleId);
}
