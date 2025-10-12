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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import chocano.pa3.org.R;
import chocano.pa3.org.models.Contact;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> implements Filterable {

    private List<Contact> data = new ArrayList<>();
    private List<Contact> filtered = new ArrayList<>();

    private OnContactActionListener listener;

    public ContactAdapter() {}

    public void setOnContactActionListener(OnContactActionListener l) {
        this.listener = l;
    }

    public void setData(List<Contact> list) {
        data = (list == null) ? new ArrayList<>() : list;
        filtered = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_contact, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Contact c = filtered.get(position);
        h.title.setText(c.name);
        h.subtitle.setText(c.phone);

        // SOLO botones tienen acción
        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(c);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(c);
        });

        // Anular cualquier click en el item completo (por si el layout lo define)
        h.itemView.setOnClickListener(null);
        h.itemView.setClickable(false);
        h.itemView.setFocusable(false);
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Contact> result = new ArrayList<>();
                if (TextUtils.isEmpty(constraint)) {
                    result.addAll(data);
                } else {
                    String term = constraint.toString().toLowerCase(Locale.ROOT);
                    for (Contact c : data) {
                        String name = c.name != null ? c.name.toLowerCase(Locale.ROOT) : "";
                        String phone = c.phone != null ? c.phone.toLowerCase(Locale.ROOT) : "";
                        if (name.contains(term) || phone.contains(term)) {
                            result.add(c);
                        }
                    }
                }
                FilterResults fr = new FilterResults();
                fr.values = result;
                return fr;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered = (List<Contact>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageButton btnEdit, btnDelete;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    public interface OnContactActionListener {
        void onEdit(Contact contact);
        void onDelete(Contact contact);
    }
}
