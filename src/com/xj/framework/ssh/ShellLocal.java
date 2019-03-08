package com.xj.framework.ssh;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ShellLocal {//////TODO使用此类执行pbs脚本或者命令，从output里面解析出来字符串插入数据库相关表的字段就行了
	
	private String cmd;
	private String outPut;
	private String errorOutPut;
	private Process subProcess;
	private int exitValue;
	private static final Log log = LogFactory.getLog(ShellLocal.class);	
	public ShellLocal() {
		setCmd(null);
		setOutPut(null);
		setErrorOutPut(null);
		setSubProcess(null);
		setExitValue(-1);
	}
	public ShellLocal(String cmdString) {
		setCmd(cmdString);
		setOutPut(null);
		setErrorOutPut(null);
		setSubProcess(null);
		setExitValue(-1);
	}
				
	private String getStream(InputStream stream) throws IOException {//将输入流读出返回字符串
		int i=0;
		StringBuffer streamBuffer=new StringBuffer();
		while ((i=stream.read())!=-1) {
			streamBuffer.append((char)i);
		}
		return streamBuffer.toString();
	}
	
	
	public String executeCmd()//执行linux命令(包括pbs命令和linux系统命令)
	{
		
		try{
			if(cmd==null||cmd.length()==0)
				throw new Exception("throw  Command Failed: the command hasn't been specified.");
			
			   
			String[] exc={"/bin/sh","-c",cmd};
			subProcess=Runtime.getRuntime().exec(exc);
			outPut=getStream(subProcess.getInputStream());
			errorOutPut=getStream(subProcess.getErrorStream());
			exitValue=subProcess.waitFor();
			if (exitValue==0) {
				return outPut;
			}
			else {
				return "";
			}
			
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	
	
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	public String getCmd() {
		return cmd;
	}
	public void setOutPut(String outPut) {
		this.outPut = outPut;
	}
	public String getOutPut() {
		return outPut;
	}
	public void setSubProcess(Process subProcess) {
		this.subProcess = subProcess;
	}
	public Process getSubProcess() {
		return subProcess;
	}
	public void setExitValue(int exitValue) {
		this.exitValue = exitValue;
	}
	public int getExitValue() {
		return exitValue;
	}
	public void setErrorOutPut(String errorOutPut) {
		this.errorOutPut = errorOutPut;
	}
	public String getErrorOutPut() {
		return errorOutPut;
	}
}