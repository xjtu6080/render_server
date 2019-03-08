<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ request.getContextPath();
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>集群渲染管理平台</title>
<link rel="stylesheet" type="text/css" href="css/style.css" />
<link rel="stylesheet" type="text/css" href="css/myform.css" />
<script type="text/javascript" src="js/clockp.js"></script>
<script type="text/javascript" src="js/clockh.js"></script>
<script type="text/javascript" src="js/jquery-1.8.2.min.js"></script>
<script type="text/javascript" src="js/editUser.js"></script>
<script type="text/javascript" src="js/Validform_v5.3_min.js"></script>

</head>
<body>
	<div id="main_container">

		<%@ include file="inc/header.jsp"%>

		<div class="main_content">

			<%@ include file="inc/menu.jsp"%>

			<div class="center_content">
				<div class="right_content">
					<h2>编辑用户</h2>

					<div class="ui-form-sitefont">
						<form id="editUser-form" action="userUpdate.action" method="post">

                           <input type="hidden" value="<s:property value="userId"/>" name="userId"/>
                           
							<fieldset>
								<p class="form-field">
									<label class="input-label" for="userName">用户名:</label>
									<input type="text" value=<s:property value="user.name"/> name="resgisterUserInfo.name" id="userName"/>
								</p>
								
								<p class="form-field">
									<label class="input-label" for="userEmail">Email:</label>
									<input type="text" value=<s:property value="user.email"/> name="resgisterUserInfo.email" id="userEmail"/>
								</p>
								
								<p class="form-field">
									<label class="input-label" for="userMobile">手机号码:</label>
									<input type="text" value=<s:property value="user.mobile"/> name="resgisterUserInfo.mobile" id="userMobile" datatype="m" nullmsg="请输入手机号码" errormsg="请输入正确的手机号码" sucmsg="&nbsp;"/>
								</p>
								<p class="form-field">
			                        <label class="input-label" for="isChgPasswd">是否修改密码</label>
			                       	<input type="checkbox" name="isChgPasswd" id="isChgPasswd" value="1" /><span>是</span>
			                    </p>
			                    <div id="passwdInfo" style="display:none">
									<p class="form-field">
										<label class="input-label" for="userPassword">密码:</label>
										<input type="password" name="resgisterUserInfo.password" id="userPassword" datatype="*5-20" sucmsg="&nbsp;" nullmsg="请输入密码" />
									</p>
									
									<p class="form-field">
										<label class="input-label" for="userRepassword">重新输入密码:</label>
										<input type="password" name="resgisterUserInfo.repassword" id="userRepassword" datatype="*" recheck="resgisterUserInfo.password" nullmsg="请再次输入密码" sucmsg="&nbsp;"/>
									</p>
								</div>												

							</fieldset>
							<p class="form-field"><input type="button" value="提交" id="editUserBtn"/></p>

						</form>
					</div>

				</div>
				<!-- end of right content-->

			</div>
			<!--end of center content -->

			<div class="clear"></div>
		</div>
		<!--end of main content-->

		<%@ include file="inc/footer.jsp"%>

	</div>
</body>
</html>