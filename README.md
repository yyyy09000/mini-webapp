# mini-webapp

Java Servlet + JDBC の小さな Web アプリ基盤です。  
一覧・登録・更新・削除（CRUD）のサンプル付きです。

## 構成

```
src/main/java/com/example/app/
  model/      … エンティティ
  dao/        … JDBC アクセス
  util/       … DB 接続
  servlet/    … コントローラ
  filter/     … 文字コード
  listener/   … 起動時の DB 初期化
src/main/webapp/
  WEB-INF/jsp/ … 画面
  css/
sql/          … スキーマ（MySQL 用にも使用）
```

## 必要環境

- JDK 17 以上（確認済み: Temurin 25）
- Maven 3.9+（または Maven Wrapper）
- （任意）Docker … MySQL を使う場合

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## 起動（H2・追加インストール不要）

デフォルトは埋め込み H2（`./data/appdb` に保存）です。

```bash
./mvnw clean package
./mvnw cargo:run
```

ブラウザで開く:

- http://localhost:8080/mini-webapp/
- http://localhost:8080/mini-webapp/items

## MySQL に切り替える

```bash
docker compose up -d
```

`src/main/resources/db.properties` を MySQL 用に書き換えてから再起動します。

## 次にやること

1. `Item` / `ItemDao` / `ItemServlet` を業務エンティティに置き換える
2. 画面を `WEB-INF/jsp` に追加する
3. 必要なら認証・バリデーション・コネクションプールを足す
