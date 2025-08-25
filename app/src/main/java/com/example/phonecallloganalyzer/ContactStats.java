package com.example.phonecallloganalyzer;

import java.io.Serializable;

public class ContactStats implements Serializable {
    private String name;
    private String number; // <-- 1. Add field for the number
    private int count;
    private int totalDuration;

    // 2. Update the constructor to accept the number
    public ContactStats(String name, String number) {
        this.name = name;
        this.number = number; // <-- Store the number
        this.count = 0;
        this.totalDuration = 0;
    }

    public void addCall(int duration) {
        count++;
        totalDuration += duration;
    }

    // --- Getters ---
    public String getName() { return name; }
    public int getCount() { return count; }
    public int getTotalDuration() { return totalDuration; }

    // 3. This is the "getter" method that was missing
    public String getNumber() { return number; }
}