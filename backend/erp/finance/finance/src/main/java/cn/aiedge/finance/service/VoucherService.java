package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Voucher;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface VoucherService extends IService<Voucher> {
    
    List<Voucher> listByPeriod(Long tenantId, String period);
    
    Voucher getByVoucherNo(Long tenantId, String voucherNo);
    
    Voucher getBySource(Long tenantId, String sourceType, Long sourceId);
    
    Page<Voucher> pageList(Long tenantId, String period, String voucherNo, Integer voucherType, Integer status, String startDate, String endDate, Page<Voucher> page);
    
    Voucher getDetail(Long tenantId, Long voucherId);
    
    boolean createVoucher(Voucher voucher);
    
    boolean updateVoucher(Voucher voucher);
    
    boolean deleteVoucher(Long tenantId, Long voucherId);
    
    boolean submitForReview(Long tenantId, Long voucherId);
    
    boolean approve(Long tenantId, Long voucherId);
    
    boolean reject(Long tenantId, Long voucherId, String reason);
    
    boolean post(Long tenantId, Long voucherId);
    
    boolean voidVoucher(Long tenantId, Long voucherId, String reason);
    
    boolean print(Long tenantId, Long voucherId);
    
    String generateVoucherNo(Long tenantId, String period, String word);
    
    boolean checkBalance(Voucher voucher);
    
    boolean checkVoucherCanEdit(Long tenantId, Long voucherId);
    
    boolean checkVoucherCanPost(Long tenantId, Long voucherId);
    
    Map<String, BigDecimal> getPeriodSummary(Long tenantId, String period);
    
    List<Voucher> generateFromInvoice(Long tenantId, Long invoiceId);
    
    List<Voucher> generateFromPayment(Long tenantId, Long paymentId);
    
    List<Voucher> generateFromReceipt(Long tenantId, Long receiptId);
}