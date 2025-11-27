package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.QuickAction;
import com.example.azimbalife.R;

import java.util.List;

public class QuickActionsAdapter extends RecyclerView.Adapter<QuickActionsAdapter.ViewHolder> {

    private List<QuickAction> quickActionsList;

    public QuickActionsAdapter(List<QuickAction> quickActionsList) {
        this.quickActionsList = quickActionsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickAction quickAction = quickActionsList.get(position);

        holder.tvActionTitle.setText(quickAction.getTitle());
        holder.tvActionDescription.setText(quickAction.getDescription());
        holder.ivActionIcon.setImageResource(quickAction.getIconRes());

        // Set click listener
        holder.itemView.setOnClickListener(quickAction.getClickListener());
    }

    @Override
    public int getItemCount() {
        return quickActionsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivActionIcon;
        TextView tvActionTitle, tvActionDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivActionIcon = itemView.findViewById(R.id.ivActionIcon);
            tvActionTitle = itemView.findViewById(R.id.tvActionTitle);
            tvActionDescription = itemView.findViewById(R.id.tvActionDescription);
        }
    }
}