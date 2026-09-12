package cn.aiedge.erp.finance.arapadjust.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustQuery;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustSaveDTO;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustVO;
import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjust;
import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjustItem;
import cn.aiedge.erp.finance.arapadjust.mapper.ArApAdjustItemMapper;
import cn.aiedge.erp.finance.arapadjust.mapper.ArApAdjustMapper;
import cn.aiedge.erp.finance.arapadjust.service.ArApAdjustService;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 应收应付调整 Service实现
 *
 * P0 红线：往来余额调整，记账必须经会计凭证（KJPZ-），严禁绕过凭证直改余额；
 *         无账户字段、不产生资金流水，仅调整应收/应付余额与对应科目。
 * P1 守恒：本单金额 = Σ科目明细金额。
 * 四向语义：应收增加→客户应收调增；应收减少→客户应收调减；应付增加→供应商应付调增；应付减少→供应商应付调减。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArApAdjustServiceImpl extends ServiceImpl<ArApAdjustMapper, ArApAdjust> implements ArApAdjustService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_POSTED = 1;
    private static final int STATUS_CANCELLED = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final int DIR_AR_INCREASE = 1;   // 应收增加
    private static final int DIR_AR_DECREASE = 2;   // 应收减少
    private static final int DIR_AP_INCREASE = 3;   // 应付增加
    private static final int DIR_AP_DECREASE = 4;   // 应付减少

    private static final String SUBJECT_AR = "1122";   // 应收账款
    private static final String SUBJECT_AP = "2202";   // 应付账款
    private static final String PARTNER_CUSTOMER = "customer";
    private static final String PARTNER_SUPPLIER = "supplier";
    private static final String SOURCE_TYPE = "ar_ap_adjust";

    private final ArApAdjustItemMapper arApAdjustItemMapper;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final BusinessAccountingService businessAccountingService;

    @Override
    public Page<ArApAdjust> pageQuery(ArApAdjustQuery query) {
        LambdaQueryWrapper<ArApAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArApAdjust::getDeleted, 0);
        if (query == null) {
            query = new ArApAdjustQuery();
        }
        if (query.getDocNo() != null && !query.getDocNo().isEmpty()) {
            wrapper.like(ArApAdjust::getDocNo, query.getDocNo());
        }
        if (query.getPartnerName() != null && !query.getPartnerName().isEmpty()) {
            wrapper.like(ArApAdjust::getPartnerName, query.getPartnerName());
        }
        if (query.getHandlerName() != null && !query.getHandlerName().isEmpty()) {
            wrapper.like(ArApAdjust::getHandlerName, query.getHandlerName());
        }
        if (query.getDeptName() != null && !query.getDeptName().isEmpty()) {
            wrapper.like(ArApAdjust::getDeptName, query.getDeptName());
        }
        if (query.getCreatorName() != null && !query.getCreatorName().isEmpty()) {
            wrapper.like(ArApAdjust::getCreatorName, query.getCreatorName());
        }
        if (query.getBookkeeperName() != null && !query.getBookkeeperName().isEmpty()) {
            wrapper.like(ArApAdjust::getBookkeeperName, query.getBookkeeperName());
        }
        if (query.getRemark() != null && !query.getRemark().isEmpty()) {
            wrapper.like(ArApAdjust::getRemark, query.getRemark());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ArApAdjust::getStatus, query.getStatus());
        }
        if (query.getDirection() != null) {
            wrapper.eq(ArApAdjust::getDirection, query.getDirection());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(ArApAdjust::getDocDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(ArApAdjust::getDocDate, query.getDateEnd());
        }
        if (query.getShowRed() != null && !query.getShowRed()) {
            wrapper.and(w -> w.isNull(ArApAdjust::getRedFlag).or().eq(ArApAdjust::getRedFlag, 0));
        }
        wrapper.orderByDesc(ArApAdjust::getDocDate).orderByDesc(ArApAdjust::getDocNo);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public String generateDocNo() {
        String prefix = "YSKZJ";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<ArApAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(ArApAdjust::getDocNo, prefix + "-" + dateStr)
                .eq(ArApAdjust::getDeleted, 0)
                .orderByDesc(ArApAdjust::getDocNo)
                .last("LIMIT 1");
        ArApAdjust last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getDocNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }

    @Override
    public ArApAdjustVO getDetail(Long id) {
        ArApAdjust adjust = getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("应收应付调整单不存在");
        }
        ArApAdjustVO vo = new ArApAdjustVO();
        BeanUtils.copyProperties(adjust, vo);
        vo.setItems(arApAdjustItemMapper.selectList(new LambdaQueryWrapper<ArApAdjustItem>()
                .eq(ArApAdjustItem::getAdjustId, id)
                .eq(ArApAdjustItem::getDeleted, 0)
                .orderByAsc(ArApAdjustItem::getLineNo)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArApAdjust saveDraft(ArApAdjustSaveDTO dto) {
        ArApAdjust adjust;
        if (dto.getId() != null) {
            adjust = getById(dto.getId());
            if (adjust == null) {
                throw BusinessException.notFound("应收应付调整单不存在");
            }
            if (adjust.getStatus() == STATUS_POSTED) {
                throw BusinessException.badRequest("已记账的调整单不能修改");
            }
        } else {
            adjust = new ArApAdjust();
            adjust.setDocNo(dto.getDocNo() != null && !dto.getDocNo().isEmpty() ? dto.getDocNo() : generateDocNo());
        }
        applyHeader(adjust, dto);
        adjust.setStatus(STATUS_DRAFT);
        adjust.setTenantId(adjust.getTenantId() != null ? adjust.getTenantId() : 1L);
        adjust.setPrintCount(adjust.getPrintCount() != null ? adjust.getPrintCount() : 0);
        // 本单金额 = Σ科目明细金额（P1）
        adjust.setTotalAmount(sumItems(dto.getItems()));
        if (dto.getId() != null) {
            updateById(adjust);
        } else {
            save(adjust);
        }
        replaceItems(adjust.getId(), dto.getItems());
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArApAdjust update(ArApAdjustSaveDTO dto) {
        return saveDraft(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArApAdjust confirm(Long id, Long operatorId, String operatorName) {
        ArApAdjust adjust = getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("应收应付调整单不存在");
        }
        if (adjust.getStatus() == STATUS_POSTED) {
            return adjust; // 幂等
        }
        if (adjust.getStatus() == STATUS_CANCELLED) {
            throw BusinessException.badRequest("已取消的调整单不能记账");
        }
        // 重算本单金额 = Σ科目明细金额（P1）
        BigDecimal totalAmount = sumItems(adjust.getId());
        if (totalAmount.compareTo(ZERO) <= 0) {
            throw BusinessException.badRequest("调整金额必须大于0");
        }
        if (nvl(adjust.getTotalAmount()).compareTo(totalAmount) != 0) {
            throw BusinessException.badRequest("金额不平衡：本单金额(" + nvl(adjust.getTotalAmount()) + ")应等于科目明细合计(" + totalAmount + ")");
        }
        adjust.setTotalAmount(totalAmount);
        // 调整应收账款/应付账款余额（P0：经凭证 + 记应收应付）
        adjustArApBalance(adjust, totalAmount);
        adjust.setBookkeeperId(operatorId);
        adjust.setBookkeeperName(operatorName != null ? operatorName : adjust.getCreatorName());
        adjust.setBookkeepingTime(LocalDateTime.now());
        adjust.setStatus(STATUS_POSTED);
        updateById(adjust);

        // 记账生成凭证（KJPZ-，P0 红线：必须经凭证）
        try {
            createVoucher(adjust);
        } catch (Exception e) {
            log.error("应收应付调整记账创建凭证失败: docNo={}, error={}", adjust.getDocNo(), e.getMessage(), e);
            throw BusinessException.badRequest("记账失败：生成会计凭证异常:" + e.getMessage());
        }
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ArApAdjust adjust = getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("应收应付调整单不存在");
        }
        if (adjust.getStatus() == STATUS_POSTED) {
            throw BusinessException.badRequest("已记账的调整单不能取消");
        }
        adjust.setStatus(STATUS_CANCELLED);
        updateById(adjust);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ArApAdjust adjust = getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("应收应付调整单不存在");
        }
        if (adjust.getStatus() == STATUS_POSTED) {
            throw BusinessException.badRequest("已记账的调整单不能删除");
        }
        removeById(id);
    }

    // ── 内部方法 ──

    private void applyHeader(ArApAdjust adjust, ArApAdjustSaveDTO dto) {
        adjust.setDocDate(dto.getDocDate() != null ? dto.getDocDate() : LocalDate.now());
        Integer direction = dto.getDirection() != null ? dto.getDirection() : DIR_AR_INCREASE;
        adjust.setDirection(direction);
        adjust.setDirectionName(directionName(direction));
        adjust.setPartnerType(dto.getPartnerType() != null ? dto.getPartnerType() : partnerTypeOf(direction));
        adjust.setPartnerId(dto.getPartnerId());
        adjust.setPartnerCode(dto.getPartnerCode());
        adjust.setPartnerName(dto.getPartnerName());
        adjust.setHandlerId(dto.getHandlerId());
        adjust.setHandlerName(dto.getHandlerName());
        adjust.setDeptId(dto.getDeptId());
        adjust.setDeptName(dto.getDeptName());
        adjust.setCreatorName(dto.getCreatorName());
        adjust.setSummary(dto.getSummary());
        adjust.setRemark(dto.getRemark());
    }

    private void replaceItems(Long adjustId, List<ArApAdjustItem> items) {
        arApAdjustItemMapper.delete(new LambdaQueryWrapper<ArApAdjustItem>().eq(ArApAdjustItem::getAdjustId, adjustId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (ArApAdjustItem item : items) {
            item.setId(null);
            item.setAdjustId(adjustId);
            item.setLineNo(line++);
            item.setTenantId(1L);
            arApAdjustItemMapper.insert(item);
        }
    }

    private BigDecimal sumItems(List<ArApAdjustItem> items) {
        if (items == null) {
            return ZERO;
        }
        return items.stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(Long adjustId) {
        List<ArApAdjustItem> items = arApAdjustItemMapper.selectList(new LambdaQueryWrapper<ArApAdjustItem>()
                .eq(ArApAdjustItem::getAdjustId, adjustId)
                .eq(ArApAdjustItem::getDeleted, 0));
        return items.stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private String directionName(Integer direction) {
        switch (direction != null ? direction : 0) {
            case DIR_AR_INCREASE: return "应收增加";
            case DIR_AR_DECREASE: return "应收减少";
            case DIR_AP_INCREASE: return "应付增加";
            case DIR_AP_DECREASE: return "应付减少";
            default: return "未知";
        }
    }

    private String partnerTypeOf(Integer direction) {
        int d = direction != null ? direction : 0;
        return (d == DIR_AP_INCREASE || d == DIR_AP_DECREASE) ? PARTNER_SUPPLIER : PARTNER_CUSTOMER;
    }

    /**
     * 按方向生成带符号的调整金额并写往应收/应付账款：
     *  应收增加 → 客户应收调增（正）；应收减少 → 客户应收调减（负）；
     *  应付增加 → 供应商应付调增（正）；应付减少 → 供应商应付调减（负）。
     */
    private void adjustArApBalance(ArApAdjust adjust, BigDecimal totalAmount) {
        int direction = adjust.getDirection() != null ? adjust.getDirection() : DIR_AR_INCREASE;
        boolean isReceivable = (direction == DIR_AR_INCREASE || direction == DIR_AR_DECREASE);
        boolean isIncrease = (direction == DIR_AR_INCREASE || direction == DIR_AP_INCREASE);
        BigDecimal signed = isIncrease ? totalAmount : totalAmount.negate();
        if (isReceivable) {
            ReceivableDTO dto = new ReceivableDTO();
            dto.setSourceType(SOURCE_TYPE);
            dto.setSourceId(adjust.getId());
            dto.setSourceNo(adjust.getDocNo());
            dto.setCustomerId(adjust.getPartnerId() != null ? String.valueOf(adjust.getPartnerId()) : null);
            dto.setCustomerName(adjust.getPartnerName());
            dto.setTotalAmount(signed);
            dto.setRemainingAmount(signed);
            dto.setPaidAmount(ZERO);
            dto.setStatus("normal");
            dto.setInvoiceDate(adjust.getDocDate());
            receivableService.create(dto);
        } else {
            PayableDTO dto = new PayableDTO();
            dto.setSourceType(SOURCE_TYPE);
            dto.setSourceId(adjust.getId());
            dto.setSourceNo(adjust.getDocNo());
            dto.setSupplierId(adjust.getPartnerId() != null ? String.valueOf(adjust.getPartnerId()) : null);
            dto.setSupplierName(adjust.getPartnerName());
            dto.setTotalAmount(signed);
            dto.setRemainingAmount(signed);
            dto.setPaidAmount(ZERO);
            dto.setStatus("normal");
            dto.setInvoiceDate(adjust.getDocDate());
            payableService.create(dto);
        }
    }

    /**
     * 记账生成凭证（KJPZ-）：
     *  应收增加：Dr 应收账款(1122) X / Cr 各明细科目(各金额)
     *  应收减少：Dr 各明细科目(各金额) / Cr 应收账款(1122) X
     *  应付增加：Dr 各明细科目(各金额) / Cr 应付账款(2202) X
     *  应付减少：Dr 应付账款(2202) X / Cr 各明细科目(各金额)
     *  借贷平衡：借方合计 = 贷方合计 = X（= Σ明细金额）。
     */
    private void createVoucher(ArApAdjust adjust) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType(SOURCE_TYPE);
        request.setSourceId(adjust.getId());
        request.setSourceNo(adjust.getDocNo());
        request.setAmount(adjust.getTotalAmount());
        request.setSummary("应收应付调整 - " + adjust.getDocNo());
        request.setVoucherDate(adjust.getDocDate() != null ? adjust.getDocDate() : LocalDate.now());
        if (PARTNER_CUSTOMER.equals(adjust.getPartnerType())) {
            request.setCustomerId(String.valueOf(adjust.getPartnerId()));
            request.setCustomerName(adjust.getPartnerName());
        } else {
            request.setSupplierId(String.valueOf(adjust.getPartnerId()));
            request.setSupplierName(adjust.getPartnerName());
        }

        int direction = adjust.getDirection() != null ? adjust.getDirection() : DIR_AR_INCREASE;
        String arApSubject = (direction == DIR_AP_INCREASE || direction == DIR_AP_DECREASE) ? SUBJECT_AP : SUBJECT_AR;
        boolean arApOnDebit = (direction == DIR_AR_INCREASE || direction == DIR_AP_DECREASE);

        List<ArApAdjustItem> itemList = arApAdjustItemMapper.selectList(new LambdaQueryWrapper<ArApAdjustItem>()
                .eq(ArApAdjustItem::getAdjustId, adjust.getId())
                .eq(ArApAdjustItem::getDeleted, 0));

        BigDecimal total = adjust.getTotalAmount() == null ? ZERO : adjust.getTotalAmount();
        String arApSummary = "应收应付调整 - " + (adjust.getDirectionName() != null ? adjust.getDirectionName() : "调整");
        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        if (arApOnDebit) {
            // 应收/应付科目在借方，各明细科目在贷方
            items.add(voucherItem(arApSummary, arApSubject, total, ZERO));
            for (ArApAdjustItem item : itemList) {
                items.add(voucherItem(item.getSubjectName(), item.getSubjectCode(), ZERO, nvl(item.getAmount())));
            }
        } else {
            // 各明细科目在借方，应收/应付科目在贷方
            for (ArApAdjustItem item : itemList) {
                items.add(voucherItem(item.getSubjectName(), item.getSubjectCode(), nvl(item.getAmount()), ZERO));
            }
            items.add(voucherItem(arApSummary, arApSubject, ZERO, total));
        }
        request.setItems(items);
        businessAccountingService.createVoucherFromBusiness(request);
    }

    private BusinessAccountingRequest.AccountingRequestItem voucherItem(String summary, String subjectCode,
                                                                        BigDecimal debit, BigDecimal credit) {
        BusinessAccountingRequest.AccountingRequestItem item = new BusinessAccountingRequest.AccountingRequestItem();
        item.setSummary(summary);
        item.setSubjectCode(subjectCode);
        item.setDebitAmount(debit);
        item.setCreditAmount(credit);
        return item;
    }
}
