package com.example.app.dao;

import com.example.app.model.Event;
import com.example.app.util.DbUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * events テーブルへのアクセス。
 */
public class EventDao {

    private static final String SELECT_COLS = "id, title, event_date, event_time, description";

    public List<Event> findByMonth(int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.plusMonths(1).atDay(1);

        String sql = """
                SELECT %s
                FROM events
                WHERE event_date >= ? AND event_date < ?
                ORDER BY event_date, event_time, id
                """.formatted(SELECT_COLS);
        List<Event> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Event> findByDate(LocalDate date) throws SQLException {
        String sql = """
                SELECT %s
                FROM events
                WHERE event_date = ?
                ORDER BY event_time, id
                """.formatted(SELECT_COLS);
        List<Event> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<Event> findById(long id) throws SQLException {
        String sql = "SELECT " + SELECT_COLS + " FROM events WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Event insert(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, event_date, event_time, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, event);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    event.setId(keys.getLong(1));
                }
            }
        }
        return event;
    }

    public boolean update(Event event) throws SQLException {
        String sql = """
                UPDATE events
                SET title = ?, event_date = ?, event_time = ?, description = ?
                WHERE id = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, event);
            ps.setLong(5, event.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private void bind(PreparedStatement ps, Event event) throws SQLException {
        ps.setString(1, event.getTitle());
        ps.setDate(2, Date.valueOf(event.getEventDate()));
        if (event.getEventTime() == null) {
            ps.setNull(3, Types.TIME);
        } else {
            ps.setTime(3, Time.valueOf(event.getEventTime()));
        }
        ps.setString(4, event.getDescription());
    }

    private Event map(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getLong("id"));
        event.setTitle(rs.getString("title"));
        Date date = rs.getDate("event_date");
        if (date != null) {
            event.setEventDate(date.toLocalDate());
        }
        Time time = rs.getTime("event_time");
        if (time != null) {
            event.setEventTime(time.toLocalTime());
        }
        event.setDescription(rs.getString("description"));
        return event;
    }
}
