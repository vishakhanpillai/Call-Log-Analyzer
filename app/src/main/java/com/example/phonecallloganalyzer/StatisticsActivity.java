package com.example.phonecallloganalyzer;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private HorizontalBarChart barChart;
    private PieChart pieChart;
    private RecyclerView statsRecyclerView; // The new RecyclerView for the list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Find all the views
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        statsRecyclerView = findViewById(R.id.statsRecyclerView);

        // Retrieve all data from MainActivity
        HashMap<String, ContactStats> statsMap = (HashMap<String, ContactStats>) getIntent().getSerializableExtra("statsMap");
        int incoming = getIntent().getIntExtra("incomingCount", 0);
        int outgoing = getIntent().getIntExtra("outgoingCount", 0);
        int missed = getIntent().getIntExtra("missedCount", 0);

        if (statsMap != null && !statsMap.isEmpty()) {
            setupBarChart(statsMap);
            setupRecyclerView(statsMap); // Call the new method to setup the list
        }

        if (incoming > 0 || outgoing > 0 || missed > 0) {
            setupPieChart(incoming, outgoing, missed);
        }
    }

    // --- NEW METHOD TO SETUP THE DETAILED STATS LIST ---
    private void setupRecyclerView(HashMap<String, ContactStats> statsMap) {
        List<CallLogItem> statsList = new ArrayList<>();

        // Convert the statsMap into a list that the adapter can use
        for (ContactStats stats : statsMap.values()) {
            String statsData = stats.getCount() + "|" + stats.getTotalDuration();
            statsList.add(new CallLogItem(stats.getName(), stats.getNumber(), "STATS", 0L, statsData));
        }

        // Sort the list alphabetically by name
        statsList.sort(Comparator.comparing(CallLogItem::getName));

        // Use the same CallLogAdapter you've already built!
        CallLogAdapter adapter = new CallLogAdapter(statsList);
        statsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        statsRecyclerView.setAdapter(adapter);
    }

    // --- BAR CHART SETUP (No changes here) ---
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
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
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
        barChart.animateY(1000);
    }

    private void styleBarChart(ArrayList<String> labels) {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setFitBars(true);

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

    // --- PIE CHART SETUP (No changes here) ---
    private void setupPieChart(int incoming, int outgoing, int missed) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(incoming, "Incoming"));
        entries.add(new PieEntry(outgoing, "Outgoing"));
        entries.add(new PieEntry(missed, "Missed"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#F44336")});
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        stylePieChart();
        pieChart.setData(pieData);
        pieChart.invalidate();
        pieChart.animateY(1000);
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

        Legend legend = pieChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
    }
}
