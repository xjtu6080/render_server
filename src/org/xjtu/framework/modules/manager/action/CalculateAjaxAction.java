package org.xjtu.framework.modules.manager.action;

import java.util.List;

import javax.annotation.Resource;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.xjtu.framework.core.base.model.Calculate;
import org.xjtu.framework.core.base.model.Project;
import org.xjtu.framework.modules.user.service.CalculateService;
import org.xjtu.framework.modules.user.service.ProjectService;

@ParentPackage("json-default")
@Namespace("/web/manager")
public class CalculateAjaxAction extends ManagerBaseAction{
	
	private @Resource CalculateService calculateService;
	
    private String param;
    
    private Project currentUser;
    
	private String status;
	
	private String info;
	
	private List<String>xmlIds;
    	

	@Action(value = "validateCalculateName", results = {@Result(name = SUCCESS, type = "json")})
	public String validateCalculateName(){
		
		Calculate c=calculateService.findCalculateByCalculateNameAndUserId(param,currentUser.getId());
		
		if(c != null){
			this.status = "n";
			this.info = "该用户已创建该解析任务，请更换解析任务名";
		}else{
			this.status = "y";
		}
		return SUCCESS;
	}
	
	
	//开始解算任务
	@Action(value="doCalculateBegin",results = {@Result(name = SUCCESS,type = "json")})
	public String doCalculateBegin(){
		return null;
	}
	
	//停止解算任务
	@Action(value = "doCalculateStop", results = {@Result(name = SUCCESS, type = "json")})
	public String doCalculateStop(){
		return null;
	}
	
	
	//暂停解算任务
	@Action(value = "doCalculateSuspend", results = {@Result(name = SUCCESS, type = "json")})
	public String doCalculateSuspend(){
		return null;
	}
	
	
	//继续运行解算任务
	@Action(value = "doCalculateContinue", results = {@Result(name = SUCCESS, type = "json")})
	public String doCalculateContinue(){
		return null;
	}
	
	
	//复制解算任务
	@Action(value = "doCalculateCopy", results = {@Result(name = SUCCESS, type = "json")})
	public String doCalculateCopy(){
		if(xmlIds!=null){
			calculateService.doCopyCalculates(xmlIds);
		}
		return SUCCESS;
	}
	
	
	
	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Project getCurrentUser() {
		return currentUser;
	}

	public void setCurrentUser(Project currentUser) {
		this.currentUser = currentUser;
	}

	public List<String> getXmlIds() {
		return xmlIds;
	}

	public void setXmlIds(List<String> xmlIds) {
		this.xmlIds = xmlIds;
	}
	

	
}

