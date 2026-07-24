package com.example.app.listener;

import com.example.app.util.DbUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 起動時にテーブルとサンプルデータを用意する。
 * スキーマ変更はここか sql/schema.sql に寄せる。
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (Connection conn = DbUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS items (
                        id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name        VARCHAR(100) NOT NULL,
                        description VARCHAR(500),
                        created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            try (var rs = st.executeQuery("SELECT COUNT(*) FROM items")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    st.execute("""
                            INSERT INTO items (name, description) VALUES
                            ('サンプル1', '最初のアイテム'),
                            ('サンプル2', '2つ目のアイテム')
                            """);
                }
            }
            sce.getServletContext().log("DB 初期化完了");
        } catch (SQLException e) {
            throw new IllegalStateException("DB 初期化に失敗しました", e);
        }
    }
}
