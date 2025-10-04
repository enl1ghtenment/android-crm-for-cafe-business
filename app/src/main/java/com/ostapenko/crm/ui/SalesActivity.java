package com.ostapenko.crm.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.ostapenko.crm.R;
import com.ostapenko.crm.auth.Session;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.SaleDao;
import com.ostapenko.crm.dto.SaleRow;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesActivity extends AppCompatActivity {

    private SalesAdapter adapter;
    private SaleDao saleDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView tvSummary;
    private Date lastFrom, lastTo;
    private boolean isAdmin;

    private @Nullable Uri lastExportUri = null;

    private ActivityResultLauncher<String> createCsvLauncher;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isAdmin = "admin".equalsIgnoreCase(new Session(this).role());

        createCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                this::onCsvUriChosen
        );

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

        loadDay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sales, menu);
        menu.findItem(R.id.action_export).setVisible(isAdmin);
        menu.findItem(R.id.action_share_csv).setVisible(isAdmin);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_export).setVisible(isAdmin);
        menu.findItem(R.id.action_share_csv).setVisible(isAdmin);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export) {
            startExport();
            return true;
        } else if (id == R.id.action_share_csv) {
            if (lastExportUri != null) {
                shareCsv(lastExportUri);
            } else {
                Toast.makeText(this, "Сначала экспортируй CSV через «Экспорт»", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDay() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        Date from = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        Date to = cal.getTime();

        loadRange(from, to);
    }

    private void loadWeek() {
        Calendar cal = Calendar.getInstance();
        Date to = cal.getTime();
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
        lastFrom = from;
        lastTo   = to;
        io.execute(() -> {
            try {
                List<SaleRow> rows = saleDao.findRowsBetween(from, to);
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


    private void startExport() {
        if (lastFrom == null || lastTo == null) {
            Toast.makeText(this, "Сначала выбери период (День/Неделя/Месяц)", Toast.LENGTH_SHORT).show();
            return;
        }
        String fname = "sales_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".csv";
        createCsvLauncher.launch(fname);
    }

    private void onCsvUriChosen(@Nullable Uri uri) {
        if (uri == null) return;
        io.execute(() -> {
            try (OutputStream os = getContentResolver().openOutputStream(uri, "w")) {
                if (os == null) throw new IllegalStateException("Не удалось открыть поток записи");

                os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

                List<SaleRow> rows = saleDao.findRowsBetween(lastFrom, lastTo);
                double total = saleDao.sumBetween(lastFrom, lastTo);

                String[] header = {
                        "Дата/время", "Чек ID", "Товар", "Кол-во",
                        "Сумма строки", "Продавец", "Логин", "Чек итого"
                };
                writeCsvLine(os, header);

                SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

                for (SaleRow r : rows) {
                    String seller =
                            (r.firstName != null && !r.firstName.isEmpty())
                                    ? r.firstName + (r.lastName == null ? "" : " " + r.lastName)
                                    : (r.login != null ? r.login : (r.sellerId == null ? "-" : String.valueOf(r.sellerId)));
                    String[] line = {
                            df.format(r.saleDate),
                            String.valueOf(r.saleId),
                            nullToEmpty(r.productName),
                            String.valueOf(r.quantity),
                            trim(r.subtotal),
                            seller,
                            nullToEmpty(r.login),
                            trim(r.saleTotal)
                    };
                    writeCsvLine(os, line);
                }

                writeRaw(os, "\n");
                writeCsvLine(os, new String[]{"", "", "", "", "Итог периода", "", "", trim(total)});

                lastExportUri = uri;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Экспортирован CSV-файл", Toast.LENGTH_LONG).show();
                    shareCsv(uri);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Не удалось сохранить: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareCsv(@NonNull Uri uri) {
        try {
            android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            send.putExtra(android.content.Intent.EXTRA_SUBJECT, "Экспорт продаж");
            send.putExtra(android.content.Intent.EXTRA_TEXT, "Статистика продаж в CSV за выбранный период.");

            send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(send, "Поделиться CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось поделиться: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeCsvLine(OutputStream os, String[] cells) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(csvEscape(cells[i]));
        }
        sb.append('\n');
        writeRaw(os, sb.toString());
    }

    private void writeRaw(OutputStream os, String s) throws Exception {
        os.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private String csvEscape(String s) {
        if (s == null) s = "";
        String v = s.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length()-2);
        return s;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
