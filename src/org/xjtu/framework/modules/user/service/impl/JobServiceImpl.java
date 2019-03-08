package org.xjtu.framework.modules.user.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xjtu.framework.core.base.constant.FrameStatus;
import org.xjtu.framework.core.base.constant.JobPriority;
import org.xjtu.framework.core.base.constant.JobStatus;
import org.xjtu.framework.core.base.constant.SystemConfig;
import org.xjtu.framework.core.base.constant.TaskStatus;
import org.xjtu.framework.core.base.constant.UnitStatus;
import org.xjtu.framework.core.base.constant.PaymentStatus;

import org.xjtu.framework.core.base.model.Frame;
import org.xjtu.framework.core.base.model.Job;
import org.xjtu.framework.core.base.model.Project;
import org.xjtu.framework.core.base.model.Task;
import org.xjtu.framework.core.base.model.Unit;
import org.xjtu.framework.core.base.model.User;
import org.xjtu.framework.core.util.LinuxInvoker;
import org.xjtu.framework.core.util.PbsExecute;
import org.xjtu.framework.core.util.StringUtil;
import org.xjtu.framework.core.util.UUIDGenerator;
import org.xjtu.framework.core.util.XMLUtil;
import org.xjtu.framework.modules.protocol.TaskDiscription;
import org.xjtu.framework.modules.socket.Client;
import org.xjtu.framework.modules.user.dao.ClusterManageCommand;
import org.xjtu.framework.modules.user.dao.FrameDao;
import org.xjtu.framework.modules.user.dao.JobDao;
import org.xjtu.framework.modules.user.dao.ProjectDao;
import org.xjtu.framework.modules.user.dao.TaskDao;
import org.xjtu.framework.modules.user.dao.UnitDao;
import org.xjtu.framework.modules.user.dao.ConfigurationDao;
import org.xjtu.framework.modules.user.dao.UserDao;
import org.xjtu.framework.modules.user.service.ClusterManageService;
import org.xjtu.framework.modules.user.service.ConfigurationService;
import org.xjtu.framework.modules.user.service.JobService;
import org.xjtu.framework.modules.user.service.UnitService;

import com.xj.framework.ssh.ShellLocal;



@Service("jobService")
public class JobServiceImpl implements JobService {
	private @Resource JobDao jobDao;
	private @Resource ProjectDao projectDao;
	
	private @Resource UserDao userDao;
	
	private @Resource FrameDao frameDao;
	
	private @Resource TaskDao taskDao;
	
	private @Resource UnitDao unitDao;
	
	private @Resource ConfigurationDao configurationDao;
	
	private @Resource ClusterManageCommand pbsCommand;
	
	private @Resource ClusterManageCommand shenweiCommand;
	
	private @Resource SystemConfig systemConfig;
	
	private static final Log log = LogFactory.getLog(JobServiceImpl.class);	
	private @Resource UnitService unitService;
	private @Resource ClusterManageService clusterManageService;
	private @Resource ConfigurationService configurationService;		
	private String pbsFilePath=this.getClass().getResource("/").getPath()+"shell/render_building_rUnit.pbs";
	
	@Override
	public List<Job> findJobsByProjectId(String projectId) {
		return jobDao.queryJobsByProjectId(projectId);
	}
	
	@Override
	public void addJob(Job job){
		jobDao.persist(job);
	}
		
	@Override
	public void deleteJob(Job job) {
		jobDao.removeJob(job);
	}
	
	@Override
	public Job findJobsById(String jobId){
		return jobDao.queryJobsById(jobId);
	}

	@Override
	public void updateJobInfo(Job job){
		jobDao.updateJob(job);
	}
	
	@Override
	public int findTotalCountByQuery(String searchText,String searchType,int jobStatus) {
		int num = 0;
		if(StringUtils.isBlank(searchText)){
			num = jobDao.queryCount(jobStatus);
		}else if(StringUtils.isBlank(searchType)){
			num = jobDao.queryCount(jobStatus);
		}else{
			if(searchType.equals("name")){
				num = jobDao.queryCountByJobName(searchText,jobStatus);
			}else if(searchType.equals("accurateProjectId")){
				num = jobDao.queryCountByAccurateProjectId(searchText,jobStatus);
			}else{
				num = jobDao.queryCount(jobStatus);
			}
		}
		return num;
	}
	
