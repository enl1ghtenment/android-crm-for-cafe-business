package com.ostapenko.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ostapenko.crm.R;
import com.ostapenko.crm.auth.Session;
import com.ostapenko.crm.db.AppDatabase;
import com.ostapenko.crm.db.dao.UserDao;
import com.ostapenko.crm.entity.User;
import com.ostapenko.crm.security.Hashing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private UserDao userDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Session session;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new Session(this);
        if (session.isLoggedIn()) {
            goToHome();
            return;
        }

        userDao = AppDatabase.getInstance(getApplicationContext()).userDao();

        io.execute(() -> {
            if (userDao.countActiveAdmins() == 0) {
                User u = new User();
                u.login = "admin";
                u.passwordHash = Hashing.sha256("admin");
                u.role = "admin";
                u.active = true;
                userDao.insert(u);
            }
        });

        EditText etLogin = findViewById(R.id.etLogin);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();
            if (login.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show(); return;
            }
            io.execute(() -> {
                User u = userDao.findByLogin(login);
                String hash = Hashing.sha256(pass);
                if (u != null && u.active && hash.equals(u.passwordHash)) {
                    session.login(u.id, u.role);
                    runOnUiThread(this::goToHome);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
