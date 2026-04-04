package cn.aiedge.notification.channel;

import cn.aiedge.notification.entity.NotificationRecord;

/**
 * 通知渠道接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface NotificationChannel {

    /**
     * 发送通知
     *
     * @param record 通知记录
     * @return 发送结果
     */
    SendResult send(NotificationRecord record);

    /**
     * 获取渠道类型
     *
     * @return 渠道类型
     */
    String getChannelType();

    /**
     * 检查渠道是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 发送结果
     */
    record SendResult(boolean success, String message, String externalId) {
        public static SendResult success() {
            return new SendResult(true, "发送成功", null);
        }

        public static SendResult success(String externalId) {
            return new SendResult(true, "发送成功", externalId);
        }

        public static SendResult failure(String message) {
            return new SendResult(false, message, null);
        }
    }
}
