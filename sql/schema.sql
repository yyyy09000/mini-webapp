-- H2 / MySQL 共通のサンプルスキーマ
-- アプリ起動時にも同等の初期化を行う（AppContextListener）

CREATE TABLE IF NOT EXISTS items (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO items (name, description) VALUES
    ('サンプル1', '最初のアイテム'),
    ('サンプル2', '2つ目のアイテム');