	@Override
	public List<Job> listJobsByQuery(String searchText,String searchType, int pageNum, int pageSize,int jobStatus) {
		List<Job> jobs = new ArrayList<Job>();
		if(StringUtils.isBlank(searchText)){
			jobs = jobDao.pagnate(pageNum, pageSize, jobStatus);
		}else if(StringUtils.isBlank(searchType)){
			jobs = jobDao.pagnate(pageNum, pageSize, jobStatus);
		}else{
			if(searchType.equals("name")){
				jobs = jobDao.pagnateByJobName(searchText, pageNum, pageSize, jobStatus);
			}else if(searchType.equals("accurateProjectId")){
				jobs = jobDao.pagnateByAccurateProjectId(searchText, pageNum, pageSize, jobStatus);
			}else{
				jobs = jobDao.pagnate(pageNum, pageSize, jobStatus);
			}
		}
		
		return jobs;
	}
	
	@Override
	public int findMaxQueueNumByPriority(int jobPriority) {
		return jobDao.queryMaxQueueNumByPriority(jobPriority);
	}
	
	@Override
	public int findMinQueueNumByPriority(int jobPriority) {
		return jobDao.queryMinQueueNumByPriority(jobPriority);
	}
	
	@Override
	public int findNewJobCountsByDate(Date date){
		return jobDao.queryNewJobCountsByDate(date);
	}
	@Override
	public int findStartJobCountsByDate(Date date){
		return jobDao.queryStartJobCountsByDate(date);
	}
	@Override
	public int findEndJobCountsByDate(Date date){
		return jobDao.queryEndJobCountsByDate(date);
	}

	@Override
	public List<Job> findQueueJobsByJobPriority(int jobPriority) {
		return jobDao.queryQueueJobsByJobPriority(jobPriority);
	}

	@Override
	public Job findJobByJobNameAndProjectId(String jobName, String projectId) {
		return jobDao.queryJobByJobNameAndProjectId(jobName,projectId);
	}
	
	@Override
	public List<Job> findDistributedJob(){
		return jobDao.queryDistributedJob();
		
	}

	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public void suspendJobByJobId(String jobId) {
		Job job=jobDao.queryJobsById(jobId);
		if(job!=null&&job.getJobStatus()==JobStatus.distributed){
			List<Unit> units=unitDao.queryUnitsByJobId(jobId);
			if(units!=null){
				for(int j=0;j<units.size();j++){
					try {
						//先销毁渲染单元
						if(systemConfig.getJobManageService().equals("openPBS")){
							pbsCommand.delJob(units.get(j).getPbsId());
						}else{
							shenweiCommand.delJob(units.get(j).getPbsId());
						}
						System.out.println(units.get(j).getPbsId()+" has been killed");
						//不删除该数据项是为了保留对应的帧信息和任务信息
						Unit u=units.get(j);
						u.setUnitStatus(UnitStatus.dead);
						unitDao.updateUnit(u);
					} catch (Exception e) {
						e.printStackTrace();
					}
											
					//找到该单元上所有未完成的帧，且由于单元跟task是一一对应的，所以这些帧属于同一task
					List<Frame> frames = frameDao.queryUnfinishedFramesByUnitId(units.get(j).getId());
					if(frames!=null&&frames.size()!=0){		        	
						//更新原先这些帧所属的task信息
						Task beforeTask=frames.get(0).getTask();
						beforeTask.setFrameNumber(beforeTask.getFrameNumber()-frames.size());
						beforeTask.setTaskProgress(100);//原来的task下剩余的帧肯定已完成的了，所以进度为100%
						beforeTask.setTaskStatus(TaskStatus.finished);
						taskDao.updateTask(beforeTask);
						//更新这些需要被重新计算的帧
						for(int k =0 ;k < frames.size(); k++){
							Frame frame = frames.get(k);		        						
							frame.setFrameProgress(0);
							frame.setFrameStatus(FrameStatus.notStart);
							frameDao.updateFrame(frame);
						}		        																	
					
					}
				}
			}
			
			//job的进度也需要更新
			job.setJobStatus(JobStatus.suspended);
			jobDao.updateJob(job);

		}
	}
	
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public void doStartCameraRender(List<String> jobIds) {
		
		for(int i=0;i<jobIds.size();i++){
			Job job=jobDao.queryJobsById(jobIds.get(i));
			if(job!=null){
				if(job.getJobStatus()==JobStatus.inQueue||job.getJobStatus()==JobStatus.distributed){
					continue;
				}
				else{
					//找对应队列的最大值
					int queueNum=jobDao.queryMaxQueueNumByPriority(job.getJobPriority())+1;
					job.setQueueNum(queueNum);
					job.setJobStatus(JobStatus.inQueue);
					jobDao.updateJob(job);
				}
			}

		}
	}
	
