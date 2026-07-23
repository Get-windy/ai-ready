package cn.aiedge.crm.visit.service;

import cn.aiedge.crm.visit.entity.VisitPlan;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

public interface VisitPlanService extends IService<VisitPlan> {

    Page<VisitPlan> pageList(Long customerId, Long salesPersonId, Integer status,
                             LocalDate planDateStart, LocalDate planDateEnd, int page, int size);

    String generatePlanNo();

    boolean cancel(Long id);
}
