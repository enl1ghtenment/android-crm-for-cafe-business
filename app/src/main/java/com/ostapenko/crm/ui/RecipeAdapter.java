package com.ostapenko.crm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ostapenko.crm.R;
import com.ostapenko.crm.entity.ProductIngredient;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.VH> {

    public interface Listener { void onDelete(ProductIngredient row); }

    private final List<ProductIngredient> data = new ArrayList<>();
    private final Listener listener;

    public RecipeAdapter(Listener l) { this.listener = l; }

    public void submit(List<ProductIngredient> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ProductIngredient r = data.get(pos);
        h.tvIngredientName.setText("ID:" + r.ingredientId); // упростим (имя подтянем в Activity)
        h.tvQty.setText(String.valueOf(r.quantity));
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(r);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIngredientName, tvQty;
        Button btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvIngredientName = v.findViewById(R.id.tvIngredientName);
            tvQty = v.findViewById(R.id.tvQty);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
