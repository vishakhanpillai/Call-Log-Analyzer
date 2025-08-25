package com.example.phonecallloganalyzer;
public class CallLogItem {
    private String name;
    private String number;
    private String type;
    private long date;
    private String duration;

    public CallLogItem(String name, String number, String type, long date, String duration) {
        this.name = name;
        this.number = number;
        this.type = type;
        this.date = date;
        this.duration = duration;
    }

    public String getName() { return name; }
    public String getNumber() { return number; }
    public String getType() { return type; }
    public long getDate() { return date; }
    public String getDuration() { return duration; }
}
