package cn.aiedge.payment.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.entity.PaymentReconciliation;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账服务接口
 */
public interface ReconciliationService {

    /**
     * 执行日对账
     * @param date 对账日期
     * @param channel 渠道(可选,为空则全部渠道)
     * @return 对账记录
     */
    List<PaymentReconciliation> executeDailyReconciliation(LocalDate date, String channel);

    /**
     * 分页查询对账记录
     */
    PageResult<PaymentReconciliation> pageReconciliation(Integer pageNum, Integer pageSize, LocalDate startDate, LocalDate endDate, String channel, Integer status);

    /**
     * 查询对账详情
     */
    PaymentReconciliation getReconciliation(Long id);

    /**
     * 处理差异
     * @param id 对账记录ID
     * @param remark 处理备注
     */
    void handleDifference(Long id, String remark);

    /**
     * 获取待对账日期列表
     */
    List<LocalDate> getPendingDates(String channel);
}