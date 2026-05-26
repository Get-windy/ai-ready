package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.PaymentReminder;
import cn.aiedge.finance.entity.Receivable;
import cn.aiedge.finance.mapper.PaymentReminderMapper;
import cn.aiedge.finance.mapper.ReceivableMapper;
import cn.aiedge.finance.service.PaymentReminderService;
import cn.aiedge.finance.service.ReceivableService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReminderServiceImpl extends ServiceImpl<PaymentReminderMapper, PaymentReminder> implements PaymentReminderService {

    private final ReceivableService receivableService;
    private final ReceivableMapper receivableMapper;

    @Override
    public PaymentReminder getByReminderNo(String reminderNo) {
        return lambdaQuery()
                .eq(PaymentReminder::getReminderNo, reminderNo)
                .eq(PaymentReminder::getDeleted, 0)
                .one();
    }

    @Override
    public List<PaymentReminder> listByCustomerId(Long customerId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return baseMapper.selectByCustomerId(tenantId, customerId);
    }

    @Override
    public List<PaymentReminder> listByReceivableId(Long receivableId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return baseMapper.selectByReceivableId(tenantId, receivableId);
    }

    @Override
    public List<PaymentReminder> listPendingReminders() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return baseMapper.selectByStatus(tenantId, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder createReminder(Long receivableId, Integer reminderType, String reminderContent) {
        Receivable receivable = receivableService.getById(receivableId);
        if (receivable == null) {
            throw new RuntimeException("应收账款不存在");
        }

        PaymentReminder reminder = new PaymentReminder();
        reminder.setTenantId(1L);
        reminder.setReminderNo(generateReminderNo());
        reminder.setReceivableId(receivableId);
        reminder.setReceivableNo(receivable.getReceivableNo());
        reminder.setCustomerId(receivable.getCustomerId());
        reminder.setCustomerName(receivable.getCustomerName());
        reminder.setOrderId(receivable.getOrderId());
        reminder.setOrderNo(receivable.getOrderNo());
        reminder.setPendingAmount(receivable.getPendingAmount());
        reminder.setOverdueAmount(receivable.getOverdueAmount());
        reminder.setOverdueDays(receivable.getOverdueDays());
        reminder.setDueDate(receivable.getDueDate());
        reminder.setReminderType(reminderType);
        reminder.setReminderLevel(1);
        reminder.setReminderMethod("phone");
        reminder.setReminderContent(reminderContent);
        reminder.setReminderTime(LocalDateTime.now());
        reminder.setStatus(0);
        reminder.setReminderCount(0);
        
        save(reminder);
        log.info("创建催款提醒: {} - 客户: {}", reminder.getReminderNo(), reminder.getCustomerName());
        return getById(reminder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder sendReminder(Long reminderId) {
        PaymentReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new RuntimeException("催款提醒不存在");
        }

        reminder.setReminderTime(LocalDateTime.now());
        reminder.setReminderCount(reminder.getReminderCount() + 1);
        reminder.setStatus(1);
        
        LocalDateTime nextTime = calculateNextReminderTime(reminder);
        reminder.setNextReminderTime(nextTime);
        
        updateById(reminder);
        log.info("发送催款提醒: {} - 第{}次提醒", reminder.getReminderNo(), reminder.getReminderCount());
        return getById(reminderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder recordResponse(Long reminderId, String responseContent, Long responseBy) {
        PaymentReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new RuntimeException("催款提醒不存在");
        }

        reminder.setResponseTime(LocalDateTime.now());
        reminder.setResponseContent(responseContent);
        reminder.setResponseBy(responseBy);
        reminder.setStatus(2);
        
        updateById(reminder);
        log.info("记录催款响应: {} - 客户响应: {}", reminder.getReminderNo(), responseContent);
        return getById(reminderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder scheduleNextReminder(Long reminderId, LocalDateTime nextTime) {
        PaymentReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new RuntimeException("催款提醒不存在");
        }

        reminder.setNextReminderTime(nextTime);
        reminder.setStatus(0);
        
        updateById(reminder);
        return getById(reminderId);
    }

    @Override
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processScheduledReminders() {
        Long tenantId = 1L;
        List<PaymentReminder> pendingReminders = baseMapper.selectPendingReminder(tenantId, LocalDateTime.now());
        
        for (PaymentReminder reminder : pendingReminders) {
            try {
                sendReminder(reminder.getId());
                log.info("自动发送催款提醒: {}", reminder.getReminderNo());
            } catch (Exception e) {
                log.error("发送催款提醒失败: {}", reminder.getReminderNo(), e);
            }
        }
        
        checkAndEscalateOverdueReminders();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder escalateReminder(Long reminderId) {
        PaymentReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new RuntimeException("催款提醒不存在");
        }

        int newLevel = reminder.getReminderLevel() + 1;
        if (newLevel > 4) {
            newLevel = 4;
        }
        
        reminder.setReminderLevel(newLevel);
        reminder.setReminderMethod(getReminderMethodByLevel(newLevel));
        reminder.setReminderContent(getEscalatedContent(reminder));
        
        updateById(reminder);
        log.info("升级催款提醒级别: {} - 新级别: {}", reminder.getReminderNo(), newLevel);
        return getById(reminderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentReminder closeReminder(Long reminderId, String closeReason) {
        PaymentReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new RuntimeException("催款提醒不存在");
        }

        reminder.setStatus(3);
        reminder.setRemark(closeReason);
        
        updateById(reminder);
        log.info("关闭催款提醒: {} - 原因: {}", reminder.getReminderNo(), closeReason);
        return getById(reminderId);
    }

    @Override
    public Map<String, Object> getReminderStatistics() {
        Long tenantId = 1L;
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("pendingCount", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getStatus, 0)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("sentCount", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getStatus, 1)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("respondedCount", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getStatus, 2)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("closedCount", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getStatus, 3)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("totalOverdueAmount", calculateTotalOverdueAmount());
        
        stats.put("level1Count", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getReminderLevel, 1)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("level2Count", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getReminderLevel, 2)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("level3Count", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getReminderLevel, 3)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        stats.put("level4Count", lambdaQuery()
                .eq(PaymentReminder::getTenantId, tenantId)
                .eq(PaymentReminder::getReminderLevel, 4)
                .eq(PaymentReminder::getDeleted, 0)
                .count());
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getOverdueCustomerSummary() {
        Long tenantId = 1L;
        List<Receivable> overdueList = receivableMapper.selectOverdue();
        
        Map<Long, Map<String, Object>> customerSummary = new HashMap<>();
        
        for (Receivable receivable : overdueList) {
            Long customerId = receivable.getCustomerId();
            
            if (!customerSummary.containsKey(customerId)) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("customerId", customerId);
                summary.put("customerName", receivable.getCustomerName());
                summary.put("totalOverdueAmount", BigDecimal.ZERO);
                summary.put("overdueCount", 0);
                summary.put("maxOverdueDays", 0);
                summary.put("reminderCount", 0);
                customerSummary.put(customerId, summary);
            }
            
            Map<String, Object> summary = customerSummary.get(customerId);
            BigDecimal totalAmount = (BigDecimal) summary.get("totalOverdueAmount");
            summary.put("totalOverdueAmount", totalAmount.add(receivable.getOverdueAmount()));
            summary.put("overdueCount", (Integer) summary.get("overdueCount") + 1);
            
            int maxDays = (Integer) summary.get("maxOverdueDays");
            if (receivable.getOverdueDays() > maxDays) {
                summary.put("maxOverdueDays", receivable.getOverdueDays());
            }
            
            Integer reminderCount = baseMapper.countPendingByCustomer(tenantId, customerId);
            summary.put("reminderCount", reminderCount);
        }
        
        List<Map<String, Object>> result = new ArrayList<>(customerSummary.values());
        result.sort((a, b) -> ((BigDecimal) b.get("totalOverdueAmount")).compareTo((BigDecimal) a.get("totalOverdueAmount")));
        
        return result;
    }

    @Override
    public Integer getReminderCountByCustomer(Long customerId) {
        Long tenantId = 1L;
        return baseMapper.countPendingByCustomer(tenantId, customerId);
    }

    @Override
    public BigDecimal calculateTotalOverdueAmount() {
        Long tenantId = 1L;
        BigDecimal amount = baseMapper.sumOverdueAmountByStatus(tenantId, 0);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private void checkAndEscalateOverdueReminders() {
        Long tenantId = 1L;
        LambdaQueryWrapper<PaymentReminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentReminder::getTenantId, tenantId)
               .eq(PaymentReminder::getDeleted, 0)
               .in(PaymentReminder::getStatus, 0, 1)
               .ge(PaymentReminder::getOverdueDays, 30);
        
        List<PaymentReminder> overdueReminders = list(wrapper);
        
        for (PaymentReminder reminder : overdueReminders) {
            int overdueDays = reminder.getOverdueDays();
            int currentLevel = reminder.getReminderLevel();
            
            int newLevel = currentLevel;
            if (overdueDays >= 90) {
                newLevel = 4;
            } else if (overdueDays >= 60) {
                newLevel = 3;
            } else if (overdueDays >= 30) {
                newLevel = 2;
            }
            
            if (newLevel > currentLevel) {
                escalateReminder(reminder.getId());
            }
        }
    }

    private String generateReminderNo() {
        String prefix = "PR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PaymentReminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PaymentReminder::getReminderNo, prefix + dateStr)
               .eq(PaymentReminder::getDeleted, 0)
               .orderByDesc(PaymentReminder::getReminderNo)
               .last("LIMIT 1");
        PaymentReminder lastReminder = getOne(wrapper);
        int seq = 1;
        if (lastReminder != null) {
            String lastNo = lastReminder.getReminderNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    private LocalDateTime calculateNextReminderTime(PaymentReminder reminder) {
        int level = reminder.getReminderLevel();
        int daysToAdd;
        
        switch (level) {
            case 1: daysToAdd = 7; break;
            case 2: daysToAdd = 5; break;
            case 3: daysToAdd = 3; break;
            case 4: daysToAdd = 1; break;
            default: daysToAdd = 7;
        }
        
        return LocalDateTime.now().plusDays(daysToAdd);
    }

    private String getReminderMethodByLevel(int level) {
        switch (level) {
            case 1: return "phone";
            case 2: return "phone_email";
            case 3: return "visit";
            case 4: return "legal";
            default: return "phone";
        }
    }

    private String getEscalatedContent(PaymentReminder reminder) {
        int level = reminder.getReminderLevel();
        String customerName = reminder.getCustomerName();
        BigDecimal amount = reminder.getOverdueAmount();
        int days = reminder.getOverdueDays();
        
        switch (level) {
            case 2:
                return String.format("尊敬的%s，贵司应收账款%s元已逾期%d天，请尽快安排付款，否则将影响双方合作关系。", customerName, amount, days);
            case 3:
                return String.format("尊敬的%s，贵司应收账款%s元已严重逾期%d天，我司将安排专人上门催收，请配合处理。", customerName, amount, days);
            case 4:
                return String.format("尊敬的%s，贵司应收账款%s元已逾期%d天，我司将启动法律程序追讨欠款，请尽快联系我司协商解决。", customerName, amount, days);
            default:
                return reminder.getReminderContent();
        }
    }
}