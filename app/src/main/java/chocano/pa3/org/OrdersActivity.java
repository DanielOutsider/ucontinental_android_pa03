package chocano.pa3.org;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import chocano.pa3.org.R;
import chocano.pa3.org.adapters.OrderAdapter;
import chocano.pa3.org.adapters.ProductPickAdapter;

import chocano.pa3.org.models.Contact;
import chocano.pa3.org.models.Order;
import chocano.pa3.org.models.OrderItem;
import chocano.pa3.org.models.Product;


public class OrdersActivity extends AppCompatActivity {

    private DatabaseReference ordersRef, contactsRef;
    private ValueEventListener ordersListener, contactsListener;
    private OrderAdapter adapter;

    // cache de contactos para mostrar nombres en el diálogo
    private final Map<String, Contact> contactsMap = new HashMap<>();

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es","PE")); // S/.

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        setTitle("Pedidos");

        Toolbar toolbar = findViewById(R.id.toolbar_orders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String uid = FirebaseAuth.getInstance().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(uid);
        contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child(uid);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        rv.setAdapter(adapter);

        adapter.setListener(new OrderAdapter.OnOrderActionListener() {
            @Override public void onEdit(Order order) { showOrderDialog(order); }
            @Override public void onDelete(Order order) {
                new AlertDialog.Builder(OrdersActivity.this)
                        .setTitle("Eliminar pedido")
                        .setMessage("¿Deseas eliminar este pedido?")
                        .setPositiveButton("Eliminar", (d,w)-> ordersRef.child(order.id).removeValue())
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showOrderDialog(null));
    }

    @Override protected void onStart(){
        super.onStart();
        contactsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactsMap.clear();
                for (DataSnapshot s: snapshot.getChildren()){
                    Contact c = s.getValue(Contact.class);
                    if (c!=null) c.id = s.getKey();
                    if (c!=null) contactsMap.put(c.id, c);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        contactsRef.addValueEventListener(contactsListener);

        ordersListener = new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> list = new ArrayList<>();
                for (DataSnapshot s: snapshot.getChildren()){
                    Order o = s.getValue(Order.class);
                    if (o!=null){ o.id = s.getKey(); list.add(o);} }
                // ordenar por fecha desc
                Collections.sort(list, (a,b)-> Long.compare(b.createdAt, a.createdAt));
                adapter.setData(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        ordersRef.addValueEventListener(ordersListener);
    }

    @Override protected void onStop(){
        super.onStop();
        if (ordersListener!=null) ordersRef.removeEventListener(ordersListener);
        if (contactsListener!=null) contactsRef.removeEventListener(contactsListener);
    }

    private void showOrderDialog(@Nullable Order existing){
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_order_products, null, false);
        Spinner spContact = view.findViewById(R.id.spContact);
        Spinner spStatus  = view.findViewById(R.id.spStatus);
        EditText edtNotes = view.findViewById(R.id.edtNotes);
        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        TextView txtTotal = view.findViewById(R.id.txtTotal);

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es","PE"));

        // contactos
        final List<String> keys = new ArrayList<>(contactsMap.keySet());
        final List<String> labels = new ArrayList<>();
        for(String k: keys){ labels.add(contactsMap.get(k).name); }
        spContact.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));

        // status
        String[] statuses = new String[]{"pending","paid","cancelled"};
        spStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses));

        // cargar productos desde /products/{uid}
        List<Product> products = new ArrayList<>();
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products").child(uid);

        ProductPickAdapter pickAdapter = new ProductPickAdapter();
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(pickAdapter);

        ValueEventListener tmp = new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot snapshot){
                products.clear();
                for(DataSnapshot s : snapshot.getChildren()){
                    Product p = s.getValue(Product.class);
                    if (p!=null){ p.id = s.getKey(); products.add(p);} }
                pickAdapter.setProducts(products);
                updateTotal();
            }
            @Override public void onCancelled(@NonNull DatabaseError error){}
            private void updateTotal(){
                double sum = 0; for(ProductPickAdapter.Pick k: pickAdapter.getSelection()){ if (k.checked) sum += (k.qty * k.product.price); }
                txtTotal.setText("Total: " + currency.format(sum));
            }
        };
        productsRef.addListenerForSingleValueEvent(tmp);

        // Observa cambios locales de qty/check para actualizar total (simple: al cerrar, recalcula)

        // Si edición, precarga
        if (existing != null){
            if (existing.contactId != null){ int idx = Math.max(0, keys.indexOf(existing.contactId)); spContact.setSelection(idx); }
            edtNotes.setText(existing.notes==null?"":existing.notes);
            for (int i=0;i<statuses.length;i++){ if (statuses[i].equals(existing.status)){ spStatus.setSelection(i); break; } }
        }

        new AlertDialog.Builder(this)
                .setTitle(existing==null?"Nuevo pedido":"Editar pedido")
                .setView(view)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d,w)->{
                    if (keys.isEmpty()) { toast("Primero crea un contacto"); return; }
                    String contactId = keys.get(Math.max(0, spContact.getSelectedItemPosition()));
                    String status = spStatus.getSelectedItem().toString();
                    String notes = edtNotes.getText().toString().trim();

                    // construir Order con snapshot de precios
                    Order o = (existing==null? new Order(): existing);
                    o.contactId = contactId; o.status = status; o.notes = notes;
                    if (existing==null) o.createdAt = System.currentTimeMillis();
                    o.items = new java.util.HashMap<>();
                    double total = 0;
                    int i = 1;
                    for(ProductPickAdapter.Pick k: pickAdapter.getSelection()){
                        if (!k.checked) continue; // solo seleccionados
                        String key = "item"+ (i++);
                        // snapshot del precio actual
                        o.items.put(key, new chocano.pa3.org.models.OrderItem(k.product.name, k.qty, k.product.price));
                        total += k.qty * k.product.price;
                    }
                    o.total = total;
                    String key = (existing==null? ordersRef.push().getKey(): o.id);
                    o.id = key; ordersRef.child(key).setValue(o);
                })
                .show();
    }

    private void toast(String s){ android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show(); }
}