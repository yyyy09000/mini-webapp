-- H2 / MySQL 共通スキーマ
-- アプリ起動時にも同等の初期化を行う（AppContextListener）

CREATE TABLE IF NOT EXISTS events (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    event_date  DATE NOT NULL,
    event_time  TIME,
    description VARCHAR(500)
);

-- サンプル予定は AppContextListener で投入
