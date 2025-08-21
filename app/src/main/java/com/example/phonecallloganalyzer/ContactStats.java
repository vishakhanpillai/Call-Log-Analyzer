package com.example.phonecallloganalyzer;

public class ContactStats {
    private String name;
    private int count;
    private int totalDuration;

    public ContactStats(String name) {
        this.name = name;
        this.count = 0;
        this.totalDuration = 0;
    }

    public void addCall(int duration) {
        count++;
        totalDuration += duration;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public int getTotalDuration() { return totalDuration; }
}


