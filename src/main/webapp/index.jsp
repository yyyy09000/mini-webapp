<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>mini-webapp</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap">
    <h1>mini-webapp</h1>
    <p>Java Servlet + JDBC の小さな Web アプリ基盤です。</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/home">ホーム（Servlet）</a></li>
        <li><a href="${pageContext.request.contextPath}/items">アイテム一覧（CRUD）</a></li>
    </ul>
</main>
</body>
</html>
