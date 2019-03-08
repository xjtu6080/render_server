package org.xjtu.framework.modules.manager.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.xjtu.framework.core.base.model.Job;
import org.xjtu.framework.core.base.model.Project;
import org.xjtu.framework.core.base.model.User;
import org.xjtu.framework.core.util.LinuxInvoker;
import org.xjtu.framework.core.util.PbsExecute;
import org.xjtu.framework.core.util.StringUtil;
import org.xjtu.framework.modules.user.service.JobService;
import org.xjtu.framework.modules.user.service.ProjectService;
import org.xjtu.framework.modules.user.service.UserService;
import org.xjtu.framework.modules.user.service.impl.ClusterManageServiceImpl;
import org.xjtu.framework.modules.user.vo.CameraProgressInfo;
import org.xjtu.framework.modules.user.vo.ProjectInfo;

import com.xj.framework.ssh.ShellLocal;

@ParentPackage("json-default")
@Namespace("/web/manager")
public class JobAjaxAction extends ManagerBaseAction{
	private @Resource JobService jobService;
	private @Resource ProjectService projectService;
	private @Resource UserService userService;
	private List<CameraProgressInfo> cameraProgressInfos;
    private int pageSize = 10;
	private int pageNum = 1;
    private String searchText = "";
    private String searchType = "name";
    private int jobStatus=-1;
    private String param;
    private Project currentProject;
	private String status;
	private String info;
	private List<String> jobIds;
	
	//doGetProjectsAndScenePath参数
	private String project_client_id;
	private List<ProjectInfo> projects;
	private Map<String,String> mapScenePath;
	
	//validateFrameRange
	private String filePath;

	private static final Log log = LogFactory.getLog(JobAjaxAction.class);
	
	@Action(value = "jobProgress", results = {@Result(name = SUCCESS, type = "json")})
	public String jobProgress(){
		List<Job> jobs = jobService.listJobsByQuery(searchText, searchType, pageNum, pageSize, jobStatus);
		
		if(jobs!=null){
		
			cameraProgressInfos=new ArrayList<CameraProgressInfo>();
			for(int i=0;i<jobs.size();i++){
				CameraProgressInfo cpi=new CameraProgressInfo();
				cpi.setCameraId(jobs.get(i).getId());
				cpi.setCameraProgress(jobs.get(i).getCameraProgress());
				cpi.setCameraStatus(jobs.get(i).getJobStatus());
				cpi.setPaymentStatus(jobs.get(i).getPaymentStatus());
				cpi.setRenderCost(jobs.get(i).getRenderCost());
				if(jobs.get(i).getEndTime()!=null&&jobs.get(i).getStartTime()!=null){
					cpi.setTimeInfo("startTime:"+jobs.get(i).getStartTime().toString().replaceAll(" ", "_")+"\n"+"endTime:"+jobs.get(i).getEndTime().toString().replaceAll(" ","_"));
				}
				else if(jobs.get(i).getStartTime()!=null){
					cpi.setTimeInfo("startTime:"+jobs.get(i).getStartTime().toString().replaceAll(" ", "_")+"\n"+"endTime:");
				}else{
					cpi.setTimeInfo("startTime:"+"\n"+"endTime");
				}
				
				
				cameraProgressInfos.add(cpi);
			}
			
		}
		return SUCCESS;
	}
	@Action(value = "validateCameraName", results = {@Result(name = SUCCESS, type = "json")})
	public String validateCameraName(){
		
		Job j=jobService.findJobByJobNameAndProjectId(param,currentProject.getId());
		
		if(j != null){
			this.status = "n";
			this.info = "工程中已有此镜头，请更换镜头名";
		}else{
			this.status = "y";
		}
		return SUCCESS;
	}
	
