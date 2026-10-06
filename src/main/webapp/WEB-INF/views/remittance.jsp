<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%@ page import="com.takabridge.model.Currency" %>
<%@ page import="com.takabridge.model.Remittance" %>
<%@ page import="com.takabridge.service.RemittanceCalculator" %>
<%@ page import="com.takabridge.util.Html" %>
<%@ page import="com.takabridge.util.MoneyFormatter" %>
<%
    request.setAttribute("title", "Remittance Calculator");

    List<Currency> currencies = (List<Currency>) request.getAttribute("currencies");
    List<Remittance> channels = (List<Remittance>) request.getAttribute("channels");
    Boolean needsDocuments = (Boolean) request.getAttribute("needsDocuments");

    String error = (String) request.getAttribute("error");
    String dbError = (String) request.getAttribute("dbError");
    String formFrom = (String) request.getAttribute("formFrom");
    String formAmount = (String) request.getAttribute("formAmount");
    String bonusPercent = (String) request.getAttribute("bonusPercent");
    BigDecimal documentsLimit = (BigDecimal) request.getAttribute("documentsLimit");

    Remittance best = (channels == null || channels.isEmpty()) ? null : channels.get(0);
    final String TAKA = "৳";
%>
<%@ include file="header.jsp" %>

<div class="container page">

    <div class="page-head">
        <div>
            <h1 class="page-title">Remittance calculator</h1>
            <p class="page-subtitle">
                Sending money home? See how many taka your family really receives &mdash;
                after transfer fees, plus the government's <%= bonusPercent %>% bonus.
            </p>
        </div>
    </div>

    <% if (dbError != null) { %>
        <p class="alert alert-danger" role="alert"><%= Html.escape(dbError) %></p>
    <% } %>

    <section class="card" aria-label="Amount to send">
        <form class="remit-form" method="get" action="<%= ctx %>/remittance">
            <div class="field">
                <label for="amount">I am sending</label>
                <input type="text" id="amount" name="amount" inputmode="decimal"
                       autocomplete="off" placeholder="500" value="<%= Html.escape(formAmount) %>">
            </div>
            <div class="field">
                <label for="from">From abroad in</label>
                <select id="from" name="from">
                    <% if (currencies != null) {
                           for (Currency c : currencies) {
                               if ("BDT".equals(c.getCode())) { continue; } %>
                        <option value="<%= c.getCode() %>"<%= c.getCode().equals(formFrom) ? " selected" : "" %>><%= Html.escape(c.getLabel()) %></option>
                    <%     }
                       } %>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Calculate</button>
        </form>

        <% if (error != null) { %>
            <p class="alert alert-danger space-top" role="alert"><%= Html.escape(error) %></p>
        <% } %>
    </section>

    <% if (best != null) { %>

        <section class="card remit-result space-top" aria-labelledby="family-heading">

            <div>
                <p class="result-label" id="family-heading">Your family receives</p>
                <% if (best.isTooSmall()) { %>
                    <p class="result-figure"><%= TAKA %>0.00</p>
                    <p class="result-line">This amount is smaller than the transfer fee. Try sending more.</p>
                <% } else { %>
                    <p class="result-figure"><%= TAKA %><%= MoneyFormatter.money(best.getFamilyGets()) %></p>
                    <p class="result-line">
                        Best way: <strong><%= Html.escape(best.getChannel()) %></strong>
                    </p>
                    <p class="bonus-pill">
                        Includes <%= TAKA %><%= MoneyFormatter.money(best.getBonus()) %> government bonus
                    </p>
                <% } %>
            </div>

            <%-- The receipt: every step from what is sent to what arrives --%>
            <dl class="receipt">
                <dt>You send</dt>
                <dd><%= MoneyFormatter.money(best.getSent()) %> <%= best.getFromCode() %></dd>

                <dt>Transfer fee</dt>
                <dd class="minus">&minus; <%= MoneyFormatter.money(best.getFee()) %> <%= best.getFromCode() %></dd>

                <dt>Amount exchanged</dt>
                <dd><%= MoneyFormatter.money(best.getExchanged()) %> <%= best.getFromCode() %></dd>

                <dt>
                    Rate given
                    <span class="receipt-hint">market rate <%= MoneyFormatter.rate(best.getMarketRate()) %></span>
                </dt>
                <dd>&times; <%= MoneyFormatter.rate(best.getChannelRate()) %></dd>

                <dt>Taka before bonus</dt>
                <dd><%= TAKA %><%= MoneyFormatter.money(best.getTakaBeforeBonus()) %></dd>

                <dt>Government bonus (<%= bonusPercent %>%)</dt>
                <dd class="plus">+ <%= TAKA %><%= MoneyFormatter.money(best.getBonus()) %></dd>

                <dt class="receipt-total">Family receives</dt>
                <dd class="receipt-total"><%= TAKA %><%= MoneyFormatter.money(best.getFamilyGets()) %></dd>
            </dl>
        </section>

        <% if (Boolean.TRUE.equals(needsDocuments)) { %>
            <p class="alert alert-info space-top" role="status">
                This is more than <%= MoneyFormatter.money(documentsLimit) %> US Dollars. For the bonus on
                a transfer this large, the bank will usually ask for documents such as proof of income.
            </p>
        <% } %>

        <section class="section" aria-labelledby="compare-heading">
            <div class="section-head">
                <h2 class="section-title" id="compare-heading">Compare ways to send</h2>
                <span class="section-note">Same <%= MoneyFormatter.money(best.getSent()) %> <%= best.getFromCode() %>, three channels</span>
            </div>
            <div class="table-wrap">
                <table class="table">
                    <thead>
                        <tr>
                            <th scope="col">Channel</th>
                            <th scope="col" class="num">Fee</th>
                            <th scope="col" class="num hide-sm">Rate given</th>
                            <th scope="col" class="num hide-sm">Bonus</th>
                            <th scope="col" class="num">Family receives</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Remittance r : channels) {
                           boolean isBest = (r == best); %>
                        <tr>
                            <td>
                                <span class="strong"><%= Html.escape(r.getChannel()) %></span>
                                <% if (isBest && !r.isTooSmall()) { %><span class="badge badge-best">Best</span><% } %>
                            </td>
                            <td class="num subtle"><%= MoneyFormatter.money(r.getFee()) %> <span class="unit"><%= r.getFromCode() %></span></td>
                            <td class="num subtle hide-sm"><%= MoneyFormatter.rate(r.getChannelRate()) %></td>
                            <td class="num hide-sm"><span class="change change-up">+<%= MoneyFormatter.money(r.getBonus()) %></span></td>
                            <td class="num strong"><%= TAKA %><%= MoneyFormatter.money(r.getFamilyGets()) %></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>

        <div class="card space-top">
            <h2 class="card-title">Good to know</h2>
            <ul class="feature-list">
                <li>
                    <strong>The <%= bonusPercent %>% bonus</strong> is paid by the Government of Bangladesh on
                    money sent through banks and other legal channels. It is added in taka when the money arrives.
                </li>
                <li>
                    <strong>Hundi gets no bonus.</strong> Informal transfers are illegal, carry no protection,
                    and the family loses the incentive.
                </li>
                <li>
                    <strong>These are estimates.</strong> Fees and rates are typical figures for study:
                    <% for (RemittanceCalculator.Channel ch : RemittanceCalculator.Channel.values()) { %>
                        <%= ch.getLabel() %> &ndash; $<%= ch.getFeeUsd() %> fee and
                        <%= ch.getRateMargin().movePointRight(2).stripTrailingZeros().toPlainString() %>% below market<%= ch.ordinal() < RemittanceCalculator.Channel.values().length - 1 ? ";" : "." %>
                    <% } %>
                    Always check the real quote before sending.
                </li>
            </ul>
        </div>

    <% } %>

</div>

<%@ include file="footer.jsp" %>
