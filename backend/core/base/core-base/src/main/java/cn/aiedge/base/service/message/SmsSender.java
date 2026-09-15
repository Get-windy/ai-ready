package cn.aiedge.base.service.message;

import cn.aiedge.base.entity.SysMessage;

/**
 * 短信发送通道（消息底座 {@code MessageSendTask} 的 msgType=3 分支）
 *
 * 设计：短信服务商差异只在实现里，业务侧统一走 {@code MessageService.sendSms} →
 * {@code sys_message} 待发送 → 定时任务消费；未配置网关时**明确返回失败**并落
 * {@code failReason}，不静默丢弃（沿用底座已有的重试 3 次 / 标记失败逻辑）。
 */
public interface SmsSender {

    /** 是否已配置可用通道 */
    boolean configured();

    /** 发送一条短信；返回 false 表示失败（由底座记录失败原因并重试） */
    boolean send(SysMessage message);

    /** 通道缺失/失败原因（写入 SysMessage.failReason，便于排障） */
    String failureReason();
}
