<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ホーム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap">
    <p><a href="${pageContext.request.contextPath}/">← トップ</a></p>
    <h1>ホーム</h1>
    <p>${message}</p>
    <p><a href="${pageContext.request.contextPath}/items">アイテム一覧へ</a></p>
</main>
</body>
</html>
