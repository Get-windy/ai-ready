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
     * 对账统计（后端聚合：已对账 / 有差异 / 处理中 笔数 + 差异金额合计）
     *
     * @return {total, matchedCount, diffCount, processingCount, diffAmount}
     */
    java.util.Map<String, Object> statReconciliation(LocalDate startDate, LocalDate endDate, String channel);

    /**
     * 查询对账详情
     */
    PaymentReconciliation getReconciliation(Long id);

    /**
     * 查询对账详情（含差异明细 diffRecords）
     *
     * <p>支付中心无对账明细子表，diffRecords 恒为空数组（见
     * {@link cn.aiedge.payment.dto.ReconciliationDetailVO#getDiffRecords()} 注释）；
     * 汇总口径与 {@link #getReconciliation(Long)} 一致。</p>
     *
     * @return 记录不存在时返回 null
     */
    cn.aiedge.payment.dto.ReconciliationDetailVO getReconciliationDetail(Long id);

    /**
     * 处理差异
     * @param id 对账记录ID
     * @param method 处理方式：MANUAL手工调账 / IGNORE忽略差异 / REPROCESS重新对账（可空，兼容旧调用）
     * @param remark 处理备注
     */
    void handleDifference(Long id, String method, String remark);

    /**
     * 获取待对账日期列表
     */
    List<LocalDate> getPendingDates(String channel);
}