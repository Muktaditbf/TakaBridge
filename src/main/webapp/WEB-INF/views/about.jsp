<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("title", "About");
%>
<%@ include file="header.jsp" %>

<div class="container page page-narrow">

    <h1 class="page-title">About TakaBridge</h1>
    <p class="page-subtitle">Simple currency conversion, with the Bangladeshi Taka at the centre.</p>

    <div class="card">
        <h2 class="card-title">What it does</h2>
        <p class="card-text">
            TakaBridge converts an amount between 23 popular world currencies, with the
            Bangladeshi Taka at the centre. Every conversion shows the figure, the rate
            that produced it, and is saved so you can look back at what you checked.
        </p>
    </div>

    <div class="card">
        <h2 class="card-title">What makes it different</h2>
        <ul class="feature-list">
            <li>
                <strong>Live daily rates.</strong> A background thread downloads fresh rates
                every day and saves them in Oracle in one transaction.
            </li>
            <li>
                <strong>Smart route finder.</strong> Rarely traded pairs cost more to exchange.
                TakaBridge tries every route with one stop in between - for example
                BDT &rarr; USD &rarr; INR - and shows the one that leaves you the most money.
            </li>
            <li>
                <strong>Remittance calculator.</strong> Shows how many taka a family at home really
                receives after the transfer fee, the exchange margin and the government's 2.5% bonus,
                and compares a bank, an exchange house and an online app.
            </li>
            <li>
                <strong>Rate trends.</strong> 7- and 30-day line charts of any pair, drawn on
                the server as SVG. Past rates are cached in memory, not stored in the database.
            </li>
            <li>
                <strong>Rate alerts.</strong> "Tell me when 1 USD goes below 120 BDT." Alerts are
                kept in a cookie in your browser and checked against today's rate on every visit.
            </li>
        </ul>
    </div>

    <div class="card">
        <h2 class="card-title">How a conversion is calculated</h2>
        <p class="card-text">
            Each currency is stored with one number: how many units of it equal one US
            Dollar. Converting from A to B is therefore a single division and a single
            multiplication.
        </p>
        <p class="code-line">rate = rate of B &divide; rate of A</p>
        <p class="code-line">result = amount &times; rate</p>
        <p class="card-text">
            All of this arithmetic uses Java's <code>BigDecimal</code> rather than
            <code>double</code>. A double stores fractions in binary and quietly loses
            paisa on large amounts, which is never acceptable when the number is money.
        </p>
    </div>

    <div class="card">
        <h2 class="card-title">How it is built</h2>
        <dl class="spec">
            <dt>Language</dt><dd>Java, on Jakarta Servlets</dd>
            <dt>Pages</dt><dd>JSP, HTML5 and hand-written CSS</dd>
            <dt>Database</dt><dd>Oracle Database 21c Express Edition (XE), reached through JDBC</dd>
            <dt>Server</dt><dd>Apache Tomcat 11</dd>
            <dt>Libraries</dt><dd>One: the Oracle JDBC driver</dd>
        </dl>
        <p class="card-text">
            There is no JavaScript anywhere in TakaBridge and no CSS framework. Even the
            dark mode is done on the server: the toggle is an ordinary link that flips a
            value stored in a cookie, and the page is drawn in the colours that value
            selects. The trend charts are SVG drawn by the server, and their tooltips are
            the browser's own.
        </p>
    </div>

    <div class="card">
        <h2 class="card-title">A note on the rates</h2>
        <p class="card-text">
            Rates are downloaded once a day from the free ExchangeRate-API feed and saved
            in the database, so the converter keeps working even when the internet is down.
            They are daily reference rates, not the exact rate a bank will give you, so
            please do not use TakaBridge to price a real transaction.
        </p>
    </div>

</div>

<%@ include file="footer.jsp" %>
