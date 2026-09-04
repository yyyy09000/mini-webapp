package com.example.app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * カレンダー1マス分。
 */
public class CalendarCell {
    private final LocalDate date;
    private final boolean currentMonth;
    private final List<Event> events = new ArrayList<>();

    public CalendarCell(LocalDate date, boolean currentMonth) {
        this.date = date;
        this.currentMonth = currentMonth;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void addEvent(Event event) {
        events.add(event);
    }
}
