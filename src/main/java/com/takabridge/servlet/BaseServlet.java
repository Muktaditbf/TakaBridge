package com.takabridge.servlet;

import com.takabridge.service.RateHistoryClient;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The behaviour every TakaBridge page shares.
 *
 * Each servlet below inherits from this one, so the theme, the character set
 * and the way a view is opened are written once instead of five times. This
 * is inheritance doing exactly the job it is meant for.
 */
public abstract class BaseServlet extends HttpServlet {

    /** Remembers the chosen theme for a year, even after the browser closes. */
    protected static final String THEME_COOKIE = "tb_theme";
    protected static final String THEME_SESSION = "theme";

    protected static final String LIGHT = "light";
    protected static final String DARK = "dark";

    /** Where the JSP files live. They sit under WEB-INF so nobody can open them directly. */
    private static final String VIEW_FOLDER = "/WEB-INF/views/";

    /**
     * Works out which theme this visitor is using: the session first because
     * it is the fastest, then the cookie which survives a browser restart,
     * and light mode when there is neither.
     */
    protected String currentTheme(HttpServletRequest request) {
        Object inSession = request.getSession().getAttribute(THEME_SESSION);
        if (DARK.equals(inSession) || LIGHT.equals(inSession)) {
            return (String) inSession;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (THEME_COOKIE.equals(cookie.getName()) && DARK.equals(cookie.getValue())) {
                    request.getSession().setAttribute(THEME_SESSION, DARK);
                    return DARK;
                }
            }
        }
        return LIGHT;
    }

    /**
     * The shared store of past rates, created by DailyRateUpdater at startup.
     * Null only if the listener did not run.
     */
    protected RateHistoryClient rateHistory() {
        return (RateHistoryClient) getServletContext().getAttribute(RateHistoryClient.CONTEXT_KEY);
    }

    /**
     * Opens a JSP after handing it the two things every page needs: the theme
     * that decides the colours, and the name of the page so the navigation bar
     * can highlight the right link.
     */
    protected void render(HttpServletRequest request, HttpServletResponse response,
                          String viewName, String pageKey) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        request.setAttribute("theme", currentTheme(request));
        request.setAttribute("page", pageKey);

        request.getRequestDispatcher(VIEW_FOLDER + viewName).forward(request, response);
    }
}
