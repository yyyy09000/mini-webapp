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

    private static final String SELECT_COLS =
            "id, title, event_date, event_date_end, event_time, description";

    public List<Event> findByMonth(int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.plusMonths(1).atDay(1);

        String sql = """
                SELECT %s
                FROM events
                WHERE event_date < ?
                  AND COALESCE(event_date_end, event_date) >= ?
                ORDER BY event_date, event_time, id
                """.formatted(SELECT_COLS);
        List<Event> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(to));
            ps.setDate(2, Date.valueOf(from));
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
                WHERE event_date <= ?
                  AND COALESCE(event_date_end, event_date) >= ?
                ORDER BY event_time, id
                """.formatted(SELECT_COLS);
        List<Event> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
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
        Optional<Event> same = findSameContent(event, null);
        if (same.isPresent()) {
            Event existing = same.get();
            event.setId(existing.getId());
            event.setTitle(existing.getTitle());
            event.setEventDate(existing.getEventDate());
            event.setEventDateEnd(existing.getEventDateEnd());
            event.setEventTime(existing.getEventTime());
            event.setDescription(existing.getDescription());
            return event;
        }
        String sql = """
                INSERT INTO events (title, event_date, event_date_end, event_time, description)
                VALUES (?, ?, ?, ?, ?)
                """;
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
                SET title = ?, event_date = ?, event_date_end = ?, event_time = ?, description = ?
                WHERE id = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, event);
            ps.setLong(6, event.getId());
            boolean updated = ps.executeUpdate() == 1;
            deleteDuplicatesOf(event);
            return updated;
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

    /**
     * 同じ日・同じ時間・同じタイトル・同じ説明の重複を探し、id が小さい方を残して削除する。
     */
    public int deleteAllDuplicates() throws SQLException {
        String sql = """
                DELETE FROM events
                WHERE id IN (
                    SELECT id FROM (
                        SELECT e1.id
                        FROM events e1
                        INNER JOIN events e2
                          ON e2.id < e1.id
                         AND e2.event_date = e1.event_date
                         AND COALESCE(e2.event_date_end, e2.event_date)
                             = COALESCE(e1.event_date_end, e1.event_date)
                         AND e2.title = e1.title
                         AND (
                              (e2.event_time IS NULL AND e1.event_time IS NULL)
                              OR e2.event_time = e1.event_time
                         )
                         AND (
                              (e2.description IS NULL AND e1.description IS NULL)
                              OR (e2.description IS NOT NULL AND e1.description IS NOT NULL
                                  AND e2.description = e1.description)
                         )
                    ) dup
                )
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /** 指定内容と同一の予定があれば返す（excludeId は更新時に自分を除外） */
    public Optional<Event> findSameContent(Event event, Long excludeId) throws SQLException {
        String sql = """
                SELECT %s
                FROM events
                WHERE event_date = ?
                  AND COALESCE(event_date_end, event_date) = ?
                  AND title = ?
                  AND (
                       (? IS NULL AND event_time IS NULL)
                       OR event_time = ?
                  )
                  AND (
                       (? IS NULL AND description IS NULL)
                       OR description = ?
                  )
                  AND (? IS NULL OR id <> ?)
                ORDER BY id
                LIMIT 1
                """.formatted(SELECT_COLS);
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setDate(i++, Date.valueOf(event.getEventDate()));
            ps.setDate(i++, Date.valueOf(event.getEndDateOrStart()));
            ps.setString(i++, event.getTitle());
            if (event.getEventTime() == null) {
                ps.setNull(i++, Types.TIME);
                ps.setNull(i++, Types.TIME);
            } else {
                Time t = Time.valueOf(event.getEventTime());
                ps.setTime(i++, t);
                ps.setTime(i++, t);
            }
            String desc = event.getDescription();
            if (desc == null) {
                ps.setNull(i++, Types.VARCHAR);
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, desc);
                ps.setString(i++, desc);
            }
            if (excludeId == null) {
                ps.setNull(i++, Types.BIGINT);
                ps.setLong(i, -1L);
            } else {
                ps.setLong(i++, excludeId);
                ps.setLong(i, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private void deleteDuplicatesOf(Event event) throws SQLException {
        String sql = """
                DELETE FROM events
                WHERE id <> ?
                  AND event_date = ?
                  AND COALESCE(event_date_end, event_date) = ?
                  AND title = ?
                  AND (
                       (? IS NULL AND event_time IS NULL)
                       OR event_time = ?
                  )
                  AND (
                       (? IS NULL AND description IS NULL)
                       OR description = ?
                  )
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, event.getId());
            ps.setDate(i++, Date.valueOf(event.getEventDate()));
            ps.setDate(i++, Date.valueOf(event.getEndDateOrStart()));
            ps.setString(i++, event.getTitle());
            if (event.getEventTime() == null) {
                ps.setNull(i++, Types.TIME);
                ps.setNull(i++, Types.TIME);
            } else {
                Time t = Time.valueOf(event.getEventTime());
                ps.setTime(i++, t);
                ps.setTime(i++, t);
            }
            String desc = event.getDescription();
            if (desc == null) {
                ps.setNull(i++, Types.VARCHAR);
                ps.setNull(i, Types.VARCHAR);
            } else {
                ps.setString(i++, desc);
                ps.setString(i, desc);
            }
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Event event) throws SQLException {
        ps.setString(1, event.getTitle());
        ps.setDate(2, Date.valueOf(event.getEventDate()));
        ps.setDate(3, Date.valueOf(event.getEndDateOrStart()));
        if (event.getEventTime() == null) {
            ps.setNull(4, Types.TIME);
        } else {
            ps.setTime(4, Time.valueOf(event.getEventTime()));
        }
        ps.setString(5, event.getDescription());
    }

    private Event map(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getLong("id"));
        event.setTitle(rs.getString("title"));
        Date date = rs.getDate("event_date");
        if (date != null) {
            event.setEventDate(date.toLocalDate());
        }
        Date dateEnd = rs.getDate("event_date_end");
        if (dateEnd != null) {
            event.setEventDateEnd(dateEnd.toLocalDate());
        } else {
            event.setEventDateEnd(event.getEventDate());
        }
        Time time = rs.getTime("event_time");
        if (time != null) {
            event.setEventTime(time.toLocalTime());
        }
        event.setDescription(rs.getString("description"));
        return event;
    }
}
