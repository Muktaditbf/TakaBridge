<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("title", "Clear History");
%>
<%@ include file="header.jsp" %>

<div class="container page page-narrow">

    <div class="card">
        <h1 class="card-title">Clear your history?</h1>
        <p class="card-text">
            Every conversion saved on this browser will be deleted from the database.
            This cannot be undone.
        </p>

        <form method="post" action="<%= ctx %>/clear-history" class="confirm-actions">
            <button type="submit" class="btn btn-danger">Yes, clear it</button>
            <a class="btn-reset" href="<%= ctx %>/history">Cancel</a>
        </form>
    </div>

</div>

<%@ include file="footer.jsp" %>
