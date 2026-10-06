<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.takabridge.model.Conversion" %>
<%@ page import="com.takabridge.util.Html" %>
<%@ page import="com.takabridge.util.MoneyFormatter" %>
<%
    request.setAttribute("title", "Recent History");

    List<Conversion> history = (List<Conversion>) request.getAttribute("history");
    String dbError = (String) request.getAttribute("dbError");
    Object notice = request.getAttribute("notice");
%>
<%@ include file="header.jsp" %>

<div class="container page">

    <div class="page-head">
        <div>
            <h1 class="page-title">Recent history</h1>
            <p class="page-subtitle">Your last 50 conversions, newest first.</p>
        </div>
        <% if (history != null && !history.isEmpty()) { %>
            <a class="btn btn-quiet" href="<%= ctx %>/clear-history">Clear history</a>
        <% } %>
    </div>

    <% if (notice != null) { %>
        <p class="alert alert-info" role="status"><%= Html.escape(notice.toString()) %></p>
    <% } %>

    <% if (dbError != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(dbError) %></p>
    <% } %>

    <% if (history == null || history.isEmpty()) { %>
        <div class="empty empty-large">
            <p class="empty-title">Nothing here yet</p>
            <p class="empty-text">
                Conversions are saved automatically. Make one and it will show up here.
            </p>
            <a class="btn btn-primary" href="<%= ctx %>/home">Convert a currency</a>
        </div>
    <% } else { %>
        <div class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th scope="col">When</th>
                        <th scope="col">Conversion</th>
                        <th scope="col" class="num">Rate used</th>
                        <th scope="col" class="num">Result</th>
                    </tr>
                </thead>
                <tbody>
                <% for (Conversion c : history) { %>
                    <tr>
                        <td>
                            <span class="when-day"><%= MoneyFormatter.day(c.getCreatedAt()) %></span>
                            <span class="when-time"><%= MoneyFormatter.time(c.getCreatedAt()) %></span>
                        </td>
                        <td>
                            <%= MoneyFormatter.money(c.getAmount()) %>
                            <span class="chip"><%= c.getFromCode() %></span>
                            <span class="arrow" aria-hidden="true">&rarr;</span>
                            <span class="chip"><%= c.getToCode() %></span>
                        </td>
                        <td class="num subtle">
                            <%= MoneyFormatter.rate(c.getRateUsed()) %>
                        </td>
                        <td class="num strong">
                            <%= MoneyFormatter.money(c.getConverted()) %>
                            <span class="unit"><%= c.getToCode() %></span>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
    <% } %>

</div>

<%@ include file="footer.jsp" %>
