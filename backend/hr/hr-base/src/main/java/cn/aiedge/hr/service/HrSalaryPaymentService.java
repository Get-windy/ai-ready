package cn.aiedge.hr.service;

import cn.aiedge.hr.salary.HrSalaryPayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface HrSalaryPaymentService extends IService<HrSalaryPayment> {
    Page<HrSalaryPayment> pagePayments(Page<HrSalaryPayment> page, Long tenantId, Long employeeId, String paymentMonth);
    void generateMonthlyPayment(String paymentMonth);
    void confirmPayment(Long id);
}
