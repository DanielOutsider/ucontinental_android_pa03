package chocano.pa3.org;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;

import chocano.pa3.org.R;
import chocano.pa3.org.adapters.ProductAdapter;
import chocano.pa3.org.models.Product;

public class ProductsActivity extends AppCompatActivity {

    private DatabaseReference productsRef; private ValueEventListener listener;
    private ProductAdapter adapter; private List<Product> current = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_products); setTitle("Productos");
        Toolbar tb = findViewById(R.id.toolbar_products); setSupportActionBar(tb);
        if (getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tb.setNavigationOnClickListener(v-> onBackPressed());

        String uid = FirebaseAuth.getInstance().getUid();
        productsRef = FirebaseDatabase.getInstance().getReference("products").child(uid);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rv.setAdapter(adapter);
        adapter.setListener(new ProductAdapter.OnProductActionListener(){
            @Override public void onEdit(Product p){ showDialog(p); }
            @Override public void onDelete(Product p){
                new AlertDialog.Builder(ProductsActivity.this)
                        .setTitle("Eliminar producto")
                        .setMessage("¿Eliminar " + p.name + "?")
                        .setPositiveButton("Eliminar", (d,w)-> productsRef.child(p.id).removeValue())
                        .setNegativeButton("Cancelar", null).show();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showDialog(null));

        SearchView search = findViewById(R.id.searchView);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override public boolean onQueryTextSubmit(String q){ return false; }
            @Override public boolean onQueryTextChange(String t){ adapter.getFilter().filter(t); return true; }
        });
    }

    @Override protected void onStart(){
        super.onStart();
        listener = new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot snapshot){
                List<Product> list = new ArrayList<>();
                for (DataSnapshot s: snapshot.getChildren()){
                    Product p = s.getValue(Product.class); if (p!=null){ p.id = s.getKey(); list.add(p);} }
                current = list; adapter.setData(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error){}
        }; productsRef.addValueEventListener(listener);
    }

    @Override protected void onStop(){ super.onStop(); if (listener!=null) productsRef.removeEventListener(listener); }

    private void showDialog(@Nullable Product existing){
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null, false);
        EditText edtName = view.findViewById(R.id.edtName);
        EditText edtStock = view.findViewById(R.id.edtStock);
        EditText edtPrice = view.findViewById(R.id.edtPrice);

        if (existing != null) {
            edtName.setText(existing.name);
            edtStock.setText(String.valueOf(existing.stock));
            edtPrice.setText(String.valueOf(existing.price));
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nuevo producto" : "Editar producto")
                .setView(view)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    String stockStr = edtStock.getText().toString().trim();
                    String priceStr = edtPrice.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(stockStr) || TextUtils.isEmpty(priceStr)) {
                        Toast.makeText(this, "Todos los campos son obligatorios (incluido Stock)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int stock;
                    double price;
                    try {
                        stock = Integer.parseInt(stockStr);
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String key = existing == null ? productsRef.push().getKey() : existing.id;
                    productsRef.child(key).setValue(new Product(key, name, stock, price));
                })
                .show();
    }

    private void toast(String s){ android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show(); }
}
