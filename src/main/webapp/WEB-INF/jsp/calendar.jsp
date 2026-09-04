<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${year}年${month}月</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="wrap calendar-wrap">
    <p><a href="${pageContext.request.contextPath}/">← トップ</a></p>

    <div class="cal-header">
        <a href="${pageContext.request.contextPath}/calendar?year=${prevYear}&amp;month=${prevMonth}">← 前月</a>
        <h1>${year}年${month}月</h1>
        <a href="${pageContext.request.contextPath}/calendar?year=${nextYear}&amp;month=${nextMonth}">翌月 →</a>
    </div>

    <p>
        <a class="btn" href="${pageContext.request.contextPath}/calendar?action=new&amp;year=${year}&amp;month=${month}">予定を追加</a>
    </p>

    <table class="calendar">
        <thead>
        <tr>
            <th>日</th>
            <th>月</th>
            <th>火</th>
            <th>水</th>
            <th>木</th>
            <th>金</th>
            <th>土</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="week" items="${weeks}">
            <tr>
                <c:forEach var="cell" items="${week}">
                    <td class="${cell.currentMonth ? 'in-month' : 'out-month'}">
                        <div class="day-num">${cell.dayOfMonth}</div>
                        <ul class="day-events">
                            <c:forEach var="ev" items="${cell.events}">
                                <li>
                                    <a class="ev-link"
                                       href="${pageContext.request.contextPath}/calendar?action=edit&amp;id=${ev.id}">
                                        <c:if test="${not empty ev.eventTimeLabel}">
                                            <span class="ev-time"><c:out value="${ev.eventTimeLabel}"/></span>
                                        </c:if>
                                        <span class="ev-title"><c:out value="${ev.title}"/></span>
                                    </a>
                                </li>
                            </c:forEach>
                        </ul>
                    </td>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h2>この月の予定一覧</h2>
    <table>
        <thead>
        <tr>
            <th>日付</th>
            <th>時間</th>
            <th>タイトル</th>
            <th>説明</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="ev" items="${events}">
            <tr>
                <td>${ev.eventDate}</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty ev.eventTimeLabel}"><c:out value="${ev.eventTimeLabel}"/></c:when>
                        <c:otherwise>—</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/calendar?action=edit&amp;id=${ev.id}">
                        <c:out value="${ev.title}"/>
                    </a>
                </td>
                <td><c:out value="${ev.description}"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty events}">
            <tr>
                <td colspan="4">この月の予定はありません</td>
            </tr>
        </c:if>
        </tbody>
    </table>
</main>
<%@ include file="/WEB-INF/jsp/fragments/notify-script.jsp" %>
</body>
</html>
