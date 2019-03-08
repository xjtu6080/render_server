package org.xjtu.framework.modules.user.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xjtu.framework.backstage.distribute.JobDistribute;
import org.xjtu.framework.backstage.schedule.JobProgressUpdate;
import org.xjtu.framework.core.base.constant.FrameStatus;
import org.xjtu.framework.core.base.constant.JobStatus;
import org.xjtu.framework.core.base.constant.MessageState;
import org.xjtu.framework.core.base.constant.PaymentStatus;
import org.xjtu.framework.core.base.constant.SystemConfig;
import org.xjtu.framework.core.base.constant.TaskStatus;
import org.xjtu.framework.core.base.constant.UnitStatus;
import org.xjtu.framework.core.base.model.Frame;
import org.xjtu.framework.core.base.model.Job;
import org.xjtu.framework.core.base.model.Message;
import org.xjtu.framework.core.base.model.Task;
import org.xjtu.framework.core.base.model.Unit;
import org.xjtu.framework.core.util.PbsExecute;
import org.xjtu.framework.core.util.StringUtil;
import org.xjtu.framework.core.util.UUIDGenerator;
import org.xjtu.framework.core.util.LinuxInvoker;
import org.xjtu.framework.modules.protocol.ProgressMessage;
import org.xjtu.framework.modules.protocol.RenderingFrameProgress;
import org.xjtu.framework.modules.user.dao.ClusterManageCommand;
import org.xjtu.framework.modules.user.dao.FrameDao;
import org.xjtu.framework.modules.user.dao.JobDao;
import org.xjtu.framework.modules.user.dao.MessageDao;
import org.xjtu.framework.modules.user.dao.TaskDao;
import org.xjtu.framework.modules.user.dao.UnitDao;
import org.xjtu.framework.modules.user.service.ClusterManageService;
import org.xjtu.framework.modules.user.service.FrameService;
import org.xjtu.framework.modules.user.service.JobService;
import org.xjtu.framework.modules.user.service.TaskService;
import org.xjtu.framework.modules.user.service.UnitService;
import org.xml.sax.InputSource;

import com.xj.framework.ssh.ShellLocal;


@Service("frameService")
public class FrameServiceImpl implements FrameService {
	
	private static Log log = LogFactory.getLog(FrameServiceImpl.class);

	private @Resource FrameDao frameDao;
	private @Resource TaskDao taskDao;
	private @Resource JobDao jobDao;
	private @Resource UnitDao unitDao;
	private @Resource MessageDao messageDao;
	
	private @Resource ClusterManageService clusterManageService;
	private @Resource JobService jobService;
	                     
	private @Resource UnitService unitService;
	private @Resource TaskService taskService;
	private @Resource FrameService frameService;
	
	private @Resource ClusterManageCommand pbsCommand;
	
	private @Resource ClusterManageCommand shenweiCommand;
	
	//new add
	private @Resource SystemConfig systemConfig;
	@Override
	public void addFrame(Frame frame) {
		frameDao.persist(frame);
	}
	
	@Override
	public void updateFrameInfo(Frame frame) {
		frameDao.updateFrame(frame);
	}
	
	@Override
	public List<Frame> findUnfinishedFramesByUnitId(String unitId){
		return frameDao.queryUnfinishedFramesByUnitId(unitId);
	}
	
	@Override
	public List<Frame> findNotStartFramesByJobId(String jobId){
		return frameDao.queryNotStartFramesByJobId(jobId);
	}
	
	@Override
	public List<Frame> findFramesByTaskId(String taskId){
		return frameDao.queryFramesByTaskId(taskId);
	}	

	@Override
	public List<Frame> findFramesByJobId(String jobId){
		return frameDao.queryFramesByJobId(jobId);
	}
	
	@Override
	public Frame findFrameByNameAndTaskId(String name, String taskId) {
		return frameDao.queryByNameAndTaskId(name, taskId);
	}

	@Override
	public List<Frame> findFrameByName(String name) {
		return frameDao.queryFramesByName(name);
	}
	
