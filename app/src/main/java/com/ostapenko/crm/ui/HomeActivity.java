package com.ostapenko.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.ostapenko.crm.R;
import com.ostapenko.crm.auth.Session;

public class HomeActivity extends AppCompatActivity {

    private Session session;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new Session(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button btnProducts = findViewById(R.id.btnProducts);
        Button btnSales = findViewById(R.id.btnSales);
        Button btnEmployees = findViewById(R.id.btnEmployees);
        Button btnInventory = findViewById(R.id.btnInventory);
        Button btnLogout = findViewById(R.id.btnLogout);

        if ("admin".equalsIgnoreCase(session.role())) {
            btnEmployees.setVisibility(View.VISIBLE);
            btnEmployees.setOnClickListener(v ->
                    startActivity(new Intent(this, EmployeesActivity.class)));

            btnInventory.setVisibility(View.VISIBLE);
            btnInventory.setOnClickListener(v ->
                    startActivity(new Intent(this, InventoryActivity.class)));
        } else {
            btnEmployees.setVisibility(View.GONE);
            btnInventory.setVisibility(View.GONE);
        }


        btnProducts.setOnClickListener(v -> startActivity(new Intent(this, ProductsActivity.class)));
        btnSales.setOnClickListener(v -> startActivity(new Intent(this, SalesActivity.class)));

        // доступ к складу только для admin
        if ("admin".equalsIgnoreCase(session.role())) {
            btnInventory.setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
            btnInventory.setVisibility(View.VISIBLE);
        } else {
            btnInventory.setVisibility(View.GONE);
        }

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
