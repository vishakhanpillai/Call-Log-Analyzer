package com.example.phonecallloganalyzer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {

    private final List<CallLogItem> callLogs;

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

        holder.dateText.setText("Date: " + formatDate(item.getDate()));

        // Format duration nicely
        String durationStr = formatDuration(item.getDuration());
        holder.durationText.setText("Duration: " + durationStr);
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nameText;
        public final TextView numberText;
        public final TextView typeText;
        public final TextView dateText;
        public final TextView durationText;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tvName);
            numberText = itemView.findViewById(R.id.tvNumber);
            typeText = itemView.findViewById(R.id.tvType);
            dateText = itemView.findViewById(R.id.tvDate);
            durationText = itemView.findViewById(R.id.tvDuration);
        }
    }

    // Helper to format duration
    private String formatDuration(String durationStr) {
        try {
            int duration = Integer.parseInt(durationStr);

            if (duration < 60) {
                return duration + " sec";
            } else {
                int minutes = duration / 60;
                int seconds = duration % 60;

                if (seconds == 0) {
                    return minutes + " min";
                } else {
                    return minutes + " min " + seconds + " sec";
                }
            }
        } catch (NumberFormatException e) {
            return durationStr + " sec"; // fallback
        }
    }

    private String formatDate(long timestamp) {
        try {
            // Create a Date object from the timestamp (which is in milliseconds)
            Date date = new Date(timestamp);
            // Define the desired format
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM,\n hh:mm a");
            // Return the formatted string
            return sdf.format(date);
        } catch (Exception e) {
            return "N/A"; // Return a default value in case of an error
        }
    }
}
