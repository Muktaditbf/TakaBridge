<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%
    request.setAttribute("title", "Something went wrong");

    Object status = request.getAttribute("jakarta.servlet.error.status_code");
    boolean notFound = (status != null) && "404".equals(status.toString());
%>
<%@ include file="header.jsp" %>

<div class="container page page-narrow">
    <div class="card">
        <h1 class="card-title">
            <%= notFound ? "That page does not exist" : "Something went wrong" %>
        </h1>
        <p class="card-text">
            <%= notFound
                ? "The address you opened is not part of TakaBridge."
                : "TakaBridge ran into an unexpected problem. Nothing was changed." %>
        </p>
        <p class="form-actions">
            <a class="btn btn-primary" href="<%= request.getContextPath() %>/home">Back to the converter</a>
        </p>
    </div>
</div>

<%@ include file="footer.jsp" %>
