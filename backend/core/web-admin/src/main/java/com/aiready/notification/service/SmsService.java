package com.aiready.notification.service;

import com.aiready.notification.entity.SmsRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 短信服务接口
 */
public interface SmsService extends IService<SmsRecord> {

    /**
     * 发送短信
     */
    void sendSms(String phone, String templateCode, Map<String, String> templateParams);

    /**
     * 批量发送短信
     */
    void sendBatchSms(List<String> phones, String templateCode, Map<String, String> templateParams);

    /**
     * 发送验证码短信
     */
    void sendVerifyCode(String phone, String code);

    /**
     * 重试发送失败的短信
     */
    void retryFailedSms();

    /**
     * 获取短信发送记录
     */
    List<SmsRecord> getSmsRecords(Long notificationId);

    /**
     * 验证短信验证码
     */
    boolean verifyCode(String phone, String code);
}
