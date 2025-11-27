package com.ostapenko.crm.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.ostapenko.crm.dto.OrderWithItems;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesActivity extends AppCompatActivity implements SalesAdapter.Listener{

    private enum Mode { DAY, WEEK, MONTH, YEAR }

    private SalesAdapter adapter;
    private SaleDao saleDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView tvSummary;
    private TextView tvPeriodLabel;
    private Date lastFrom, lastTo;
    private boolean isAdmin;

    private @Nullable Uri lastExportUri = null;

    private ActivityResultLauncher<String> createCsvLauncher;

    private Mode currentMode = Mode.DAY;

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
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvSales);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalesAdapter(this);
        rv.setAdapter(adapter);

        Button btnDay   = findViewById(R.id.btnDay);
        Button btnWeek  = findViewById(R.id.btnWeek);
        Button btnMonth = findViewById(R.id.btnMonth);
        Button btnYear  = findViewById(R.id.btnYear);
        Button btnPrev  = findViewById(R.id.btnPrevPeriod);
        Button btnNext  = findViewById(R.id.btnNextPeriod);

        btnDay.setOnClickListener(v -> { currentMode = Mode.DAY;   goToNow(); });
        btnWeek.setOnClickListener(v -> { currentMode = Mode.WEEK;  goToNow(); });
        btnMonth.setOnClickListener(v -> { currentMode = Mode.MONTH; goToNow(); });
        btnYear.setOnClickListener(v -> { currentMode = Mode.YEAR;  goToNow(); });

        btnPrev.setOnClickListener(v -> shiftPeriod(-1));
        btnNext.setOnClickListener(v -> shiftPeriod(+1));

        goToNow();
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

    private void goToNow() {
        Calendar now = Calendar.getInstance();
        switch (currentMode) {
            case DAY   -> setDay(now.getTime());
            case WEEK  -> setWeek(now);
            case MONTH -> setMonth(now);
            case YEAR  -> setYear(now);
        }
    }
    private void shiftPeriod(int delta) {
        if (lastFrom == null || lastTo == null) return;
        Calendar cal = Calendar.getInstance();
        switch (currentMode) {
            case DAY -> {
                cal.setTime(lastFrom);
                cal.add(Calendar.DATE, delta);
                setDay(cal.getTime());
            }
            case WEEK -> {
                cal.setTime(lastFrom);
                cal.add(Calendar.DATE, 7 * delta);
                Date from = startOfDay(cal.getTime());
                cal.add(Calendar.DATE, 6);
                Date to = endOfDay(cal.getTime());
                loadRange(from, to);
                updatePeriodLabelWeek(from);
            }
            case MONTH -> {
                cal.setTime(lastFrom);
                cal.add(Calendar.MONTH, delta);

                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date from = startOfDay(cal.getTime());

                Calendar calTo = (Calendar) cal.clone();
                calTo.set(Calendar.DAY_OF_MONTH, calTo.getActualMaximum(Calendar.DAY_OF_MONTH));
                Date to = endOfDay(calTo.getTime());

                loadRange(from, to);
                updatePeriodLabelMonth(from);
            }
            case YEAR -> {
                cal.setTime(lastFrom);
                cal.add(Calendar.YEAR, delta);

                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date from = startOfDay(cal.getTime());

                Calendar calTo = (Calendar) cal.clone();
                calTo.set(Calendar.MONTH, Calendar.DECEMBER);
                calTo.set(Calendar.DAY_OF_MONTH, 31);
                Date to = endOfDay(calTo.getTime());

                loadRange(from, to);
                updatePeriodLabelYear(from);
            }
        }
    }

    private void setDay(Date day) {
        Date from = startOfDay(day);
        Date to   = endOfDay(day);
        loadRange(from, to);
        tvPeriodLabel.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(from));
    }

    private void setWeek(Calendar today) {
        Calendar fromCal = (Calendar) today.clone();
        fromCal.add(Calendar.DATE, -6);
        Date from = startOfDay(fromCal.getTime());
        Date to   = endOfDay(today.getTime());
        loadRange(from, to);
        updatePeriodLabelWeek(from);
    }

    private void setMonth(Calendar today) {
        // from = 1 число этого месяца 00:00
        Calendar fromCal = (Calendar) today.clone();
        fromCal.set(Calendar.DAY_OF_MONTH, 1);
        Date from = startOfDay(fromCal.getTime());

        // to = последний день этого месяца 23:59
        Calendar toCal = (Calendar) today.clone();
        toCal.set(Calendar.DAY_OF_MONTH, toCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date to = endOfDay(toCal.getTime());

        loadRange(from, to);
        updatePeriodLabelMonth(from);
    }

    private void setYear(Calendar today) {
        // from = 1 января этого года 00:00
        Calendar fromCal = (Calendar) today.clone();
        fromCal.set(Calendar.MONTH, Calendar.JANUARY);
        fromCal.set(Calendar.DAY_OF_MONTH, 1);
        Date from = startOfDay(fromCal.getTime());

        // to = 31 декабря этого года 23:59
        Calendar toCal = (Calendar) today.clone();
        toCal.set(Calendar.MONTH, Calendar.DECEMBER);
        toCal.set(Calendar.DAY_OF_MONTH, 31);
        Date to = endOfDay(toCal.getTime());

        loadRange(from, to);
        updatePeriodLabelYear(from);
    }

    private void updatePeriodLabelWeek(Date from) {
        SimpleDateFormat dLeft  = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SimpleDateFormat dRight = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        String left = dLeft.format(from);
        c.add(Calendar.DATE, 6);
        String right = dRight.format(c.getTime());
        tvPeriodLabel.setText(left + " — " + right);
    }

    private void updatePeriodLabelMonth(Date monthStart) {
        SimpleDateFormat df = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
        tvPeriodLabel.setText(df.format(monthStart));
    }

    private void updatePeriodLabelYear(Date yearStart) {
        SimpleDateFormat dfL = new SimpleDateFormat("LLL yyyy", new Locale("ru"));
        Calendar c = Calendar.getInstance();
        c.setTime(yearStart);
        String left = dfL.format(c.getTime());
        c.add(Calendar.YEAR, 1);
        c.add(Calendar.MILLISECOND, -1);
        String right = dfL.format(c.getTime());
        tvPeriodLabel.setText(left + " — " + right);
    }

    private Date startOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date endOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private Date startOfMonth(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.DAY_OF_MONTH, 1);
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x.getTime();
    }
    private Date endOfMonth(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.DAY_OF_MONTH, x.getActualMaximum(Calendar.DAY_OF_MONTH));
        x.set(Calendar.HOUR_OF_DAY, 23);
        x.set(Calendar.MINUTE, 59);
        x.set(Calendar.SECOND, 59);
        x.set(Calendar.MILLISECOND, 999);
        return x.getTime();
    }
    private Date startOfYear(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.MONTH, Calendar.JANUARY);
        x.set(Calendar.DAY_OF_MONTH, 1);
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x.getTime();
    }
    private Date endOfYear(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.MONTH, Calendar.DECEMBER);
        x.set(Calendar.DAY_OF_MONTH, 31);
        x.set(Calendar.HOUR_OF_DAY, 23);
        x.set(Calendar.MINUTE, 59);
        x.set(Calendar.SECOND, 59);
        x.set(Calendar.MILLISECOND, 999);
        return x.getTime();
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
            Toast.makeText(this, "Сначала выбери период (День/Неделя/Месяц/Год)", Toast.LENGTH_SHORT).show();
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
                os.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});

                List<SaleRow> rows = saleDao.findRowsBetween(lastFrom, lastTo);
                double total = saleDao.sumBetween(lastFrom, lastTo);

                writeCsvLine(os, new String[]{
                        "Дата/время", "Чек ID", "Товар", "Кол-во",
                        "Сумма строки", "Продавец", "Логин", "Чек итого"
                });

                SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

                for (SaleRow r : rows) {
                    String seller = (r.firstName != null && !r.firstName.isEmpty())
                            ? r.firstName + (r.lastName == null ? "" : " " + r.lastName)
                            : (r.login != null ? r.login : (r.sellerId == null ? "-" : String.valueOf(r.sellerId)));

                    writeCsvLine(os, new String[]{
                            df.format(r.saleDate),
                            String.valueOf(r.saleId),
                            nullToEmpty(r.productName),
                            String.valueOf(r.quantity),
                            trim(r.subtotal),
                            seller,
                            nullToEmpty(r.login),
                            trim(r.saleTotal)
                    });
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
            sb.append(csvEscape(cells[i] == null ? "" : cells[i]));
        }
        sb.append('\n');
        writeRaw(os, sb.toString());
    }

    private void writeRaw(OutputStream os, String s) throws Exception {
        os.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private String csvEscape(String s) {
        String v = s.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private static String trim(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    @Override
    public void onSaleClick(int saleId, @Nullable Date saleDate) {
        io.execute(() -> {
            try {
                List<OrderWithItems.OrderLine> lines = saleDao.findLinesForOrder(saleId);
                runOnUiThread(() -> showSaleLinesDialog(saleId, saleDate, lines));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Ошибка позиций: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void showSaleLinesDialog(int saleId,
                                     @Nullable Date saleDate,
                                     List<OrderWithItems.OrderLine> lines) {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_order_lines, null, false);

        androidx.recyclerview.widget.RecyclerView rv =
                dialogView.findViewById(R.id.rvOrderLines);
        rv.setLayoutManager(new LinearLayoutManager(this));

        OrderLinesAdapter linesAdapter = new OrderLinesAdapter();
        rv.setAdapter(linesAdapter);
        linesAdapter.submit(lines);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

        String title = "Чек #" + saleId;
        if (saleDate != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            title += " (" + df.format(saleDate) + ")";
        }
        tvTitle.setText(title);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_CRM_Dialog)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }
}
