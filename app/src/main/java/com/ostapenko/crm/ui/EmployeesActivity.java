package com.ostapenko.crm.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ostapenko.crm.R;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.UserDao;
import com.ostapenko.crm.entity.User;
import com.ostapenko.crm.security.Hashing;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.ostapenko.crm.auth.Session;

public class EmployeesActivity extends AppCompatActivity {

    private UserDao userDao;
    private EmployeeAdapter adapter;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Session session = new Session(this);
        if (!"admin".equalsIgnoreCase(session.role())) {
            Toast.makeText(this, "Доступ запрещён: только для администратора", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_employees);

        userDao = AppDatabase.getInstance(getApplicationContext()).userDao();

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvEmployees);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter(this::onClickUser);
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.fabAdd).setOnClickListener(v -> showEditDialog(null));
        load();
    }

    private void load() {
        io.execute(() -> {
            List<User> all = userDao.findAll();
            runOnUiThread(() -> adapter.submit(all));
        });
    }

    private void onClickUser(User u) {
        showEditDialog(u);
    }

    private void showEditDialog(@Nullable User existing) {
        // ВАЖНО: инфлейтим через контекст билдера
        var builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_employee_edit, null, false);

        EditText etFirst = view.findViewById(R.id.etFirst);
        EditText etLast = view.findViewById(R.id.etLast);
        EditText etLogin = view.findViewById(R.id.etLogin);
        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etRole = view.findViewById(R.id.etRole);
        EditText etActive = view.findViewById(R.id.etActive);

        if (existing != null) {
            etFirst.setText(existing.firstName);
            etLast.setText(existing.lastName);
            etLogin.setText(existing.login);
            etLogin.setEnabled(false);
            etRole.setText(existing.role);
            etActive.setText(existing.active ? "1" : "0");
        } else {
            etActive.setText("1");
            etRole.setText("employee");
        }

        builder
                .setTitle(existing == null ? "Новый сотрудник" : "Редактировать сотрудника")
                .setView(view)
                .setPositiveButton("Сохранить", (d,w) -> {
                    String first = etFirst.getText().toString().trim();
                    String last  = etLast.getText().toString().trim();
                    String login = etLogin.getText().toString().trim();
                    String pass  = etPassword.getText().toString();
                    String role  = etRole.getText().toString().trim();
                    boolean active = "1".equals(etActive.getText().toString().trim());

                    if (existing == null && (login.isEmpty() || pass.isEmpty())) {
                        Toast.makeText(this, "Логин/пароль обязателен", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!"admin".equalsIgnoreCase(role) && !"employee".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Роль: admin или employee", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    io.execute(() -> {
                        try {
                            if (existing == null) {
                                User u = new User();
                                u.login = login;
                                u.passwordHash = Hashing.sha256(pass);
                                u.role = role.toLowerCase();
                                u.active = active;
                                u.firstName = first;
                                u.lastName = last;
                                userDao.insert(u);
                            } else {
                                existing.firstName = first;
                                existing.lastName = last;
                                existing.role = role.toLowerCase();
                                existing.active = active;
                                if (!pass.isEmpty()) {
                                    existing.passwordHash = Hashing.sha256(pass);
                                }
                                userDao.update(existing);
                            }
                            load();
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
