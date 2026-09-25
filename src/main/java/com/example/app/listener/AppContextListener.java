package com.example.app.listener;

import com.example.app.util.DbUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 起動時にテーブルとサンプルデータを用意する。
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (Connection conn = DbUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS events (
                        id             BIGINT PRIMARY KEY AUTO_INCREMENT,
                        title          VARCHAR(100) NOT NULL,
                        event_date     DATE NOT NULL,
                        event_date_end DATE,
                        event_time     TIME,
                        description    VARCHAR(500)
                    )
                    """);
            ensureEventTimeColumn(st);
            ensureEventDateEndColumn(st);

            try (var rs = st.executeQuery("SELECT COUNT(*) FROM events")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    LocalDate today = LocalDate.now();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO events (title, event_date, event_date_end, event_time, description) VALUES (?, ?, ?, ?, ?)")) {
                        insertSample(ps, "キックオフ", today, LocalTime.of(10, 0), "カレンダーのサンプル予定");
                        insertSample(ps, "レビュー", today.plusDays(3), LocalTime.of(15, 30), "3日後の予定");
                    }
                }
            }

            sce.getServletContext().log("DB 初期化完了");
        } catch (SQLException e) {
            throw new IllegalStateException("DB 初期化に失敗しました", e);
        }
    }

    private static void ensureEventTimeColumn(Statement st) {
        try {
            st.execute("ALTER TABLE events ADD COLUMN event_time TIME");
        } catch (SQLException ignored) {
            // 既に存在するなど
        }
    }

    private static void ensureEventDateEndColumn(Statement st) {
        try {
            st.execute("ALTER TABLE events ADD COLUMN event_date_end DATE");
        } catch (SQLException ignored) {
            // 既に存在するなど
        }
        try {
            st.execute("UPDATE events SET event_date_end = event_date WHERE event_date_end IS NULL");
        } catch (SQLException ignored) {
            // 既存DBで列が使えない場合など
        }
    }

    private static void insertSample(PreparedStatement ps, String title, LocalDate date,
                                     LocalTime time, String description) throws SQLException {
        ps.setString(1, title);
        ps.setDate(2, Date.valueOf(date));
        ps.setDate(3, Date.valueOf(date));
        ps.setTime(4, Time.valueOf(time));
        ps.setString(5, description);
        ps.executeUpdate();
    }
}
