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
        version = 4,
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

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "crm.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    db.execSQL(
                                            "INSERT OR IGNORE INTO users(login, passwordHash, role, firstName, lastName, active) " +
                                                    "VALUES('admin','admin','admin','Админ',NULL,1)"
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
