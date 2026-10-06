<%-- Shared bottom of every page. --%>
</main>

<footer class="site-footer">
    <div class="container footer-inner">

        <div class="footer-brand">
            <span class="wordmark">
                <span class="logo-mark" aria-hidden="true"></span>
                <span><span class="wordmark-taka">Taka</span><span class="wordmark-bridge">Bridge</span></span>
            </span>
            <p class="footer-note">
                Simple currency conversion, wherever you go. Rates update once a day and
                are reference rates for study, not quotes for trading.
            </p>
        </div>

        <nav class="footer-nav" aria-label="Footer">
            <a href="<%= request.getContextPath() %>/home">Converter</a>
            <a href="<%= request.getContextPath() %>/remittance">Remittance</a>
            <a href="<%= request.getContextPath() %>/trends">Rate trends</a>
            <a href="<%= request.getContextPath() %>/alerts">Rate alerts</a>
            <a href="<%= request.getContextPath() %>/history">History</a>
            <a href="<%= request.getContextPath() %>/about">About</a>
        </nav>

    </div>
    <div class="container footer-legal">
        &copy; 2026 TakaBridge &middot; University project built with Java, JSP and Oracle
    </div>
</footer>

</body>
</html>
