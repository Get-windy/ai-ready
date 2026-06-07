package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.dto.VoucherItemDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import cn.aiedge.erp.finance.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 业财集成网关Service实现类
 * 接收来自业务系统（采购、销售、费用等）的记账请求，统一转换为财务凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessAccountingServiceImpl implements BusinessAccountingService {

    private final VoucherService voucherService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final AccountSubjectMapper accountSubjectMapper;

    @Override
    @Transactional
    public VoucherDTO createVoucherFromBusiness(BusinessAccountingRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest("记账明细项不能为空");
        }

        LocalDate voucherDate = request.getVoucherDate() != null ? request.getVoucherDate() : LocalDate.now();
        Integer fiscalYear = voucherDate.getYear();
        Integer fiscalPeriod = voucherDate.getMonthValue();

        List<VoucherItemDTO> items = new ArrayList<>();
        for (BusinessAccountingRequest.AccountingRequestItem reqItem : request.getItems()) {
            AccountSubject subject = accountSubjectMapper.findBySubjectCode(reqItem.getSubjectCode())
                    .orElseThrow(() -> BusinessException.notFound("科目编码不存在: " + reqItem.getSubjectCode()));

            VoucherItemDTO item = new VoucherItemDTO();
            item.setSummary(reqItem.getSummary() != null ? reqItem.getSummary() : request.getSummary());
            item.setSubjectId(subject.getId());
            item.setSubjectCode(subject.getSubjectCode());
            item.setSubjectName(subject.getSubjectName());
            item.setDebitAmount(reqItem.getDebitAmount() != null ? reqItem.getDebitAmount() : BigDecimal.ZERO);
            item.setCreditAmount(reqItem.getCreditAmount() != null ? reqItem.getCreditAmount() : BigDecimal.ZERO);
            item.setSourceType(request.getSourceType());
            item.setSourceId(request.getSourceId());
            item.setSourceNo(request.getSourceNo());
            items.add(item);
        }

        VoucherDTO voucherDTO = new VoucherDTO();
        voucherDTO.setVoucherDate(voucherDate);
        voucherDTO.setFiscalYear(fiscalYear);
        voucherDTO.setFiscalPeriod(fiscalPeriod);
        voucherDTO.setRemark(request.getSummary());
        voucherDTO.setAttachments(0);
        voucherDTO.setItems(items);

        BigDecimal totalDebit = items.stream()
                .map(i -> i.getDebitAmount() != null ? i.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = items.stream()
                .map(i -> i.getCreditAmount() != null ? i.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        voucherDTO.setTotalDebit(totalDebit);
        voucherDTO.setTotalCredit(totalCredit);

        VoucherDTO created = voucherService.create(voucherDTO);

        try {
            String systemUser = "system";
            VoucherDTO audited = voucherService.audit(created.getId(), systemUser);
            VoucherDTO posted = voucherService.post(audited.getId(), systemUser);
            log.info("业财集成自动过账完成: sourceType={}, sourceId={}, voucherNo={}",
                    request.getSourceType(), request.getSourceId(), posted.getVoucherNo());
            return posted;
        } catch (Exception e) {
            log.warn("业财集成自动过账失败，保留草稿状态: sourceType={}, sourceId={}, voucherId={}, error={}",
                    request.getSourceType(), request.getSourceId(), created.getId(), e.getMessage());
            return created;
        }
    }

    @Override
    @Transactional
    public ReceivableDTO createReceivableFromBusiness(BusinessAccountingRequest request) {
        ReceivableDTO dto = new ReceivableDTO();
        dto.setSourceType(request.getSourceType());
        dto.setSourceId(request.getSourceId());
        dto.setSourceNo(request.getSourceNo());
        dto.setCustomerId(request.getCustomerId());
        dto.setCustomerName(request.getCustomerName());
        dto.setTotalAmount(request.getAmount());
        dto.setRemainingAmount(request.getAmount());
        dto.setPaidAmount(BigDecimal.ZERO);
        dto.setDueDate(request.getDueDate());
        dto.setInvoiceDate(request.getVoucherDate());
        dto.setStatus("normal");

        ReceivableDTO created = receivableService.create(dto);
        log.info("业财集成创建应收账款: sourceType={}, sourceId={}",
                request.getSourceType(), request.getSourceId());
        return created;
    }

    @Override
    @Transactional
    public PayableDTO createPayableFromBusiness(BusinessAccountingRequest request) {
        PayableDTO dto = new PayableDTO();
        dto.setSourceType(request.getSourceType());
        dto.setSourceId(request.getSourceId());
        dto.setSourceNo(request.getSourceNo());
        dto.setSupplierId(request.getSupplierId());
        dto.setSupplierName(request.getSupplierName());
        dto.setTotalAmount(request.getAmount());
        dto.setRemainingAmount(request.getAmount());
        dto.setPaidAmount(BigDecimal.ZERO);
        dto.setDueDate(request.getDueDate());
        dto.setInvoiceDate(request.getVoucherDate());
        dto.setStatus("normal");

        PayableDTO created = payableService.create(dto);
        log.info("业财集成创建应付账款: sourceType={}, sourceId={}",
                request.getSourceType(), request.getSourceId());
        return created;
    }
}
