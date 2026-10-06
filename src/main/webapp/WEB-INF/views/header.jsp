<%--
    Shared top of every page: the document head, the sticky header bar,
    the navigation and the light / dark toggle.

    Included with <%@ include %>, so it carries no page directive of its own.
--%>
<%
    String ctx = request.getContextPath();

    String theme = (String) request.getAttribute("theme");
    if (theme == null) { theme = "light"; }

    String activePage = (String) request.getAttribute("page");
    if (activePage == null) { activePage = ""; }

    String pageTitle = (String) request.getAttribute("title");
    if (pageTitle == null) { pageTitle = "Currency Converter"; }

    boolean isDark = "dark".equals(theme);

    // Pages that check the alerts pass the ones that fired, for the red badge.
    java.util.List<?> hitList = (java.util.List<?>) request.getAttribute("triggeredAlerts");
    int alertsHit = (hitList == null) ? 0 : hitList.size();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= pageTitle %> &middot; TakaBridge</title>
    <meta name="description" content="TakaBridge - live daily exchange rates with the Bangladeshi Taka at the centre.">
    <meta name="theme-color" content="#004E3A">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="<%= theme %>">

<a class="skip-link" href="#main">Skip to main content</a>

<header class="site-header">
    <div class="container header-inner">

        <a class="wordmark" href="<%= ctx %>/home" aria-label="TakaBridge home">
            <span class="logo-mark" aria-hidden="true"></span>
            <span><span class="wordmark-taka">Taka</span><span class="wordmark-bridge">Bridge</span></span>
        </a>

        <nav class="site-nav" aria-label="Main">
            <a href="<%= ctx %>/home"
               class="<%= "home".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "home".equals(activePage) ? "aria-current=\"page\"" : "" %>>Home</a>

            <a href="<%= ctx %>/remittance"
               class="<%= "remittance".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "remittance".equals(activePage) ? "aria-current=\"page\"" : "" %>>Remittance</a>

            <a href="<%= ctx %>/trends"
               class="<%= "trends".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "trends".equals(activePage) ? "aria-current=\"page\"" : "" %>>Trends</a>

            <a href="<%= ctx %>/alerts"
               class="<%= "alerts".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "alerts".equals(activePage) ? "aria-current=\"page\"" : "" %>>Alerts<% if (alertsHit > 0) { %><span class="nav-badge" title="Alerts that reached their target"><%= alertsHit %></span><% } %></a>

            <a href="<%= ctx %>/history"
               class="<%= "history".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "history".equals(activePage) ? "aria-current=\"page\"" : "" %>>History</a>

            <a href="<%= ctx %>/about"
               class="<%= "about".equals(activePage) ? "nav-link is-active" : "nav-link" %>"
               <%= "about".equals(activePage) ? "aria-current=\"page\"" : "" %>>About</a>
        </nav>

        <%-- An ordinary link. ThemeServlet flips the stored theme and sends
             the visitor straight back here, so no scripting is needed. --%>
        <a class="theme-toggle" href="<%= ctx %>/theme"
           title="<%= isDark ? "Switch to light mode" : "Switch to dark mode" %>">
            <span class="theme-icon" aria-hidden="true"><%= isDark ? "&#9788;" : "&#9790;" %></span>
            <span class="theme-label"><%= isDark ? "Light" : "Dark" %></span>
        </a>

    </div>
</header>

<main id="main">
