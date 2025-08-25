package com.example.phonecallloganalyzer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    // UI Views
    private RecyclerView recyclerView;
    private RecyclerView statsRecyclerView; // New RecyclerView for the stats list
    private Button btnShowLogs, btnShowStats;
    private ProgressBar progressBar;
    private View slidingSelector;
    private NestedScrollView statsContainer;
    private HorizontalBarChart barChart;
    private PieChart pieChart;

    // Data
    private List<CallLogItem> callLogList = new ArrayList<>();
    private CallLogAdapter adapter;
    private HashMap<String, ContactStats> statsMap = new HashMap<>();
    private int incomingCalls = 0, outgoingCalls = 0, missedCalls = 0;
    private boolean isDataLoaded = false;

    // Threading
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all views
        recyclerView = findViewById(R.id.recyclerView);
        statsRecyclerView = findViewById(R.id.statsRecyclerView); // Find the new RecyclerView
        btnShowLogs = findViewById(R.id.showLogsButton);
        btnShowStats = findViewById(R.id.showStatsButton);
        progressBar = findViewById(R.id.progressBar);
        slidingSelector = findViewById(R.id.slidingSelector);
        statsContainer = findViewById(R.id.statsContainer);
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CallLogAdapter(callLogList);
        recyclerView.setAdapter(adapter);

        // Setup Click Listeners
        btnShowLogs.setOnClickListener(v -> showLogsView());
        btnShowStats.setOnClickListener(v -> showStatsView());

        checkPermissionsAndLoad();
    }

    private void showLogsView() {
        animateSlider(btnShowLogs);
        statsContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showStatsView() {
        animateSlider(btnShowStats);
        if (isDataLoaded) {
            recyclerView.setVisibility(View.GONE);
            statsContainer.setVisibility(View.VISIBLE);
            // Re-animate charts for a nice effect when they appear
            barChart.animateY(1000);
            pieChart.animateY(1000);
        } else {
            // If data isn't loaded yet, load it. The charts will appear when done.
            checkPermissionsAndLoad();
        }
    }

    private void checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS},
                    REQUEST_CODE);
        } else {
            loadCallLogsInBackground();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadCallLogsInBackground();
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        statsContainer.setVisibility(isLoading ? View.INVISIBLE : statsContainer.getVisibility());
    }

    private void loadCallLogsInBackground() {
        showLoading(true);
        executor.execute(() -> {
            // Reset counters
            incomingCalls = 0;
            outgoingCalls = 0;
            missedCalls = 0;

            List<CallLogItem> loadedLogs = new ArrayList<>();
            HashMap<String, ContactStats> loadedStats = new HashMap<>();
            HashMap<String, String> contactCache = new HashMap<>();

            try (Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC")) {
                if (cursor == null) return;

                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

                while (cursor.moveToNext()) {
                    String number = cursor.getString(numberIndex);
                    String typeStr = cursor.getString(typeIndex);
                    long dateLong = cursor.getLong(dateIndex);
                    String durationStr = cursor.getString(durationIndex);
                    String name = getContactName(this, number, contactCache);

                    String type;
                    switch (Integer.parseInt(typeStr)) {
                        case CallLog.Calls.INCOMING_TYPE: type = "Incoming"; incomingCalls++; break;
                        case CallLog.Calls.OUTGOING_TYPE: type = "Outgoing"; outgoingCalls++; break;
                        case CallLog.Calls.MISSED_TYPE: type = "Missed"; missedCalls++; break;
                        default: type = "Other"; break;
                    }
                    loadedLogs.add(new CallLogItem(name, number, type, dateLong, durationStr));
                    int duration = Integer.parseInt(durationStr);
                    loadedStats.computeIfAbsent(number, k -> new ContactStats(name, number)).addCall(duration);
                }
            }

            // Post the result back to the UI thread
            handler.post(() -> {
                callLogList.clear();
                callLogList.addAll(loadedLogs);
                statsMap = loadedStats;
                adapter.notifyDataSetChanged();

                // Now, setup the charts and the new stats list with the new data
                setupBarChart(statsMap);
                setupPieChart(incomingCalls, outgoingCalls, missedCalls);
                setupStatsRecyclerView(statsMap); // Call the new method

                isDataLoaded = true;
                showLoading(false);
            });
        });
    }

    private void setupStatsRecyclerView(HashMap<String, ContactStats> statsMap) {
        List<CallLogItem> statsList = new ArrayList<>();

        for (ContactStats stats : statsMap.values()) {
            String statsData = stats.getCount() + "|" + stats.getTotalDuration();
            statsList.add(new CallLogItem(stats.getName(), stats.getNumber(), "STATS", 0L, statsData));
        }

        statsList.sort(Comparator.comparing(CallLogItem::getName));

        CallLogAdapter statsAdapter = new CallLogAdapter(statsList);
        statsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        statsRecyclerView.setAdapter(statsAdapter);
    }

    private String getContactName(Context context, String phoneNumber, HashMap<String, String> cache) {
        if (cache.containsKey(phoneNumber)) return cache.get(phoneNumber);
        String name = phoneNumber;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
        }
        cache.put(phoneNumber, name);
        return name;
    }

    private void animateSlider(View clickedButton) {
        slidingSelector.animate().x(clickedButton.getX()).setDuration(250).start();
    }

    // --- CHART SETUP METHODS ---

    private void setupBarChart(HashMap<String, ContactStats> statsMap) {
        List<Map.Entry<String, ContactStats>> sortedList = new ArrayList<>(statsMap.entrySet());
        sortedList.sort((o1, o2) -> Integer.compare(o2.getValue().getTotalDuration(), o1.getValue().getTotalDuration()));

        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int limit = Math.min(sortedList.size(), 5);

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, ContactStats> entry = sortedList.get(i);
            entries.add(new BarEntry(limit - 1 - i, entry.getValue().getTotalDuration() / 60f));
            labels.add(entry.getValue().getName());
        }
        Collections.reverse(labels);

        BarDataSet dataSet = new BarDataSet(entries, "Total Call Duration (Minutes)");

        // **NEW**: A professional, cohesive color palette
        final int[] BAR_CHART_COLORS = {
                Color.rgb(0, 227, 150),
                Color.rgb(0, 184, 212),
                Color.rgb(0, 145, 234),
                Color.rgb(48, 79, 254),
                Color.rgb(98, 0, 234)
        };
        dataSet.setColors(BAR_CHART_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f min", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        styleBarChart(labels);
        barChart.setData(barData);
        barChart.invalidate();
    }

    private void styleBarChart(ArrayList<String> labels) {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setFitBars(true);

        // **EDIT**: Add extra space on the right side for the value text
        barChart.setExtraRightOffset(35f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
    }

    private void setupPieChart(int incoming, int outgoing, int missed) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (incoming > 0) entries.add(new PieEntry(incoming, "Incoming"));
        if (outgoing > 0) entries.add(new PieEntry(outgoing, "Outgoing"));
        if (missed > 0) entries.add(new PieEntry(missed, "Missed"));

        PieDataSet dataSet = new PieDataSet(entries, "");

        // **NEW**: A professional, cohesive color palette
        final int[] PIE_CHART_COLORS = {
                Color.rgb(0, 227, 150), // Teal
                Color.rgb(0, 184, 212), // Cyan
                Color.rgb(255, 64, 129)  // Pink
        };
        dataSet.setColors(PIE_CHART_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(16f);
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        dataSet.setSliceSpace(3f);
        dataSet.setValueLinePart1OffsetPercentage(80.f);

        // **EDIT**: Make the connector lines shorter
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.3f);

        // **EDIT**: Change the connector line color to white
        dataSet.setValueLineColor(Color.WHITE);

        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        stylePieChart();
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void stylePieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);
        pieChart.setCenterText("Call Types");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(18f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(false); // We use labels on the slices, so the legend is redundant
    }
}
