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

    CapitalFlow createFlow(CapitalFlow flow);

    void recordReceiptFlow(Receipt receipt);

    void recordPaymentFlow(Payment payment);

    void recordPreReceiptFlow(PreReceipt preReceipt);

    void recordPrePaymentFlow(PrePayment prePayment);

    void recordOffsetFlow(Offset offset);

    List<CapitalFlow> exportList(String flowType, String direction, String partyType,
                                 Long partyId, LocalDate startDate, LocalDate endDate);
}
