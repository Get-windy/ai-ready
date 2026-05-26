package com.qizhilian.monitoring.notification;

import com.qizhilian.monitoring.entity.AlertHistoryEntity;

/**
 * 告警通知服务接口
 * 支持邮件、钉钉、企业微信等多渠道通知
 */
public interface NotificationService {

    /**
     * 发送告警通知
     *
     * @param alert      告警历史记录
     * @param channels   通知渠道JSON列表 ["email","dingtalk","wecom"]
     * @param receivers  接收人JSON列表
     */
    void sendNotification(AlertHistoryEntity alert, String channels, String receivers);

    /**
     * 发送邮件通知
     *
     * @param alert     告警记录
     * @param receivers 接收人列表
     */
    void sendEmail(AlertHistoryEntity alert, String receivers);

    /**
     * 发送钉钉通知
     *
     * @param alert     告警记录
     * @param receivers 接收人/群列表
     */
    void sendDingTalk(AlertHistoryEntity alert, String receivers);

    /**
     * 发送企业微信通知
     *
     * @param alert     告警记录
     * @param receivers 接收人/群列表
     */
    void sendWeCom(AlertHistoryEntity alert, String receivers);
}
