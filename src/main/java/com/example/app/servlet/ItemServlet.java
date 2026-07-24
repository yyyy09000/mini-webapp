package com.example.app.servlet;

import com.example.app.dao.ItemDao;
import com.example.app.model.Item;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * items の一覧・登録・更新・削除。
 * GET  /items           一覧
 * GET  /items?action=new  新規フォーム
 * GET  /items?action=edit&id=  編集フォーム
 * POST /items  action=create|update|delete
 */
@WebServlet(name = "ItemServlet", urlPatterns = {"/items"})
public class ItemServlet extends HttpServlet {

    private final ItemDao itemDao = new ItemDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = Optional.ofNullable(req.getParameter("action")).orElse("list");
        try {
            switch (action) {
                case "new" -> {
                    req.setAttribute("item", new Item());
                    req.setAttribute("mode", "create");
                    req.getRequestDispatcher("/WEB-INF/jsp/item-form.jsp").forward(req, resp);
                }
                case "edit" -> {
                    long id = Long.parseLong(req.getParameter("id"));
                    Item item = itemDao.findById(id)
                            .orElseThrow(() -> new ServletException("アイテムが見つかりません: id=" + id));
                    req.setAttribute("item", item);
                    req.setAttribute("mode", "update");
                    req.getRequestDispatcher("/WEB-INF/jsp/item-form.jsp").forward(req, resp);
                }
                default -> {
                    req.setAttribute("items", itemDao.findAll());
                    req.getRequestDispatcher("/WEB-INF/jsp/items.jsp").forward(req, resp);
                }
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
                    Item item = new Item(
                            blankToNull(req.getParameter("name")),
                            blankToNull(req.getParameter("description"))
                    );
                    if (item.getName() == null || item.getName().isBlank()) {
                        req.setAttribute("error", "名前は必須です");
                        req.setAttribute("item", item);
                        req.setAttribute("mode", "create");
                        req.getRequestDispatcher("/WEB-INF/jsp/item-form.jsp").forward(req, resp);
                        return;
                    }
                    itemDao.insert(item);
                }
                case "update" -> {
                    Item item = new Item();
                    item.setId(Long.parseLong(req.getParameter("id")));
                    item.setName(blankToNull(req.getParameter("name")));
                    item.setDescription(blankToNull(req.getParameter("description")));
                    itemDao.update(item);
                }
                case "delete" -> itemDao.delete(Long.parseLong(req.getParameter("id")));
                default -> throw new ServletException("不明な action: " + action);
            }
            resp.sendRedirect(req.getContextPath() + "/items");
        } catch (SQLException e) {
            throw new ServletException("DB エラー", e);
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
