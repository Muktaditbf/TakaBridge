package com.takabridge.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Switches between light and dark mode.
 *
 * The toggle in the header is an ordinary link. Clicking it lands here, the
 * stored theme is flipped, and the visitor is sent straight back to the page
 * they were reading. No scripting of any kind is involved.
 */
@WebServlet("/theme")
public class ThemeServlet extends BaseServlet {

    /** One year, in seconds. */
    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String next = DARK.equals(currentTheme(request)) ? LIGHT : DARK;

        request.getSession().setAttribute(THEME_SESSION, next);

        Cookie cookie = new Cookie(THEME_COOKIE, next);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(ONE_YEAR);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        response.sendRedirect(safeReturnUrl(request));
    }

    /**
     * Sends the visitor back where they came from, but only if that address
     * belongs to this application. Anything else falls back to the home page,
     * so the link can never be used to bounce somebody to another site.
     */
    private String safeReturnUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String home = request.getContextPath() + "/home";

        if (referer == null || referer.isBlank()) {
            return home;
        }

        String base = request.getRequestURL().toString();
        base = base.substring(0, base.length() - request.getRequestURI().length())
             + request.getContextPath();

        return referer.startsWith(base) ? referer : home;
    }
}