	@Override
	public List<Frame> findByFrameStatusAndTaskId(int frameStatus , String taskId) {
		return frameDao.queryByFrameStatusAndTaskId(frameStatus, taskId);
	}
	@Override
	public int findCountByFrameStatusAndTaskId(int frameStatus , String taskId) {
		return frameDao.queryCountByFrameStatusAndTaskId(frameStatus, taskId);
	}
	
	@Override
	public void updateFrameInfoByProgressMessage(ProgressMessage progressMessage){

		Map<String,Frame> mapFrames=new HashMap<String,Frame>();
		List<Frame> listFrames=frameDao.queryFramesByTaskId(progressMessage.getTaskId());
		Task task=listFrames.get(0).getTask();
		Unit unit=task.getUnit();
	
		
		//
		int taskFinish=progressMessage.getAllFinished();

		
			unit.setIdleNumber(progressMessage.getIdleNumber());
			if(progressMessage.getIdleNumber()>0){
				unit.setUnitStatus(UnitStatus.available);
			}
			if(progressMessage.getIdleNumber()==0){
				unit.setUnitStatus(UnitStatus.busy);
			}
			unitDao.updateUnit(unit);
		
		
	/*	if(taskFinish==1){//所有帧全部完成 杀死对应的渲染单元
			task.setEndTime(new Date());
			task.setTaskStatus(TaskStatus.finished);
			task.setTaskProgress(100);
			Unit unit=task.getUnit();

			try {
				clusterManageService.dismissUnit(unit.getPbsId());
			} catch (Exception e) {
				e.printStackTrace();
			}

			unit.setUnitStatus(UnitStatus.dead);
			unitDao.updateUnit(unit);
			
		}*/
		
		
		
		if(listFrames!=null&&listFrames.size()>0){
			//Task task=listFrames.get(0).getTask();
			
			Job job=task.getJob();
							
			//如果该帧未开始，则证明该作业被暂停，此消息乃延迟消息，则不更新任何操作
			if(job.getJobStatus()==JobStatus.suspended)
				return;
				
			for(int i=0;i<listFrames.size();i++){
				mapFrames.put(listFrames.get(i).getFrameName(),listFrames.get(i));
			}
						
			for(int i=0; i<progressMessage.getReportedFrameNumber();i++){
				RenderingFrameProgress renderingFrameProgress=progressMessage.getReportedFrames().get(i);
				String frameName=renderingFrameProgress.getFrameName();
	
				Frame frame=mapFrames.get(frameName);		
				
				if(frame!=null && frame.getFrameStatus()!=FrameStatus.finished){
					
					frame.setFrameProgress((int)renderingFrameProgress.getProgress());
					frame.setErrorInfo(renderingFrameProgress.getErrorInfo());
					if((int)renderingFrameProgress.getError_Tag()!=0)
					{
						frame.setFrameProgress(100);
						frame.setFrameStatus(FrameStatus.isError); //  非0: 有错误 将状态设置为3
						frame.setEndTime(new Date());		
					}
					
					//frame.setFrameStatus(renderingFrameProgress.getFinishedTag());
									
					if((int)renderingFrameProgress.getProgress() == 100 ){
						
						frame.setFrameStatus(FrameStatus.finished);
						frame.setEndTime(new Date());
					}
					
					frameDao.updateFrame(frame);
				}
			}
			
			//Queue_e.task_queue.add(task);
		}
	}
	
	//从祯的日志文件读取进度

