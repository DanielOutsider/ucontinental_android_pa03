package chocano.pa3.org.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import chocano.pa3.org.R;
import chocano.pa3.org.models.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> implements Filterable {

    // 🔹 Interfaz para acciones del usuario
    public interface OnProductActionListener {
        void onEdit(Product p);
        void onDelete(Product p);
    }

    private final List<Product> data = new ArrayList<>();
    private final List<Product> filtered = new ArrayList<>();
    private OnProductActionListener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

    // 🔹 Asignar listener desde la Activity
    public void setListener(OnProductActionListener l) {
        this.listener = l;
    }

    // 🔹 Actualizar lista
    public void setData(List<Product> list) {
        data.clear();
        filtered.clear();
        if (list != null) {
            data.addAll(list);
            filtered.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Product p = filtered.get(pos);

        // Mostrar nombre y detalles
        h.title.setText(p.name);
        String stockTxt = "Stock: " + p.stock;
        h.subtitle.setText(stockTxt);

        h.price.setText("Precio: " + currency.format(p.price));

        // Acciones
        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(p);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(p);
        });
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle, price;
        ImageButton btnEdit, btnDelete;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.txtName);
            subtitle = v.findViewById(R.id.txtStock);
            price = v.findViewById(R.id.txtPrice);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    // 🔹 Búsqueda y filtrado
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence c) {
                List<Product> res = new ArrayList<>();
                if (TextUtils.isEmpty(c)) {
                    res.addAll(data);
                } else {
                    String q = c.toString().toLowerCase(Locale.ROOT);
                    for (Product p : data) {
                        if ((p.name != null && p.name.toLowerCase(Locale.ROOT).contains(q)) ||
                                String.valueOf(p.stock).contains(q)) {
                            res.add(p);
                        }
                    }
                }
                FilterResults fr = new FilterResults();
                fr.values = res;
                return fr;
            }

            @Override
            protected void publishResults(CharSequence c, FilterResults r) {
                filtered.clear();
                filtered.addAll((List<Product>) r.values);
                notifyDataSetChanged();
            }
        };
    }
}
