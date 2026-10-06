<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.takabridge.model.Conversion" %>
<%@ page import="com.takabridge.model.Currency" %>
<%@ page import="com.takabridge.model.RateAlert" %>
<%@ page import="com.takabridge.model.Route" %>
<%@ page import="com.takabridge.model.Trend" %>
<%@ page import="com.takabridge.util.Html" %>
<%@ page import="com.takabridge.util.MoneyFormatter" %>
<%
    request.setAttribute("title", "Currency Converter");

    List<Currency> currencies = (List<Currency>) request.getAttribute("currencies");
    List<Conversion> recent = (List<Conversion>) request.getAttribute("recent");

    Conversion result = (Conversion) request.getAttribute("result");
    String error = (String) request.getAttribute("error");
    String dbError = (String) request.getAttribute("dbError");
    LocalDateTime ratesUpdated = (LocalDateTime) request.getAttribute("ratesUpdated");

    Route bestRoute = (Route) request.getAttribute("bestRoute");
    Route directRoute = (Route) request.getAttribute("directRoute");
    BigDecimal saved = (BigDecimal) request.getAttribute("routeSaving");

    Trend miniTrend = (Trend) request.getAttribute("miniTrend");
    Map<String, BigDecimal> weekChange = (Map<String, BigDecimal>) request.getAttribute("weekChange");

    List<RateAlert> triggeredAlerts = (List<RateAlert>) request.getAttribute("triggeredAlerts");
    Integer alertCount = (Integer) request.getAttribute("alertCount");

    String formFrom = (String) request.getAttribute("formFrom");
    String formTo = (String) request.getAttribute("formTo");
    String formAmount = (String) request.getAttribute("formAmount");
%>
<%@ include file="header.jsp" %>

<section class="hero">
    <div class="container">
        <p class="live-pill">
            <span class="live-dot" aria-hidden="true"></span>
            <% if (ratesUpdated != null) { %>
                Live rates &middot; updated <%= MoneyFormatter.day(ratesUpdated) %>, <%= MoneyFormatter.time(ratesUpdated) %>
            <% } else { %>
                Daily reference rates
            <% } %>
        </p>
        <h1 class="hero-title">Every currency, measured in taka.</h1>
        <p class="hero-subtitle">
            Convert between 23 world currencies, find the cheapest way to exchange,
            and see what your family gets when you <a class="hero-link" href="<%= ctx %>/remittance">send money home</a>.
        </p>
    </div>
</section>

