package org.xjtu.framework.backstage.distribute;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xjtu.framework.backstage.initialize.MayaJobInitialize;
import org.xjtu.framework.backstage.initialize.RndrJobInitialize;
import org.xjtu.framework.core.base.constant.FrameStatus;
import org.xjtu.framework.core.base.constant.JobStatus;
import org.xjtu.framework.core.base.constant.TaskStatus;
import org.xjtu.framework.core.base.model.Calculate;
import org.xjtu.framework.core.base.model.Frame;
import org.xjtu.framework.core.base.model.Job;
import org.xjtu.framework.core.base.model.Task;
import org.xjtu.framework.core.util.PbsExecute;
import org.xjtu.framework.core.util.StringUtil;
import org.xjtu.framework.core.util.UUIDGenerator;
import org.xjtu.framework.modules.user.service.ClusterManageService;
import org.xjtu.framework.modules.user.service.ConfigurationService;
import org.xjtu.framework.modules.user.service.FrameService;
import org.xjtu.framework.modules.user.service.JobService;
import org.xjtu.framework.modules.user.service.TaskService;

public class CalculateDistribute {
	
	private static Log log = LogFactory.getLog(CalculateDistribute.class);
	
	private @Resource JobService jobService;
	private @Resource TaskService taskService;
	private @Resource FrameService frameService;
	private @Resource ClusterManageService clusterManageService;
	private @Resource ConfigurationService configurationService;
	
	private int nodesNumPerUnit;

	public boolean distributeCalculate(Calculate calculate) throws Exception {
		
		/*nodesNumPerUnit = configurationService.findAllConfiguration().getNodesNumPerUnit();
		List<Frame> fs=frameService.findNotStartFramesByJobId(job.getId());
		log.info("get frames is "+fs); 
		String renderEngineName="";//job.getRenderEngine().getName();
	    //different renderEngine
		if(renderEngineName.equals("rUnit")||renderEngineName.equals("rUnit-hustdm")){//第一种渲染引擎
			int freeNodesNum=clusterManageService.getFreeNodesNum();
			int unitsNumber = job.getUnitsNumber();		
			if(freeNodesNum<unitsNumber*nodesNumPerUnit){
				log.info("there is no enough unit avaliable"); 
				return false;
			}
			//当此作业为暂停后继续的作业
			if(fs!=null&&fs.size()!=0){
				log.info("暂停的祯继续运行");
				return false;
				//return jobService.continueRunitJobtoTask(job, unitsNumber, nodesNumPerUnit, fs,renderEngineName);
			}else{
				log.info("begin distributeJob first"); 
				return jobService.distributeRunitJob(job, unitsNumber, nodesNumPerUnit,renderEngineName);
			}
		}
		else if(job.getRenderEngine().getName().equals("rndr")){
			log.info("renderman is starting");
			return disRndrJobtoTask(job);
		}
		else if(job.getRenderEngine().getName().equals("maya")){
			return disMayaJobtoTask(job);
		}
		else{
			return false;
		}*/
		
		return true;

	}
	
