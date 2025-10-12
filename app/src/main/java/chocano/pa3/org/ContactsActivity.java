package chocano.pa3.org;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import chocano.pa3.org.adapters.ContactAdapter;
import chocano.pa3.org.models.Contact;

public class ContactsActivity extends AppCompatActivity {

    private DatabaseReference contactsRef;
    private ValueEventListener listener;

    // Adaptador del RecyclerView que muestra la lista de contactos

    private ContactAdapter adapter;

    // Lista de contactos
    private List<Contact> currentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        setTitle("Contactos");

        // Toolbar con botón atrás
        Toolbar toolbar = findViewById(R.id.toolbar_contacts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Firebase
        String uid = FirebaseAuth.getInstance().getUid();
        contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child(uid);

        // Lista que mantiene los contactos actuales en memoria
        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter();
        rv.setAdapter(adapter);

        // Listener de acciones del adapter
        adapter.setOnContactActionListener(new ContactAdapter.OnContactActionListener() {
            @Override
            public void onEdit(Contact contact) {
                showContactDialog(contact); // Editar contacto
            }

            @Override
            public void onDelete(Contact contact) {
                new AlertDialog.Builder(ContactsActivity.this)
                        .setTitle("Eliminar contacto")
                        .setMessage("¿Deseas eliminar a " + contact.name + "?")
                        .setPositiveButton("Eliminar", (d, w) ->
                                contactsRef.child(contact.id).removeValue())
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        // Botón flotante para nuevo contacto
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showContactDialog(null));

        // Búsqueda
        SearchView search = findViewById(R.id.searchView);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Contact> list = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Contact c = s.getValue(Contact.class);
                    if (c != null) {
                        c.id = s.getKey();
                        list.add(c);
                    }
                }
                currentList = list;
                adapter.setData(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        contactsRef.addValueEventListener(listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) contactsRef.removeEventListener(listener);
    }

    private void showContactDialog(@Nullable Contact existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null, false);
        EditText name = view.findViewById(R.id.edtName);
        EditText phone = view.findViewById(R.id.edtPhone);

        if (existing != null) {
            name.setText(existing.name);
            phone.setText(existing.phone);
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nuevo contacto" : "Editar contacto")
                .setView(view)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String p = phone.getText().toString().trim();
                    if (TextUtils.isEmpty(n) || TextUtils.isEmpty(p)) {
                        Toast.makeText(this, "Nombre y teléfono requeridos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validar contacto duplicado
                    for (Contact c1 : currentList) {
                        if (existing == null || !c1.id.equals(existing.id)) {
                            if (c1.name.equalsIgnoreCase(n) || c1.phone.equals(p)) {
                                Toast.makeText(this, "Contacto duplicado", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    String key = existing == null ? contactsRef.push().getKey() : existing.id;
                    contactsRef.child(key).setValue(new Contact(key, n, p));
                })
                .show();
    }
}
