package cn.aiedge.base.service.message;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.mapper.SysMessageMapper;
import cn.aiedge.base.service.message.impl.EmailSenderImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息发送任务
 * 定时扫描待发送消息并执行发送
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendTask {

    private final SysMessageMapper messageMapper;
    private final EmailSender emailSender;

    /**
     * 每30秒扫描一次待发送消息
     */
    @Scheduled(fixedRate = 30000)
    public void scanAndSendMessages() {
        log.debug("开始扫描待发送消息...");
        
        List<SysMessage> pendingMessages = messageMapper.selectPendingMessages(100);
        
        for (SysMessage message : pendingMessages) {
            try {
                sendMessage(message);
            } catch (Exception e) {
                log.error("发送消息失败: messageId={}", message.getId(), e);
                handleSendFailure(message, e.getMessage());
            }
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(SysMessage message) {
        // 更新为发送中状态
        message.setSendStatus(1);
        messageMapper.updateById(message);

        boolean success = false;
        
        switch (message.getMsgType()) {
            case 1: // 邮件
                success = emailSender.send(message);
                break;
            case 2: // 站内信
                success = true; // 站内信直接成功
                break;
            default:
                log.warn("不支持的消息类型: msgType={}", message.getMsgType());
        }

        if (success) {
            message.setSendStatus(2); // 发送成功
            message.setSendTime(LocalDateTime.now());
        } else {
            handleSendFailure(message, "发送失败");
        }
        
        messageMapper.updateById(message);
    }

    /**
     * 处理发送失败
     */
    private void handleSendFailure(SysMessage message, String reason) {
        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
        message.setRetryCount(retryCount + 1);
        
        // 重试超过3次标记为失败
        if (message.getRetryCount() >= 3) {
            message.setSendStatus(3); // 发送失败
            message.setFailReason(reason);
            log.error("消息发送失败，超过重试次数: messageId={}", message.getId());
        } else {
            message.setSendStatus(0); // 继续保持待发送状态
            log.warn("消息发送失败，等待重试: messageId={}, retryCount={}", 
                message.getId(), message.getRetryCount());
        }
    }
}
