package com.takabridge.util;

/**
 * Escapes text before it is written into a page.
 *
 * Anything a visitor typed - the amount, for example - is sent back to the
 * browser when the form is refilled. Escaping it first means a stray angle
 * bracket stays harmless text instead of becoming part of the markup.
 */
public final class Html {

    private Html() {
    }

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
