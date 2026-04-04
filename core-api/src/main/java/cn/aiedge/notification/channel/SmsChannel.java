package cn.aiedge.notification.channel;

import cn.aiedge.notification.entity.NotificationRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 短信渠道
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsChannel implements NotificationChannel {

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    @Value("${notification.sms.provider:}")
    private String provider;

    // 实际项目中应注入短信服务客户端，如阿里云、腾讯云短信SDK
    // private final SmsClient smsClient;

    @Override
    public SendResult send(NotificationRecord record) {
        if (!isAvailable()) {
            return SendResult.failure("短信服务未启用");
        }

        try {
            String phone = record.getReceiverAddress();
            if (phone == null || phone.isEmpty()) {
                return SendResult.failure("手机号为空");
            }

            // 实际发送逻辑（根据provider选择不同服务商）
            // 这里是模拟实现
            log.info("发送短信: phone={}, content={}", maskPhone(phone), 
                    record.getContent().length() > 20 ? record.getContent().substring(0, 20) + "..." : record.getContent());

            // 模拟发送成功
            // String externalId = smsClient.send(phone, record.getContent());
            String externalId = "SMS_" + System.currentTimeMillis();

            log.info("短信发送成功: phone={}, externalId={}", maskPhone(phone), externalId);
            return SendResult.success(externalId);

        } catch (Exception e) {
            log.error("短信发送失败: recordId={}", record.getId(), e);
            return SendResult.failure("短信发送失败: " + e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return NotificationTemplate.TYPE_SMS;
    }

    @Override
    public boolean isAvailable() {
        return enabled && provider != null && !provider.isEmpty();
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
