package com.xj.framework.ssh;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Shell {
    //远程主机的ip地址
    private String ip;
    //远程主机登录用户名
    private String username;
    //远程主机的登录密码
    private String password;
    //设置ssh连接的远程端口
    public static final int DEFAULT_SSH_PORT = 22;  
    //保存输出内容的容器
    private String stdout;

    /**
     * 初始化登录信息
     * @param ip
     * @param username
     * @param password
     */
    public Shell(final String ip, final String username, final String password) {
         this.ip = ip;
         this.username = username;
         this.password = password;
         stdout = "";
    }
    /**
     * 执行shell命令
     * @param command
     * @return
     */
    public int execute(final String command) {
        int returnCode = 0;
        JSch jsch = new JSch();
        MyUserInfo userInfo = new MyUserInfo();
   
        try {
            //创建session并且打开连接，因为创建session之后要主动打开连接
            Session session = jsch.getSession(username, ip, DEFAULT_SSH_PORT);
            session.setPassword(password);
            session.setUserInfo(userInfo);
            session.setConfig( "StrictHostKeyChecking" , "no" ); // 不验证host-key，验证会失败。
           
            session.connect();

            //打开通道，设置通道类型，和执行的命令
            Channel channel = session.openChannel("exec");
            ChannelExec channelExec = (ChannelExec)channel;
            channelExec.setCommand(command);
            channelExec.setInputStream(null);
           
            InputStream input=channelExec.getInputStream();
           

            channelExec.connect();
            System.out.println("The remote command is :" + command);

            //接收远程服务器执行命令的结果
           
            stdout=getStream(input);
            
            input.close();  

            // 得到returnCode
            if (channelExec.isClosed()) {  
                returnCode = channelExec.getExitStatus();  
            }  

            // 关闭通道
            channelExec.disconnect();
            //关闭session
            session.disconnect();

        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnCode;
    }
   
    
    
    
    
    //将远程的文件下载到本地
    public void getFile(String src,String dist) {
    	 JSch jsch = new JSch();
         Session session=null;;
         ChannelSftp channelSftp = null;
		try {
			session = jsch.getSession(username, ip, DEFAULT_SSH_PORT);
	         session.setPassword(password);
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			channelSftp = (ChannelSftp)session.openChannel("sftp");
			channelSftp.connect();
			// channelSftp.setFilenameEncoding("gbk");
			channelSftp.get(src,dist);
			System.out.println("download successful");   
		} catch (Exception e1) {
			e1.printStackTrace();
		}finally{
			if(channelSftp!=null && channelSftp.isConnected()){
				channelSftp.disconnect();
			}
			if(session!=null && session.isConnected()){
				session.disconnect();
			}
		}
 	}
    
    private String getStream(InputStream stream) throws IOException {//将输入流读出返回字符串
		int i=0;
		StringBuffer streamBuffer=new StringBuffer();
		while ((i=stream.read())!=-1) {
			streamBuffer.append((char)i);
		}
		return streamBuffer.toString();
	}

    
    
    
    public String getStdout() {
		return stdout;
	}
	public void setStdout(String stdout) {
		this.stdout = stdout;
	}
	public static void main(final String [] args) {  
        Shell shell = new Shell("41.0.0.188", "swsdu", "123456");
        shell.execute(" ls /home/export/online1/systest/swsdu/xijiao/Users/xjtu/camera1");
        System.out.println("res is"+shell.getStdout());
    }  
}