	public String updateFrameInfoByLog(String FrameLogPath) {
        File file=new File(FrameLogPath);
        if(!file.exists())return null;
        
		String latestProcessInfo;
		String rato="";
		try {
			latestProcessInfo = StringUtil.readLastLine(file);
		String ss[]=latestProcessInfo.split("\t");
		if(ss.length==3)
		{
			rato=ss[1].split(":")[1];
			return rato.substring(0, rato.indexOf("%"));
		}
		} catch (IOException e) {
			
			e.printStackTrace();
	}
		return null;
	}
	
	
	
	

	
	public String GetRation(String logLastLine) {
		String rato="";
		if(logLastLine==null||logLastLine.length()==0)return null;
		String ss[]=logLastLine.split("\t");
		String tmp[]=null;
		if(ss.length==3)
		{
				tmp=ss[1].split(":");
				if(tmp[0].equals("task"))return "100";
				if(tmp.length==2)
			     rato=tmp[1];
				else return null;
			return rato.substring(0, rato.indexOf("%"));
		}
		return null;
	}
	
	
	
	
	
	
	public int GetJobStatus(String id) {
		if(systemConfig.getJobManageService().equals("openPBS")){	
			String result=pbsCommand.getJobInfoByXmlFormat(id);
			if (result!=null) {
				//解析result获得节点名
				StringReader read = new StringReader(result);			
				InputSource source = new InputSource(read);			
				SAXBuilder sb = new SAXBuilder();
				Document doc;
				try {
					doc = sb.build(source);
					Element data = doc.getRootElement();
					List list = data.getChildren();
					Element nodes = (Element)list.get(0);
					Element sta = nodes.getChild("job_state");
					log.info("the job status is----------------------"+sta.getText());
					if(sta.getText().equals("C"))return 1;
					else return 0;
				} catch (Exception e) {
					
					e.printStackTrace();
				} 
				//exec_host字符串具体到每个节点每个核，因此较长，需要处理
				//处理后只定位到节点
			}else return 1;
		}else{
			//神威来获取作业的进度信息
			shenweiCommand.getJobInfoByXmlFormat(id);
			
			
			
		}
		
		
	return 1;
	}
	
	
	
	
	//预处理
	public void HandalMessage(){
		int jobstatus;
			List<Unit> units=unitService.findUnitsByStatus(UnitStatus.busy);
			log.info("busy unit size is"+units.size());	
			if(units==null)return;
			/*while(units==null){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				units=unitService.findUnitsByStatus(1);
			}*/
			for(Unit unit:units){
				jobstatus=GetJobStatus(unit.getPbsId());
			List<Task> tasks=taskService.findTasksByUnitId(unit.getId());
			if(tasks==null || tasks.size()==0){
				break;
			}
			for(Task task:tasks){
			Job existedJob=task.getJob();
			List<Frame> frames=frameService.findFramesByTaskId(task.getId());
			if(frames==null || frames.size()==0){
				break;
			}
			for(Frame frame:frames){
				if(jobstatus==1){
					frame.setEndTime(new Date());
					frame.setFrameProgress(100);
					frame.setFrameStatus(3);//3代表完成
					frameDao.updateFrame(frame);
				}
				
			}
				
		}
			}
}
	
	
	
	
	
