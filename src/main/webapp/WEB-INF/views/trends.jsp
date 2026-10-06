<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="com.takabridge.model.Currency" %>
<%@ page import="com.takabridge.model.Trend" %>
<%@ page import="com.takabridge.util.Html" %>
<%@ page import="com.takabridge.util.MoneyFormatter" %>
<%
    request.setAttribute("title", "Rate Trends");

    List<Currency> currencies = (List<Currency>) request.getAttribute("currencies");
    Trend trend = (Trend) request.getAttribute("trend");
    String error = (String) request.getAttribute("error");
    String dbError = (String) request.getAttribute("dbError");

    String from = (String) request.getAttribute("from");
    String to = (String) request.getAttribute("to");
    int days = (Integer) request.getAttribute("days");

    // The chart is drawn in a fixed 720 x 260 box and scaled by the browser.
    final double W = 720;
    final double H = 260;
%>
<%@ include file="header.jsp" %>

<div class="container page">

    <div class="page-head">
        <div>
            <h1 class="page-title">Rate trends</h1>
            <p class="page-subtitle">How one currency moved against another, day by day.</p>
        </div>
        <nav class="segmented" aria-label="Time range">
            <a href="<%= ctx %>/trends?from=<%= from %>&amp;to=<%= to %>&amp;days=7"
               class="<%= days == 7 ? "is-active" : "" %>"<%= days == 7 ? " aria-current=\"page\"" : "" %>>7 days</a>
            <a href="<%= ctx %>/trends?from=<%= from %>&amp;to=<%= to %>&amp;days=30"
               class="<%= days == 30 ? "is-active" : "" %>"<%= days == 30 ? " aria-current=\"page\"" : "" %>>30 days</a>
        </nav>
    </div>

    <% if (dbError != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(dbError) %></p>
    <% } %>

    <section class="card" aria-label="Choose a currency pair">
        <form class="pair-form" method="get" action="<%= ctx %>/trends">
            <input type="hidden" name="days" value="<%= days %>">
            <div class="field">
                <label for="from">1 unit of</label>
                <select id="from" name="from">
                    <% if (currencies != null) {
                           for (Currency c : currencies) { %>
                        <option value="<%= c.getCode() %>"<%= c.getCode().equals(from) ? " selected" : "" %>><%= Html.escape(c.getLabel()) %></option>
                    <%     }
                       } %>
                </select>
            </div>
            <span class="pair-arrow" aria-hidden="true">&rarr;</span>
            <div class="field">
                <label for="to">priced in</label>
                <select id="to" name="to">
                    <% if (currencies != null) {
                           for (Currency c : currencies) { %>
                        <option value="<%= c.getCode() %>"<%= c.getCode().equals(to) ? " selected" : "" %>><%= Html.escape(c.getLabel()) %></option>
                    <%     }
                       } %>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Show trend</button>
        </form>
    </section>

    <% if (error != null) { %>
        <p class="alert alert-danger space-top" role="alert"><%= Html.escape(error) %></p>
    <% } %>

    <% if (trend != null) {
           int dir = trend.getDirection();
           String changeClass = dir > 0 ? "change-up" : dir < 0 ? "change-down" : "change-flat";
           String arrow = dir > 0 ? "&#9650;" : dir < 0 ? "&#9660;" : ""; %>

        <div class="stats">
            <div class="stat">
                <p class="stat-label">Latest</p>
                <p class="stat-value"><%= MoneyFormatter.rate(trend.getLast()) %></p>
            </div>
            <div class="stat">
                <p class="stat-label">Change</p>
                <p class="stat-value change <%= changeClass %>"><%= arrow %> <%= MoneyFormatter.percent(trend.getChangePercent()) %></p>
            </div>
            <div class="stat">
                <p class="stat-label">Lowest</p>
                <p class="stat-value"><%= MoneyFormatter.rate(trend.getMin()) %></p>
            </div>
            <div class="stat">
                <p class="stat-label">Highest</p>
                <p class="stat-value"><%= MoneyFormatter.rate(trend.getMax()) %></p>
            </div>
        </div>

        <section class="card chart-card" aria-labelledby="chart-heading">
            <div class="card-head">
                <h2 class="card-title" id="chart-heading">1 <%= from %> in <%= to %></h2>
                <span class="section-note">
                    <%= MoneyFormatter.fullDay(trend.getFirstDate()) %> &ndash; <%= MoneyFormatter.fullDay(trend.getLastDate()) %>
                </span>
            </div>

            <div class="chart">
                <%-- Labels sit at the same heights as the dashed lines: 10%, 50% and 90%. --%>
                <div class="chart-y" aria-hidden="true">
                    <span style="top: 10%"><%= MoneyFormatter.rate(trend.getMax()) %></span>
                    <span style="top: 50%"><%= MoneyFormatter.rate(trend.getMid()) %></span>
                    <span style="top: 90%"><%= MoneyFormatter.rate(trend.getMin()) %></span>
                </div>

                <svg viewBox="0 0 <%= (int) W %> <%= (int) H %>" role="img"
                     aria-label="Line chart of 1 <%= from %> in <%= to %> over <%= trend.getDays() %> days, from <%= MoneyFormatter.rate(trend.getFirst()) %> to <%= MoneyFormatter.rate(trend.getLast()) %>">
                    <line class="chart-grid" x1="0" x2="<%= (int) W %>" y1="<%= H * 0.1 %>" y2="<%= H * 0.1 %>"/>
                    <line class="chart-grid" x1="0" x2="<%= (int) W %>" y1="<%= H * 0.5 %>" y2="<%= H * 0.5 %>"/>
                    <line class="chart-grid" x1="0" x2="<%= (int) W %>" y1="<%= H * 0.9 %>" y2="<%= H * 0.9 %>"/>

                    <polygon class="chart-area" points="<%= trend.getArea(W, H) %>"/>
                    <polyline class="chart-line" points="<%= trend.getPolyline(W, H) %>"/>

                    <%-- Hovering a dot shows the browser's own tooltip, no script needed. --%>
                    <% for (Trend.Point p : trend.getPoints(W, H)) { %>
                        <circle class="chart-dot" cx="<%= String.format(Locale.US, "%.1f", p.x()) %>"
                                cy="<%= String.format(Locale.US, "%.1f", p.y()) %>" r="<%= trend.getDays() > 10 ? 3 : 4.5 %>">
                            <title><%= MoneyFormatter.fullDay(p.date()) %>: <%= MoneyFormatter.rate(p.rate()) %> <%= to %></title>
                        </circle>
                    <% } %>
                </svg>
            </div>

            <div class="chart-x" aria-hidden="true">
                <span><%= MoneyFormatter.shortDay(trend.getFirstDate()) %></span>
                <span><%= MoneyFormatter.shortDay(trend.getLastDate()) %></span>
            </div>
        </section>

        <section class="section" aria-labelledby="daily-heading">
            <div class="section-head">
                <h2 class="section-title" id="daily-heading">Day by day</h2>
                <a class="link-more" href="<%= ctx %>/alerts">Set an alert for this pair</a>
            </div>
            <div class="table-wrap">
                <table class="table">
                    <thead>
                        <tr>
                            <th scope="col">Date</th>
                            <th scope="col" class="num">1 <%= from %> in <%= to %></th>
                            <th scope="col" class="num">Change from day before</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Trend.Point p : trend.getPointsNewestFirst(W, H)) {
                           BigDecimal dayChange = p.dayChange(); %>
                        <tr>
                            <td><%= MoneyFormatter.fullDay(p.date()) %></td>
                            <td class="num strong"><%= MoneyFormatter.rate(p.rate()) %></td>
                            <td class="num">
                                <% if (dayChange != null) {
                                       int d = dayChange.signum(); %>
                                    <span class="change <%= d > 0 ? "change-up" : d < 0 ? "change-down" : "change-flat" %>">
                                        <%= d > 0 ? "&#9650;" : d < 0 ? "&#9660;" : "" %> <%= MoneyFormatter.percent(dayChange) %>
                                    </span>
                                <% } else { %>
                                    <span class="subtle">&mdash;</span>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <p class="section-note space-top-sm">
                Past daily rates from the free fawazahmed0 currency API, kept in memory on the server.
            </p>
        </section>
    <% } %>

</div>

<%@ include file="footer.jsp" %>
