package com.takabridge.util;

import com.takabridge.model.RateAlert;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Keeps a visitor's rate alerts in a cookie, so no database table is needed.
 *
 * Each alert is written as FROM:TO:SIDE:TARGET and alerts are joined with "~",
 * for example   USD:BDT:B:120.5~EUR:BDT:A:140
 *
 * A cookie can be edited by its owner, so every part is checked again when it
 * is read back and anything that does not look right is simply ignored.
 */
public final class AlertCookie {

    public static final String NAME = "tb_alerts";

    /** A cookie is small, and five alerts is plenty for one person. */
    public static final int MAX_ALERTS = 5;

    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    private static final Pattern CODE = Pattern.compile("[A-Z]{3}");
    private static final BigDecimal MAX_TARGET = new BigDecimal("1000000000000");

    private AlertCookie() {
    }

    /** The visitor's alerts, oldest first. Never null. */
    public static List<RateAlert> read(HttpServletRequest request) {
        List<RateAlert> alerts = new ArrayList<>();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return alerts;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName()) && cookie.getValue() != null) {
                for (String item : cookie.getValue().split("~")) {
                    RateAlert alert = parse(item);
                    if (alert != null && alerts.size() < MAX_ALERTS) {
                        alerts.add(alert);
                    }
                }
            }
        }
        return alerts;
    }

    /** Saves the list, or deletes the cookie when the list is empty. */
    public static void write(HttpServletRequest request, HttpServletResponse response,
                             List<RateAlert> alerts) {
        StringBuilder value = new StringBuilder();
        for (RateAlert a : alerts) {
            if (value.length() > 0) {
                value.append('~');
            }
            value.append(a.getFromCode()).append(':')
                 .append(a.getToCode()).append(':')
                 .append(a.getDirection().getCode()).append(':')
                 .append(a.getTarget().stripTrailingZeros().toPlainString());
        }

        Cookie cookie = new Cookie(NAME, value.toString());
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(alerts.isEmpty() ? 0 : ONE_YEAR);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /** One FROM:TO:SIDE:TARGET item, or null if any part is wrong. */
    private static RateAlert parse(String item) {
        String[] parts = item.split(":");
        if (parts.length != 4
                || !CODE.matcher(parts[0]).matches()
                || !CODE.matcher(parts[1]).matches()
                || parts[0].equals(parts[1])) {
            return null;
        }
        RateAlert.Direction side = RateAlert.Direction.parse(parts[2]);
        if (side == null) {
            return null;
        }
        try {
            BigDecimal target = new BigDecimal(parts[3]);
            if (target.signum() <= 0 || target.compareTo(MAX_TARGET) > 0) {
                return null;
            }
            return new RateAlert(parts[0], parts[1], side, target);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
