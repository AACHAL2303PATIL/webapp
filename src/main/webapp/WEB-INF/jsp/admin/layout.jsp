<!DOCTYPE html>
<html lang="en" data-content-language="${fn:toLowerCase(applicationScope.configProperties['content.language'])}">
    <head>
        <title><content:gettitle /> | ${fn:toLowerCase(applicationScope.configProperties['content.language'])}.elimu.ai</title>

        <meta charset="UTF-8" />

        <meta name="viewport" content="width=device-width, initial-scale=1.0" />

        <link rel="shortcut icon" href="<spring:url value='/static/img/favicon.ico' />" />
        
        <%-- CSS --%>
        <link href="http://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.100.2/css/materialize.min.css" />
        <link rel="stylesheet" href="http://fonts.googleapis.com/css?family=Poppins" />
        <link rel="stylesheet" href="<spring:url value='/static/css/styles.css' />" />
        <link rel="stylesheet" href="<spring:url value='/static/css/admin/styles.css' />" />
        
        <%-- JavaScripts --%>
        <script src="<spring:url value='/static/js/jquery-2.1.4.min.js' />"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.100.2/js/materialize.min.js"></script>
        <script src="<spring:url value='/static/js/init.js' />"></script>
        <script src="https://cdn.jsdelivr.net/npm/web3@1.3.6/dist/web3.min.js"></script>
        <%@ include file="/WEB-INF/jsp/error/javascript-error.jsp" %>
    </head>

    <body>
        <div id="formLoadingOverlay">
            <div class="preloader-wrapper big active">
                <div class="spinner-layer spinner-blue-only">
                    <div class="circle-clipper left">
                        <div class="circle"></div>
                    </div><div class="gap-patch">
                        <div class="circle"></div>
                    </div><div class="circle-clipper right">
                        <div class="circle"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <nav class="deep-purple lighten-1">
            <div class="row nav-wrapper">
                <div class="col s1">
                    <ul id="nav-mobile" class="side-nav">
                        <li>
                            <a href="<spring:url value='/admin' />">
                                <img style="max-width: 100%; vertical-align: middle; max-height: 60%;" src="<spring:url value='/static/img/logo-text-256x78.png' />" alt="elimu.ai" />
                            </a>
                        </li>
                        
                        <li class="divider"></li>
                        <li class="grey-text"><b><fmt:message key="applications" /></b></li>
                        <li><a href="<spring:url value='/admin/application/list' />"><i class="material-icons left">android</i><fmt:message key="applications" /></a></li>
                    </ul>
                    <a id="navButton" href="<spring:url value='/admin' />" data-activates="nav-mobile" class="waves-effect waves-light"><i class="material-icons">dehaze</i></a>
                </div>
                <div class="col s5">
                    <a href="<spring:url value='/admin' />" class="breadcrumb"><fmt:message key="administration" /></a>
                    <c:if test="${!fn:contains(pageContext.request.requestURI, '/jsp/admin/main.jsp')}">
                        <c:choose>
                            <c:when test="${fn:contains(pageContext.request.requestURI, '/admin/application/')
                                            || fn:contains(pageContext.request.requestURI, '/admin/application-version/')
                                            && !fn:endsWith(pageContext.request.requestURI, '/list.jsp')}">
                                <a class="breadcrumb" href="<spring:url value='/admin/application/list' />"><fmt:message key="applications" /></a>
                            </c:when>
                        </c:choose>
                        <a class="breadcrumb"><content:gettitle /></a>
                    </c:if>
                </div>
                <div class="col s6">
                    <ul class="right">
                        <a class="dropdown-button" data-activates="contributorDropdown" data-beloworigin="true" >
                            <div class="chip">
                                <c:choose>
                                    <c:when test="${empty contributor.providerIdWeb3}">
                                        <img src="<spring:url value='${contributor.imageUrl}' />" alt="${contributor.firstName}" /> 
                                        <c:out value="${contributor.firstName}" />&nbsp;<c:out value="${contributor.lastName}" /> &lt;${contributor.email}&gt;
                                    </c:when>
                                    <c:otherwise>
                                        <img src="http://62.75.236.14:3000/identicon/<c:out value="${contributor.providerIdWeb3}" />" />
                                        <c:out value="${fn:substring(contributor.providerIdWeb3, 0, 6)}" />&#8230;<c:out value="${fn:substring(contributor.providerIdWeb3, 38, 42)}" />
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </a>
                        <ul id='contributorDropdown' class='dropdown-content'>
                            <li><a href="<spring:url value='/content/contributor/edit-name' />"><i class="material-icons left">mode_edit</i><fmt:message key="edit.name" /></a></li>
                            <%--<li class="divider"></li>
                            <li><a href="<spring:url value='/content/contributor/edit-email' />"><i class="material-icons left">mail</i><fmt:message key="edit.email" /></a></li>--%>
                            <sec:authorize access="hasRole('ROLE_ADMIN')">
                                <li class="divider"></li>
                                <li><a href="<spring:url value='/admin' />"><i class="material-icons left">build</i><fmt:message key="administration" /></a></li>
                            </sec:authorize>
                            <sec:authorize access="hasRole('ROLE_ANALYST')">
                                <li class="divider"></li>
                                <li><a href="<spring:url value='/analytics' />"><i class="material-icons left">timeline</i><fmt:message key="analytics" /></a></li>
                            </sec:authorize>
                            <li class="divider"></li>
                            <li><a href="<spring:url value='/logout' />"><i class="material-icons left">power_settings_new</i><fmt:message key="sign.out" /></a></li>
                        </ul>
                    </ul>
                </div>
            </div>
        </nav>
                    
        <c:if test="${hasBanner}">
            <div class="section no-pad-bot" id="index-banner">
                <div class="container">
                    <content:getbanner />
                </div>
            </div>
        </c:if>

        <div id="${cssId}" class="container <c:if test="${cssClass != null}">${cssClass}</c:if>">
            <div class="section row">
                <c:choose>
                    <c:when test="${!hasAside}">
                        <div class="col s12">
                            <content:getsection />
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="col s12 m8">
                            <content:getsection />
                        </div>
                        <div class="col s12 m4">
                            <content:getaside />
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </body>
</html>
