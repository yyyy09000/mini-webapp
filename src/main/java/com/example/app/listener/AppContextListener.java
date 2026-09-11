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
                        id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                        title       VARCHAR(100) NOT NULL,
                        event_date  DATE NOT NULL,
                        event_time  TIME,
                        description VARCHAR(500)
                    )
                    """);
            ensureEventTimeColumn(st);

            try (var rs = st.executeQuery("SELECT COUNT(*) FROM events")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    LocalDate today = LocalDate.now();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO events (title, event_date, event_time, description) VALUES (?, ?, ?, ?)")) {
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

    private static void insertSample(PreparedStatement ps, String title, LocalDate date,
                                     LocalTime time, String description) throws SQLException {
        ps.setString(1, title);
        ps.setDate(2, Date.valueOf(date));
        ps.setTime(3, Time.valueOf(time));
        ps.setString(4, description);
        ps.executeUpdate();
    }
}
