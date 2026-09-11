# カレンダー Web アプリ

Java Servlet + JDBC の予定カレンダーです。

## 起動

ターミナルで:

```bash
./start.sh
```

http://localhost:8080/

終了は `Ctrl+C`。コード変更は自動再読込を試みます。反映されないときは止めて再実行してください。

（補足）フォルダ名に日本語があるため、起動スクリプトは `/tmp/calendar-app` 経由で Jetty を動かします。

## MySQL に切り替える場合

```bash
docker compose up -d
```

`src/main/resources/db.properties` を MySQL 用に書き換えて再起動します。
