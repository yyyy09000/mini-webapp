<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>アイテム一覧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap">
    <p><a href="${pageContext.request.contextPath}/">← トップ</a></p>
    <h1>アイテム一覧</h1>
    <p><a class="btn" href="${pageContext.request.contextPath}/items?action=new">新規登録</a></p>

    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>名前</th>
            <th>説明</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="item" items="${items}">
            <tr>
                <td>${item.id}</td>
                <td><c:out value="${item.name}"/></td>
                <td><c:out value="${item.description}"/></td>
                <td class="actions">
                    <a href="${pageContext.request.contextPath}/items?action=edit&amp;id=${item.id}">編集</a>
                    <form method="post" action="${pageContext.request.contextPath}/items" class="inline">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id" value="${item.id}">
                        <button type="submit" onclick="return confirm('削除しますか？')">削除</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty items}">
            <tr>
                <td colspan="4">データがありません</td>
            </tr>
        </c:if>
        </tbody>
    </table>
</main>
</body>
</html>