	//神威下的作业信息进度实时获取
	public void HandalMessage2(){
	
		log.info("update frame process start");
		
		int jobstatus;
			List<Unit> units=unitService.findUnitsByStatus(UnitStatus.busy);
			String frameLogPath="";
			String filePath="";
			String ProcessInfo="";
			log.info("busy unit size is"+units.size());	
			if(units==null)return;
			for(Unit unit:units){
				
			List<Task> tasks=taskService.findTasksByUnitId(unit.getId());
			if(tasks==null || tasks.size()==0){
				break;
			}
			for(Task task:tasks){
			Job existedJob=task.getJob();
			List<Frame> frames=frameService.findFramesByTaskId(task.getId());
			if(frames==null || frames.size()==0){
				break;
			}
			for(Frame frame:frames){
				filePath=existedJob.getFilePath();
				frameLogPath=filePath+"/"+frame.getFrameName().split("\\.")[0]+".log";
				log.info("update log path is"+frameLogPath);
				
				//ProcessInfo=updateFrameInfoByLog(filePath);
				//这里改成ssh去读取祯的log日志的进度
				String GetProgressCmd="tail -1 "+frameLogPath;
				ShellLocal executor=new ShellLocal(GetProgressCmd);
				String line=executor.executeCmd();
				ProcessInfo=GetRation(line);
				log.info("update frame ratio1 is"+ProcessInfo);
				if(ProcessInfo!=null&&ProcessInfo.length()>0){
					int ratio=(int)Double.parseDouble(ProcessInfo);
					log.info("update frame ratio is"+ratio);
					frame.setEndTime(new Date());
					frame.setFrameProgress(ratio);
					if(ratio==100)
					frame.setFrameStatus(FrameStatus.finished);//3代表完成
					frameDao.updateFrame(frame);
				}
			}
			}
			/*String result=pbsCommand.getJobInfoByXmlFormat(id);
			ArrayList<String> frameNames = new ArrayList<String>();
				job_state
				unit.setUnitNodesInfo(clusterManageService.getNodeString(pbsId));
				*/
				
		}
			}

	
	
	
	
	
	//2015-04定时查询数据库计算所有的job进度
	public void updateTaskAndJobProgress(){
		//HandalMessage();
		
		//需要在这里实时捕获神威再初始化作业distrbute是提交的作业的状态，根据作业状态设置他们的完成值，然后计算
		HandalMessage2();
		
		List<Job> jobs=jobDao.queryDistributedJob();
		for(int j=0; j<jobs.size(); j++){		
			Job job=jobs.get(j);
			if(job.getCameraProgress()!=100){
				List<Task> tasks=taskDao.queryTasksByJobId(job.getId());
				int finishTasksum=0;
				for(int k=0; k<tasks.size(); k++){
					Task task = tasks.get(k);
						if(task.getTaskProgress()!=100){
							/*List<Frame> listFrames=frameDao.queryFramesByTaskId(task.getId());
							if(listFrames!=null&&listFrames.size()>0){
								int sum=0;
								for(int i=0;i<listFrames.size();i++){
									sum+=listFrames.get(i).getFrameProgress();
								}*/
							List<Integer>listFramesProgress =frameDao.queryFramesProgressByTaskId(task.getId());
							if(listFramesProgress!=null&&listFramesProgress.size()>0){
								int sum=0;
								int taskPro=0;
								for(int i=0;i<listFramesProgress.size();i++){
									sum+=listFramesProgress.get(i);
								}
								taskPro=sum/task.getFrameNumber();
								task.setTaskProgress(taskPro);
								//task.setUpdateTime(new Date());//最新的跟新的进度
								taskDao.updateTask(task);
								if(taskPro==100){
									task.setEndTime(new Date());
									task.setTaskStatus(TaskStatus.finished);
									task.setTaskProgress(100);
									Unit unit=task.getUnit();
									try {
										clusterManageService.dismissUnit(unit.getPbsId());
									} catch (Exception e) {
										e.printStackTrace();
									}
									unit.setUnitStatus(UnitStatus.dead);
									unitDao.updateUnit(unit);		
								}
							}
						}	
						finishTasksum+=task.getTaskProgress()*task.getFrameNumber();	
				}
				
				
				//更新job进度
				job.setCameraProgress(finishTasksum/job.getFrameNumbers());
				//log.info("wanglei_setCameraProgress"+finishTasksum/job.getFrameNumbers());
								
				if(finishTasksum/job.getFrameNumbers()==100){	
					Message message=new Message();
					message.setId(UUIDGenerator.getUUID());
					message.setCreateTime(new Date());
					message.setContents("用户"+job.getProject().getUser().getName()+"完成了镜头"+job.getCameraName()+"的渲染");
					message.setTitle("渲染完成情况");
					message.setState(MessageState.UNCHECKED);
					message.setUser(job.getProject().getUser());
					messageDao.addMessage(message);
					job.setEndTime(new Date());
					job.setJobStatus(JobStatus.finished);
					//job渲染完成后计费与扣费
					job.setRenderCost(jobService.JobRenderPrice(job.getId()));
					if(job.getPaymentStatus()==null){
						job.setPaymentStatus(PaymentStatus.notPayment);
					}
					jobDao.updateJob(job);
					if(job.getPaymentStatus()!=PaymentStatus.hasPayment){
						jobService.JobPayment(job.getId());	
					}	
				}			
				jobDao.updateJob(job);
			}
		}
	}
	
