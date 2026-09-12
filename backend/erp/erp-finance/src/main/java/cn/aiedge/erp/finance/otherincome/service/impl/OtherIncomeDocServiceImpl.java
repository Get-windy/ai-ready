package cn.aiedge.erp.finance.otherincome.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeCreateDTO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeItemDTO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeItemDetailVO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeVO;
import cn.aiedge.erp.finance.otherincome.entity.OtherIncomeDoc;
import cn.aiedge.erp.finance.otherincome.entity.OtherIncomeDocItem;
import cn.aiedge.erp.finance.otherincome.mapper.OtherIncomeDocItemMapper;
import cn.aiedge.erp.finance.otherincome.mapper.OtherIncomeDocMapper;
import cn.aiedge.erp.finance.otherincome.service.OtherIncomeDocService;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherIncomeDocServiceImpl extends ServiceImpl<OtherIncomeDocMapper, OtherIncomeDoc>
        implements OtherIncomeDocService {

    private final OtherIncomeDocItemMapper otherIncomeDocItemMapper;
    private final BusinessAccountingService businessAccountingService;

    private static final String PREFIX = "QTSRD-";

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final String SUBJECT_CASH = "1001";     // 库存现金
    private static final String SUBJECT_BANK = "1002";     // 银行存款
    private static final String SUBJECT_RECEIVABLE = "1122"; // 应收账款
    private static final String SUBJECT_OTHER_INCOME = "6301"; // 营业外收入

    @Override
    public Page<OtherIncomeVO> pageList(String keyword, String docNo, String partnerName, String handlerName,
                                        String departmentName, String creatorName, String bookkeeperName,
                                        Integer status, Integer settleStatus, String incomeSubject,
                                        String summary, String remark, Integer showRed,
                                        LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<OtherIncomeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OtherIncomeDoc::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(OtherIncomeDoc::getDocNo, keyword)
                    .or().like(OtherIncomeDoc::getPartnerName, keyword)
                    .or().like(OtherIncomeDoc::getReceiptAccount1, keyword)
                    .or().like(OtherIncomeDoc::getReceiptAccount2, keyword)
                    .or().like(OtherIncomeDoc::getReceiptAccount3, keyword)
                    .or().like(OtherIncomeDoc::getReceiptAccount4, keyword));
        }
        if (docNo != null && !docNo.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getDocNo, docNo);
        }
        if (partnerName != null && !partnerName.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getPartnerName, partnerName);
        }
        if (handlerName != null && !handlerName.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getHandlerName, handlerName);
        }
        if (departmentName != null && !departmentName.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getDepartmentName, departmentName);
        }
        if (creatorName != null && !creatorName.isEmpty()) {
            Long id = parseLongOrNull(creatorName);
            if (id != null) {
                wrapper.eq(OtherIncomeDoc::getCreateBy, id);
            } else {
                wrapper.like(OtherIncomeDoc::getCreatorName, creatorName);
            }
        }
        if (bookkeeperName != null && !bookkeeperName.isEmpty()) {
            Long id = parseLongOrNull(bookkeeperName);
            if (id != null) {
                wrapper.eq(OtherIncomeDoc::getBookkeeperId, id);
            } else {
                wrapper.like(OtherIncomeDoc::getBookkeeperName, bookkeeperName);
            }
        }
        if (status != null) {
            wrapper.eq(OtherIncomeDoc::getStatus, status);
        }
        if (settleStatus != null) {
            wrapper.eq(OtherIncomeDoc::getSettleStatus, settleStatus);
        }
        if (showRed != null && showRed == 0) {
            // 默认不含红冲(红冲单以负数标识)，此处简化：不做特殊过滤
        }
        if (summary != null && !summary.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getSummary, summary);
        }
        if (remark != null && !remark.isEmpty()) {
            wrapper.like(OtherIncomeDoc::getRemark, remark);
        }
        if (startDate != null) {
            wrapper.ge(OtherIncomeDoc::getIncomeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(OtherIncomeDoc::getIncomeDate, endDate);
        }
        wrapper.orderByDesc(OtherIncomeDoc::getCreateTime);
        Page<OtherIncomeDoc> page = page(new Page<>(pageNum, pageSize), wrapper);

        Page<OtherIncomeVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<OtherIncomeItemDetailVO> pageDetail(String keyword, String docNo, String partnerName, String handlerName,
                                                    String departmentName, String creatorName, String bookkeeperName,
                                                    Integer status, String incomeSubject,
                                                    LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        boolean hasDocFilter = docNo != null || partnerName != null || handlerName != null || departmentName != null
                || creatorName != null || bookkeeperName != null || status != null
                || (startDate != null) || (endDate != null);
        List<Long> matchedDocIds = null;
        if (hasDocFilter) {
            LambdaQueryWrapper<OtherIncomeDoc> rw = new LambdaQueryWrapper<>();
            rw.eq(OtherIncomeDoc::getDeleted, 0);
            if (docNo != null && !docNo.isEmpty()) rw.like(OtherIncomeDoc::getDocNo, docNo);
            if (partnerName != null && !partnerName.isEmpty()) rw.like(OtherIncomeDoc::getPartnerName, partnerName);
            if (handlerName != null && !handlerName.isEmpty()) rw.like(OtherIncomeDoc::getHandlerName, handlerName);
            if (departmentName != null && !departmentName.isEmpty()) rw.like(OtherIncomeDoc::getDepartmentName, departmentName);
            if (creatorName != null && !creatorName.isEmpty()) {
                Long id = parseLongOrNull(creatorName);
                if (id != null) rw.eq(OtherIncomeDoc::getCreateBy, id);
            }
            if (bookkeeperName != null && !bookkeeperName.isEmpty()) {
                Long id = parseLongOrNull(bookkeeperName);
                if (id != null) rw.eq(OtherIncomeDoc::getBookkeeperId, id);
            }
            if (status != null) rw.eq(OtherIncomeDoc::getStatus, status);
            if (startDate != null) rw.ge(OtherIncomeDoc::getIncomeDate, startDate);
            if (endDate != null) rw.le(OtherIncomeDoc::getIncomeDate, endDate);
            matchedDocIds = list(rw).stream().map(OtherIncomeDoc::getId).collect(Collectors.toList());
            if (matchedDocIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
        }
        LambdaQueryWrapper<OtherIncomeDocItem> iw = new LambdaQueryWrapper<>();
        iw.eq(OtherIncomeDocItem::getDeleted, 0);
        if (matchedDocIds != null) {
            iw.in(OtherIncomeDocItem::getDocId, matchedDocIds);
        }
        if (keyword != null && !keyword.isEmpty()) {
            iw.and(w -> w.like(OtherIncomeDocItem::getIncomeNo, keyword)
                    .or().like(OtherIncomeDocItem::getIncomeName, keyword));
        }
        if (incomeSubject != null && !incomeSubject.isEmpty()) {
            iw.like(OtherIncomeDocItem::getSubjectCode, incomeSubject);
        }
        iw.orderByDesc(OtherIncomeDocItem::getCreateTime);
        Page<OtherIncomeDocItem> itemPage = otherIncomeDocItemMapper.selectPage(new Page<>(pageNum, pageSize), iw);
        List<Long> ids = itemPage.getRecords().stream().map(OtherIncomeDocItem::getDocId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, OtherIncomeDoc> docMap = ids.isEmpty() ? new HashMap<>()
                : listByIds(ids).stream().collect(Collectors.toMap(OtherIncomeDoc::getId, d -> d));
        List<OtherIncomeItemDetailVO> vos = itemPage.getRecords().stream().map(item -> {
            OtherIncomeItemDetailVO vo = new OtherIncomeItemDetailVO();
            BeanUtils.copyProperties(item, vo);
            OtherIncomeDoc d = docMap.get(item.getDocId());
            if (d != null) {
                vo.setIncomeDate(d.getIncomeDate());
                vo.setDocNo(d.getDocNo());
                vo.setDocStatus(d.getStatus());
                vo.setSettleStatus(d.getSettleStatus());
                vo.setPartnerName(d.getPartnerName());
                vo.setPartnerCode(d.getPartnerCode());
                vo.setHandlerName(d.getHandlerName());
                vo.setDepartmentName(d.getDepartmentName());
                vo.setCreatorName(d.getCreatorName());
                vo.setBookkeeperName(d.getBookkeeperName());
                vo.setSummary(d.getSummary());
                vo.setDocRemark(d.getRemark());
                vo.setDocCreateTime(d.getCreateTime());
                vo.setBookkeepingTime(d.getBookkeepingTime());
                vo.setPrintCount(d.getPrintCount() == null ? 0 : d.getPrintCount());
            }
            return vo;
        }).collect(Collectors.toList());
        Page<OtherIncomeItemDetailVO> voPage = new Page<>(pageNum, pageSize, itemPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public String nextNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<OtherIncomeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(OtherIncomeDoc::getDocNo, PREFIX + dateStr)
                .eq(OtherIncomeDoc::getDeleted, 0)
                .orderByDesc(OtherIncomeDoc::getDocNo)
                .last("LIMIT 1");
        OtherIncomeDoc last = getOne(wrapper);
        int seq = 1;
        if (last != null && last.getDocNo() != null) {
            String tail = last.getDocNo().substring(last.getDocNo().lastIndexOf('-') + 1);
            seq = parseTail(tail) + 1;
        }
        return PREFIX + dateStr + "-" + String.format("%03d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OtherIncomeVO saveDoc(Long id, OtherIncomeCreateDTO dto, boolean confirm) {
        OtherIncomeDoc doc;
        if (id != null) {
            doc = getById(id);
            if (doc == null) throw BusinessException.notFound("其他收入单不存在");
            if (doc.getStatus() != null && doc.getStatus() != 0 && !confirm) {
                throw new BusinessException("已记账的其他收入单不可编辑");
            }
        } else {
            doc = new OtherIncomeDoc();
        }
        applyDto(doc, dto);
        BigDecimal total = ZERO;
        if (dto.getItems() != null) {
            for (OtherIncomeItemDTO item : dto.getItems()) {
                total = total.add(nvl(item.getAmount()));
            }
        }
        doc.setAmount(total);
        if (doc.getStatus() == null) doc.setStatus(0);
        if (doc.getSettleStatus() == null) doc.setSettleStatus(0);
        if (doc.getCurrency() == null) doc.setCurrency("CNY");
        if (doc.getIncomeDate() == null) doc.setIncomeDate(LocalDate.now());
        if (doc.getDocNo() == null || doc.getDocNo().isBlank()) {
            doc.setDocNo(nextNo());
        }
        if (id != null) {
            updateById(doc);
            // 替换明细
            otherIncomeDocItemMapper.delete(new LambdaQueryWrapper<OtherIncomeDocItem>()
                    .eq(OtherIncomeDocItem::getDocId, doc.getId()));
        } else {
            save(doc);
        }

        // 收入项
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            int lineNo = 1;
            for (OtherIncomeItemDTO item : dto.getItems()) {
                OtherIncomeDocItem r = new OtherIncomeDocItem();
                BeanUtils.copyProperties(item, r);
                r.setDocId(doc.getId());
                r.setLineNo(lineNo++);
                r.setTenantId(doc.getTenantId());
                otherIncomeDocItemMapper.insert(r);
            }
        }

        if (confirm) {
            return confirm(doc.getId());
        }
        return getDetail(doc.getId());
    }

    private void applyDto(OtherIncomeDoc doc, OtherIncomeCreateDTO dto) {
        doc.setIncomeType(dto.getIncomeType());
        doc.setPartnerId(dto.getPartnerId());
        doc.setPartnerName(dto.getPartnerName());
        doc.setPartnerCode(dto.getPartnerCode());
        doc.setHandlerId(dto.getHandlerId());
        doc.setHandlerName(dto.getHandlerName());
        doc.setDepartmentId(dto.getDepartmentId());
        doc.setDepartmentName(dto.getDepartmentName());
        doc.setIncomeDate(dto.getIncomeDate());
        doc.setReceiptAccount1(dto.getReceiptAccount1());
        doc.setReceiptAmount1(dto.getReceiptAmount1());
        doc.setReceiptAccount2(dto.getReceiptAccount2());
        doc.setReceiptAmount2(dto.getReceiptAmount2());
        doc.setReceiptAccount3(dto.getReceiptAccount3());
        doc.setReceiptAmount3(dto.getReceiptAmount3());
        doc.setReceiptAccount4(dto.getReceiptAccount4());
        doc.setReceiptAmount4(dto.getReceiptAmount4());
        doc.setAccountSubjectCode(dto.getAccountSubjectCode());
        doc.setSummary(dto.getSummary());
        doc.setAttachment(dto.getAttachment());
        doc.setRemark(dto.getRemark());
    }

    @Override
    public OtherIncomeVO confirm(Long id) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) throw BusinessException.notFound("其他收入单不存在");
        if (doc.getStatus() != null && doc.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的其他收入单可以记账");
        }
        List<OtherIncomeDocItem> items = otherIncomeDocItemMapper.selectList(
                new LambdaQueryWrapper<OtherIncomeDocItem>().eq(OtherIncomeDocItem::getDocId, id)
                        .eq(OtherIncomeDocItem::getDeleted, 0));
        if (items.isEmpty()) {
            throw new BusinessException("请先录入收入项明细");
        }
        BigDecimal total = items.stream().map(OtherIncomeDocItem::getAmount).map(this::nvl)
                .reduce(ZERO, BigDecimal::add);
        if (total.compareTo(ZERO) <= 0) {
            throw new BusinessException("收入项金额必须大于0");
        }
        doc.setAmount(total);
        // 记账生成凭证（KJPZ-）
        VoucherDTO voucher = createVoucher(doc, items, total);
        doc.setStatus(1);
        doc.setSettleStatus(1);
        doc.setBookkeeperName(doc.getCreatorName());
        doc.setBookkeepingTime(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        if (voucher != null) {
            doc.setVoucherNo(voucher.getVoucherNo());
        }
        doc.setBookkeepingTime(now);
        updateById(doc);
        return getDetail(id);
    }

    private VoucherDTO createVoucher(OtherIncomeDoc doc, List<OtherIncomeDocItem> items, BigDecimal total) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType("OTHER_INCOME");
        request.setSourceId(doc.getId());
        request.setSourceNo(doc.getDocNo());
        request.setAmount(total);
        request.setSummary("其他收入 - " + doc.getDocNo());
        request.setVoucherDate(doc.getIncomeDate() != null ? doc.getIncomeDate() : LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> accountItems = new ArrayList<>();
        // 借方：收款账户/往来单位（收入入账）
        BusinessAccountingRequest.AccountingRequestItem debit = new BusinessAccountingRequest.AccountingRequestItem();
        debit.setSummary(doc.getReceiptAccount1() != null ? doc.getReceiptAccount1()
                : (doc.getPartnerName() != null ? doc.getPartnerName() : "其他收入"));
        debit.setSubjectCode(subjectCodeOf(doc));
        debit.setDebitAmount(total);
        debit.setCreditAmount(ZERO);
        accountItems.add(debit);
        // 贷方：各收入项（收益类科目）
        for (OtherIncomeDocItem item : items) {
            BigDecimal amt = nvl(item.getAmount());
            if (amt.compareTo(ZERO) <= 0) {
                continue;
            }
            BusinessAccountingRequest.AccountingRequestItem credit = new BusinessAccountingRequest.AccountingRequestItem();
            credit.setSummary(item.getIncomeName() != null ? item.getIncomeName() : "其他收入");
            credit.setSubjectCode(item.getSubjectCode() != null ? item.getSubjectCode() : SUBJECT_OTHER_INCOME);
            credit.setDebitAmount(ZERO);
            credit.setCreditAmount(amt);
            accountItems.add(credit);
        }
        request.setItems(accountItems);
        try {
            return businessAccountingService.createVoucherFromBusiness(request);
        } catch (Exception e) {
            log.warn("[其他收入] 记账凭证生成失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String subjectCodeOf(OtherIncomeDoc doc) {
        if (doc.getAccountSubjectCode() != null && !doc.getAccountSubjectCode().isEmpty()) {
            return doc.getAccountSubjectCode();
        }
        // 往来单位收入 → 应收账款；内部收入 → 库存现金
        return (doc.getIncomeType() != null && doc.getIncomeType() == 1) ? SUBJECT_RECEIVABLE : SUBJECT_CASH;
    }

    @Override
    public OtherIncomeVO getDetail(Long id) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) return null;
        OtherIncomeVO vo = convertToVO(doc);
        vo.setItems(otherIncomeDocItemMapper.selectList(
                new LambdaQueryWrapper<OtherIncomeDocItem>().eq(OtherIncomeDocItem::getDocId, id)
                        .eq(OtherIncomeDocItem::getDeleted, 0)
                        .orderByAsc(OtherIncomeDocItem::getLineNo)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDoc(Long id) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) throw BusinessException.notFound("其他收入单不存在");
        if (doc.getStatus() != null && doc.getStatus() != 0) {
            throw new BusinessException("已记账的其他收入单不可删除");
        }
        otherIncomeDocItemMapper.delete(new LambdaQueryWrapper<OtherIncomeDocItem>()
                .eq(OtherIncomeDocItem::getDocId, id));
        removeById(id);
    }

    private OtherIncomeVO convertToVO(OtherIncomeDoc doc) {
        OtherIncomeVO vo = new OtherIncomeVO();
        BeanUtils.copyProperties(doc, vo);
        if (doc.getIncomeType() != null) {
            vo.setIncomeTypeName(doc.getIncomeType() == 1 ? "往来单位收入" : "内部收入");
        }
        if (doc.getStatus() != null) {
            vo.setStatusDesc(doc.getStatus() == 1 ? "已记账" : "草稿");
        }
        return vo;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private Long parseLongOrNull(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseTail(String tail) {
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
