package com.example.app.servlet;

import com.example.app.dao.EventDao;
import com.example.app.model.CalendarCell;
import com.example.app.model.Event;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 月カレンダーの表示・予定の登録/更新/削除。
 * URL は http://localhost:8080/ のみ。
 */
@WebServlet(name = "CalendarServlet", urlPatterns = {""})
public class CalendarServlet extends HttpServlet {

    private final EventDao eventDao = new EventDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            showMonth(req, resp);
        } catch (SQLException e) {
            throw new ServletException("DB エラー", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = Optional.ofNullable(req.getParameter("action")).orElse("");
        boolean ajax = isAjax(req);
        try {
            switch (action) {
                case "create" -> {
                    Event event = bindEvent(req);
                    if (!validate(req, resp, event, ajax)) {
                        return;
                    }
                    eventDao.insert(event);
                    if (ajax) {
                        writeEventJson(resp, event);
                    } else {
                        redirectToMonth(req, resp, event.getEventDate());
                    }
                }
                case "update" -> {
                    Event event = bindEvent(req);
                    event.setId(Long.parseLong(req.getParameter("id")));
                    if (!validate(req, resp, event, ajax)) {
                        return;
                    }
                    eventDao.update(event);
                    if (ajax) {
                        writeEventJson(resp, event);
                    } else {
                        redirectToMonth(req, resp, event.getEventDate());
                    }
                }
                case "delete" -> {
                    long id = Long.parseLong(req.getParameter("id"));
                    Optional<Event> existing = eventDao.findById(id);
                    eventDao.delete(id);
                    if (ajax) {
                        resp.setCharacterEncoding("UTF-8");
                        resp.setContentType("application/json; charset=UTF-8");
                        resp.getWriter().print("{\"ok\":true,\"deleted\":" + id + "}");
                    } else if (existing.isPresent()) {
                        redirectToMonth(req, resp, existing.get().getEventDate());
                    } else {
                        YearMonth ym = resolveYearMonth(req);
                        resp.sendRedirect(homeUrl(req, "year=" + ym.getYear() + "&month=" + ym.getMonthValue()));
                    }
                }
                default -> throw new ServletException("不明な action: " + action);
            }
        } catch (SQLException e) {
            if (ajax) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().print("{\"ok\":false,\"error\":\"DB エラー\"}");
                return;
            }
            throw new ServletException("DB エラー", e);
        }
    }

    private void showMonth(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        // 同じ日・時間・内容の重複があれば片方を削除
        eventDao.deleteAllDuplicates();
        YearMonth ym = resolveYearMonth(req);
        List<Event> events = eventDao.findByMonth(ym.getYear(), ym.getMonthValue());
        List<List<CalendarCell>> weeks = buildWeeks(ym, events);
        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);

        req.setAttribute("year", ym.getYear());
        req.setAttribute("month", ym.getMonthValue());
        req.setAttribute("prevYear", prev.getYear());
        req.setAttribute("prevMonth", prev.getMonthValue());
        req.setAttribute("nextYear", next.getYear());
        req.setAttribute("nextMonth", next.getMonthValue());
        req.setAttribute("weeks", weeks);
        req.setAttribute("events", events);
        req.getRequestDispatcher("/WEB-INF/jsp/calendar.jsp").forward(req, resp);
    }

    private Event bindEvent(HttpServletRequest req) {
        Event event = new Event();
        event.setTitle(blankToNull(req.getParameter("title")));
        event.setDescription(blankToNull(req.getParameter("description")));
        try {
            String dateText = blankToNull(req.getParameter("eventDate"));
            if (dateText != null) {
                event.setEventDate(LocalDate.parse(dateText));
            }
        } catch (DateTimeParseException ignored) {
            event.setEventDate(null);
        }
        try {
            String endText = blankToNull(req.getParameter("eventDateEnd"));
            if (endText != null) {
                event.setEventDateEnd(LocalDate.parse(endText));
            }
        } catch (DateTimeParseException ignored) {
            event.setEventDateEnd(null);
        }
        normalizeDateRange(event);
        try {
            String timeText = blankToNull(req.getParameter("eventTime"));
            if (timeText != null) {
                event.setEventTime(LocalTime.parse(timeText));
            }
        } catch (DateTimeParseException ignored) {
            event.setEventTime(null);
        }
        return event;
    }

    private static void normalizeDateRange(Event event) {
        if (event.getEventDate() == null) {
            return;
        }
        if (event.getEventDateEnd() == null) {
            event.setEventDateEnd(event.getEventDate());
            return;
        }
        if (event.getEventDateEnd().isBefore(event.getEventDate())) {
            LocalDate tmp = event.getEventDate();
            event.setEventDate(event.getEventDateEnd());
            event.setEventDateEnd(tmp);
        }
    }

    private boolean validate(HttpServletRequest req, HttpServletResponse resp, Event event, boolean ajax)
            throws IOException {
        if (event.getTitle() == null || event.getEventDate() == null) {
            writeValidationError(resp, ajax, "タイトルと日付は必須です");
            return false;
        }
        String action = Optional.ofNullable(req.getParameter("action")).orElse("");
        LocalDate today = LocalDate.now();
        if ("create".equals(action)) {
            if (event.getEndDateOrStart().isBefore(today)) {
                writeValidationError(resp, ajax, "過去の日付には予定を追加できません");
                return false;
            }
            if (event.getEventDate().isBefore(today)) {
                event.setEventDate(today);
            }
        }
        long days = ChronoUnit.DAYS.between(event.getEventDate(), event.getEndDateOrStart()) + 1;
        if (days > 93) {
            writeValidationError(resp, ajax, "一度に指定できるのは最大93日分です");
            return false;
        }
        return true;
    }

    private static void writeValidationError(HttpServletResponse resp, boolean ajax, String message)
            throws IOException {
        if (ajax) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().print("{\"ok\":false,\"error\":" + jsonString(message) + "}");
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
        }
    }

    private static boolean isAjax(HttpServletRequest req) {
        return "1".equals(req.getParameter("ajax"))
                || "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

    private static void writeEventJson(HttpServletResponse resp, Event event) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"ok\":true,\"id\":").append(event.getId());
        sb.append(",\"title\":").append(jsonString(event.getTitle()));
        sb.append(",\"eventDate\":\"").append(event.getEventDate()).append('"');
        sb.append(",\"eventDateEnd\":\"").append(event.getEndDateOrStart()).append('"');
        sb.append(",\"eventTime\":");
        if (event.getEventTime() == null) {
            sb.append("null");
        } else {
            sb.append(jsonString(event.getEventTimeLabel()));
        }
        sb.append(",\"description\":").append(jsonString(event.getDescription()));
        sb.append('}');
        resp.getWriter().print(sb);
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private void redirectToMonth(HttpServletRequest req, HttpServletResponse resp, LocalDate date)
            throws IOException {
        YearMonth ym = YearMonth.from(date);
        resp.sendRedirect(homeUrl(req, "year=" + ym.getYear() + "&month=" + ym.getMonthValue()));
    }

    /** コンテキストルート（/）への URL を組み立てる */
    static String homeUrl(HttpServletRequest req, String query) {
        String base = req.getContextPath().isEmpty() ? "/" : req.getContextPath() + "/";
        if (query == null || query.isBlank()) {
            return base;
        }
        return base + "?" + query;
    }

    private static YearMonth resolveYearMonth(HttpServletRequest req) {
        String yearText = req.getParameter("year");
        String monthText = req.getParameter("month");
        if (yearText == null || monthText == null || yearText.isBlank() || monthText.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.of(Integer.parseInt(yearText), Integer.parseInt(monthText));
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private static List<List<CalendarCell>> buildWeeks(YearMonth ym, List<Event> events) {
        Map<LocalDate, List<Event>> byDate = new HashMap<>();
        for (Event event : events) {
            LocalDate day = event.getEventDate();
            LocalDate end = event.getEndDateOrStart();
            if (day == null) {
                continue;
            }
            if (end == null || end.isBefore(day)) {
                end = day;
            }
            int n = 0;
            for (LocalDate d = day; !d.isAfter(end) && n < 93; d = d.plusDays(1), n++) {
                byDate.computeIfAbsent(d, x -> new ArrayList<>()).add(event);
            }
        }

        LocalDate first = ym.atDay(1);
        int leading = first.getDayOfWeek().getValue() % 7;
        LocalDate start = first.minusDays(leading);

        List<List<CalendarCell>> weeks = new ArrayList<>();
        LocalDate cursor = start;
        for (int w = 0; w < 6; w++) {
            List<CalendarCell> week = new ArrayList<>(7);
            for (int d = 0; d < 7; d++) {
                boolean inMonth = YearMonth.from(cursor).equals(ym);
                CalendarCell cell = new CalendarCell(cursor, inMonth);
                List<Event> dayEvents = byDate.get(cursor);
                if (dayEvents != null) {
                    dayEvents.forEach(cell::addEvent);
                }
                week.add(cell);
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
            if (w >= 4 && week.stream().noneMatch(CalendarCell::isCurrentMonth)) {
                weeks.remove(weeks.size() - 1);
                break;
            }
        }
        return weeks;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
