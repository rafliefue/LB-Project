<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="petclinic" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" %> <!-- Para  tildes, ñ y caracteres especiales como el € %-->

<petclinic:layout pageName="referees">

    <h2><fmt:message key="refereeDetails"/></h2>


    <table class="table table-striped">
        <tr>
            <th><fmt:message key="nameRefereeDetails"/></th>
            <td><b><c:out value="${referee.firstName} ${referee.lastName}"/></b></td>
        </tr>
        <tr>
            <th><fmt:message key="emailRefereeDetails"/></th>
            <td><c:out value="${referee.email}"/></td>
        </tr>
        <tr>
            <th><fmt:message key="dniRefereeDetails"/></th>
            <td><c:out value="${referee.dni}"/></td>
        </tr>
        <tr>
            <th><fmt:message key="tlphRefereeDetails"/></th>
            <td><c:out value="${referee.telephone}"/></td>
        </tr>
    </table>
    
    <!-- Tomo el valor del nombre de usuario actual %-->  
    
	<security:authorize access="isAuthenticated()">
   		<security:authentication var="principalUsername" property="principal.username" /> 
	</security:authorize>
	
	<!-- Muestro el botón de editar si el usuario coincide con el usuario actual %-->  

	<c:if test="${referee.user.username == principalUsername}">
    	<spring:url value="{refereeId}/edit" var="editUrl">
        	<spring:param name="refereeId" value="${referee.id}"/>
    	</spring:url>
    	<a href="${fn:escapeXml(editUrl)}" class="btn btn-default"><fmt:message key="editRefereeDetails"/></a>
    	
    	
    	<spring:url value="/deleteReferee/${principalUsername}" var="editUrl"></spring:url>
    	<a href="${fn:escapeXml(editUrl)}" class="btn btn-default2" style="color:white"><b><fmt:message key="deleteReferee"/></b></a>
    	
    </c:if>  
    
    <br/>
    <br/>
    <br/>
    
</petclinic:layout>
