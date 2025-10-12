package chocano.pa3.org;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;


public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    private DrawerLayout drawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


// Toggle hamburguesa
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open, R.string.nav_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


// Carga inicial: Dashboard
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
            setTitle(R.string.menu_dashboard);
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new DashboardFragment())
                    .commit();
            setTitle(R.string.menu_dashboard);

        } else if (id == R.id.nav_contacts) {
            startActivity(new Intent(this, ContactsActivity.class));
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(this, ProductsActivity.class));
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, OrdersActivity.class));

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.nav_logout) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}