	@Action(value = "validateFrameRange", results = {@Result(name = SUCCESS, type = "json")})
	public String validateFrameRange(){
		
		if(filePath!=null&&!filePath.equals("")){
		
			/*修改成命令方式*/
			//filePath=filePath.substring(0,filePath.lastIndexOf("/"));
			//String cmd="cd "+filePath+" && ls *.rib";
			String cmd="cd "+filePath+" && ls *.xml";
			ShellLocal shell=new ShellLocal(cmd);
			String result=shell.executeCmd();
			result=result.trim();
			String[] filenames=result.split("\\n");
			Map<Integer,String> mapScenePath=new HashMap<Integer,String>();
			
			for(int i=0;i<filenames.length;i++){
				String[] frameNo=filenames[i].split("\\.");
				Integer key;
				try{
					String frameID=StringUtil.getNumb(frameNo[frameNo.length-2]);
					key=Integer.parseInt(frameID);
				}catch(NumberFormatException e){
					continue;
				}
				mapScenePath.put(key, filenames[i]);
			}
			/*检测要渲染的rib文件是否存在*/
			String[] ss=param.split(",");//eg:1-9
			if(ss!=null&&ss.length>0){
			
				for(int i=0;i<ss.length;i++){

					String[] temp=ss[i].split("-");
			
					if(temp!=null&&temp.length>0){
					
						if(temp.length==1){
						
							if(mapScenePath.get(Integer.parseInt(temp[0]))==null){
								this.status = "n";
								this.info="您要渲染的部分帧找不到对应的xml文件";
								return SUCCESS;
							}
												
						}else if(temp.length==2){
							try{
								Integer end=Integer.parseInt(temp[1]);
								Integer begin=Integer.parseInt(temp[0]);
							
								if(end<begin)
								{		
									this.status = "n";
						    		this.info="您填写的帧范围格式错误";
						    		return SUCCESS;
								}
								for(int z=0;z<end-begin+1;z++){
									if(mapScenePath.get(begin+z)==null){
										this.status = "n";
							    		this.info="您要渲染的部分帧找不到对应的xml文件";
							    		return SUCCESS;
									}
								}
							}catch(Exception e){
								this.status = "n";
					    		this.info="您填写的帧范围格式错误";
					    		return SUCCESS;
							}

						}else{
							this.status = "n";
				    		this.info="您填写的帧范围格式错误";
				    		return SUCCESS;
						}
					}else{
						this.status = "n";
			    		this.info="您填写的帧范围格式错误";
			    		return SUCCESS;
					}
				
				}
			}else{
				this.status = "n";
	    		this.info="您填写的帧范围格式错误";
	    		return SUCCESS;
			}
		}		
		this.status = "y";
		return SUCCESS;
	}
	@Action(value = "doGetProjectsByUser", results = {@Result(name = SUCCESS, type = "json")})
	public String doGetProjectsByUser(){
		if(project_client_id!=null){
			List<Project> projs=projectService.findProjectsByUserId(project_client_id);
			if(projs!=null&&projs.size()>0){
				projects=new ArrayList<ProjectInfo>();
				for(int i=0;i<projs.size();i++){
					ProjectInfo pi=new ProjectInfo();
					pi.setId(projs.get(i).getId());
					pi.setName(projs.get(i).getName());
					projects.add(pi);
				}
			}
		}
		return SUCCESS;
	}
	@Action(value = "doGetScenePathByUser", results = {@Result(name = SUCCESS, type = "json")})
	public String doGetScenePathByUser(){
		if(project_client_id!=null){
			User existedUser=userService.findUserById(project_client_id);
			if(existedUser!=null){
				try {
					String homeDir=existedUser.getHomeDir();
					ShellLocal shell=new ShellLocal("ls "+homeDir);
					String result=shell.executeCmd();
					result=result.trim();
					
					
					mapScenePath=new HashMap<String, String>();
					String[]filesName=result.split("\n");
					for(int i=0;i<filesName.length;i++){
				    	 String s=filesName[i];
						 mapScenePath.put(s, homeDir+s);//文件名，文件的全路径
					}
					System.out.println("mapScenePath is"+mapScenePath);
					/*File file=new File(existedUser.getHomeDir());
					if(file.isDirectory()){
						File[] files=file.listFiles();
						if (files != null && files.length > 0) {
							mapScenePath=new HashMap<String, String>();
							for(int i=0;i<files.length;i++){
						    	 String s=files[i].getAbsolutePath();
								 String[] ss=s.split("/");
								 mapScenePath.put(ss[ss.length-1], s);
							}
						}						
					}*/
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
				
		return SUCCESS;
	}
	@Action(value = "doJobBegin", results = {@Result(name = SUCCESS, type = "json")})
	public String doJobBegin(){

		if(jobIds!=null){
			jobService.doStartCameraRender(jobIds);
		}
		return SUCCESS;
	}
	@Action(value = "doJobStop", results = {@Result(name = SUCCESS, type = "json")})
	public String doJobStop(){
		if(jobIds!=null){
			for(int i=0;i<jobIds.size();i++){
				
				jobService.doStopCameraRender(jobIds.get(i));
			}
		}
		return SUCCESS;
	}
	
	@Action(value = "doJobSuspend", results = {@Result(name = SUCCESS, type = "json")})
	public String doJobSuspend(){

		if(jobIds!=null){
			for(int i=0;i<jobIds.size();i++){
				jobService.suspendJobByJobId(jobIds.get(i));
			}
			
		}
		
		return SUCCESS;
	}
	
	@Action(value = "doJobContinue", results = {@Result(name = SUCCESS, type = "json")})
	public String doJobContinue(){

		if(jobIds!=null){
			jobService.doStartCameraRender(jobIds);
			
		}
		
		return SUCCESS;
	}
	
	@Action(value = "doJobCopy", results = {@Result(name = SUCCESS, type = "json")})
	public String doJobCopy(){

		if(jobIds!=null){
			
			jobService.doCopyjobs(jobIds);
		}
		return SUCCESS;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public int getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(int jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public Project getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(Project currentProject) {
		this.currentProject = currentProject;
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

	public List<String> getJobIds() {
		return jobIds;
	}

	public void setJobIds(List<String> jobIds) {
		this.jobIds = jobIds;
	}

	public List<CameraProgressInfo> getCameraProgressInfos() {
		return cameraProgressInfos;
	}

	public void setCameraProgressInfos(List<CameraProgressInfo> cameraProgressInfos) {
		this.cameraProgressInfos = cameraProgressInfos;
	}

	public String getProject_client_id() {
		return project_client_id;
	}

	public void setProject_client_id(String project_client_id) {
		this.project_client_id = project_client_id;
	}

	public Map<String, String> getMapScenePath() {
		return mapScenePath;
	}

	public void setMapScenePath(Map<String, String> mapScenePath) {
		this.mapScenePath = mapScenePath;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<ProjectInfo> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjectInfo> projects) {
		this.projects = projects;
	}
	
}

