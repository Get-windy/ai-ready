package cn.aiedge.crm.visit.service.impl;

import cn.aiedge.crm.visit.entity.VisitPlan;
import cn.aiedge.crm.visit.entity.VisitRecord;
import cn.aiedge.crm.visit.mapper.VisitPlanMapper;
import cn.aiedge.crm.visit.mapper.VisitRecordMapper;
import cn.aiedge.crm.visit.service.VisitRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitRecordServiceImpl extends ServiceImpl<VisitRecordMapper, VisitRecord> implements VisitRecordService {

    private final VisitPlanMapper visitPlanMapper;

    @Override
    public Page<VisitRecord> pageList(Long customerId, Long salesPersonId, Integer result,
                                      LocalDate visitDateStart, LocalDate visitDateEnd, int page, int size) {
        LambdaQueryWrapper<VisitRecord> wrapper = buildWrapper(customerId, salesPersonId, result,
                visitDateStart, visitDateEnd);
        wrapper.orderByDesc(VisitRecord::getVisitTime);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitRecord checkIn(VisitRecord record) {
        if (record.getVisitTime() == null) {
            record.setVisitTime(LocalDateTime.now());
        }
        baseMapper.insert(record);
        // 关联计划置为已完成
        if (record.getPlanId() != null) {
            VisitPlan plan = visitPlanMapper.selectById(record.getPlanId());
            if (plan != null && (plan.getDeleted() == null || plan.getDeleted() == 0)) {
                plan.setStatus(2);
                visitPlanMapper.updateById(plan);
            }
        }
        return record;
    }

    @Override
    public Map<String, Object> reviewPage(Integer result, LocalDate visitDateStart, LocalDate visitDateEnd,
                                          int page, int size) {
        // 各结果计数
        LambdaQueryWrapper<VisitRecord> countWrapper = buildWrapper(null, null, null, visitDateStart, visitDateEnd);
        countWrapper.select(VisitRecord::getResult);
        List<VisitRecord> all = baseMapper.selectList(countWrapper);
        long interested = all.stream().filter(r -> r.getResult() != null && r.getResult() == 1).count();
        long normal = all.stream().filter(r -> r.getResult() != null && r.getResult() == 2).count();
        long noIntention = all.stream().filter(r -> r.getResult() != null && r.getResult() == 3).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", all.size());
        summary.put("interested", interested);
        summary.put("normal", normal);
        summary.put("noIntention", noIntention);

        // 按日期分组计数
        QueryWrapper<VisitRecord> dateWrapper = new QueryWrapper<>();
        dateWrapper.select("visit_time::date AS visitDate", "count(*) AS cnt");
        dateWrapper.eq("deleted", 0);
        if (visitDateStart != null) {
            dateWrapper.ge("visit_time", visitDateStart.atStartOfDay());
        }
        if (visitDateEnd != null) {
            dateWrapper.lt("visit_time", visitDateEnd.plusDays(1).atStartOfDay());
        }
        dateWrapper.groupBy("visit_time::date");
        dateWrapper.orderByDesc("visit_time::date");
        List<Map<String, Object>> byDate = baseMapper.selectMaps(dateWrapper);

        // 分页列表
        Page<VisitRecord> recordPage = pageList(null, null, result, visitDateStart, visitDateEnd, page, size);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("summary", summary);
        resultMap.put("byDate", byDate);
        resultMap.put("page", recordPage);
        return resultMap;
    }

    @Override
    public Map<String, Object> statsSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        LambdaQueryWrapper<VisitRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(VisitRecord::getDeleted, 0);
        todayWrapper.ge(VisitRecord::getVisitTime, today.atStartOfDay());
        todayWrapper.lt(VisitRecord::getVisitTime, today.plusDays(1).atStartOfDay());
        long todayCount = baseMapper.selectCount(todayWrapper);

        LambdaQueryWrapper<VisitRecord> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(VisitRecord::getDeleted, 0);
        weekWrapper.ge(VisitRecord::getVisitTime, weekStart.atStartOfDay());
        long weekCount = baseMapper.selectCount(weekWrapper);

        // 覆盖率 = 已完成计划 / 计划总数
        LambdaQueryWrapper<VisitPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(VisitPlan::getDeleted, 0);
        long planTotal = visitPlanMapper.selectCount(planWrapper);

        LambdaQueryWrapper<VisitPlan> executedWrapper = new LambdaQueryWrapper<>();
        executedWrapper.eq(VisitPlan::getDeleted, 0);
        executedWrapper.eq(VisitPlan::getStatus, 2);
        long planExecuted = visitPlanMapper.selectCount(executedWrapper);

        BigDecimal coverage = planTotal == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(planExecuted * 100.0 / planTotal).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> summary = new HashMap<>();
        summary.put("todayCount", todayCount);
        summary.put("weekCount", weekCount);
        summary.put("planTotal", planTotal);
        summary.put("planExecuted", planExecuted);
        summary.put("coverage", coverage);
        return summary;
    }

    private LambdaQueryWrapper<VisitRecord> buildWrapper(Long customerId, Long salesPersonId, Integer result,
                                                         LocalDate visitDateStart, LocalDate visitDateEnd) {
        LambdaQueryWrapper<VisitRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitRecord::getDeleted, 0);
        if (customerId != null) {
            wrapper.eq(VisitRecord::getCustomerId, customerId);
        }
        if (salesPersonId != null) {
            wrapper.eq(VisitRecord::getSalesPersonId, salesPersonId);
        }
        if (result != null) {
            wrapper.eq(VisitRecord::getResult, result);
        }
        if (visitDateStart != null) {
            wrapper.ge(VisitRecord::getVisitTime, visitDateStart.atStartOfDay());
        }
        if (visitDateEnd != null) {
            wrapper.lt(VisitRecord::getVisitTime, visitDateEnd.plusDays(1).atStartOfDay());
        }
        return wrapper;
    }
}
