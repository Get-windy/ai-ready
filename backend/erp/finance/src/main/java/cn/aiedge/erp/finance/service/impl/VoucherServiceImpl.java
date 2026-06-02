package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.dto.VoucherItemDTO;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.mapper.VoucherMapper;
import cn.aiedge.erp.finance.model.entity.Voucher;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.LedgerService;
import cn.aiedge.erp.finance.service.VoucherService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记账凭证Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherServiceImpl implements VoucherService {

    private final VoucherMapper voucherMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    public VoucherDTO create(VoucherDTO dto) {
        // 验证借贷平衡
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (VoucherItemDTO item : dto.getItems()) {
            totalDebit = totalDebit.add(item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO);
            totalCredit = totalCredit.add(item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("借方金额合计与贷方金额合计不平: debit=" + totalDebit + ", credit=" + totalCredit);
        }

        // 生成凭证编号
        Integer fiscalYear = dto.getFiscalYear() != null ? dto.getFiscalYear() : LocalDate.now().getYear();
        Integer fiscalPeriod = dto.getFiscalPeriod() != null ? dto.getFiscalPeriod() : LocalDate.now().getMonthValue();
        String voucherNo = generateVoucherNo(fiscalYear, fiscalPeriod);

        // 创建凭证
        Voucher entity = new Voucher();
        entity.setVoucherNo(voucherNo);
        entity.setVoucherDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now());
        entity.setFiscalYear(fiscalYear);
        entity.setFiscalPeriod(fiscalPeriod);
        entity.setAttachments(dto.getAttachments() != null ? dto.getAttachments() : 0);
        entity.setPrepBy(dto.getPrepBy());
        entity.setPrepAt(LocalDateTime.now());
        entity.setStatus("draft");
        entity.setTotalDebit(totalDebit);
        entity.setTotalCredit(totalCredit);
        entity.setRemark(dto.getRemark());

        // 保存凭证
        voucherMapper.insert(entity);

        // 创建凭证明细
        List<VoucherItem> items = new ArrayList<>();
        for (VoucherItemDTO itemDTO : dto.getItems()) {
            VoucherItem item = new VoucherItem();
            item.setVoucherId(entity.getId());
            item.setSummary(itemDTO.getSummary());
            item.setSubjectId(itemDTO.getSubjectId());
            item.setSubjectCode(itemDTO.getSubjectCode());
            item.setSubjectName(itemDTO.getSubjectName());
            item.setDebitAmount(itemDTO.getDebitAmount() != null ? itemDTO.getDebitAmount() : BigDecimal.ZERO);
            item.setCreditAmount(itemDTO.getCreditAmount() != null ? itemDTO.getCreditAmount() : BigDecimal.ZERO);
            item.setSourceType(itemDTO.getSourceType());
            item.setSourceId(itemDTO.getSourceId());
            item.setSourceNo(itemDTO.getSourceNo());
            items.add(item);
            voucherItemMapper.insert(item);
        }

        // 设置明细
        entity.setItems(items);
        log.info("创建凭证: voucherNo={}, debit={}, credit={}", voucherNo, totalDebit, totalCredit);
        return toDTO(entity);
    }

    @Override
    public VoucherDTO getById(Long id) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在: " + id);
        }
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(id);
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    public VoucherDTO getByVoucherNo(String voucherNo) {
        Voucher voucher = voucherMapper.findByVoucherNo(voucherNo)
                .orElseThrow(() -> new RuntimeException("凭证不存在: " + voucherNo));
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(voucher.getId());
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    public IPage<VoucherDTO> list(Integer fiscalYear, Integer fiscalPeriod, String status, Page<VoucherDTO> page) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        if (fiscalYear != null) {
            wrapper.eq(Voucher::getFiscalYear, fiscalYear);
        }
        if (fiscalPeriod != null) {
            wrapper.eq(Voucher::getFiscalPeriod, fiscalPeriod);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Voucher::getStatus, status);
        }
        wrapper.orderByDesc(Voucher::getCreateTime);

        Page<Voucher> entityPage = voucherMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        Page<VoucherDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::toDTOWithItems).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional
    public VoucherDTO audit(Long id, String auditor) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在: " + id);
        }

        if (!"draft".equals(voucher.getStatus())) {
            throw new RuntimeException("只有草稿状态的凭证才能审核，当前状态: " + voucher.getStatus());
        }

        voucher.setStatus("audited");
        voucher.setAuditBy(auditor);
        voucher.setAuditAt(LocalDateTime.now());
        voucherMapper.updateById(voucher);
        log.info("审核凭证: id={}, voucherNo={}, auditor={}", id, voucher.getVoucherNo(), auditor);

        List<VoucherItem> items = voucherItemMapper.findByVoucherId(id);
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    @Transactional
    public VoucherDTO post(Long id, String poster) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在: " + id);
        }

        if (!"audited".equals(voucher.getStatus())) {
            throw new RuntimeException("只有已审核状态的凭证才能过账，当前状态: " + voucher.getStatus());
        }

        voucher.setStatus("posted");
        voucher.setPostBy(poster);
        voucher.setPostAt(LocalDateTime.now());
        voucherMapper.updateById(voucher);

        // 更新分类账
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(id);
        voucher.setItems(items);
        ledgerService.postToLedger(voucher);

        log.info("过账凭证: id={}, voucherNo={}, poster={}", id, voucher.getVoucherNo(), poster);
        return toDTO(voucher);
    }

    @Override
    @Transactional
    public VoucherDTO reverse(Long id, String reason) {
        Voucher original = voucherMapper.selectById(id);
        if (original == null) {
            throw new RuntimeException("凭证不存在: " + id);
        }

        List<VoucherItem> originalItems = voucherItemMapper.findByVoucherId(id);

        // 创建冲销凭证
        Voucher reverseVoucher = new Voucher();
        reverseVoucher.setVoucherNo(generateVoucherNo(original.getFiscalYear(), original.getFiscalPeriod()));
        reverseVoucher.setVoucherDate(LocalDate.now());
        reverseVoucher.setFiscalYear(original.getFiscalYear());
        reverseVoucher.setFiscalPeriod(original.getFiscalPeriod());
        reverseVoucher.setAttachments(0);
        reverseVoucher.setPrepBy(original.getPostBy());
        reverseVoucher.setPrepAt(LocalDateTime.now());
        reverseVoucher.setStatus("posted");
        reverseVoucher.setTotalDebit(original.getTotalCredit());
        reverseVoucher.setTotalCredit(original.getTotalDebit());
        reverseVoucher.setRemark("冲销凭证: " + reason);

        voucherMapper.insert(reverseVoucher);

        // 创建冲销明细（借贷方向相反）
        List<VoucherItem> reverseItems = new ArrayList<>();
        for (VoucherItem item : originalItems) {
            VoucherItem reverseItem = new VoucherItem();
            reverseItem.setVoucherId(reverseVoucher.getId());
            reverseItem.setSummary("冲销: " + item.getSummary());
            reverseItem.setSubjectId(item.getSubjectId());
            reverseItem.setSubjectCode(item.getSubjectCode());
            reverseItem.setSubjectName(item.getSubjectName());
            // 借贷互换
            reverseItem.setDebitAmount(item.getCreditAmount());
            reverseItem.setCreditAmount(item.getDebitAmount());
            reverseItem.setSourceType(item.getSourceType());
            reverseItem.setSourceId(item.getSourceId());
            reverseItem.setSourceNo(item.getSourceNo());
            reverseItems.add(reverseItem);
            voucherItemMapper.insert(reverseItem);
        }
        reverseVoucher.setItems(reverseItems);

        // 冲销凭证立即过账到分类账
        ledgerService.postToLedger(reverseVoucher);

        // 更新原始凭证状态
        original.setStatus("reversed");
        original.setRemark(reason);
        voucherMapper.updateById(original);

        log.info("冲销凭证: originalId={}, originalNo={}, reverseId={}, reason={}",
                id, original.getVoucherNo(), reverseVoucher.getId(), reason);

        return toDTO(reverseVoucher);
    }

    @Override
    @Transactional
    public String generateVoucherNo(Integer fiscalYear, Integer fiscalPeriod) {
        long count = voucherMapper.countByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod);
        String periodStr = String.format("%04d%02d", fiscalYear, fiscalPeriod);
        String seqStr = String.format("%04d", count + 1);
        return periodStr + "-" + seqStr;
    }

    // ======== DTO <-> Entity 转换 ========

    private VoucherDTO toDTO(Voucher entity) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(entity.getId());
        dto.setVoucherNo(entity.getVoucherNo());
        dto.setVoucherDate(entity.getVoucherDate());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setFiscalPeriod(entity.getFiscalPeriod());
        dto.setAttachments(entity.getAttachments());
        dto.setPrepBy(entity.getPrepBy());
        dto.setPrepAt(entity.getPrepAt());
        dto.setAuditBy(entity.getAuditBy());
        dto.setAuditAt(entity.getAuditAt());
        dto.setPostBy(entity.getPostBy());
        dto.setPostAt(entity.getPostAt());
        dto.setStatus(entity.getStatus());
        dto.setTotalDebit(entity.getTotalDebit());
        dto.setTotalCredit(entity.getTotalCredit());
        dto.setRemark(entity.getRemark());
        if (entity.getItems() != null) {
            dto.setItems(entity.getItems().stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private VoucherDTO toDTOWithItems(Voucher entity) {
        VoucherDTO dto = toDTO(entity);
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(entity.getId());
        if (items != null) {
            dto.setItems(items.stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private VoucherItemDTO toItemDTO(VoucherItem entity) {
        VoucherItemDTO dto = new VoucherItemDTO();
        dto.setId(entity.getId());
        dto.setVoucherId(entity.getVoucherId());
        dto.setSummary(entity.getSummary());
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectCode(entity.getSubjectCode());
        dto.setSubjectName(entity.getSubjectName());
        dto.setDebitAmount(entity.getDebitAmount());
        dto.setCreditAmount(entity.getCreditAmount());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceNo(entity.getSourceNo());
        return dto;
    }
}
