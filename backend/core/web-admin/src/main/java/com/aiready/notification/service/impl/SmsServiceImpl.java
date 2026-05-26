package com.aiready.notification.service.impl;

import com.aiready.notification.entity.SmsRecord;
import com.aiready.notification.mapper.SmsRecordMapper;
import com.aiready.notification.service.SmsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl extends ServiceImpl<SmsRecordMapper, SmsRecord> 
        implements SmsService {

    private final SmsRecordMapper smsRecordMapper;
    
    // 验证码缓存（实际项目中应该使用Redis）
    private static final Map<String, String> VERIFY_CODE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> VERIFY_CODE_EXPIRE = new ConcurrentHashMap<>();
    
    @Value("${sms.access-key:}")
    private String accessKey;
    
    @Value("${sms.access-secret:}")
    private String accessSecret;
    
    @Value("${sms.sign-name:}")
    private String defaultSignName;

    @Override
    public void sendSms(String phone, String templateCode, Map<String, String> templateParams) {
        SmsRecord record = new SmsRecord();
        record.setPhone(phone);
        record.setTemplateCode(templateCode);
        record.setSignName(defaultSignName);
        record.setStatus(1);
        save(record);
        
        try {
            // 这里集成实际的短信平台SDK（如阿里云、腾讯云等）
            // 示例代码：
            // DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKey, accessSecret);
            // IAcsClient client = new DefaultAcsClient(profile);
            // ...
            
            log.info("发送短信到: {}, 模板: {}", phone, templateCode);
            
            // 模拟发送成功
            record.setStatus(2);
            record.setSendTime(LocalDateTime.now());
            record.setPlatformMsgId("MSG" + System.currentTimeMillis());
            updateById(record);
            
        } catch (Exception e) {
            log.error("短信发送失败: {}", e.getMessage());
            record.setStatus(3);
            record.setFailReason(e.getMessage());
            updateById(record);
        }
    }

    @Override
    public void sendBatchSms(List<String> phones, String templateCode, Map<String, String> templateParams) {
        for (String phone : phones) {
            sendSms(phone, templateCode, templateParams);
        }
    }

    @Override
    public void sendVerifyCode(String phone, String code) {
        // 保存验证码到缓存
        VERIFY_CODE_CACHE.put(phone, code);
        VERIFY_CODE_EXPIRE.put(phone, LocalDateTime.now().plusMinutes(5));
        
        // 发送验证码短信
        Map<String, String> params = Map.of("code", code);
        sendSms(phone, "SMS_VERIFY_CODE", params);
        
        log.info("发送验证码到: {}, 验证码: {}", phone, code);
    }

    @Override
    public void retryFailedSms() {
        LambdaQueryWrapper<SmsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsRecord::getStatus, 3)
               .lt(SmsRecord::getRetryCount, 3)
               .eq(SmsRecord::getDeleted, 0);
        
        List<SmsRecord> failedRecords = list(wrapper);
        
        for (SmsRecord record : failedRecords) {
            try {
                record.setRetryCount(record.getRetryCount() + 1);
                record.setStatus(1);
                updateById(record);
                
                // 重试发送
                log.info("重试发送短信到: {}", record.getPhone());
                
                record.setStatus(2);
                record.setSendTime(LocalDateTime.now());
                updateById(record);
            } catch (Exception e) {
                log.error("重试发送短信失败: {}", e.getMessage());
                record.setFailReason(e.getMessage());
                updateById(record);
            }
        }
    }

    @Override
    public List<SmsRecord> getSmsRecords(Long notificationId) {
        LambdaQueryWrapper<SmsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsRecord::getNotificationId, notificationId)
               .eq(SmsRecord::getDeleted, 0)
               .orderByDesc(SmsRecord::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String cachedCode = VERIFY_CODE_CACHE.get(phone);
        LocalDateTime expireTime = VERIFY_CODE_EXPIRE.get(phone);
        
        if (cachedCode == null || expireTime == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(expireTime)) {
            VERIFY_CODE_CACHE.remove(phone);
            VERIFY_CODE_EXPIRE.remove(phone);
            return false;
        }
        
        boolean valid = cachedCode.equals(code);
        if (valid) {
            VERIFY_CODE_CACHE.remove(phone);
            VERIFY_CODE_EXPIRE.remove(phone);
        }
        
        return valid;
    }
    
    /**
     * 生成随机验证码
     */
    public String generateVerifyCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