	public boolean disRndrJobtoTask(Job job) throws Exception{

		log.info("Begin to distribute job to task...");
		log.info("first,get the jobinfo");

		
		/*初始化变量*/
		int nodesNumber = job.getNodesNumber();
		String filePath = job.getFilePath();
		int preRenderTag = job.getPreRenderingTag();
		int frameNums = job.getFrameNumbers();
		String frameRange = job.getFrameRange();		
		
		/*查看节点是否够用*/		
		int freeNodesNum=clusterManageService.getFreeNodesNum();
		log.info("my test"+ nodesNumber+" freeNodesNum:" +freeNodesNum);
		if(freeNodesNum<nodesNumber){
			log.info("there is no enough nodes avaliable"); 
			return false;
		}
		
		/*修改成命令方式*/
		
		filePath=filePath.substring(0,filePath.lastIndexOf("/"));
		
		
		String cmd="cd "+filePath+" && ls *.xml";
		PbsExecute pbs=new PbsExecute(cmd);
		String result=pbs.executeCmd();
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
		
		
	   log.info("get frameScenceMap is"+mapScenePath);
		
		/*将要渲染的帧号放入数组中*/
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
		
		
		log.info("start distributing...");
		
		// 需要将unitsNumber分成两类，其中一类要多渲染1帧的画面。
		int between = frameNums / nodesNumber; // 定义每个节点需要承担的任务量
		int remain = frameNums % nodesNumber; // 计算出余数,根据数学知识，余数的个数为需要多渲染一帧的节点数量
				
		int s=0;
		int e=frameNums;
		
		for (int i = 0; i < nodesNumber; i++) {

			int temp;
			// 进行作业的划分执行编写
			if (i < nodesNumber - remain) {
				// 承担任务量为between的节点个数为nu-remain
				temp = s + between - 1;// 定义一个临时终点变量
			} else {
				temp = s + between;// 定义一个临时终点变量
			}
			log.info("temp begin is "+temp);

			ArrayList<Frame> frames = new ArrayList<Frame>();
			
			for (int j = s; j < temp + 1; j++) {
				Frame frame = new Frame();
				frame.setId(UUIDGenerator.getUUID());
				frame.setFrameName(mapScenePath.get(frameToRender[j]));
				frame.setFrameStatus(FrameStatus.unfinished);
				frame.setStartTime(new Date());
				frame.setFrameProgress(0);
				frames.add(frame);
			}
			//RndrJobInitialize ri=new RndrJobInitialize(frames,job.getFilePath());//修改后
			log.info("renderman frame  length is "+frames.size());
			RndrJobInitialize ri=new RndrJobInitialize(frames,filePath);
			ri.dobegin();
			// 记录任务信息
			Task task=new Task();
			task.setId(UUIDGenerator.getUUID());
			task.setFrameNumber(temp-s+1);
			task.setTaskProgress(0);
			task.setTaskStatus(TaskStatus.started);
			task.setStartTime(new Date());
			task.setJob(job);
			task.setUnit(ri.getUnit());
			taskService.addTask(task);
			
			
			
			for(int j=0;j<frames.size();j++){
				frames.get(j).setTask(task);
				frameService.addFrame(frames.get(j));
			}
									
			// 进行变量的更新
			s = temp + 1; // 每次节点分发完以后，进行起始节点的更新；
			log.info("temp is "+temp);
			
		}
		
		
		
		
		
		log.info("job is ditributed over...");
		
		
		
		
		
		
		/*更新作业状态信息*/
		job.setStartTime(new Date());
		job.setJobStatus(JobStatus.distributed);
		jobService.updateJobInfo(job);
		
		return true;

	}
	public boolean disMayaJobtoTask(Job job) throws Exception{

		log.info("Begin to distribute job to task...");
		log.info("first,get the jobinfo");

		
		/*初始化变量*/
		int nodesNumber = job.getNodesNumber();
		String filePath = job.getFilePath();
		int preRenderTag = job.getPreRenderingTag();
		int frameNums = job.getFrameNumbers();
		String frameRange = job.getFrameRange();
		
		/*修改成命令方式*/
		String cmd="cd "+filePath+" && ls *.mb";
		PbsExecute pbs=new PbsExecute(cmd);
		String result=pbs.executeCmd();
		result=result.trim();
		
		String[] filenames=result.split("\\n");
		
		String mayafile = null;
		if(filenames!=null&&filenames.length>0){
			mayafile=filenames[0];
		}
					
		/*查看节点是否够用*/		
		int freeNodesNum=clusterManageService.getFreeNodesNum();
		log.info("my test"+ nodesNumber+" freeNodesNum:" +freeNodesNum);
		if(freeNodesNum<nodesNumber){
			log.info("there is no enough nodes avaliable"); 
			return false;
		}		
		
		/*将要渲染的帧号放入数组中*/
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
		
		log.info("start distributing...");
		
		// 需要将unitsNumber分成两类，其中一类要多渲染1帧的画面。
		int between = frameNums / nodesNumber; // 定义每个节点需要承担的任务量
		int remain = frameNums % nodesNumber; // 计算出余数,根据数学知识，余数的个数为需要多渲染一帧的节点数量
				
		int s=0;
		int e=frameNums;
		for (int i = 0; i < nodesNumber; i++) {

			int temp;

			// 进行作业的划分执行编写
			if (i < nodesNumber - remain) {
				// 承担任务量为between的节点个数为nu-remain
				temp = s + between - 1;// 定义一个临时终点变量
			} else {
				temp = s + between;// 定义一个临时终点变量
			}

			ArrayList<Frame> frames = new ArrayList<Frame>();
			
			for (int j = s; j < temp + 1; j++) {

				Frame frame = new Frame();
				frame.setId(UUIDGenerator.getUUID());
				
				frame.setFrameName(""+frameToRender[j]);
				
				frame.setFrameStatus(FrameStatus.unfinished);
				frame.setStartTime(new Date());
				frame.setFrameProgress(0);
				
				frames.add(frame);

			}
			
			MayaJobInitialize ma=new MayaJobInitialize(frames,job.getFilePath(),mayafile);
			ma.dobegin();
			
			// 记录任务信息
			Task task=new Task();
			task.setId(UUIDGenerator.getUUID());
			task.setFrameNumber(temp-s+1);
			task.setTaskProgress(0);
			task.setTaskStatus(TaskStatus.started);
			task.setStartTime(new Date());
			task.setJob(job);
			task.setUnit(ma.getUnit());
			
			taskService.addTask(task);
			
			for(int j=0;j<frames.size();j++){
				frames.get(j).setTask(task);
				frameService.addFrame(frames.get(j));
			}
									
			// 进行变量的更新
			s = temp + 1; // 每次节点分发完以后，进行起始节点的更新；
		}
		log.info("job is ditributed over...");
		
		/*更新作业状态信息*/
		job.setStartTime(new Date());
		job.setJobStatus(JobStatus.distributed);
		jobService.updateJobInfo(job);
		
		return true;

	}

}
