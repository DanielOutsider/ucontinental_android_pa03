package chocano.pa3.org;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// 🔹 Realtime Database
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etMessage;
    private MaterialButton btnSave, btnShow;
    private RecyclerView rvList;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference contactsRef;

    private ContactAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Mostrar ActionBar sólo aquí
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
            getSupportActionBar().setTitle("Inicio");
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { goToLogin(); return; }

        // RTDB: referencia a contacts table
        contactsRef = FirebaseDatabase.getInstance().getReference("contacts");

        // controles
        etName    = findViewById(R.id.etName);
        etEmail   = findViewById(R.id.etEmail);
        etMessage = findViewById(R.id.etMessage);
        btnSave   = findViewById(R.id.btnSave);
        btnShow   = findViewById(R.id.btnShow);
        rvList    = findViewById(R.id.rvList);

        if (user.getEmail() != null) etEmail.setText(user.getEmail());

        // Adaptador seteado a la lista
        adapter = new ContactAdapter();
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvList.setAdapter(adapter);

        // Guardar
        btnSave.setOnClickListener(v -> save());
        // Mostrar lista
        btnShow.setOnClickListener(v -> { rvList.setVisibility(View.VISIBLE); loadOnce(); });
    }

    // Menú: Cerrar sesión
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }
    // Manejar selección del logout
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Metodo que redirecciona al login y limpiar historial
    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    //  Guardar nuevo contacto
    private void save() {
        String nombre  = text(etName);
        String email   = text(etEmail);
        String mensaje = text(etMessage);

        if (TextUtils.isEmpty(nombre))  { etName.setError("Requerido"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Correo inválido"); return; }
        if (TextUtils.isEmpty(mensaje)) { etMessage.setError("Requerido"); return; }

        // Usamos push para crear un ID único
        String key = contactsRef.push().getKey();
        if (key == null) { Toast.makeText(this, "No se pudo generar ID", Toast.LENGTH_LONG).show(); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("email", email);
        data.put("mensaje", mensaje);
        data.put("createdAt", ServerValue.TIMESTAMP); // timestamp del servidor

        btnSave.setEnabled(false);
        contactsRef.child(key).setValue(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Guardado ✅", Toast.LENGTH_SHORT).show();
                    etName.setText(""); etMessage.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show())
                .addOnCompleteListener(t -> btnSave.setEnabled(true));
    }

    //  metodo para Listar
    private void loadOnce() {
        // Leemos contacts y ordena por fecha de creacion DESC
        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Contact> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Contact c = child.getValue(Contact.class);
                    if (c != null) list.add(c);
                }
                // Orden descendente por fecha de creacion
                Collections.sort(list, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                adapter.setItems(list);
                Toast.makeText(HomeActivity.this, "Registros: " + list.size(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Error al cargar: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    //  Modelo Adaptador
    public static class Contact {
        public String nombre;
        public String email;
        public String mensaje;
        public long createdAt;

        public Contact() {} // requerido por RTDB
    }

    // RecyclerView Adapter y ViewHolder
    static class ContactAdapter extends RecyclerView.Adapter<ContactVH> {
        // Lista de items
        private final List<Contact> items = new ArrayList<>();

        // Actualiza la lista y notifica cambios
        void setItems(List<Contact> list) { items.clear(); items.addAll(list); notifyDataSetChanged(); }

        // item_row.xml
        @NonNull @Override
        public ContactVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
            return new ContactVH(view);
        }

        // Bind de datos
        @Override
        public void onBindViewHolder(@NonNull ContactVH h, int pos) {
            Contact c = items.get(pos);
            h.tvName.setText(c.nombre);
            h.tvEmail.setText(c.email);
            h.tvMessage.setText(c.mensaje);
        }

        // retorna el conteo de la cantidad de items
        @Override public int getItemCount() { return items.size(); }
    }

    // ViewHolder
    static class ContactVH extends RecyclerView.ViewHolder {
        final android.widget.TextView tvName, tvEmail, tvMessage;
        ContactVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvEmail = v.findViewById(R.id.tvEmail);
            tvMessage = v.findViewById(R.id.tvMessage);
        }
    }
}