<div class="container hero-content">

    <%-- Rate alerts that reached their target --%>
    <% if (triggeredAlerts != null) {
           for (RateAlert a : triggeredAlerts) { %>
        <div class="banner" role="status">
            <span class="banner-dot" aria-hidden="true"></span>
            <p class="banner-text">
                <strong>Rate alert:</strong>
                1 <%= a.getFromCode() %> <%= a.getDirection() == RateAlert.Direction.ABOVE ? "is now above" : "is now below" %>
                <%= MoneyFormatter.rate(a.getTarget()) %> <%= a.getToCode() %>
                &mdash; today it is <strong><%= MoneyFormatter.rate(a.getCurrentRate()) %></strong>.
            </p>
            <a class="link-more" href="<%= ctx %>/alerts">Manage</a>
        </div>
    <%     }
       } %>

    <% if (dbError != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(dbError) %></p>
    <% } %>

    <section class="card converter" id="converter" aria-labelledby="converter-heading">
        <h2 class="card-title" id="converter-heading">Currency converter</h2>

        <% if (error != null) { %>
            <p class="alert alert-danger" role="alert"><%= Html.escape(error) %></p>
        <% } %>

        <form class="converter-form" method="post" action="<%= ctx %>/convert">

            <%-- This hidden button is the first submit in the form, so pressing
                 Enter converts instead of swapping. --%>
            <button type="submit" name="action" value="convert" class="visually-hidden"
                    tabindex="-1" aria-hidden="true">Convert</button>

            <div class="converter-grid">
                <div class="field field-amount">
                    <label for="amount">Amount</label>
                    <input type="text" id="amount" name="amount" inputmode="decimal"
                           autocomplete="off" placeholder="100"
                           value="<%= Html.escape(formAmount) %>">
                </div>

                <div class="field">
                    <label for="from">From</label>
                    <select id="from" name="from">
                        <% if (currencies != null) {
                               for (Currency c : currencies) {
                                   boolean selected = c.getCode().equals(formFrom); %>
                            <option value="<%= c.getCode() %>"<%= selected ? " selected" : "" %>><%= Html.escape(c.getLabel()) %></option>
                        <%     }
                           } %>
                    </select>
                </div>

                <button type="submit" name="action" value="swap" class="btn-swap"
                        title="Swap the two currencies">
                    <span aria-hidden="true">&#8644;</span>
                    <span class="visually-hidden">Swap the two currencies</span>
                </button>

                <div class="field">
                    <label for="to">To</label>
                    <select id="to" name="to">
                        <% if (currencies != null) {
                               for (Currency c : currencies) {
                                   boolean selected = c.getCode().equals(formTo); %>
                            <option value="<%= c.getCode() %>"<%= selected ? " selected" : "" %>><%= Html.escape(c.getLabel()) %></option>
                        <%     }
                           } %>
                    </select>
                </div>
            </div>

            <div class="form-actions">
                <button type="submit" name="action" value="convert" class="btn btn-primary">Convert</button>
                <a class="btn-reset" href="<%= ctx %>/home?reset=1">Reset</a>
            </div>
        </form>

        <% if (result != null) { %>
            <div class="result" role="status">

                <div>
                    <p class="result-label">You get</p>
                    <p class="result-figure"><%= Html.escape(MoneyFormatter.withSymbol(result.getToSymbol(), result.getConverted())) %></p>
                    <p class="result-line">
                        <%= MoneyFormatter.money(result.getAmount()) %> <%= result.getFromCode() %>
                        =
                        <%= MoneyFormatter.money(result.getConverted()) %> <%= result.getToCode() %>
                    </p>
                    <p class="result-line result-rate">
                        1 <%= result.getFromCode() %> = <%= MoneyFormatter.rate(result.getRateUsed()) %> <%= result.getToCode() %>
                        &middot; mid-market rate
                    </p>
                </div>

                <%-- Smart route: the cheapest way to make this exchange in real life --%>
                <% if (bestRoute != null && directRoute != null) {
                       boolean better = !bestRoute.isDirect(); %>
                    <div class="route<%= better ? " route-better" : "" %>">
                        <p class="result-label"><%= better ? "Smarter route found" : "Best route" %></p>
                        <div class="route-path">
                            <% List<String> path = bestRoute.getPath();
                               for (int i = 0; i < path.size(); i++) { %>
                                <% if (i > 0) { %><span class="arrow" aria-hidden="true">&rarr;</span><% } %>
                                <span class="chip"><%= path.get(i) %></span>
                            <% } %>
                        </div>
                        <% if (better) { %>
                            <p class="route-text">
                                Going through <strong><%= bestRoute.getVia() %></strong> you would receive about
                                <strong><%= Html.escape(MoneyFormatter.withSymbol(result.getToSymbol(), bestRoute.getReceived())) %></strong>
                                after fees &mdash;
                                <span class="route-save"><%= Html.escape(MoneyFormatter.withSymbol(result.getToSymbol(), saved)) %> more</span>
                                than exchanging directly.
                            </p>
                        <% } else { %>
                            <p class="route-text">
                                Exchanging directly is already the cheapest way. After typical fees
                                (about <%= bestRoute.getFeePercent().stripTrailingZeros().toPlainString() %>%) you would receive about
                                <strong><%= Html.escape(MoneyFormatter.withSymbol(result.getToSymbol(), bestRoute.getReceived())) %></strong>.
                            </p>
                        <% } %>
                        <p class="route-note">
                            Estimated fees per exchange: 0.5% between major currencies,
                            1.5% between a major and a minor one, 4% between two minor ones.
                        </p>
                    </div>
                <% } %>

            </div>
        <% } %>
    </section>

    <div class="tiles">

        <%-- 7-day trend of the selected pair --%>
        <div class="tile">
            <div class="tile-head">
                <p class="tile-title"><%= Html.escape(formFrom) %> &rarr; <%= Html.escape(formTo) %> &middot; last 7 days</p>
                <% if (miniTrend != null) {
                       int dir = miniTrend.getDirection(); %>
                    <span class="change <%= dir > 0 ? "change-up" : dir < 0 ? "change-down" : "change-flat" %>">
                        <%= dir > 0 ? "&#9650;" : dir < 0 ? "&#9660;" : "" %> <%= MoneyFormatter.percent(miniTrend.getChangePercent()) %>
                    </span>
                <% } %>
            </div>
            <% if (miniTrend != null) { %>
                <p class="tile-figure"><%= MoneyFormatter.rate(miniTrend.getLast()) %> <span class="unit"><%= Html.escape(formTo) %></span></p>
                <svg class="spark" viewBox="0 0 300 56" preserveAspectRatio="none" role="img"
                     aria-label="<%= Html.escape(formFrom) %> to <%= Html.escape(formTo) %> over the last 7 days">
                    <polygon class="spark-area" points="<%= miniTrend.getArea(300, 56) %>"/>
                    <polyline class="spark-line" points="<%= miniTrend.getPolyline(300, 56) %>"/>
                </svg>
            <% } else { %>
                <p class="tile-text">The 7-day chart appears here once past rates have been downloaded.</p>
            <% } %>
            <a class="tile-link" href="<%= ctx %>/trends?from=<%= Html.escape(formFrom) %>&amp;to=<%= Html.escape(formTo) %>">Open full chart &rarr;</a>
        </div>

        <%-- Rate alerts --%>
        <div class="tile">
            <div class="tile-head">
                <p class="tile-title">Rate alerts</p>
                <% if (triggeredAlerts != null && !triggeredAlerts.isEmpty()) { %>
                    <span class="badge badge-hit"><%= triggeredAlerts.size() %> reached</span>
                <% } %>
            </div>
            <% if (alertCount == null || alertCount == 0) { %>
                <p class="tile-figure">No alerts yet</p>
                <p class="tile-text">Get told when 1 USD drops below your target, or any other pair crosses a rate you choose.</p>
                <a class="tile-link" href="<%= ctx %>/alerts">Create an alert &rarr;</a>
            <% } else { %>
                <p class="tile-figure"><%= alertCount %> <span class="unit"><%= alertCount == 1 ? "alert" : "alerts" %> watching</span></p>
                <p class="tile-text">
                    <%= (triggeredAlerts == null || triggeredAlerts.isEmpty())
                        ? "None has reached its target yet. They are checked every time you visit."
                        : "Some of your targets have been reached. See the details above." %>
                </p>
                <a class="tile-link" href="<%= ctx %>/alerts">Manage alerts &rarr;</a>
            <% } %>
        </div>

    </div>

    <section class="section" aria-labelledby="recent-heading">
        <div class="section-head">
            <h2 class="section-title" id="recent-heading">Recent conversions</h2>
            <% if (recent != null && !recent.isEmpty()) { %>
                <a class="link-more" href="<%= ctx %>/history">View all</a>
            <% } %>
        </div>

        <% if (recent == null || recent.isEmpty()) { %>
            <div class="empty">
                <p class="empty-title">No conversions yet</p>
                <p class="empty-text">Your five most recent conversions will appear here.</p>
            </div>
        <% } else { %>
            <div class="table-wrap">
                <table class="table">
                    <thead>
                        <tr>
                            <th scope="col">When</th>
                            <th scope="col">Conversion</th>
                            <th scope="col" class="num">Result</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Conversion c : recent) { %>
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
    </section>

    <% if (currencies != null && !currencies.isEmpty()) { %>
    <section class="section" aria-labelledby="rates-heading">
        <div class="section-head">
            <h2 class="section-title" id="rates-heading">Today's rates</h2>
            <span class="section-note">
                Per 1 US Dollar<% if (ratesUpdated != null) { %>
                &middot; updated <%= MoneyFormatter.day(ratesUpdated) %>, <%= MoneyFormatter.time(ratesUpdated) %><% } %>
            </span>
        </div>
        <div class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th scope="col">Currency</th>
                        <th scope="col" class="num">1 USD equals</th>
                        <% if (weekChange != null) { %>
                            <th scope="col" class="num">7 days</th>
                        <% } %>
                        <th scope="col" class="num hide-sm"><span class="visually-hidden">Chart</span></th>
                    </tr>
                </thead>
                <tbody>
                <% for (Currency c : currencies) {
                       BigDecimal change = (weekChange == null) ? null : weekChange.get(c.getCode()); %>
                    <tr>
                        <td>
                            <div class="currency-cell">
                                <span class="currency-symbol" aria-hidden="true"><%= Html.escape(c.getSymbol()) %></span>
                                <span>
                                    <span class="currency-name"><%= Html.escape(c.getName()) %></span>
                                    <span class="currency-code"><%= c.getCode() %></span>
                                </span>
                            </div>
                        </td>
                        <td class="num strong">
                            <%= MoneyFormatter.rate(c.getRatePerUsd()) %>
                            <span class="unit"><%= c.getCode() %></span>
                        </td>
                        <% if (weekChange != null) { %>
                            <td class="num">
                                <% if (change != null && !"USD".equals(c.getCode())) {
                                       int dir = change.signum(); %>
                                    <span class="change <%= dir > 0 ? "change-up" : dir < 0 ? "change-down" : "change-flat" %>">
                                        <%= dir > 0 ? "&#9650;" : dir < 0 ? "&#9660;" : "" %> <%= MoneyFormatter.percent(change) %>
                                    </span>
                                <% } else { %>
                                    <span class="subtle">&mdash;</span>
                                <% } %>
                            </td>
                        <% } %>
                        <td class="num hide-sm">
                            <% if (!"USD".equals(c.getCode())) { %>
                                <a class="link-more" href="<%= ctx %>/trends?from=USD&amp;to=<%= c.getCode() %>">Chart</a>
                            <% } %>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
    <% } %>

</div>

<%@ include file="footer.jsp" %>
