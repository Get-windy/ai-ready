package cn.aiedge.dms.settlement.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.event.entity.DmsEventOutbox;
import cn.aiedge.dms.event.mapper.DmsEventOutboxMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 费用结算服务
 *
 * 提供配送费计算、结算报表生成、ERP推送等结算相关功能。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final DmsTaskMapper taskMapper;
    private final DmsEventOutboxMapper eventOutboxMapper;

    /** 基础配送费 */
    private static final BigDecimal BASE_FEE = new BigDecimal("5.00");

    /** 每公里单价（元） */
    private static final BigDecimal PER_KM_RATE = new BigDecimal("2.00");

    /** 时段附加费系数（22:00-06:00） */
    private static final BigDecimal TIME_SURCHARGE_RATE = new BigDecimal("1.50");

    /** 加急附加费 */
    private static final BigDecimal URGENT_SURCHARGE = new BigDecimal("10.00");

    /**
     * 计算配送费
     *
     * 费用 = 基础费 + 每公里单价 * 距离 + 时段附加费 + 加急附加费
     *
     * @param taskId 任务ID
     * @return 配送费详情
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculateDeliveryFee(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }

        // 基础费
        BigDecimal baseFee = BASE_FEE;

        // 里程费用：使用 estimatedDistance，默认取10km
        BigDecimal distance = task.getEstimatedDistance() != null
                ? task.getEstimatedDistance()
                : BigDecimal.TEN;
        BigDecimal mileageFee = PER_KM_RATE.multiply(distance).setScale(2, RoundingMode.HALF_UP);

        // 时段附加费：22:00-06:00期间加收50%
        BigDecimal timeSurcharge = BigDecimal.ZERO;
        if (task.getDispatchTime() != null) {
            int hour = task.getDispatchTime().getHour();
            if (hour >= 22 || hour < 6) {
                timeSurcharge = baseFee.multiply(TIME_SURCHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 加急附加费：priority>=2 视为加急
        BigDecimal urgentSurcharge = BigDecimal.ZERO;
        if (task.getPriority() != null && task.getPriority() >= 2) {
            urgentSurcharge = URGENT_SURCHARGE;
        }

        BigDecimal total = baseFee.add(mileageFee).add(timeSurcharge).add(urgentSurcharge)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("baseFee", baseFee);
        result.put("distanceKm", distance);
        result.put("mileageFee", mileageFee);
        result.put("timeSurcharge", timeSurcharge);
        result.put("urgentSurcharge", urgentSurcharge);
        result.put("totalFee", total);
        return result;
    }

    /**
     * 生成结算报表（用于ERP）
     *
     * @param tenantId  租户ID
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 结算汇总数据
     */
    public Map<String, Object> generateSettlementReport(Long tenantId, LocalDate startDate, LocalDate endDate) {
        // 查询时间段内的已完成任务
        List<DmsTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<DmsTask>()
                        .eq(DmsTask::getTenantId, tenantId)
                        .eq(DmsTask::getStatus, 4) // 假设4=已完成
                        .ge(DmsTask::getUpdateTime, startDate.atStartOfDay())
                        .le(DmsTask::getUpdateTime, endDate.plusDays(1).atStartOfDay())
        );

        BigDecimal totalFee = BigDecimal.ZERO;
        int totalTasks = tasks.size();

        for (DmsTask task : tasks) {
            BigDecimal distance = task.getEstimatedDistance() != null
                    ? task.getEstimatedDistance()
                    : BigDecimal.TEN;
            BigDecimal fee = BASE_FEE.add(PER_KM_RATE.multiply(distance));
            totalFee = totalFee.add(fee);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("tenantId", tenantId);
        report.put("startDate", startDate.toString());
        report.put("endDate", endDate.toString());
        report.put("totalTasks", totalTasks);
        report.put("totalFee", totalFee.setScale(2, RoundingMode.HALF_UP));
        report.put("generatedAt", LocalDateTime.now());
        return report;
    }

    /**
     * 推送结算数据到ERP
     *
     * 通过事件发件箱模式异步推送结算数据到ERP系统。
     *
     * @param settlementData 结算数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void pushToErp(Map<String, Object> settlementData) {
        // 创建事件发件箱记录
        DmsEventOutbox outbox = new DmsEventOutbox();
        outbox.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        outbox.setEventType("SETTLEMENT_PUSH_ERP");
        outbox.setSource("DMS");
        outbox.setTarget("ERP");
        outbox.setPayload(settlementData.toString());
        outbox.setStatus(0); // 待发送
        outbox.setRetryCount(0);
        eventOutboxMapper.insert(outbox);

        log.info("结算数据已写入事件发件箱, traceId={}, eventId={}", outbox.getTraceId(), outbox.getId());
    }
}
