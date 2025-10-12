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

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

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

        MaterialAutoCompleteTextView autoContact = view.findViewById(R.id.autoContact);
        MaterialAutoCompleteTextView autoStatus  = view.findViewById(R.id.autoStatus);
        TextView txtTotal = view.findViewById(R.id.txtTotal);
        EditText edtNotes = view.findViewById(R.id.edtNotes);
        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es","PE"));

        // ---- clientes (autocompletar) ----
        // contactsMap: Map<String, Contact> que ya usas en OrdersActivity (asegúrate de tenerlo cargado)
        final List<String> contactKeys = new ArrayList<>(contactsMap.keySet());
        final List<String> contactLabels = new ArrayList<>();
        for(String k: contactKeys) contactLabels.add(contactsMap.get(k).name);

        ArrayAdapter<String> contactsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactLabels);
        autoContact.setAdapter(contactsAdapter);

        // Si estás editando, precarga el nombre del contacto
        if (existing != null && existing.contactId != null) {
            int idx = Math.max(0, contactKeys.indexOf(existing.contactId));
            if (idx >= 0 && idx < contactLabels.size()) autoContact.setText(contactLabels.get(idx), false);
        }

        // Al guardar recuperaremos el contactId resolviendo el nombre seleccionado.

        // ---- estado (dropdown) ----
        String[] statuses = new String[]{"pendiente", "pagado", "cancelado"};
        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses);
        autoStatus.setAdapter(statusAdapter);
        if (existing != null && existing.status != null) autoStatus.setText(existing.status, false);
        else autoStatus.setText(statuses[0], false);

        // ---- productos (opcionales) ----
        ProductPickAdapter pickAdapter = new ProductPickAdapter();
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(pickAdapter);

        // carga productos una vez
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products").child(uid);
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> prods = new ArrayList<>();
                for (DataSnapshot s: snapshot.getChildren()){
                    Product p = s.getValue(Product.class);
                    if (p!=null){ p.id = s.getKey(); prods.add(p); }
                }
                pickAdapter.setProducts(prods);

                // Si editas, marca los que están en el pedido
                if (existing != null && existing.items != null) {
                    for (ProductPickAdapter.Pick k : pickAdapter.getSelection()) {
                        for (OrderItem oi : existing.items.values()) {
                            if (oi.name != null && oi.name.equalsIgnoreCase(k.product.name)) {
                                k.checked = true;
                                k.qty = Math.max(1, oi.qty);
                            }
                        }
                    }
                    pickAdapter.notifyDataSetChanged();
                }
                updateTotal();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
            private void updateTotal(){
                double sum = 0;
                for (ProductPickAdapter.Pick k: pickAdapter.getSelection()){
                    if (k.checked) sum += k.qty * k.product.price;
                }
                txtTotal.setText("Total: " + currency.format(sum));
            }
        });

        // recalcular total en vivo
        pickAdapter.setOnChangeListener(() -> {
            double sum = 0;
            for (ProductPickAdapter.Pick k: pickAdapter.getSelection()){
                if (k.checked) sum += k.qty * k.product.price;
            }
            txtTotal.setText("Total: " + currency.format(sum));
        });

        // Precargar notas
        if (existing != null) edtNotes.setText(existing.notes == null ? "" : existing.notes);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(existing==null ? "Nuevo pedido" : "Editar pedido")
                .setView(view)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d,w)->{
                    // validar cliente seleccionado
                    String chosenName = autoContact.getText()==null ? "" : autoContact.getText().toString().trim();
                    int idx = contactLabels.indexOf(chosenName);
                    if (idx < 0) { toast("Selecciona un cliente"); return; }
                    String contactId = contactKeys.get(idx);

                    String status = autoStatus.getText()==null ? "pending" : autoStatus.getText().toString();
                    String notes  = edtNotes.getText()==null ? "" : edtNotes.getText().toString().trim();

                    Order o = (existing==null) ? new Order() : existing;
                    if (existing==null) o.createdAt = System.currentTimeMillis();
                    o.contactId = contactId;
                    o.status = status;
                    o.notes = notes;

                    // construir items (opcional — si no hay ninguno seleccionado, queda vacío)
                    o.items = new java.util.HashMap<>();
                    double total = 0;
                    int i = 1;
                    for (ProductPickAdapter.Pick k: pickAdapter.getSelection()){
                        if (!k.checked) continue;
                        String ikey = "item"+(i++);
                        o.items.put(ikey, new OrderItem(k.product.name, k.qty, k.product.price));
                        total += k.qty * k.product.price;
                    }
                    o.total = total;

                    String key = (existing==null) ? ordersRef.push().getKey() : o.id;
                    o.id = key;
                    ordersRef.child(key).setValue(o);
                })
                .show();
    }

    private void toast(String s){ android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show(); }
}