package cn.aiedge.notification.channel;

import cn.aiedge.notification.entity.NotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信渠道
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class SiteMessageChannel implements NotificationChannel {

    @Override
    public SendResult send(NotificationRecord record) {
        try {
            // 站内信直接存储到数据库，不需要外部服务
            log.info("发送站内信: receiver={}, title={}", record.getReceiverId(), record.getTitle());
            
            // 这里只是日志记录，实际存储由NotificationService完成
            return SendResult.success("MSG_" + record.getId());
            
        } catch (Exception e) {
            log.error("站内信发送失败: recordId={}", record.getId(), e);
            return SendResult.failure("发送失败: " + e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return NotificationTemplate.TYPE_SITE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