	//调用其他渲染引擎跟新进度函数
	/*@Override
	public void UpdateProgressMessage(List<Job> jobs){
		log.info("begin UpdateProgressMessage()");
		Job job;
		Task task;
		Frame frame;
		List<Task> tasks;
		List<Frame> frames;
		int finishFrames;//完成的帧数
		int allFrames;//所有的帧数
		int progress;
		//统计目前已经完成的任务量
		String filepath;	    
		for(int i=0;i<jobs.size();i++){
			job=jobs.get(i);
			if(job.getRenderEngine().getName()!="rUnit"){
				log.info("begin UpdateProgress");
				filepath=job.getFilePath();
				String cmd = "ls -lR "+filepath+"/Pictures"	+"| grep \"^-\" | wc -l";
				LinuxInvoker ink = new LinuxInvoker();
				ink.setCmd(cmd);
				try {
					ink.executeCommand();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String stdout = ink.getStdOut().trim();
			    finishFrames = Integer.parseInt(stdout);
		   
				allFrames=job.getFrameNumbers();
				progress = (int)(((double)finishFrames/(double)allFrames)*100);
				job.setCameraProgress(progress);//更新job进度		
				jobDao.updateJob(job);
				//job渲染完成
				if(progress==100){
					Message message=new Message();
					message.setId(UUIDGenerator.getUUID());
					message.setCreateTime(new Date());
					message.setContents("用户"+job.getProject().getUser().getName()+"完成了镜头"+job.getCameraName()+"的渲染");
					message.setTitle("渲染完成情况");
					message.setState(MessageState.UNCHECKED);
					message.setUser(job.getProject().getUser());
					
					messageDao.addMessage(message);
					job.setEndTime(new Date());
					job.setJobStatus(JobStatus.finished);
					jobDao.updateJob(job);	
					
					tasks=job.getTasks();
					for(int j=0; j<tasks.size(); j++){
						task=tasks.get(j);
						task.setEndTime(new Date());
						task.setTaskStatus(TaskStatus.finished);
						task.setTaskProgress(100);
						Unit unit=task.getUnit();

						try {
							clusterManageService.dismissUnit(unit.getPbsId());
						} catch (Exception e) {
							e.printStackTrace();
						}

						unit.setUnitStatus(UnitStatus.dead);
						unitDao.updateUnit(unit);
						taskDao.updateTask(task);
						
						frames=task.getFrames();
						for(int k=0; k<frames.size(); k++){
							frame=frames.get(k);
							frame.setFrameStatus(FrameStatus.finished);
							frame.setEndTime(new Date());
							frame.setFrameProgress(100);
							frameDao.updateFrame(frame);
						}
					}
				}
						
				
				//job渲染完成后计费与扣费
				job.setRenderCost(jobService.JobRenderPrice(job.getId()));
				jobDao.updateJob(job);
				jobService.JobPayment(job.getId());	
				jobDao.updateJob(job);
			}	
		}
	}*/
	

	@Override
	public int findTotalCountByQuery(String searchText,String searchType,int frameStatus) {
		int num = 0;
		if(StringUtils.isBlank(searchText)){
			num = frameDao.queryCount(frameStatus);
		}else if(StringUtils.isBlank(searchType)){
			num = frameDao.queryCount(frameStatus);
		}else{
			if(searchType.equals("name")){
				num = frameDao.queryCountByFrameName(searchText,frameStatus);
			}else if(searchType.equals("accurateJobId")){
				num = frameDao.queryCountByAccurateJobId(searchText,frameStatus);
			}else{
				num = frameDao.queryCount(frameStatus);
			}
		}
		return num;
	}
	
	@Override
	public List<Frame> listFramesByQuery(String searchText,String searchType, int pageNum, int pageSize,int frameStatus) {
		List<Frame> frames = new ArrayList<Frame>();
		if(StringUtils.isBlank(searchText)){
			frames = frameDao.pagnate(pageNum, pageSize, frameStatus);
		}else if(StringUtils.isBlank(searchType)){
			frames = frameDao.pagnate(pageNum, pageSize, frameStatus);
		}else{
			if(searchType.equals("name")){
				frames = frameDao.pagnateByFrameName(searchText, pageNum, pageSize, frameStatus);
			}else if(searchType.equals("accurateJobId")){
				frames = frameDao.pagnateByAccurateJobId(searchText, pageNum, pageSize, frameStatus);
			}else{
				frames = frameDao.pagnate(pageNum, pageSize, frameStatus);
			}
		}
		
		return frames;
	}
}
