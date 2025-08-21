package com.example.phonecallloganalyzer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {
    private List<CallLogItem> callLogs;

    public CallLogAdapter(List<CallLogItem> callLogs) {
        this.callLogs = callLogs;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_call_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CallLogItem item = callLogs.get(position);
        holder.nameText.setText("Name: " + item.getName());
        holder.numberText.setText("Number: " + item.getNumber());
        holder.typeText.setText("Type: " + item.getType());
        holder.dateText.setText("Date: " + item.getDate());
        holder.durationText.setText("Duration: " + item.getDuration() + " sec");
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView numberText, typeText, dateText, durationText, nameText;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            numberText = itemView.findViewById(R.id.numberText);
            typeText = itemView.findViewById(R.id.typeText);
            dateText = itemView.findViewById(R.id.dateText);
            durationText = itemView.findViewById(R.id.durationText);
        }
    }
}
