package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.Offset;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PrePayment;
import cn.aiedge.erp.payment.entity.PreReceipt;
import cn.aiedge.erp.payment.entity.Receipt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface CapitalFlowService extends IService<CapitalFlow> {

    Page<CapitalFlow> pageList(String flowType, String direction, String partyType,
                               Long partyId, LocalDate startDate, LocalDate endDate,
                               int pageNum, int pageSize);

    /**
     * 按支付相关条件分页查询（在线支付对账单：支付类型/状态/方式/对账标记）
     */
    Page<CapitalFlow> pageListForReconcile(Integer paymentType, String direction,
                                           Integer paymentMethod, Integer reconcileFlag,
                                           String keyword, String customerName, String payStatus,
                                           LocalDate startDate, LocalDate endDate,
                                           int pageNum, int pageSize);

    /**
     * 切换对账标记
     */
    void toggleReconcileFlag(Long id, Integer flag, String operator);

    CapitalFlow createFlow(CapitalFlow flow);

    void recordReceiptFlow(Receipt receipt);

    void recordPaymentFlow(Payment payment);

    void recordPreReceiptFlow(PreReceipt preReceipt);

    void recordPrePaymentFlow(PrePayment prePayment);

    void recordOffsetFlow(Offset offset);

    List<CapitalFlow> exportList(String flowType, String direction, String partyType,
                                 Long partyId, LocalDate startDate, LocalDate endDate);
}
