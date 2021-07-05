<content:title>
    <fmt:message key="edit.number" />
</content:title>

<content:section cssId="numberEditPage">
    <h4><content:gettitle /></h4>
    <div class="card-panel">
        <form:form modelAttribute="number">
            <tag:formErrors modelAttribute="number" />
            
            <form:hidden path="revisionNumber" value="${number.revisionNumber}" />
            <input type="hidden" name="timeStart" value="${timeStart}" />

            <div class="row">
                <div class="input-field col s12">
                    <form:label path="value" cssErrorClass="error"><fmt:message key='value' /> (<fmt:message key='number' />)</form:label>
                    <form:input path="value" cssErrorClass="error" type="number" />
                </div>
            </div>
            
            <div class="row">
                <div class="input-field col s12">
                    <form:label path="symbol" cssErrorClass="error"><fmt:message key='symbol' /></form:label>
                    <form:input path="symbol" cssErrorClass="error" />
                </div>
            </div>
            
            <div class="row">
                <div class="col s12">
                    <label><fmt:message key="number.words" /></label>
                    <div id="numberWordsContainer">
                        <c:forEach var="word" items="${number.words}">
                            <input name="words" type="hidden" value="${word.id}" />
                            <div class="chip" data-wordid="${word.id}" data-wordvalue="${word.text}">
                                <a href="<spring:url value='/content/word/edit/${word.id}' />">
                                    ${word.text}<c:if test="${not empty word.wordType}"> (${word.wordType})</c:if><c:out value=" ${emojisByWordId[word.id]}" />
                                </a>
                                <a href="#" class="wordDeleteLink" data-wordid="${word.id}">
                                    <i class="close material-icons">clear</i>
                                </a>
                            </div>
                        </c:forEach>
                        <script>
                            $(function() {
                                $('.wordDeleteLink').on("click", function() {
                                    console.log('.wordDeleteLink on click');
                                    
                                    var wordId = $(this).attr("data-wordid");
                                    console.log('wordId: ' + wordId);
                                    
                                    $(this).parent().remove();
                                    
                                    var $hiddenInput = $('input[name="words"][value="' + wordId + '"]');
                                    $hiddenInput.remove();
                                });
                            });
                        </script>
                    </div>
                    
                    <select id="numberWords" class="browser-default" style="margin: 0.5em 0;">
                        <option value="">-- <fmt:message key='select' /> --</option>
                        <c:forEach var="word" items="${words}">
                            <option value="${word.id}"><c:out value="${word.text}" /><c:if test="${not empty word.wordType}"> (${word.wordType})</c:if><c:out value=" ${emojisByWordId[word.id]}" /></option>
                        </c:forEach>
                    </select>
                    <script>
                        $(function() {
                            $('#numberWords').on("change", function() {
                                console.log('#numberWords on change');
                                
                                var wordId = $(this).val();
                                console.log('wordId: ' + wordId);
                                var wordText = $(this).find('option[value="' + wordId + '"]').text();
                                console.log('wordText: ' + wordText);
                                if (wordId != "") {
                                    $('#numberWordsContainer').append('<input name="words" type="hidden" value="' + wordId + '" />');
                                    $('#numberWordsContainer').append('<div class="chip">' + wordText + '</div>');
                                    $(this).val("");
                                }
                            });
                        });
                    </script>
                    
                    <a href="<spring:url value='/content/word/create' />" target="_blank"><fmt:message key="add.word" /> <i class="material-icons">launch</i></a>
                </div>
            </div>
            
            <div class="row">
                <div class="input-field col s12">
                    <label for="contributionComment"><fmt:message key='comment' /></label>
                    <textarea id="contributionComment" name="contributionComment" class="materialize-textarea" placeholder="A comment describing your contribution."><c:if test="${not empty param.contributionComment}"><c:out value="${param.contributionComment}" /></c:if></textarea>
                </div>
            </div>

            <button id="submitButton" class="btn waves-effect waves-light" type="submit">
                <fmt:message key="edit" /> <i class="material-icons right">send</i>
            </button>
            <a href="<spring:url value='/content/number/delete/${number.id}' />" class="waves-effect waves-red red-text btn-flat right"><fmt:message key="delete" /></a>
        </form:form>
    </div>
    
    <div id="contributionEvents" class="collection">
        <c:forEach var="numberContributionEvent" items="${numberContributionEvents}">
            <div class="collection-item">
                <span class="badge">
                    <fmt:message key="revision" /> #${numberContributionEvent.revisionNumber} 
                    (<fmt:formatNumber maxFractionDigits="0" value="${numberContributionEvent.timeSpentMs / 1000 / 60}" /> min). 
                    <fmt:formatDate value="${numberContributionEvent.time.time}" pattern="yyyy-MM-dd HH:mm" />
                </span>
                <div class="chip">
                    <c:choose>
                        <c:when test="${not empty numberContributionEvent.contributor.imageUrl}">
                            <img src="${numberContributionEvent.contributor.imageUrl}" />
                        </c:when>
                        <c:when test="${not empty numberContributionEvent.contributor.providerIdWeb3}">
                            <img src="http://62.75.236.14:3000/identicon/<c:out value="${numberContributionEvent.contributor.providerIdWeb3}" />" />
                        </c:when>
                        <c:otherwise>
                            <img src="<spring:url value='/static/img/placeholder.png' />" />
                        </c:otherwise>
                    </c:choose>
                    <c:choose>
                        <c:when test="${not empty numberContributionEvent.contributor.firstName}">
                            <c:out value="${numberContributionEvent.contributor.firstName}" />&nbsp;<c:out value="${numberContributionEvent.contributor.lastName}" />
                        </c:when>
                        <c:when test="${not empty numberContributionEvent.contributor.providerIdWeb3}">
                            ${fn:substring(numberContributionEvent.contributor.providerIdWeb3, 0, 6)}...${fn:substring(numberContributionEvent.contributor.providerIdWeb3, 38, 42)}
                        </c:when>
                    </c:choose>
                </div>
                <blockquote><c:out value="${numberContributionEvent.comment}" /></blockquote>
            </div>
        </c:forEach>
    </div>
</content:section>

<content:aside>
    <h5 class="center"><fmt:message key="preview" /></h5>
    
    <div class="previewContainer valignwrapper">
        <img src="<spring:url value='/static/img/device-pixel-c.png' />" alt="<fmt:message key="preview" />" />
        <div id="previewContentContainer">
            <div id="previewContent" class="previewContentGrapheme">

            </div>
        </div>
    </div>
    <script>
        $(function() {
            initializePreview();
            
            $('#symbol, #value').on("change", function() {
                console.debug('#symbol/#value on change');
                initializePreview();
            });
            
            function initializePreview() {
                console.debug('initializePreview');
                var symbol = $('#symbol').val();
                var value = $('#value').val();
                if ((symbol != undefined) && (symbol != "")) {
                    $('#previewContent').html(symbol);
                } else {
                    $('#previewContent').html(value);
                }
            };
        });
    </script>
</content:aside>
