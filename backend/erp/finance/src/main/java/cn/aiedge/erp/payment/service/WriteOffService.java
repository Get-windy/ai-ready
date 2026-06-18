package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.WriteOff;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface WriteOffService extends IService<WriteOff> {

    WriteOff getByWriteOffNo(String writeOffNo);

    Page<WriteOff> pageList(String keyword, String writeOffType, Long receiptId, Long paymentId,
                            Long receivableId, Long payableId, String startDate, String endDate,
                            int pageNum, int pageSize);

    /**
     * 收款核销应收
     * @param receiptId 收款单ID
     * @param receiptNo 收款单编号
     * @param receivableId 应收单ID
     * @param customerId 客户ID
     * @param customerName 客户名称
     * @param totalAmount 收款总金额
     * @param writeOffAmount 本次核销金额
     * @param remainingAmount 核销后剩余
     */
    WriteOff createReceiptWriteOff(Long receiptId, String receiptNo, Long receivableId,
                                    Long customerId, String customerName,
                                    BigDecimal totalAmount, BigDecimal writeOffAmount,
                                    BigDecimal remainingAmount);

    /**
     * 付款核销应付
     */
    WriteOff createPaymentWriteOff(Long paymentId, String paymentNo, Long payableId,
                                    Long supplierId, String supplierName,
                                    BigDecimal totalAmount, BigDecimal writeOffAmount,
                                    BigDecimal remainingAmount);

    List<WriteOff> getByReceiptId(Long receiptId);

    List<WriteOff> getByPaymentId(Long paymentId);

    String generateWriteOffNo();
}
