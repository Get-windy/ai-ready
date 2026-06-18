package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.WriteOff;
import cn.aiedge.erp.payment.mapper.WriteOffMapper;
import cn.aiedge.erp.payment.service.WriteOffService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WriteOffServiceImpl extends ServiceImpl<WriteOffMapper, WriteOff> implements WriteOffService {

    @Override
    public WriteOff getByWriteOffNo(String writeOffNo) {
        return lambdaQuery()
                .eq(WriteOff::getWriteOffNo, writeOffNo)
                .one();
    }

    @Override
    public Page<WriteOff> pageList(String keyword, String writeOffType, Long receiptId, Long paymentId,
                                    Long receivableId, Long payableId, String startDate, String endDate,
                                    int pageNum, int pageSize) {
        LambdaQueryWrapper<WriteOff> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(WriteOff::getWriteOffNo, keyword)
                    .or().like(WriteOff::getCustomerName, keyword)
                    .or().like(WriteOff::getSupplierName, keyword));
        }
        if (writeOffType != null && !writeOffType.isEmpty()) {
            wrapper.eq(WriteOff::getWriteOffType, writeOffType);
        }
        if (receiptId != null) wrapper.eq(WriteOff::getReceiptId, receiptId);
        if (paymentId != null) wrapper.eq(WriteOff::getPaymentId, paymentId);
        if (receivableId != null) wrapper.eq(WriteOff::getReceivableId, receivableId);
        if (payableId != null) wrapper.eq(WriteOff::getPayableId, payableId);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(WriteOff::getWriteOffDate, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(WriteOff::getWriteOffDate, endDate);
        }
        wrapper.orderByDesc(WriteOff::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WriteOff createReceiptWriteOff(Long receiptId, String receiptNo, Long receivableId,
                                           Long customerId, String customerName,
                                           BigDecimal totalAmount, BigDecimal writeOffAmount,
                                           BigDecimal remainingAmount) {
        WriteOff writeOff = new WriteOff();
        writeOff.setWriteOffNo(generateWriteOffNo());
        writeOff.setWriteOffType("receipt_write_off");
        writeOff.setReceiptId(receiptId);
        writeOff.setReceiptNo(receiptNo);
        writeOff.setReceivableId(receivableId);
        writeOff.setCustomerId(customerId);
        writeOff.setCustomerName(customerName);
        writeOff.setTotalAmount(totalAmount);
        writeOff.setWriteOffAmount(writeOffAmount);
        writeOff.setRemainingAmount(remainingAmount);
        writeOff.setWriteOffDate(LocalDate.now());
        writeOff.setStatus("completed");
        save(writeOff);
        log.info("收款核销记录创建成功: writeOffNo={}, receiptNo={}, amount={}",
                writeOff.getWriteOffNo(), receiptNo, writeOffAmount);
        return writeOff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WriteOff createPaymentWriteOff(Long paymentId, String paymentNo, Long payableId,
                                           Long supplierId, String supplierName,
                                           BigDecimal totalAmount, BigDecimal writeOffAmount,
                                           BigDecimal remainingAmount) {
        WriteOff writeOff = new WriteOff();
        writeOff.setWriteOffNo(generateWriteOffNo());
        writeOff.setWriteOffType("payment_write_off");
        writeOff.setPaymentId(paymentId);
        writeOff.setPaymentNo(paymentNo);
        writeOff.setPayableId(payableId);
        writeOff.setSupplierId(supplierId);
        writeOff.setSupplierName(supplierName);
        writeOff.setTotalAmount(totalAmount);
        writeOff.setWriteOffAmount(writeOffAmount);
        writeOff.setRemainingAmount(remainingAmount);
        writeOff.setWriteOffDate(LocalDate.now());
        writeOff.setStatus("completed");
        save(writeOff);
        log.info("付款核销记录创建成功: writeOffNo={}, paymentNo={}, amount={}",
                writeOff.getWriteOffNo(), paymentNo, writeOffAmount);
        return writeOff;
    }

    @Override
    public List<WriteOff> getByReceiptId(Long receiptId) {
        return lambdaQuery()
                .eq(WriteOff::getReceiptId, receiptId)
                .orderByDesc(WriteOff::getCreateTime)
                .list();
    }

    @Override
    public List<WriteOff> getByPaymentId(Long paymentId) {
        return lambdaQuery()
                .eq(WriteOff::getPaymentId, paymentId)
                .orderByDesc(WriteOff::getCreateTime)
                .list();
    }

    @Override
    public String generateWriteOffNo() {
        String prefix = "WO";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<WriteOff> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(WriteOff::getWriteOffNo, prefix + dateStr)
                .orderByDesc(WriteOff::getWriteOffNo)
                .last("LIMIT 1");
        WriteOff last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getWriteOffNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }
}
