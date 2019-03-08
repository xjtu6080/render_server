package org.xjtu.framework.core.util;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public final class MailUtil {
	// 邮件发送者地址
    private static final String SenderEmailAddr = "mcgrady900821@163.com";
    // 邮件发送者邮箱用户
    private static final String SMTPUserName = "mcgrady900821";
    
    // 邮件发送者邮箱密码
    private static final String SMTPPassword = "HUANGhuang0821";
    // 邮件发送者邮箱SMTP服务器
    private static final String SMTPServerName = "smtp.163.com";
    // 传输类型
    private static final String TransportType = "smtp";
    // 属性
    private static Properties props;
    /**
     * 私有构造函数，防止外界新建本实用类的实例，因为直接使用MailUtil.sendMail发送邮件即可
     *
     */
    private MailUtil() {
    }
    /**
     * 静态构造器
     */
    static {
        MailUtil.props = new Properties();
        // 存储发送邮件服务器的信息
        MailUtil.props.put("mail.smtp.host", MailUtil.SMTPServerName);
        // 同时通过验证
        MailUtil.props.put("mail.smtp.auth", "true");
    }
    /**
     * 发送邮件
     * @param emailAddr:收信人邮件地址
     * @param mailTitle:邮件标题
     * @param mailConcept:邮件内容
     */
    public static boolean sendMail(String emailAddr, String mailTitle,
            String mailConcept,String contentType,String attachmentPath,String attachmentName) {
        // 根据属性新建一个邮件会话，null参数是一种Authenticator(验证程序) 对象
        Session s = Session.getInstance(MailUtil.props, null);
        // 设置调试标志,要查看经过邮件服务器邮件命令，可以用该方法
        s.setDebug(false);
        
        // 由邮件会话新建一个消息对象
        Message message = new MimeMessage(s);
        try {
            // 设置发件人
            Address from = new InternetAddress(MailUtil.SenderEmailAddr);
            message.setFrom(from);
            // 设置收件人
            Address to = new InternetAddress(emailAddr);
            message.setRecipient(Message.RecipientType.TO, to);
            // 设置主题
            message.setSubject(mailTitle);
            // 设置信件内容
            message.setText(mailConcept);
            if(contentType!=null){
            	message.setContent(mailConcept, contentType);
            }
            // 设置发信时间
            message.setSentDate(new Date());
            //设置附件
            if(attachmentPath!=null&&attachmentName!=null){
                DataSource source = new FileDataSource(attachmentPath);
                message.setDataHandler(new DataHandler(source));
                sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
                message.setFileName("=?GBK?B?"+enc.encode(attachmentName.getBytes())+"?=");
            }
            // 存储邮件信息
            message.saveChanges();
            
            Transport transport = s.getTransport(MailUtil.TransportType);
            // 要填入你的用户名和密码；
            transport.connect(MailUtil.SMTPServerName, MailUtil.SMTPUserName,
                    MailUtil.SMTPPassword);
            // 发送邮件,其中第二个参数是所有已设好的收件人地址
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            System.out.println("发送邮件,邮件地址:" + emailAddr + " 标题:" + mailTitle
                    + " 内容:" + mailConcept + "成功!");
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("发送邮件,邮件地址:" + emailAddr + " 标题:" + mailTitle
                    + " 内容:" + mailConcept + "失败! 原因是" + e.getMessage());
            return false;
        }
    }
}
