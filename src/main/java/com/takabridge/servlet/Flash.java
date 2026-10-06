package com.takabridge.servlet;

import jakarta.servlet.http.HttpSession;

/**
 * A tiny message box that survives exactly one redirect.
 *
 * TakaBridge follows the Post / Redirect / Get pattern: the form is posted,
 * the browser is redirected, and the result is read back here. That is why
 * pressing F5 after a conversion never records the same conversion twice.
 */
final class Flash {

    static final String RESULT = "flash.result";
    static final String ERROR = "flash.error";
    static final String FROM = "flash.from";
    static final String TO = "flash.to";
    static final String AMOUNT = "flash.amount";

    /** Kept apart from the converter messages so the two pages never mix them up. */
    static final String ALERT_ERROR = "flash.alertError";
    static final String ALERT_NOTICE = "flash.alertNotice";

    private Flash() {
    }

    /** Stores a value until the next page reads it. */
    static void put(HttpSession session, String key, Object value) {
        session.setAttribute(key, value);
    }

    /** Reads a value and removes it, so it is never shown a second time. */
    static Object take(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        session.removeAttribute(key);
        return value;
    }

    /** The same, for text, with a fallback when nothing was stored. */
    static String takeText(HttpSession session, String key, String fallback) {
        Object value = take(session, key);
        return (value == null) ? fallback : value.toString();
    }

    /** Empties the box completely, used by the Reset link. */
    static void clear(HttpSession session) {
        session.removeAttribute(RESULT);
        session.removeAttribute(ERROR);
        session.removeAttribute(FROM);
        session.removeAttribute(TO);
        session.removeAttribute(AMOUNT);
    }
}
