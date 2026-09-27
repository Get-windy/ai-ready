package cn.aiedge.crm.visit.service.impl;

import cn.aiedge.common.serial.BizNumberGeneratorService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitPlanServiceImpl extends ServiceImpl<VisitPlanMapper, VisitPlan> implements VisitPlanService {

    /** 系统统一号段服务（biz_number_sequence，行锁 + 按日重置） */
    private final BizNumberGeneratorService bizNumberGeneratorService;

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
        // 走系统统一号段（biz_number_sequence + SELECT FOR UPDATE），不再「查最大号 +1」
        return bizNumberGeneratorService.nextNumber("CRM_VISITPLAN");
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
