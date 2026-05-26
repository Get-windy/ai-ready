package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.PaymentReminder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaymentReminderService {
    
    PaymentReminder getByReminderNo(String reminderNo);
    
    List<PaymentReminder> listByCustomerId(Long customerId);
    
    List<PaymentReminder> listByReceivableId(Long receivableId);
    
    List<PaymentReminder> listPendingReminders();
    
    PaymentReminder createReminder(Long receivableId, Integer reminderType, String reminderContent);
    
    PaymentReminder sendReminder(Long reminderId);
    
    PaymentReminder recordResponse(Long reminderId, String responseContent, Long responseBy);
    
    PaymentReminder scheduleNextReminder(Long reminderId, LocalDateTime nextTime);
    
    void processScheduledReminders();
    
    PaymentReminder escalateReminder(Long reminderId);
    
    PaymentReminder closeReminder(Long reminderId, String closeReason);
    
    Map<String, Object> getReminderStatistics();
    
    List<Map<String, Object>> getOverdueCustomerSummary();
    
    Integer getReminderCountByCustomer(Long customerId);
    
    BigDecimal calculateTotalOverdueAmount();
}