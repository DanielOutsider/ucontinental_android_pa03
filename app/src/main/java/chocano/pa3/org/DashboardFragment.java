package chocano.pa3.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import chocano.pa3.org.models.Order;
import chocano.pa3.org.models.OrderItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvContactsCount, tvProductsCount, tvOrdersPending, tvOrdersPaid, tvOrdersToday, tvRevenueToday;

    // Referencias a los nodos de Firebase
    private DatabaseReference
            contactsRef, productsRef, ordersRef;

    // Listeners para escuchar cambios en tiempo real
    private ValueEventListener contactsListener, productsListener, ordersListener;

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Cards
        CardView cardContacts = v.findViewById(R.id.card_contacts);
        CardView cardProducts = v.findViewById(R.id.card_products);
        CardView cardOrders   = v.findViewById(R.id.card_orders);

        cardContacts.setOnClickListener(view ->
                startActivity(new android.content.Intent(requireContext(), ContactsActivity.class)));

        cardProducts.setOnClickListener(view ->
                startActivity(new android.content.Intent(requireContext(), ProductsActivity.class)));

        cardOrders.setOnClickListener(view ->
                startActivity(new android.content.Intent(requireContext(), OrdersActivity.class)));


        // Bienvenida
        TextView subtitle = v.findViewById(R.id.txtSubtitle);
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) subtitle.setText("Hola, " + (user.getEmail() != null ? user.getEmail() : "usuario"));

        // Firebase Refs
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            DatabaseReference root = FirebaseDatabase.getInstance().getReference();
            contactsRef = root.child("contacts").child(uid);
            productsRef = root.child("products").child(uid);
            ordersRef   = root.child("orders").child(uid);
        }

        // Vistas métricas
        tvContactsCount = v.findViewById(R.id.tvContactsCount);
        tvProductsCount = v.findViewById(R.id.tvProductsCount);
        tvOrdersPending = v.findViewById(R.id.tvOrdersPending);
        tvOrdersPaid    = v.findViewById(R.id.tvOrdersPaid);
        tvOrdersToday   = v.findViewById(R.id.tvOrdersToday);
        tvRevenueToday  = v.findViewById(R.id.tvRevenueToday);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // addValueEventListener se suscribe a cambios en tiempo real.
        if (contactsRef != null) {
            contactsListener = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tvContactsCount.setText("Contactos: " + snapshot.getChildrenCount());
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
            contactsRef.addValueEventListener(contactsListener);
        }

        // addValueEventListener se suscribe a cambios en tiempo real.
        if (productsRef != null) {
            productsListener = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tvProductsCount.setText("Productos: " + snapshot.getChildrenCount());
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
            productsRef.addValueEventListener(productsListener);
        }

        // addValueEventListener se suscribe a cambios en tiempo real.
        if (ordersRef != null) {
            ordersListener = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long pending = 0, paid = 0, todayCount = 0;
                    double todayRevenue = 0.0;

                    long[] todayRange = dayRangeMillis();
                    long startDay = todayRange[0];
                    long endDay   = todayRange[1];

                    for (DataSnapshot s : snapshot.getChildren()) {
                        Order o = s.getValue(Order.class);
                        if (o == null) continue;

                        String status = o.status == null ? "pendiente" : o.status;
                        if ("pendiente".equals(status)) pending++;
                        else if ("pagado".equals(status)) paid++;

                        long ts = o.createdAt;
                        if (ts >= startDay && ts <= endDay) {
                            todayCount++;
                            todayRevenue += o.total;
                        }
                    }

                    tvOrdersPending.setText("Pedidos pendientes: " + pending);
                    tvOrdersPaid.setText("Pedidos pagados: " + paid);
                    tvOrdersToday.setText("Pedidos hoy: " + todayCount);
                    NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
                    currency.setMaximumFractionDigits(2);
                    tvRevenueToday.setText("Ingresos hoy: " + currency.format(todayRevenue));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
            ordersRef.addValueEventListener(ordersListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (contactsRef != null && contactsListener != null) contactsRef.removeEventListener(contactsListener);
        if (productsRef != null && productsListener != null) productsRef.removeEventListener(productsListener);
        if (ordersRef   != null && ordersListener   != null) ordersRef.removeEventListener(ordersListener);
    }

    // Rango de hoy [00:00, 23:59:59.999] en millis (zona del dispositivo)
    // permite calcular si los pedidos fueron de hoy
    private long[] dayRangeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }

}
