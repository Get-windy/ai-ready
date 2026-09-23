package cn.aiedge.hr.service;

import cn.aiedge.hr.salary.HrSalaryPayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 薪资发放服务
 */
public interface HrSalaryPaymentService extends IService<HrSalaryPayment> {

    Page<HrSalaryPayment> pagePayments(Page<HrSalaryPayment> page, Long tenantId,
                                       Long employeeId, String paymentMonth,
                                       Integer status, Long deptId, String keyword);

    /** 生成月度薪资（幂等：已存在同月记录则跳过该员工） */
    Map<String, Object> generateMonthlyPayment(String paymentMonth);

    void confirmPayment(Long id);

    void batchConfirm(List<Long> ids);

    /** 撤销发放（已发放 → 待发放） */
    void revokePayment(Long id);

    void deletePayment(Long id);

    /** 薪资统计：人数 / 应发合计 / 实发合计 / 社保合计 / 公积金合计 / 个税合计 */
    Map<String, Object> statistics(String paymentMonth, Long deptId);

    /** 工资条明细（按员工+月份单条回读，含薪资结构口径） */
    Map<String, Object> payslip(Long id);
}
