package com.example.phonecallloganalyzer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {

    private final List<CallLogItem> callLogs;
    private Context context; // Context is needed for colors

    public CallLogAdapter(List<CallLogItem> callLogs) {
        this.callLogs = callLogs;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext(); // Store context
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_call_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CallLogItem item = callLogs.get(position);

        holder.nameText.setText(item.getName());
        holder.numberText.setText(item.getNumber());

        if (item.getType().equals("STATS")) {
            // --- STATS VIEW ---
            holder.callDetailsRow.setVisibility(View.GONE);
            holder.statsDetailsRow.setVisibility(View.VISIBLE);

            String[] statsParts = item.getDuration().split("\\|");
            if (statsParts.length == 2) {
                String callCount = statsParts[0];
                int totalDurationInSeconds = Integer.parseInt(statsParts[1]);

                holder.tvCallCount.setText("📞 " + callCount + " Calls");
                holder.tvTotalDuration.setText("⏱ " + formatStatsDuration(totalDurationInSeconds));

                holder.tvCallCount.setTextColor(Color.parseColor("#33B5E5"));
                holder.tvTotalDuration.setTextColor(Color.parseColor("#FFBB33"));
            }
        } else {
            // --- REGULAR LOG VIEW ---
            holder.callDetailsRow.setVisibility(View.VISIBLE);
            holder.statsDetailsRow.setVisibility(View.GONE);

            holder.tvType.setText(item.getType());
            // Set color based on call type
            switch (item.getType()) {
                case "Incoming":
                    holder.tvType.setTextColor(Color.parseColor("#4CAF50")); // Green
                    break;
                case "Outgoing":
                    holder.tvType.setTextColor(Color.parseColor("#2196F3")); // Blue
                    break;

                case "Missed":
                    holder.tvType.setTextColor(Color.parseColor("#F44336")); // Red
                    break;
                default:
                    holder.tvType.setTextColor(Color.parseColor("#B0BEC5")); // Grey
                    break;
            }

            holder.tvDate.setText(formatDate(item.getDate()));
            holder.tvDuration.setText(formatLogDuration(item.getDuration()));
        }
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText, numberText;
        final LinearLayout callDetailsRow, statsDetailsRow;
        final TextView tvType, tvDate, tvDuration;
        final TextView tvCallCount, tvTotalDuration;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tvName);
            numberText = itemView.findViewById(R.id.tvNumber);
            callDetailsRow = itemView.findViewById(R.id.callDetailsRow);
            statsDetailsRow = itemView.findViewById(R.id.statsDetailsRow);
            tvType = itemView.findViewById(R.id.tvType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvCallCount = itemView.findViewById(R.id.tvCallCount);
            tvTotalDuration = itemView.findViewById(R.id.tvTotalDuration);
        }
    }

    private String formatStatsDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm %ds", minutes, totalSeconds % 60);
        }
    }

    private String formatLogDuration(String durationStr) {
        try {
            int seconds = Integer.parseInt(durationStr);
            if (seconds == 0) return "0s";
            if (seconds < 60) return seconds + "s";
            int minutes = seconds / 60;
            return minutes + "m " + (seconds % 60) + "s";
        } catch (NumberFormatException e) {
            return "-";
        }
    }

    private String formatDate(long timestamp) {

        Calendar now = Calendar.getInstance();
        Calendar callTime = Calendar.getInstance();
        callTime.setTimeInMillis(timestamp);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (now.get(Calendar.YEAR) == callTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == callTime.get(Calendar.DAY_OF_YEAR)) {
            return "Today " + timeFormat.format(callTime.getTime());
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (yesterday.get(Calendar.YEAR) == callTime.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == callTime.get(Calendar.DAY_OF_YEAR)){
            return "Yesterday " + timeFormat.format(callTime.getTime());
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
        return dateFormat.format(callTime.getTime());



//        now.add(Calendar.DAY_OF_YEAR, -1);
//        if (now.get(Calendar.YEAR) == callTime.get(Calendar.YEAR) &&
//                now.get(Calendar.DAY_OF_YEAR) == callTime.get(Calendar.DAY_OF_YEAR)) {
//            return "Yesterday, " + timeFormat.format(callTime.getTime());
//        }
//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
//        return dateFormat.format(callTime.getTime());
    }
}
