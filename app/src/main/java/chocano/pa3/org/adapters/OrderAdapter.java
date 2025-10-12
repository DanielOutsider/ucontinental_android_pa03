package chocano.pa3.org.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import chocano.pa3.org.R;
import chocano.pa3.org.models.Order;


public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {


    public interface OnOrderActionListener {
        void onEdit(Order order);
        void onDelete(Order order);
    }


    private final List<Order> data = new ArrayList<>();
    private OnOrderActionListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es","PE")); // S/.


    public OrderAdapter() {}


    public void setListener(OnOrderActionListener l){ this.listener = l; }


    public void setData(List<Order> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }


    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_order, parent, false);
        return new VH(v);
    }


    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Order o = data.get(pos);
        h.title.setText("Pedido · " + (o.status == null ? "pending" : o.status));
        h.subtitle.setText(sdf.format(new Date(o.createdAt)) + (o.notes!=null && !o.notes.isEmpty()? " · " + o.notes: ""));
        h.amount.setText(currency.format(o.total));


        h.itemView.setOnClickListener(v -> { if (listener!=null) listener.onEdit(o); });
        h.btnEdit.setOnClickListener(v -> { if (listener!=null) listener.onEdit(o); });
        h.btnDelete.setOnClickListener(v -> { if (listener!=null) listener.onDelete(o); });
    }


    @Override public int getItemCount(){ return data.size(); }


    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle, amount; ImageButton btnEdit, btnDelete;
        VH(View v){
            super(v);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
            amount = v.findViewById(R.id.amount);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}