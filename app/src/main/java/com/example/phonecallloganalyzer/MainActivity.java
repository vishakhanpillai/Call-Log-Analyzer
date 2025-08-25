package com.example.phonecallloganalyzer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Import ProgressBar

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private List<CallLogItem> callLogList = new ArrayList<>();
    private CallLogAdapter adapter;
    private HashMap<String, ContactStats> statsMap = new HashMap<>();

    private Button btnShowLogs, btnShowStats;
    private ProgressBar progressBar; // Add a ProgressBar for visual feedback

    private View slidingSelector;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Handler to post results back to the main thread
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int incomingCalls = 0, outgoingCalls = 0, missedCalls = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure you have a ProgressBar with id 'progressBar' in your layout

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        btnShowLogs = findViewById(R.id.showLogsButton);
        btnShowStats = findViewById(R.id.showStatsButton);
        progressBar = findViewById(R.id.progressBar);
        slidingSelector = findViewById(R.id.slidingSelector);


        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CallLogAdapter(callLogList);
        recyclerView.setAdapter(adapter);

        btnShowLogs.setOnClickListener(v -> {
            animateSlider(btnShowLogs);
            checkPermissionsAndLoad();
        });
        btnShowStats.setOnClickListener(v ->{
            animateSlider(btnShowStats);
            showStats();
        });

        checkPermissionsAndLoad();
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
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    // This is our new background loading method
    private void loadCallLogsInBackground() {
        showLoading(true);
        executor.execute(() -> {

            incomingCalls = 0;
            outgoingCalls = 0;
            missedCalls = 0;

            // This code now runs on a background thread
            List<CallLogItem> loadedLogs = new ArrayList<>();
            HashMap<String, ContactStats> loadedStats = new HashMap<>();
            HashMap<String, String> contactCache = new HashMap<>(); // Cache for contact names

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

                    String name = getContactName(this, number, contactCache); // Use cache

                    String type;
                    switch (Integer.parseInt(typeStr)) {
                        case CallLog.Calls.OUTGOING_TYPE:
                            type = "Outgoing";
                            outgoingCalls++;
                            break;
                        case CallLog.Calls.INCOMING_TYPE:
                            type = "Incoming";
                            incomingCalls++;
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            type = "Missed";
                            missedCalls++;
                            break;
                        default:
                            type = "Other";
                            break;
                    }

                    loadedLogs.add(new CallLogItem(name, number, type, dateLong, durationStr));

                    // Update stats
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
                showLoading(false);
            });
        });
    }

    private void showStats() {
        if (statsMap.isEmpty()) {
            // If stats aren't loaded yet, load everything first, then show stats.
            //loadCallLogsInBackground();
            // A better implementation might wait for the load to finish.
            // But for now, just clicking again after load will work.
            return;
        }

        Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
        intent.putExtra("statsMap", statsMap);
        intent.putExtra("incomingCount", incomingCalls);
        intent.putExtra("outgoingCount", outgoingCalls);
        intent.putExtra("missedCount", missedCalls);
        startActivity(intent);


//        callLogList.clear();
//        for (ContactStats stats : statsMap.values()) {
//            String number = stats.getNumber(); // You need to add this getter to ContactStats
//            String name = stats.getName();
//
////            String duration = "Calls: " + stats.getCount() + " | Total: " + stats.getTotalDuration() + "s";
////            callLogList.add(new CallLogItem(name, number, "STATS", 0L, duration));
//            String statsData = stats.getCount() + "|" + stats.getTotalDuration();
//            callLogList.add(new CallLogItem(name, number, "STATS", 0L, statsData));
//        }
//        adapter.notifyDataSetChanged();
    }

    // Modified to use a cache
    private String getContactName(Context context, String phoneNumber, HashMap<String, String> cache) {
        if (cache.containsKey(phoneNumber)) {
            return cache.get(phoneNumber); // Return from cache if present
        }

        String name = phoneNumber; // Fallback
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
        }

        cache.put(phoneNumber, name); // Store result in cache
        return name;
    }

    private void animateSlider(View clickedButton){
        slidingSelector.animate()
                .x(clickedButton.getX())
                .setDuration(250) // A quick, smooth animation
                .start();
    }
}