	@Override
	public void doCopyjobs(List<String> jobIds){
		for(int i=0;i<jobIds.size();i++){
			Job job=jobDao.queryJobsById(jobIds.get(i));
			if(job!=null){
				Job j=new Job();
				int num;
				j.setId(UUIDGenerator.getUUID());
				num=jobDao.queryCountcopyJobName(job.getCameraName());
				j.setCameraName(job.getCameraName()+"-副本"+num);
				j.setCreateTime(new Date());
				j.setJobStatus(JobStatus.notStart);
				j.setQueueNum(-1);
				j.setCameraProgress(0);
				j.setJobPriority(job.getJobPriority());					
				j.setFrameRange(job.getFrameRange());					
				j.setFrameNumbers(job.getFrameNumbers());															
				j.setFilePath(job.getFilePath());
				j.setRenderCost(0.0);
				j.setPreRenderingTag(job.getPreRenderingTag());
				j.setxResolution(job.getxResolution());
				j.setyResolution(job.getyResolution());
				j.setSampleRate(job.getSampleRate());
				j.setRenderEngine(job.getRenderEngine());
				j.setUnitsNumber(job.getUnitsNumber());
				Project p= job.getProject();
				j.setProject(p);
				jobDao.persist(j);
				p.setCamerasNum(p.getCamerasNum()+1);
				projectDao.updateProject(p);
				
				
				}
			
		}
		
	}
	
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public void doStopCameraRender(String jobId) {
		
		Job job=jobDao.queryJobsById(jobId);
			
		List<Unit> units=unitDao.queryUnitsByJobId(jobId);
		if(units!=null&&units.size()>0){
			for(int j=0;j<units.size();j++){
				try {
					if(systemConfig.getJobManageService().equals("openPBS")){
						pbsCommand.delJob(units.get(j).getPbsId());
					}else{
						shenweiCommand.delJob(units.get(j).getPbsId());
					}
					unitDao.removeUnit(units.get(j));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		job.setEndTime(null);
		job.setStartTime(null);
		job.setCameraProgress(0);
		job.setJobStatus(JobStatus.notStart);
		jobDao.updateJob(job);
		
	}
	
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public void changeQueuingJobToTop(String jobId) {
		Job j=jobDao.queryJobsById(jobId);
		if(j!=null&&j.getJobStatus()==JobStatus.inQueue){
			int priority=j.getJobPriority();
			int queueNum=jobDao.queryMinQueueNumByPriority(priority)-1;
			j.setQueueNum(queueNum);
			jobDao.updateJob(j);

		}
	}

	@Override
	public Job findHeadQueuingJob() {
		return jobDao.queryHeadQueuingJob();
	}
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public Boolean distributeRunitJob(Job job, int unitsNumber, int nodesNumPerUnit, String renderEngineName) {
		Job existedJob=jobDao.queryJobsById(job.getId());
		log.info("distribute job is starting"); 
		if(job.equals(existedJob)){
			/*将所有rib文件放入hash表中*/
			/*将要渲染的帧号放入数组中*/
			String filePath = existedJob.getFilePath();
			String frameRange = existedJob.getFrameRange();
			int frameNums = existedJob.getFrameNumbers();							
			//新增调整代码
			//filePath=filePath.substring(0,filePath.lastIndexOf("/"));
			/*修改成命令方式*/
			
			String cmd="cd "+filePath+" && ls *.xml";
			ShellLocal shell=new ShellLocal(cmd);
			String result=shell.executeCmd();
			result=result.trim();
			String[] filenames=result.split("\\n");//渲染的文件名+帧号
			Map<Integer,String> mapScenePath=new HashMap<Integer,String>();
			for(int i=0;i<filenames.length;i++){
				String[] frameNo=filenames[i].split("\\.");
				Integer key;
				try{
					//eg；frame1.xml
					String frameID=StringUtil.getNumb(frameNo[frameNo.length-2]);
					key=Integer.parseInt(frameID);
					//key=Integer.parseInt(frameNo[frameNo.length-2]);
				}catch(NumberFormatException e){
					continue;
				}
				mapScenePath.put(key, filenames[i]);//(帧号和文件名字)
			}
		
			int[] frameToRender=new int[frameNums];
			int k=0;
			String[] ss=frameRange.split(",");
			for(int i=0;i<ss.length;i++){
				String[] temp=ss[i].split("-");
				if(temp.length==1){
					frameToRender[k]=Integer.parseInt(temp[0]);
					k++;
				}else{
						Integer end=Integer.parseInt(temp[1]);
						Integer begin=Integer.parseInt(temp[0]);
						for(int z=0;z<end-begin+1;z++){
							frameToRender[k]=begin+z;
							k++;
						}
				}
			}
			
			
			// 需要将unitsNumber分成两类，其中一类要多渲染1帧的画面。
			int between = frameNums / unitsNumber; // 定义每个节点需要承担的任务量
			int remain = frameNums % unitsNumber; // 计算出余数,根据数学知识，余数的个数为需要多渲染一帧的节点数量
			int preRenderTag = existedJob.getPreRenderingTag();
			int s=0;
			int e=frameNums;
			int NodesNumPerUnit=configurationService.findAllConfiguration().getNodesNumPerUnit();
			String fuWuListName=configurationService.findAllConfiguration().getFuWuListName();
			String sceneMem=configurationService.findAllConfiguration().getSceneMemory();
			String hostStack=configurationService.findAllConfiguration().getHostStack();
			String shareMen=configurationService.findAllConfiguration().getShareSize();
			
			
			//这里是用户指定了几个单元，每个单元分配几祯任务
			for (int i = 0; i < unitsNumber; i++) {
				int temp;
				// 确定执行节点
				String stdout="";
				String newUnitId=UUIDGenerator.getUUID();
				
				
			
				log.info("preRender handal....");
				/*在这里做两件事，1，分配正常的作业渲染任务。
				2分配预渲染，预渲染首先根据采样路找到要预渲染的祯，然后修改这些祯的像素点数和照片宽和高,,
				构成新的预渲染渲染文件xml,命名为frame1_pre.xml,最后给用户展现*/
				int PreRenderingTag=job.getPreRenderingTag();
				List<String>PreFramePath=new ArrayList<String>();//用来存储预渲染的的路径
				if(PreRenderingTag==1){
					//分配预渲染任务
					int FrameNumbers=job.getFrameNumbers();
					String FrameRange=job.getFrameRange();
					int SampleRate=job.getSampleRate();
					int x=job.getxResolution();
					int y=job.getyResolution();
		            
					//sed -i -e '1 iaaaa\nbbbb\ncccc'  test
					String sedContent="";

					int PreframeNum=(int)(FrameNumbers*SampleRate*0.01);
					String PreframeNo[]=StringUtil.getSelectFrame(FrameRange,PreframeNum); //要先对字符串排序
					for(String str:PreframeNo){
					try {
						//备份渲染文件，修改预渲染的分辨率和像素，假定设置为100个像素,第一次复制后修改，第二次在新文件修改
						//替换采样率
						//
						//sed -i 's;<integer name="height" value=".*"/>;<integer name="height" value="8888"/>;g'  ./frame1.xml
						//sed -i 's;<integer name="width" value=".*"/>;<integer name="width" value="8888"/>;g'  ./frame1.xml
						String cpCmd="cp "+filePath+"/frame"+str+".xml"+" "+filePath+"/frame"+str+"_pre.xml";
						String sampleCountCmd="sed -i 's;<integer name=\"sampleCount\" value=\".*\"/>;<integer name=\"sampleCount\" value=\"100\"/>;g' "+ filePath+"/frame"+str+"_pre.xml";
						String heightCmd="sed -i 's;<integer name=\"height\" value=\".*\"/>;<integer name=\"height\" value=\""+x+"\"/>;g' "+filePath+"/frame"+str+"_pre.xml";
						String widthCmd="sed -i 's;<integer name=\"width\" value=\".*\"/>;<integer name=\"width\" value=\""+y+"\"/>;g' "+filePath+"/frame"+str+"_pre.xml";
						
						String Cmd=cpCmd+" && "+sampleCountCmd+" && "+heightCmd+" && "+widthCmd;
						ShellLocal ink = new ShellLocal();
		    			ink.setCmd(Cmd);
						ink.executeCmd();
						
						/*XMLUtil.changeAttribute(filePath+"/frame"+str+".xml", "scene/sensor/sampler/integer","name:sampleCount","value:100");
						XMLUtil.changeAttribute(filePath+"/frame"+str+"_pre.xml","scene/sensor/film/integer","name:width","value:"+x);
						XMLUtil.changeAttribute(filePath+"/frame"+str+"_pre.xml","scene/sensor/film/integer","name:height","value:"+y);*/
						sedContent+="";
						PreFramePath.add(filePath+"/frame"+str+"_pre.xml");
					//收集起新备份的修改像素后的祯，交给神威渲染。
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					}
					
					log.info("preRender frame list is"+PreFramePath);
					//神威对预渲染祯进行渲染
				}
				
				
				
				//------------------------------------处理阶段-------------------------------------------
				
				
				if(systemConfig.getJobManageService().equals("openPBS")){	
					stdout = clusterManageService.submit(pbsFilePath);
				}else{
					//这里可以删除，也可以做一些初始化的工作，后面再确定
					 SystemConfig systemConfig=new SystemConfig();
					 ///home/export/online1/systest/swsdu/xijiao/Users配置文件的工作目录
		    		String renderingWorkDir=systemConfig.getRenderingWorkDir();
	                    //指令地址后期更改  
		    		//bsub -q q_sw_share -I -b -share_size 7168 -n 31 /home/export/online1/systest/swsdu/xijiao/mitsuba-0420/dist/mitsuba /home/export/online1/systest/swsdu/xijiao/framedir2/ |tee result-frame-n21
		    		String camraPath="/home/export/online1/systest/swsdu/xijiao/framedir2/";
		    		String renderInstruct="/home/export/online1/systest/swsdu/xijiao/mitsuba-diao-end/dist/mitsuba";
		    		String Cmd="cd /home/export/online1/systest/swsdu/xijiao/mitsuba-diao-end";//进入引擎目录
					String shenweiPbsCommand="";
						Date stime=new Date();//此处先全部渲染，后面更改
						//shenweiPbsCommand="-q q_sw_share -I -b -share_size 7168 -n 31 "+ renderInstruct+" "+filePath+" / |tee result-frame-n21";
						//临时使用就引擎，后面更换过来新的镜头组织结构
						///home/export/online1/systest/swsdu/xijiao/Users/xjzhang/camera3/   1,2,4,7,9,12,13,20 |tee test
						//shenweiPbsCommand="-q q_sw_expr"+" -o /home/export/online1/systest/swsdu/xijiao/Outprint/"+newUnitId+".out"+" -b -share_size 7168 -n 31 "+ renderInstruct+" "+camraPath+"/  1,2,4,7,9,12,13,20 |tee result-frame-n21";
						//全部渲染
						if(renderEngineName.equals("rUnit"))
							shenweiPbsCommand="-q "+fuWuListName+" -o /home/export/online1/systest/swsdu/xijiao/Outprint/"+newUnitId+".out"+"-l -b -share_size 7168 -n 31 "+ renderInstruct+" "+filePath+"/";
						else
							shenweiPbsCommand="-b -m 1 -p -q "+fuWuListName+" -o /home/export/online1/systest/swsdu/Outprint/"+newUnitId+".out -host_stack "+hostStack+" -share_size "+shareMen+" -n "+NodesNumPerUnit+" -cgsp 64 /home/export/online1/systest/swsdu/RenderWing_hust/bin/rUnit -t 1 -b "+sceneMem+" -s "; 
						//stdout = clusterManageService.submit(shenweiPbsCommand);
						PbsExecute  pbs=new PbsExecute(Cmd+" && bsub "+shenweiPbsCommand);
					     stdout=pbs.executeCmd().trim();
					     log.info("return stdout is "+stdout);
					     if(stdout!=null&&stdout.length()>0)
					    	 stdout=stdout.substring(stdout.indexOf('<')+1,stdout.indexOf('>'));
						/*Date etime=new Date();
						log.info("WLBSUB"+ (etime.getTime()-stime.getTime()));*/
				          }
				
				log.info("Initializing Rendering Unit..."+stdout);
				
				
				
				Unit unit=new Unit();
				unit.setId(newUnitId);
				unit.setPbsId(stdout);
				//unit.setUnitStatus(UnitStatus.unavailable);
				unit.setUnitStatus(UnitStatus.busy);
				unit.setUnitNodesNum(nodesNumPerUnit);
				unit.setIdleNumber(0);
				unitService.addUnit(unit);
				log.info("Rendering Unit initialized");
				// 进行作业的划分执行编写
				if (i < unitsNumber - remain) {
					// 承担任务量为between的节点个数为nu-remain
					temp = s + between - 1;// 定义一个临时终点变量
				} else {
					temp = s + between;// 定义一个临时终点变量
				}
				// 记录任务信息
				Task task=new Task();
				task.setId(UUIDGenerator.getUUID());
				System.out.println("temp :"+temp+",s:"+s+",between:"+between+",:remain:"+remain);
				task.setFrameNumber(temp-s+1);
				task.setTaskProgress(0);
				task.setTaskStatus(TaskStatus.started);
				task.setStartTime(new Date());
				task.setJob(existedJob);
				task.setUnit(unit);
				task.setUpdateTime(new Date());
				taskDao.persist(task);
				for (int j = s; j < temp + 1; j++) {
					Frame frame = new Frame();
					frame.setStartTime(new Date());
					frame.setId(UUIDGenerator.getUUID());
					frame.setFrameName(mapScenePath.get(frameToRender[j]));				
					frame.setFrameStatus(FrameStatus.notStart);
					frame.setFrameProgress(0);
					frame.setTask(task);
					frameDao.persist(frame);
				}								
				// 进行变量的更新
				s = temp + 1; // 每次节点分发完以后，进行起始节点的更新；
			}
			/*更新作业状态信息*/
			existedJob.setStartTime(new Date());
			existedJob.setJobStatus(JobStatus.distributed);
			jobDao.updateJob(existedJob);
			log.info("distibute is finish=====================>");
			return true;
		}else{
			return false;
		}
	}
	
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public Boolean continueRunitJobtoTask(Job job, int unitsNumber, int nodesNumPerUnit, List<Frame> fs, String renderEngineName) {
		Job existedJob=jobDao.queryJobsById(job.getId());
		if(job.equals(existedJob)){
			int s = 0;
			int e = fs.size()-1;			
			String filePath = existedJob.getFilePath();
			int preRenderTag = existedJob.getPreRenderingTag();
			
			// 进行作业分发，作业分发的思想是在多个渲染节点上进行均分
			// 设计了一个新的更能体现均分思想的算法（除法定理）
			int all = e - s + 1; // 定义需要渲染的帧数总量
			// 需要将unitsNumber分成两类，其中一类要多渲染1帧的画面。
			int between = all / unitsNumber; // 定义每个节点需要承担的任务量
			int remain = all % unitsNumber; // 计算出余数,根据数学知识，余数的个数为需要多渲染一帧的节点数量
			int NodesNumPerUnit=configurationService.findAllConfiguration().getNodesNumPerUnit();
			String fuWuListName=configurationService.findAllConfiguration().getFuWuListName();
			String sceneMem=configurationService.findAllConfiguration().getSceneMemory();
			SystemConfig config=new SystemConfig();
			//String renderingWorkDir=config.getRenderingWorkDir();
			for (int i = 0; i < unitsNumber; i++) {
				int temp;
				// 确定执行节点
				String stdout="";
				String newUnitId=UUIDGenerator.getUUID();
				if(systemConfig.getJobManageService().equals("openPBS")){	
					stdout = clusterManageService.submit(pbsFilePath);
				}else{
					//计算祯的操作
					 SystemConfig systemConfig=new SystemConfig();
		    		String renderingWorkDir=systemConfig.getRenderingWorkDir();
	                    //指令地址后期更改  
		    		String camraPath="/home/export/online1/systest/swsdu/xijiao/framedir2/";
		    		String renderInstruct="/home/export/online1/systest/swsdu/xijiao/mitsuba-0420/dist/mitsuba";
		    		String Cmd="cd /home/export/online1/systest/swsdu/xijiao/mitsuba-0420";//进入引擎目录
					String shenweiPbsCommand="";
						Date stime=new Date();//此处先全部渲染，后面更改
							//临时使用就引擎，后面更换过来新的镜头组织结构
						//shenweiPbsCommand="-q q_sw_share"+" -o /home/export/online1/systest/swsdu/xijiao/Outprint/"+newUnitId+".out"+" -b -share_size 7168 -n 31 "+ renderInstruct+" "+camraPath+" / |tee result-frame-n21";
						//stdout = clusterManageService.submit(shenweiPbsCommand);
						/*shenweiPbsCommand="-q q_sw_expr"+" -o /home/export/online1/systest/swsdu/xijiao/Outprint/"+newUnitId+".out"+" -b -share_size 7168 -n 31 "+ renderInstruct+" "+filePath+"/";
						PbsExecute pbs=new PbsExecute(Cmd+" && bsub "+shenweiPbsCommand);
					     stdout=pbs.executeCmd().trim();
					     log.info("return stdout is "+stdout);
					     if(stdout!=null&&stdout.length()>0)
					    	 stdout=stdout.substring(stdout.indexOf('<')+1,stdout.indexOf('>'));
						Date etime=new Date();
						log.info("WLBSUB"+ (etime.getTime()-stime.getTime()));*/
				}					
				log.info("Initializing Rendering Unit..."+stdout);	
				Unit unit=new Unit();
				unit.setId(newUnitId);
				unit.setPbsId(stdout);
				unit.setUnitStatus(UnitStatus.unavailable);
				unit.setUnitNodesNum(nodesNumPerUnit);
				unit.setIdleNumber(0);
				unitService.addUnit(unit);
				
				log.info("Rendering Unit initialized");
				
				// 进行作业的划分执行编写
				if (i < unitsNumber - remain) {
					// 承担任务量为between的节点个数为nu-remain
					temp = s + between - 1;// 定义一个临时终点变量
				} else {
					temp = s + between;// 定义一个临时终点变量
				}
				
				// 记录任务信息
				Task task=new Task();
				task.setId(UUIDGenerator.getUUID());
				task.setFrameNumber(temp-s+1);
				task.setTaskProgress(0);
				task.setTaskStatus(TaskStatus.started);
				task.setStartTime(new Date());
				task.setJob(existedJob);
				task.setUnit(unit);
				taskDao.persist(task);
				
				for (int j = s; j < temp + 1; j++) {
					Frame frame=fs.get(j);
					frame.setFrameStatus(FrameStatus.notStart);
					frame.setFrameProgress(0);
					frame.setTask(task);
					frameDao.updateFrame(frame);
				}						
				// 进行变量的更新
				s = temp + 1; // 每次节点分发完以后，进行起始节点的更新；
			}
			/*更新作业状态信息*/
			existedJob.setStartTime(new Date());
			existedJob.setJobStatus(JobStatus.distributed);
			existedJob.setPaymentStatus(PaymentStatus.notPayment);
			jobDao.updateJob(existedJob);
			return true;
		}else{
			return false;
		}
	}

	
	
	
	
	
	@Override
	public List<Job> findJobs() {
		return jobDao.queryJobs();
	}
	
	
	@Override
	public double JobRenderPrice(String jodId){
		//int frameFinishNum;
		double RenderPrice;
		Job job;
		long mins=0;
		List<Frame> finishFrame;
			finishFrame=frameDao.queryFramesByJobId(jodId);
			if(finishFrame!=null &&finishFrame.size()>0){
				for(int j=0 ; j<finishFrame.size() ; j++){
					long temp=finishFrame.get(j).getEndTime().getTime() -finishFrame.get(j).getStartTime().getTime();//相差毫秒数
					long temp2 = temp % (1000 * 3600);
			        mins += temp2 / 1000 / 60;                    //相差分钟数
				}
			}
			if(mins==0){
				RenderPrice=1*configurationDao.queryUnitPrice();
			}else{
				RenderPrice=mins*configurationDao.queryUnitPrice();
			}
			DecimalFormat df=new DecimalFormat(".##");//精确到小数点后两位
			String st=df.format(RenderPrice);
			RenderPrice= Double.parseDouble(st);
		return RenderPrice;
	}
	
	@Override
	public void JobPayment(String jobId){
		Job job=jobDao.queryJobsById(jobId);
		User user=job.getProject().getUser();
		if(user.getCardBalances()==null){
			user.setCardBalances(0.0);//默认赋值
			userDao.updateUser(user);
		}
		if(job.getPaymentStatus()==null){
			job.setPaymentStatus(PaymentStatus.notPayment);
			//jobDao.updateJob(job);
		}		
		if(job.getPaymentStatus()!=PaymentStatus.hasPayment){
			if(job.getJobStatus()==JobStatus.finished){
				if(user.getCardBalances()<job.getRenderCost()){
					job.setPaymentStatus(PaymentStatus.notEnoughPayment);
				}
				else{
					user.setCardBalances(user.getCardBalances() - job.getRenderCost());//更新用户余额
					job.setPaymentStatus(PaymentStatus.hasPayment);
				}
			}
			jobDao.updateJob(job);
			userDao.updateUser(user);
		}	
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRED,isolation=Isolation.SERIALIZABLE)
	public void unitBalaing(String existedJobId , String taskBalacingId){
		
	
	List<Task> alltasks =jobDao.queryJobsById(existedJobId).getTasks();
	ArrayList<Task> tasks = new ArrayList<Task>();
	for(int i =0; i<alltasks.size() ; i++){
		if(alltasks.get(i).getTaskStatus()==TaskStatus.started){
			tasks.add(alltasks.get(i));
		}
	}
	
	
	Task taskBalacing=taskDao.queryTasksById(taskBalacingId);
	Task task;
	int unitIdleNum=taskBalacing.getUnit().getIdleNumber();
	 Random rand = new Random();
	 int recycle;
	
	 if(tasks != null && tasks.size()>0 ){
		 if(tasks.size()>20){
			 recycle= 20;
		 }else{
			 recycle=tasks.size();
		 }
		 
		 for(int j =0 ; j<recycle ; j++){//循环小于5次
			 if(tasks.size()>20){
				 task=tasks.get(rand.nextInt(tasks.size()));
			 }else{
				 task=tasks.get(j);
			 }
			 if(!task.getId().equals(taskBalacing.getId())){
				 Unit unitForThisTask =task.getUnit();
				 if(task.getTaskStatus()==TaskStatus.started && unitForThisTask.getIdleNumber()==0){//task没有剩余帧则调度到taskBalacing
					 List<Frame> NotStartframesForThisTask=frameDao.queryByFrameStatusAndTaskId(FrameStatus.notStart, task.getId());
					 if(NotStartframesForThisTask.size()>0 && NotStartframesForThisTask!=null ){
						 int taskFrameNumber=task.getFrameNumber();
						 int taskBalacintFrameNumber = taskBalacing.getFrameNumber();
						 if (NotStartframesForThisTask.size() < unitIdleNum){
							 
							 for(int k =0 ; k<NotStartframesForThisTask.size(); k++){
								 NotStartframesForThisTask.get(k).setTask(taskBalacing);
								 frameDao.updateFrame(NotStartframesForThisTask.get(k));
							 }
																											 
							 task.setFrameNumber(taskFrameNumber-NotStartframesForThisTask.size());
							 log.info("1wanglei_task"+task.getId()+"  ");
							 
							taskBalacing.setFrameNumber(taskBalacintFrameNumber+NotStartframesForThisTask.size());
							 log.info("1wanglei_tasktaskBalacing"+taskBalacing.getId()+"  ");
							 taskDao.updateTask(task);
							 taskDao.updateTask(taskBalacing);
							 
							 log.info("task.getFrameNumber()");
							 log.info(task.getFrameNumber());
							 log.info("taskBalacing.getFrameNumber()");
							 log.info(taskBalacing.getFrameNumber());
							 break;
							 
						 }//end if
						 else{
								
							 for(int k =0 ; k<unitIdleNum; k++){
								 NotStartframesForThisTask.get(k).setTask(taskBalacing);
								 frameDao.updateFrame(NotStartframesForThisTask.get(k));
							 }
									
							 task.setFrameNumber(taskFrameNumber-unitIdleNum);
							 log.info("2wanglei_task"+task.getId()+"  ");
							
							 
							 taskBalacing.setFrameNumber(taskBalacintFrameNumber+unitIdleNum);
							 log.info("2wanglei_taskBalacing"+taskBalacing.getId()+"  ");
						
							 log.info("unitIdleNum");
							 log.info(unitIdleNum);
							
							 
							 taskDao.updateTask(task);
							 taskDao.updateTask(taskBalacing);
							 
							 
							 log.info(" task.getFrameNumber()");
							 log.info(task.getFrameNumber());
							 log.info("taskBalacing.getFrameNumber()");
							 log.info(taskBalacing.getFrameNumber());
							 
							 break;
							
							 
							 
						 }
						 
					 }
					 
					 
				 }
			 }
		 }
	 }
	}
	@Override
	public void taskadding(String existedJobId , String taskBalacingId){//任务追加
		Job existedJob = jobDao.queryJobsById(existedJobId);
		Task taskBalacing = taskDao.queryTasksById(taskBalacingId);
		List<Frame> frames=frameDao.queryByFrameStatusAndTaskId(FrameStatus.notStart, taskBalacing.getId());
		Unit existunit= taskBalacing.getUnit();
		int notStartFramesNum = 0;
		int unitIdleNum = existunit.getIdleNumber();
		
		 if (frames!=null && frames.size()>0){
			 notStartFramesNum = frames.size();		
		 }   
				
		
		ArrayList<String> frameNames = new ArrayList<String>();
		TaskDiscription td = new TaskDiscription();
	
	 	td.setTaskID(taskBalacing.getId());
		td.setCameraPath(existedJob.getFilePath());
		td.setMemorySize(10240);
		td.setPreRenderTag(existedJob.getPreRenderingTag());
		if(existedJob.getPreRenderingTag() == 1){
			td.setX_Resolution(existedJob.getxResolution());
			td.setY_Resolution(existedJob.getyResolution());
			td.setSampleRate(existedJob.getSampleRate());
		}
		if (unitIdleNum < notStartFramesNum+1 || unitIdleNum == notStartFramesNum){
			td.setFrameNumber(unitIdleNum);//(2015-05-06修改发送任务的帧数)
			for(int i=0;i<unitIdleNum;i++){
				frameNames.add(frames.get(i).getFrameName());
				frames.get(i).setFrameStatus(FrameStatus.unfinished);//实时跟新数据库
				frames.get(i).setStartTime(new Date());
				frameDao.updateFrame(frames.get(i));
				
			}
			td.setFrameNames(frameNames);
			existunit.setIdleNumber(0);
			existunit.setUnitStatus(UnitStatus.busy);
		
			unitDao.updateUnit(existunit);
		}else{
			td.setFrameNumber(notStartFramesNum);//(2015-05-06修改发送任务的帧数)
			for(int i=0;i<notStartFramesNum;i++){
				frameNames.add(frames.get(i).getFrameName());
				frames.get(i).setFrameStatus(FrameStatus.unfinished);//实时跟新数据库
				frames.get(i).setStartTime(new Date());
				frameDao.updateFrame(frames.get(i));
			}
			td.setFrameNames(frameNames);
			existunit.setIdleNumber(unitIdleNum-notStartFramesNum);
			unitDao.updateUnit(existunit);
		}
		String message = td.connectString();
		
		
		boolean isSucceed=Client.sendToC(message, existunit.getUnitMasterName(), 5169);//发送任务信息
		if(!isSucceed){
			log.info("wangleiUnitsendfail"+existunit.getId());
		}
			
		
			
		
		
	}
	
	
	
	
}


