package com.example.phonecallloganalyzer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private List<CallLogItem> callLogList = new ArrayList<>();
    private CallLogAdapter adapter;
    private HashMap<String, ContactStats> statsMap = new HashMap<>();

    private Button btnShowLogs, btnShowStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CallLogAdapter(callLogList);
        recyclerView.setAdapter(adapter);

        btnShowLogs = findViewById(R.id.showLogsButton);
        btnShowStats = findViewById(R.id.showStatsButton);

        btnShowLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCallLogs();
            }
        });

        btnShowStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStats();
            }
        });

        checkPermissionsAndLoad();
    }

    private void checkPermissionsAndLoad() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_CALL_LOG);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_CONTACTS);
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionList.toArray(new String[0]),
                    REQUEST_CODE);
        } else {
            loadCallLogs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = true;
        if (requestCode == REQUEST_CODE) {
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadCallLogs();
            }
        }
    }

    private void loadCallLogs() {
        callLogList.clear();
        statsMap.clear();

        try {
            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    null,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

                while (cursor.moveToNext()) {
                    String number = cursor.getString(numberIndex);
                    String typeStr = cursor.getString(typeIndex);
                    String dateStr = cursor.getString(dateIndex);
                    String durationStr = cursor.getString(durationIndex);

                    // Call type
                    String type;
                    int typeCode = Integer.parseInt(typeStr);
                    switch (typeCode) {
                        case CallLog.Calls.OUTGOING_TYPE: type = "OUTGOING"; break;
                        case CallLog.Calls.INCOMING_TYPE: type = "INCOMING"; break;
                        case CallLog.Calls.MISSED_TYPE: type = "MISSED"; break;
                        default: type = "OTHER"; break;
                    }

                    long dateLong = Long.parseLong(dateStr);
                    String name = getContactName(this, number);

                    // Add to list
                    callLogList.add(new CallLogItem(name, number, type, dateLong, durationStr));

                    // Update stats
                    int dur = 0;
                    try { dur = Integer.parseInt(durationStr); } catch (NumberFormatException ignored){}
                    if (!statsMap.containsKey(number)) {
                        statsMap.put(number, new ContactStats(name));
                    }
                    statsMap.get(number).addCall(dur);
                }

                cursor.close();
                adapter.notifyDataSetChanged();
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void showStats() {
        callLogList.clear();

        for (String num : statsMap.keySet()) {
            ContactStats s = statsMap.get(num);
            String callType = "STATS";
            //String callDate = "-";
            String callDuration = "Calls: " + s.getCount() + " | Total Duration: " + s.getTotalDuration() + " sec";

            callLogList.add(new CallLogItem(s.getName(), num, callType, 0L, callDuration));
        }

        adapter.notifyDataSetChanged();
    }

    private String getContactName(Context context, String phoneNumber) {
        String name = phoneNumber; // fallback
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.PhoneLookup.DISPLAY_NAME));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }
}
