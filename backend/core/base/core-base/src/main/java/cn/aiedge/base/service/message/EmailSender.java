package cn.aiedge.base.service.message;

import cn.aiedge.base.entity.SysMessage;

/**
 * 邮件发送接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface EmailSender {

    /**
     * 发送邮件
     * 
     * @param message 消息实体
     * @return 是否发送成功
     */
    boolean send(SysMessage message);

    /**
     * 发送HTML邮件
     * 
     * @param to 收件人
     * @param subject 主题
     * @param htmlContent HTML内容
     * @return 是否发送成功
     */
    boolean sendHtml(String to, String subject, String htmlContent);

    /**
     * 发送文本邮件
     * 
     * @param to 收件人
     * @param subject 主题
     * @param textContent 文本内容
     * @return 是否发送成功
     */
    boolean sendText(String to, String subject, String textContent);
}
