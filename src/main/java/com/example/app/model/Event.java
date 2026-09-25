package com.example.app.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * カレンダー予定。
 */
public class Event {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private long id;
    private String title;
    private LocalDate eventDate;
    private LocalDate eventDateEnd;
    private LocalTime eventTime;
    private String description;

    public Event() {
    }

    public Event(String title, LocalDate eventDate, LocalTime eventTime, String description) {
        this.title = title;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getEventDateEnd() {
        return eventDateEnd;
    }

    public void setEventDateEnd(LocalDate eventDateEnd) {
        this.eventDateEnd = eventDateEnd;
    }

    /** 終了日。未設定なら開始日と同じ。 */
    public LocalDate getEndDateOrStart() {
        return eventDateEnd != null ? eventDateEnd : eventDate;
    }

    /** 一覧用。複数日なら「開始日 〜 終了日」。 */
    public String getEventDateLabel() {
        if (eventDate == null) {
            return "";
        }
        LocalDate end = getEndDateOrStart();
        if (end == null || end.equals(eventDate)) {
            return eventDate.toString();
        }
        return eventDate + " 〜 " + end;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    /** 表示用（例: 14:30）。未設定なら空文字。 */
    public String getEventTimeLabel() {
        return eventTime == null ? "" : eventTime.format(TIME_FMT);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
