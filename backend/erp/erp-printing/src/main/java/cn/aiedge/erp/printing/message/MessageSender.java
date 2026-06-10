package cn.aiedge.erp.printing.message;

import java.util.Map;

/**
 * 消息发送抽象接口
 *
 * 预留用于将打印截图发送至微信、邮件等渠道。
 * 当前阶段只定义接口，不实现具体逻辑。
 *
 * 后续扩展：
 * - WechatMessageSender（对接公众号/企业微信 API）
 * - EmailMessageSender（发送邮件附件）
 * - SmsMessageSender（发送短信通知）
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    SendResult send(SendRequest request);

    /**
     * 获取渠道类型
     */
    String getTargetType();

    @lombok.Data
    class SendRequest {
        private Long screenshotId;
        private String targetType;
        private String targetId;
        private String messageType;
        private Map<String, Object> extraParams;
    }

    @lombok.Data
    class SendResult {
        private boolean success;
        private String messageId;
        private String errorMessage;

        public static SendResult ok(String messageId) {
            SendResult r = new SendResult();
            r.setSuccess(true);
            r.setMessageId(messageId);
            return r;
        }

        public static SendResult fail(String error) {
            SendResult r = new SendResult();
            r.setSuccess(false);
            r.setErrorMessage(error);
            return r;
        }
    }
}
