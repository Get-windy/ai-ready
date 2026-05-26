package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Payable;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PayableService extends IService<Payable> {

    Payable getByPayableNo(String payableNo);

    Page<Payable> pageList(String keyword, Long supplierId, Integer status, int pageNum, int pageSize);

    List<Payable> listBySupplierId(Long supplierId);

    List<Payable> listByOrderId(Long orderId);

    List<Payable> listOverdue();

    String generatePayableNo();

    Payable createFromOrder(Long orderId, BigDecimal amount, LocalDate dueDate);

    Payable createFromInvoice(Long invoiceId);

    Payable pay(Long payableId, BigDecimal amount);

    Payable close(Long payableId);

    Payable cancel(Long payableId);

    void updateOverdueStatus();

    Map<String, Object> getStatistics();

    Map<String, BigDecimal> getAgingAnalysis();
}