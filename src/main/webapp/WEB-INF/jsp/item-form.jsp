<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>
        <c:choose>
            <c:when test="${mode == 'create'}">新規登録</c:when>
            <c:otherwise>編集</c:otherwise>
        </c:choose>
    </title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap">
    <p><a href="${pageContext.request.contextPath}/items">← 一覧</a></p>
    <h1>
        <c:choose>
            <c:when test="${mode == 'create'}">新規登録</c:when>
            <c:otherwise>編集</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/items" class="form">
        <input type="hidden" name="action" value="${mode}">
        <c:if test="${mode == 'update'}">
            <input type="hidden" name="id" value="${item.id}">
        </c:if>

        <label>
            名前
            <input type="text" name="name" value="<c:out value='${item.name}'/>" required maxlength="100">
        </label>
        <label>
            説明
            <textarea name="description" rows="4" maxlength="500"><c:out value="${item.description}"/></textarea>
        </label>
        <button type="submit">保存</button>
    </form>
</main>
</body>
</html>
