package cn.aiedge.erp.printing.message.impl;

import cn.aiedge.erp.printing.message.MessageSender;
import org.springframework.stereotype.Component;

/**
 * 占位实现 —— 接口已定义，功能即将开放
 */
@Component
public class PlaceholderMessageSender implements MessageSender {

    @Override
    public SendResult send(SendRequest request) {
        return SendResult.fail("消息发送功能即将开放，敬请期待。targetType="
                + request.getTargetType() + ", targetId=" + request.getTargetId());
    }

    @Override
    public String getTargetType() {
        return "placeholder";
    }
}
