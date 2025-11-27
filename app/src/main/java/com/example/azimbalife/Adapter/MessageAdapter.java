package com.example.azimbalife.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Activity.MessageModel;

import com.example.azimbalife.R;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private Context context;
    private ArrayList<MessageModel> messageList;

    // Constructor properly initializing the adapter
    public MessageAdapter(Context context, ArrayList<MessageModel> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(context).inflate(R.layout.item_user_message, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_ai_message, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? 0 : 1;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textMessage.setText(messageList.get(position).getMessage());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
        }
    }
}
