package cn.aiedge.crm.visit.service.impl;

import cn.aiedge.crm.visit.entity.VisitPlan;
import cn.aiedge.crm.visit.mapper.VisitPlanMapper;
import cn.aiedge.crm.visit.service.VisitPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitPlanServiceImpl extends ServiceImpl<VisitPlanMapper, VisitPlan> implements VisitPlanService {

    @Override
    public Page<VisitPlan> pageList(Long customerId, Long salesPersonId, Integer status,
                                    LocalDate planDateStart, LocalDate planDateEnd, int page, int size) {
        LambdaQueryWrapper<VisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitPlan::getDeleted, 0);
        if (customerId != null) {
            wrapper.eq(VisitPlan::getCustomerId, customerId);
        }
        if (salesPersonId != null) {
            wrapper.eq(VisitPlan::getSalesPersonId, salesPersonId);
        }
        if (status != null) {
            wrapper.eq(VisitPlan::getStatus, status);
        }
        if (planDateStart != null) {
            wrapper.ge(VisitPlan::getPlanDate, planDateStart);
        }
        if (planDateEnd != null) {
            wrapper.le(VisitPlan::getPlanDate, planDateEnd);
        }
        wrapper.orderByDesc(VisitPlan::getPlanDate);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public String generatePlanNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "VP-" + dateStr + String.format("%04d", count + 1);
    }

    @Override
    public boolean cancel(Long id) {
        VisitPlan plan = baseMapper.selectById(id);
        if (plan == null || (plan.getDeleted() != null && plan.getDeleted() != 0)) {
            return false;
        }
        // 已完成的计划不可取消
        if (plan.getStatus() != null && plan.getStatus() == 2) {
            return false;
        }
        plan.setStatus(3);
        return baseMapper.updateById(plan) > 0;
    }
}
