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
<main class="wrap calendar-wrap"
      data-context="${pageContext.request.contextPath}"
      data-year="${year}"
      data-month="${month}">
    <div class="cal-header">
        <a href="${pageContext.request.contextPath}/?year=${prevYear}&amp;month=${prevMonth}">← 前月</a>
        <h1>${year}年${month}月</h1>
        <a href="${pageContext.request.contextPath}/?year=${nextYear}&amp;month=${nextMonth}">翌月 →</a>
    </div>

    <p>
        <button type="button" class="btn" id="btn-new-event">予定を追加</button>

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
                    <td class="${cell.currentMonth ? 'in-month' : 'out-month'} js-day-cell" data-date="${cell.date}">
                        <button type="button" class="day-num js-add-on-date" data-date="${cell.date}" title="この日に予定を追加">
                            ${cell.dayOfMonth}
                        </button>
                        <ul class="day-events">
                            <c:forEach var="ev" items="${cell.events}">
                                <li>
                                    <button type="button"
                                            class="ev-link js-open-event"
                                            data-id="${ev.id}"
                                            data-title="<c:out value='${ev.title}'/>"
                                            data-date="${ev.eventDate}"
                                            data-time="<c:out value='${ev.eventTimeLabel}'/>"
                                            data-description="<c:out value='${ev.description}'/>">
                                        <c:if test="${not empty ev.eventTimeLabel}">
                                            <span class="ev-time"><c:out value="${ev.eventTimeLabel}"/></span>
                                        </c:if>
                                        <span class="ev-title"><c:out value="${ev.title}"/></span>
                                    </button>
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

    <table class="event-list">
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
            <tr class="event-row"
                data-id="${ev.id}"
                data-title="<c:out value='${ev.title}'/>"
                data-date="${ev.eventDate}"
                data-time="<c:out value='${ev.eventTimeLabel}'/>"
                data-description="<c:out value='${ev.description}'/>">
                <td class="js-inline-edit" data-field="date" data-input="date">${ev.eventDate}</td>
                <td class="js-inline-edit" data-field="time" data-input="time">
                    <c:choose>
                        <c:when test="${not empty ev.eventTimeLabel}"><c:out value="${ev.eventTimeLabel}"/></c:when>
                        <c:otherwise>—</c:otherwise>
                    </c:choose>
                </td>
                <td class="js-inline-edit" data-field="title" data-input="text"><c:out value="${ev.title}"/></td>
                <td class="js-inline-edit" data-field="description" data-input="text"><c:out value="${ev.description}"/></td>
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

<dialog id="event-dialog" class="event-dialog">
    <form method="dialog" id="event-form" class="form">
        <h2 id="event-dialog-title">予定の編集</h2>
        <p id="event-error" class="error" hidden></p>
        <input type="hidden" id="event-id" value="">
        <label>
            タイトル
            <input type="text" id="event-title" required maxlength="100">
        </label>
        <label>
            開始日
            <input type="date" id="event-date" required>
        </label>
        <label id="event-end-wrap">
            終了日
            <input type="date" id="event-date-end">
        </label>
        <label>
            時間（任意）
            <input type="time" id="event-time">
        </label>
        <label>
            説明
            <textarea id="event-description" rows="4" maxlength="500"></textarea>
        </label>
        <div class="dialog-actions">
            <button type="submit" class="btn" id="event-save">保存</button>
            <button type="button" class="btn btn-ghost" id="event-cancel">キャンセル</button>
            <button type="button" class="btn btn-danger" id="event-delete" hidden>削除</button>
        </div>
    </form>
</dialog>

<script src="${pageContext.request.contextPath}/js/inline-edit.js"
        data-context="${pageContext.request.contextPath}"></script>
<%@ include file="/WEB-INF/jsp/fragments/notify-script.jsp" %>
</body>
</html>
