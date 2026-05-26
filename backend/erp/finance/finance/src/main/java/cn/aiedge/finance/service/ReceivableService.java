package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Receivable;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReceivableService extends IService<Receivable> {

    Receivable getByReceivableNo(String receivableNo);

    Page<Receivable> pageList(String keyword, Long customerId, Integer status, int pageNum, int pageSize);

    List<Receivable> listByCustomerId(Long customerId);

    List<Receivable> listByOrderId(Long orderId);

    List<Receivable> listOverdue();

    String generateReceivableNo();

    Receivable createFromOrder(Long orderId, BigDecimal amount, LocalDate dueDate);

    Receivable createFromInvoice(Long invoiceId);

    Receivable receive(Long receivableId, BigDecimal amount);

    Receivable close(Long receivableId);

    Receivable cancel(Long receivableId);

    void updateOverdueStatus();

    Map<String, Object> getStatistics();

    Map<String, BigDecimal> getAgingAnalysis();
}