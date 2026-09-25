package com.example.app.servlet;

import com.example.app.dao.EventDao;
import com.example.app.model.Event;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 今日の予定を JSON で返す（ブラウザ通知用）。
 * GET /api/today-events
 */
@WebServlet(name = "TodayEventsServlet", urlPatterns = {"/api/today-events"})
public class TodayEventsServlet extends HttpServlet {

    private final EventDao eventDao = new EventDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        LocalDate today = LocalDate.now();
        try {
            List<Event> events = eventDao.findByDate(today);
            PrintWriter out = resp.getWriter();
            out.print("{\"date\":\"");
            out.print(today);
            out.print("\",\"events\":[");
            for (int i = 0; i < events.size(); i++) {
                if (i > 0) {
                    out.print(',');
                }
                writeEvent(out, events.get(i));
            }
            out.print("]}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"db\"}");
        }
    }

    private static void writeEvent(PrintWriter out, Event event) {
        out.print("{\"id\":");
        out.print(event.getId());
        out.print(",\"title\":");
        writeJsonString(out, event.getTitle());
        out.print(",\"eventDate\":\"");
        out.print(event.getEventDate());
        out.print("\",\"eventDateEnd\":\"");
        out.print(event.getEndDateOrStart());
        out.print("\",\"eventTime\":");
        if (event.getEventTime() == null) {
            out.print("null");
        } else {
            writeJsonString(out, event.getEventTimeLabel());
        }
        out.print(",\"description\":");
        writeJsonString(out, event.getDescription());
        out.print('}');
    }

    private static void writeJsonString(PrintWriter out, String value) {
        if (value == null) {
            out.print("null");
            return;
        }
        out.print('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.print("\\\"");
                case '\\' -> out.print("\\\\");
                case '\n' -> out.print("\\n");
                case '\r' -> out.print("\\r");
                case '\t' -> out.print("\\t");
                default -> {
                    if (c < 0x20) {
                        out.printf("\\u%04x", (int) c);
                    } else {
                        out.print(c);
                    }
                }
            }
        }
        out.print('"');
    }
}
