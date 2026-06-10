package cn.aiedge.erp.expense.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;

/**
 * 付款服务接口
 */
public interface FeePaymentService {

    /**
     * 创建付款记录
     */
    Long createPayment(FeePaymentCreateRequest request);

    /**
     * 确认付款
     */
    void confirmPayment(Long paymentId, Long confirmUserId, String confirmUserName);

    /**
     * 取消付款
     */
    void cancelPayment(Long paymentId, String reason);

    /**
     * 分页查询付款记录
     */
    PageResult<FeePaymentRecordVO> pageList(FeePaymentQueryRequest request);

    /**
     * 获取付款记录详情
     */
    FeePaymentRecordVO getDetail(Long id);

    /**
     * 获取业务单据的付款记录
     */
    PageResult<FeePaymentRecordVO> getByBusiness(String businessType, Long businessId, Integer pageNum, Integer pageSize);
}
