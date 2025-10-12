package chocano.pa3.org.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class ProductPickAdapter extends RecyclerView.Adapter<ProductPickAdapter.VH> {

    public static class Pick {
        public Product product;
        public boolean checked;
        public int qty = 1;
    }

    private final List<Pick> picks = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

    public void setProducts(List<Product> products) {
        picks.clear();
        if (products != null) {
            for (Product p : products) {
                Pick k = new Pick();
                k.product = p;
                k.checked = false;
                k.qty = 1;
                picks.add(k);
            }
        }
        notifyDataSetChanged();
    }

    public List<Pick> getSelection() {
        return picks;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_pick_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Pick k = picks.get(pos);
        h.chk.setOnCheckedChangeListener(null);
        h.chk.setText(k.product.name);
        h.chk.setChecked(k.checked);
        h.price.setText(currency.format(k.product.price));
        h.qty.setText(String.valueOf(k.qty));

        h.chk.setOnCheckedChangeListener((buttonView, isChecked) -> k.checked = isChecked);
        h.btnMinus.setOnClickListener(v -> {
            int q = Math.max(1, k.qty - 1);
            k.qty = q;
            h.qty.setText(String.valueOf(q));
        });
        h.btnPlus.setOnClickListener(v -> {
            int q = Math.min(999, k.qty + 1);
            k.qty = q;
            h.qty.setText(String.valueOf(q));
        });
    }

    @Override
    public int getItemCount() {
        return picks.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox chk;
        TextView price;
        EditText qty;
        ImageButton btnMinus, btnPlus;

        VH(View v) {
            super(v);
            chk = v.findViewById(R.id.chk);
            price = v.findViewById(R.id.txtPrice);
            qty = v.findViewById(R.id.edtQty);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);
        }
    }
}
