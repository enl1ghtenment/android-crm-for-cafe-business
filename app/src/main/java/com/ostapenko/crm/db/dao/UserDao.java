package com.ostapenko.crm.db.dao;

import androidx.room.*;
import com.ostapenko.crm.entity.User;
import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User u);

    @Update
    int update(User u);

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    User findByLogin(String login);

    @Query("SELECT COUNT(*) FROM users WHERE role = 'admin' AND active = 1")
    int countActiveAdmins();

    @Query("SELECT * FROM users ORDER BY active DESC, role ASC, lastName ASC, firstName ASC")
    List<User> findAll();

    @Query("SELECT * FROM users " +
            "WHERE (:q IS NULL OR :q = '' " +
            "   OR login LIKE :qLike " +
            "   OR firstName LIKE :qLike " +
            "   OR lastName LIKE :qLike) " +
            "ORDER BY active DESC, role ASC, lastName ASC, firstName ASC")
    List<User> search(String q, String qLike);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(int id);
}
