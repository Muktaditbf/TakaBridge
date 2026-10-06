package com.takabridge.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The About page. It shows no data, but it still goes through a servlet so
 * that the theme and the navigation behave exactly like every other page.
 */
@WebServlet("/about")
public class AboutServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "about.jsp", "about");
    }
}
