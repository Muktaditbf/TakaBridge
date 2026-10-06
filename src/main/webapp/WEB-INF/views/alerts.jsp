<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.takabridge.model.Currency" %>
<%@ page import="com.takabridge.model.RateAlert" %>
<%@ page import="com.takabridge.util.Html" %>
<%@ page import="com.takabridge.util.MoneyFormatter" %>
<%
    request.setAttribute("title", "Rate Alerts");

    List<Currency> currencies = (List<Currency>) request.getAttribute("currencies");
    List<RateAlert> alerts = (List<RateAlert>) request.getAttribute("alerts");
    int maxAlerts = (Integer) request.getAttribute("maxAlerts");

    Object error = request.getAttribute("error");
    Object notice = request.getAttribute("notice");
    String dbError = (String) request.getAttribute("dbError");

    boolean full = alerts != null && alerts.size() >= maxAlerts;
%>
<%@ include file="header.jsp" %>

<div class="container page page-narrow">

    <div class="page-head">
        <div>
            <h1 class="page-title">Rate alerts</h1>
            <p class="page-subtitle">
                Pick a target rate. Whenever you open TakaBridge, we check it against
                today's rate and tell you once it has been reached.
            </p>
        </div>
    </div>

    <% if (notice != null) { %>
        <p class="alert alert-info" role="status"><%= Html.escape(notice.toString()) %></p>
    <% } %>
    <% if (error != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(error.toString()) %></p>
    <% } %>
    <% if (dbError != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(dbError) %></p>
    <% } %>

    <section class="card" aria-labelledby="new-alert-heading">
        <div class="card-head">
            <h2 class="card-title" id="new-alert-heading">New alert</h2>
            <span class="section-note"><%= alerts == null ? 0 : alerts.size() %> of <%= maxAlerts %> used</span>
        </div>

        <% if (full) { %>
            <p class="card-text">You have reached the limit of <%= maxAlerts %> alerts. Remove one below to add another.</p>
        <% } else { %>
            <form class="alert-form" method="post" action="<%= ctx %>/alerts">
                <input type="hidden" name="action" value="add">
                <div class="field">
                    <label for="from">When 1</label>
                    <select id="from" name="from">
                        <% if (currencies != null) {
                               for (Currency c : currencies) { %>
                            <option value="<%= c.getCode() %>"<%= "USD".equals(c.getCode()) ? " selected" : "" %> title="<%= Html.escape(c.getName()) %>"><%= c.getCode() %></option>
                        <%     }
                           } %>
                    </select>
                </div>
                <div class="field">
                    <label for="direction">Goes</label>
                    <select id="direction" name="direction">
                        <option value="B">below</option>
                        <option value="A">above</option>
                    </select>
                </div>
                <div class="field">
                    <label for="target">Target rate</label>
                    <input type="text" id="target" name="target" inputmode="decimal"
                           autocomplete="off" placeholder="120.00">
                </div>
                <div class="field">
                    <label for="to">In</label>
                    <select id="to" name="to">
                        <% if (currencies != null) {
                               for (Currency c : currencies) { %>
                            <option value="<%= c.getCode() %>"<%= "BDT".equals(c.getCode()) ? " selected" : "" %> title="<%= Html.escape(c.getName()) %>"><%= c.getCode() %></option>
                        <%     }
                           } %>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary">Add alert</button>
            </form>
        <% } %>
    </section>

    <section class="section" aria-labelledby="your-alerts-heading">
        <div class="section-head">
            <h2 class="section-title" id="your-alerts-heading">Your alerts</h2>
            <span class="section-note">Saved in this browser for a year</span>
        </div>

        <% if (alerts == null || alerts.isEmpty()) { %>
            <div class="empty">
                <p class="empty-title">No alerts yet</p>
                <p class="empty-text">For example: tell me when 1 USD goes below 120 BDT.</p>
            </div>
        <% } else { %>
            <ul class="alert-list">
                <% for (int i = 0; i < alerts.size(); i++) {
                       RateAlert a = alerts.get(i);
                       boolean hit = a.isTriggered(); %>
                    <li class="alert-item<%= hit ? " is-hit" : "" %>">
                        <div>
                            <p class="alert-rule">
                                1 <%= a.getFromCode() %> <%= a.getDirection().getWords() %>
                                <%= MoneyFormatter.rate(a.getTarget()) %> <%= a.getToCode() %>
                            </p>
                            <p class="alert-now">
                                <% if (a.getCurrentRate() != null) { %>
                                    Today: <%= MoneyFormatter.rate(a.getCurrentRate()) %> <%= a.getToCode() %>
                                    <% if (!hit) { %>&middot; <%= MoneyFormatter.money(a.getDistancePercent()) %>% to go<% } %>
                                <% } else { %>
                                    Today's rate is not available right now.
                                <% } %>
                            </p>
                        </div>
                        <span class="badge <%= hit ? "badge-hit" : "badge-wait" %>"><%= hit ? "Reached" : "Waiting" %></span>
                        <form method="post" action="<%= ctx %>/alerts">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="index" value="<%= i %>">
                            <button type="submit" class="btn btn-quiet btn-small"
                                    aria-label="Remove the alert for <%= a.getFromCode() %> in <%= a.getToCode() %>">Remove</button>
                        </form>
                    </li>
                <% } %>
            </ul>
        <% } %>
    </section>

</div>

<%@ include file="footer.jsp" %>
