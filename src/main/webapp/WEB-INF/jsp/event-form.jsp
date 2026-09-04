<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>
        <c:choose>
            <c:when test="${mode == 'create'}">予定の追加</c:when>
            <c:otherwise>予定の編集</c:otherwise>
        </c:choose>
    </title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap">
    <p>
        <a href="${pageContext.request.contextPath}/calendar?year=${year}&amp;month=${month}">← カレンダー</a>
    </p>
    <h1>
        <c:choose>
            <c:when test="${mode == 'create'}">予定の追加</c:when>
            <c:otherwise>予定の編集</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/calendar" class="form">
        <input type="hidden" name="action" value="${mode}">
        <c:if test="${mode == 'update'}">
            <input type="hidden" name="id" value="${event.id}">
        </c:if>

        <label>
            タイトル
            <input type="text" name="title" value="<c:out value='${event.title}'/>" required maxlength="100">
        </label>
        <label>
            日付
            <input type="date" name="eventDate" value="${event.eventDate}" required>
        </label>
        <label>
            時間（任意）
            <input type="time" name="eventTime" value="${event.eventTimeLabel}">
        </label>
        <label>
            説明
            <textarea name="description" rows="4" maxlength="500"><c:out value="${event.description}"/></textarea>
        </label>
        <button type="submit">保存</button>
    </form>

    <c:if test="${mode == 'update'}">
        <form method="post" action="${pageContext.request.contextPath}/calendar" class="form danger-form"
              onsubmit="return confirm('この予定を削除しますか？');">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="id" value="${event.id}">
            <button type="submit" class="btn-danger">削除</button>
        </form>
    </c:if>
</main>
<%@ include file="/WEB-INF/jsp/fragments/notify-script.jsp" %>
</body>
</html>
