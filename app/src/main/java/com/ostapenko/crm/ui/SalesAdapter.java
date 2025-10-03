    package com.ostapenko.crm.ui;

    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.TextView;
    import androidx.annotation.NonNull;
    import androidx.recyclerview.widget.RecyclerView;
    import com.ostapenko.crm.R;
    import com.ostapenko.crm.dto.SaleRow;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Locale;

    public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.VH> {
        private final List<SaleRow> data = new ArrayList<>();
        private final List<SaleRow> all = new ArrayList<>();
        private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        public void submit(List<SaleRow> items) {
            all.clear(); data.clear();
            if (items != null) { all.addAll(items); data.addAll(items); }
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            SaleRow r = data.get(position);

            h.tvDate.setText(df.format(r.saleDate));
            h.tvTotal.setText("₴" + trim(r.subtotal)); // показываем сумму по строке (а не общий total всего чека)

            // что продали
            h.tvProduct.setText(r.productName + " × " + r.quantity);

            // кто продал
            String seller = (r.firstName != null && !r.firstName.isEmpty())
                    ? r.firstName + (r.lastName == null ? "" : " " + r.lastName)
                    : (r.login != null && !r.login.isEmpty())
                    ? r.login
                    : (r.sellerId == null ? "-" : String.valueOf(r.sellerId));
            h.tvSeller.setText("Продавец: " + seller);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvTotal, tvProduct, tvSeller;
            VH(@NonNull View v) {
                super(v);
                tvDate = v.findViewById(R.id.tvDate);
                tvTotal = v.findViewById(R.id.tvTotal);
                tvProduct = v.findViewById(R.id.tvProduct);
                tvSeller = v.findViewById(R.id.tvSeller);
            }
        }

        private static String trim(double d) {
            String s = String.valueOf(d);
            if (s.endsWith(".0")) return s.substring(0, s.length()-2);
            return s;
        }

        public void filter(String q) {
            data.clear();
            if (q==null || q.trim().isEmpty()) {
                data.addAll(all);
            } else {
                String s = q.toLowerCase();
                for (SaleRow r : all) {
                    String dateStr = df.format(r.saleDate).toLowerCase();
                    String sumStr  = String.valueOf(r.subtotal).toLowerCase(); // фильтр по сумме строки
                    String prod    = (r.productName == null ? "" : r.productName.toLowerCase());
                    String seller  = ((r.firstName==null?"":r.firstName) + " " + (r.lastName==null?"":r.lastName)).trim().toLowerCase();
                    String login   = (r.login==null?"":r.login.toLowerCase());

                    if (dateStr.contains(s) || sumStr.contains(s) || prod.contains(s) || seller.contains(s) || login.contains(s)) {
                        data.add(r);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }
