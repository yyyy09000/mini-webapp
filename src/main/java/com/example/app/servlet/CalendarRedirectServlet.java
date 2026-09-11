package com.example.app.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 旧 URL /calendar をルート / へ寄せる。
 */
@WebServlet(name = "CalendarRedirectServlet", urlPatterns = {"/calendar"})
public class CalendarRedirectServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getQueryString();
        resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        resp.setHeader("Location", CalendarServlet.homeUrl(req, query));
    }
}
