package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.dto.SaleWithUser;
import com.ostapenko.crm.entity.Sale;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesActivity extends AppCompatActivity {

    private SalesAdapter adapter;
    private SaleDao saleDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView tvSummary;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        EditText etSearch = findViewById(R.id.etSearchSales);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        saleDao = AppDatabase.getInstance(getApplicationContext()).saleDao();

        tvSummary = findViewById(R.id.tvSummary);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvSales);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalesAdapter();
        rv.setAdapter(adapter);

        Button btnDay = findViewById(R.id.btnDay);
        Button btnWeek = findViewById(R.id.btnWeek);
        Button btnMonth = findViewById(R.id.btnMonth);

        btnDay.setOnClickListener(v -> loadDay());
        btnWeek.setOnClickListener(v -> loadWeek());
        btnMonth.setOnClickListener(v -> loadMonth());

        loadDay(); // по умолчанию
    }

    private void loadDay() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        // начало дня
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        Date from = cal.getTime();
        // конец дня
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        Date to = cal.getTime();
        loadRange(from, to);
    }

    private void loadWeek() {
        Calendar cal = Calendar.getInstance();
        // конец = сейчас
        Date to = cal.getTime();
        // начало = 7 дней назад
        cal.add(Calendar.DATE, -7);
        Date from = cal.getTime();
        loadRange(from, to);
    }

    private void loadMonth() {
        Calendar cal = Calendar.getInstance();
        Date to = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        Date from = cal.getTime();
        loadRange(from, to);
    }

    private void loadRange(Date from, Date to) {
        io.execute(() -> {
            try {
                // строки продаж
                List<com.ostapenko.crm.dto.SaleRow> rows = saleDao.findRowsBetween(from, to);
                // итог по всем чекам за период — оставляем как было
                double total = saleDao.sumBetween(from, to);
                runOnUiThread(() -> {
                    adapter.submit(rows);
                    tvSummary.setText("Итог: ₴" + total);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }


}
