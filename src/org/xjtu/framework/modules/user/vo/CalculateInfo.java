package org.xjtu.framework.modules.user.vo;

import java.util.Date;

import org.xjtu.framework.core.base.model.Job;
import org.xjtu.framework.core.base.model.User;

public class CalculateInfo {
	
	private String id;
	private String xmlName;
	private Date xmlCreateTime;
	private int xmlStatus;
	private int xmlProgress;
	private int xmlPriority;
	private String xmlFilePath;
	private User user;
	private Job job;
	
	
	

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public String getXmlFilePath() {
		return xmlFilePath;
	}

	public void setXmlFilePath(String xmlFilePath) {
		this.xmlFilePath = xmlFilePath;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getXmlName() {
		return xmlName;
	}

	public void setXmlName(String xmlName) {
		this.xmlName = xmlName;
	}

	public Date getXmlCreateTime() {
		return xmlCreateTime;
	}

	public void setXmlCreateTime(Date xmlCreateTime) {
		this.xmlCreateTime = xmlCreateTime;
	}

	public int getXmlStatus() {
		return xmlStatus;
	}

	public void setXmlStatus(int xmlStatus) {
		this.xmlStatus = xmlStatus;
	}

	public int getXmlProgress() {
		return xmlProgress;
	}

	public void setXmlProgress(int xmlProgress) {
		this.xmlProgress = xmlProgress;
	}

	public int getXmlPriority() {
		return xmlPriority;
	}

	public void setXmlPriority(int xmlPriority) {
		this.xmlPriority = xmlPriority;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	
}
