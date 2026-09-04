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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 月カレンダーの表示・予定の登録/更新/削除。
 * GET  /calendar
 * GET  /calendar?action=new
 * GET  /calendar?action=edit&id=
 * POST /calendar  action=create|update|delete
 */
@WebServlet(name = "CalendarServlet", urlPatterns = {"/calendar"})
public class CalendarServlet extends HttpServlet {

    private final EventDao eventDao = new EventDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = Optional.ofNullable(req.getParameter("action")).orElse("month");
        try {
            switch (action) {
                case "new" -> {
                    YearMonth ym = resolveYearMonth(req);
                    Event event = new Event();
                    event.setEventDate(ym.atDay(Math.min(LocalDate.now().getDayOfMonth(), ym.lengthOfMonth())));
                    if (YearMonth.now().equals(ym)) {
                        event.setEventDate(LocalDate.now());
                    }
                    showForm(req, resp, event, "create", ym);
                }
                case "edit" -> {
                    long id = Long.parseLong(req.getParameter("id"));
                    Event event = eventDao.findById(id)
                            .orElseThrow(() -> new ServletException("予定が見つかりません: id=" + id));
                    YearMonth ym = YearMonth.from(event.getEventDate());
                    showForm(req, resp, event, "update", ym);
                }
                default -> showMonth(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("DB エラー", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = Optional.ofNullable(req.getParameter("action")).orElse("");
        try {
            switch (action) {
                case "create" -> {
                    Event event = bindEvent(req);
                    if (!validate(req, resp, event, "create")) {
                        return;
                    }
                    eventDao.insert(event);
                    redirectToMonth(req, resp, event.getEventDate());
                }
                case "update" -> {
                    Event event = bindEvent(req);
                    event.setId(Long.parseLong(req.getParameter("id")));
                    if (!validate(req, resp, event, "update")) {
                        return;
                    }
                    eventDao.update(event);
                    redirectToMonth(req, resp, event.getEventDate());
                }
                case "delete" -> {
                    long id = Long.parseLong(req.getParameter("id"));
                    Optional<Event> existing = eventDao.findById(id);
                    eventDao.delete(id);
                    if (existing.isPresent()) {
                        redirectToMonth(req, resp, existing.get().getEventDate());
                    } else {
                        YearMonth ym = resolveYearMonth(req);
                        resp.sendRedirect(req.getContextPath()
                                + "/calendar?year=" + ym.getYear() + "&month=" + ym.getMonthValue());
                    }
                }
                default -> throw new ServletException("不明な action: " + action);
            }
        } catch (SQLException e) {
            throw new ServletException("DB エラー", e);
        }
    }

    private void showMonth(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
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

    private void showForm(HttpServletRequest req, HttpServletResponse resp,
                          Event event, String mode, YearMonth ym)
            throws ServletException, IOException {
        req.setAttribute("event", event);
        req.setAttribute("mode", mode);
        req.setAttribute("year", ym.getYear());
        req.setAttribute("month", ym.getMonthValue());
        req.getRequestDispatcher("/WEB-INF/jsp/event-form.jsp").forward(req, resp);
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
            String timeText = blankToNull(req.getParameter("eventTime"));
            if (timeText != null) {
                event.setEventTime(LocalTime.parse(timeText));
            }
        } catch (DateTimeParseException ignored) {
            event.setEventTime(null);
        }
        return event;
    }

    private boolean validate(HttpServletRequest req, HttpServletResponse resp, Event event, String mode)
            throws ServletException, IOException {
        if (event.getTitle() == null || event.getEventDate() == null) {
            req.setAttribute("error", "タイトルと日付は必須です");
            YearMonth ym = event.getEventDate() != null
                    ? YearMonth.from(event.getEventDate())
                    : YearMonth.now();
            showForm(req, resp, event, mode, ym);
            return false;
        }
        return true;
    }

    private void redirectToMonth(HttpServletRequest req, HttpServletResponse resp, LocalDate date)
            throws IOException {
        YearMonth ym = YearMonth.from(date);
        resp.sendRedirect(req.getContextPath()
                + "/calendar?year=" + ym.getYear() + "&month=" + ym.getMonthValue());
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
            byDate.computeIfAbsent(event.getEventDate(), d -> new ArrayList<>()).add(event);
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
