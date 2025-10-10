package com.ostapenko.crm.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ostapenko.crm.db.dao.*;
import com.ostapenko.crm.entity.*;

@Database(
        entities = {
                Ingredient.class,
                Product.class,
                ProductIngredient.class,
                Sale.class,
                SaleItem.class,
                User.class
        },
        version = 5,
        exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract IngredientDao ingredientDao();
    public abstract ProductDao productDao();
    public abstract ProductIngredientDao productIngredientDao();
    public abstract SaleDao saleDao();
    public abstract SaleItemDao saleItemDao();
    public abstract UserDao userDao();

    private static volatile AppDatabase INSTANCE;

    private static final String ADMIN_HASH_103668 =
            "c5666cc83d0c96df4ccdbef90aef3f8d65b4aad259bf6ef11ec6214c9281c3b8";

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "  login TEXT NOT NULL UNIQUE," +
                            "  passwordHash TEXT NOT NULL," +
                            "  role TEXT NOT NULL DEFAULT 'employee'," +
                            "  firstName TEXT," +
                            "  lastName TEXT," +
                            "  active INTEGER NOT NULL DEFAULT 1" +
                            ")"
            );
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE sales ADD COLUMN sellerId INTEGER");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "INSERT OR IGNORE INTO users(login, passwordHash, role, firstName, lastName, active) " +
                            "VALUES('ostapenko', ?, 'admin', 'Остапенко', NULL, 1)",
                    new Object[]{ADMIN_HASH_103668}
            );
            db.execSQL("UPDATE users SET active = 0 WHERE role = 'admin' AND login <> 'ostapenko'");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "INSERT OR IGNORE INTO users(login, passwordHash, role, firstName, lastName, active) " +
                            "VALUES('admin', ?, 'admin', 'Админ', NULL, 1)",
                    new Object[]{ADMIN_HASH_103668}
            );
            db.execSQL("UPDATE users SET active = 0 WHERE role = 'admin' AND login <> 'admin'");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "crm.db")
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6
                            )
                            .addCallback(new Callback() {
                                @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    db.execSQL(
                                            "INSERT OR IGNORE INTO users(login, passwordHash, role, firstName, lastName, active) " +
                                                    "VALUES(?, ?, 'admin', ?, NULL, 1)",
                                            new Object[]{"admin", ADMIN_HASH_103668, "Админ"}
                                    );
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
