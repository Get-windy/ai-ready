package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.dto.VoucherItemDTO;
import cn.aiedge.erp.finance.dto.VoucherQuery;
import cn.aiedge.erp.finance.mapper.AccountingPeriodMapper;
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
import java.time.format.DateTimeFormatter;
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

    /** 无租户上下文时回落的租户（定时任务/初始化场景，与既有初始化数据一致） */
    private static final Long FALLBACK_TENANT_ID = 1L;

    /**
     * 本模块所属租户：会话上下文优先，取不到才回落。
     *
     * <p><b>2026-09-23 修复</b>：此前关账校验一律写死租户 1 ⇒ 非 1 租户下
     * {@code findByPeriodCode} 查不到期间记录，{@code ifPresent} 不触发 ⇒
     * <b>「已关账期间禁止新增/修改凭证」这条 P0 红线在非 1 租户下静默失效</b>。
     * 取法与 {@code AccountingPeriodServiceImpl#currentTenantId} 保持一致。</p>
     */
    private Long currentTenantId() {
        Long tid = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        return tid != null ? tid : FALLBACK_TENANT_ID;
    }

    private final VoucherMapper voucherMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final AccountingPeriodMapper accountingPeriodMapper;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    public VoucherDTO create(VoucherDTO dto) {
        // 验证借贷平衡
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<VoucherItemDTO> itemDTOs = dto.getItems() != null ? dto.getItems() : new ArrayList<>();
        for (VoucherItemDTO item : itemDTOs) {
            totalDebit = totalDebit.add(item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO);
            totalCredit = totalCredit.add(item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw BusinessException.badRequest("借方金额合计与贷方金额合计不平: debit=" + totalDebit + ", credit=" + totalCredit);
        }
        if (itemDTOs.isEmpty()) {
            throw BusinessException.badRequest("凭证分录不能为空");
        }

        Integer fiscalYear = dto.getFiscalYear() != null ? dto.getFiscalYear() : LocalDate.now().getYear();
        Integer fiscalPeriod = dto.getFiscalPeriod() != null ? dto.getFiscalPeriod() : LocalDate.now().getMonthValue();
        // P0 红线：已关闭会计期间禁止新增/修改凭证（月结为唯一关账入口）
        assertPeriodOpen(fiscalYear, fiscalPeriod);
        // 优先使用前端生成的凭证号（原样落库），否则后端生成 KJPZ-
        String voucherNo = StringUtils.hasText(dto.getVoucherNo()) ? dto.getVoucherNo() : generateVoucherNo();

        // 创建凭证
        Voucher entity = new Voucher();
        entity.setVoucherNo(voucherNo);
        entity.setVoucherDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now());
        entity.setFiscalYear(fiscalYear);
        entity.setFiscalPeriod(fiscalPeriod);
        entity.setVoucherType(StringUtils.hasText(dto.getVoucherType()) ? dto.getVoucherType() : "manual");
        entity.setSummary(dto.getSummary());
        entity.setHandlerName(dto.getHandlerName());
        entity.setDeptName(dto.getDeptName());
        entity.setSourceNo(dto.getSourceNo());
        entity.setAttachments(dto.getAttachments() != null ? dto.getAttachments() : 0);
        entity.setPrintCount(dto.getPrintCount() != null ? dto.getPrintCount() : 0);
        entity.setPrepBy(dto.getPrepBy());
        entity.setPrepAt(LocalDateTime.now());
        entity.setStatus("draft");
        entity.setTotalDebit(totalDebit);
        entity.setTotalCredit(totalCredit);
        entity.setRemark(dto.getRemark());

        // 保存凭证
        voucherMapper.insert(entity);

        // 创建凭证明细
        for (VoucherItemDTO itemDTO : itemDTOs) {
            voucherItemMapper.insert(buildItem(entity.getId(), itemDTO));
        }

        log.info("创建凭证: voucherNo={}, debit={}, credit={}", voucherNo, totalDebit, totalCredit);
        return toDTO(entity);
    }

    /**
     * P0 红线：校验会计期间未被关闭。已关闭期间严禁新增凭证。
     * 期间不存在（未建账期）视为未关闭放行。
     */
    private void assertPeriodOpen(Integer fiscalYear, Integer fiscalPeriod) {
        if (fiscalYear == null || fiscalPeriod == null) {
            return;
        }
        String periodCode = String.format("%04d-%02d", fiscalYear, fiscalPeriod);
        accountingPeriodMapper.findByPeriodCode(currentTenantId(), periodCode)
                .ifPresent(p -> {
                    if (p.getStatus() != null && p.getStatus() == 0) {
                        throw BusinessException.badRequest("会计期间已关闭，禁止新增/修改凭证: " + periodCode);
                    }
                });
    }

    @Override
    @Transactional
    public VoucherDTO update(Long id, VoucherDTO dto) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw BusinessException.notFound("凭证不存在: " + id);
        }
        if (!"draft".equals(voucher.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的凭证才能修改，当前状态: " + voucher.getStatus());
        }
        // P0 红线：已关闭会计期间禁止新增/修改凭证
        assertPeriodOpen(voucher.getFiscalYear(), voucher.getFiscalPeriod());

        // 校验借贷平衡
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<VoucherItemDTO> itemDTOs = dto.getItems() != null ? dto.getItems() : new ArrayList<>();
        for (VoucherItemDTO item : itemDTOs) {
            totalDebit = totalDebit.add(item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO);
            totalCredit = totalCredit.add(item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO);
        }
        if (itemDTOs.isEmpty()) {
            throw BusinessException.badRequest("凭证分录不能为空");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw BusinessException.badRequest("借方金额合计与贷方金额合计不平: debit=" + totalDebit + ", credit=" + totalCredit);
        }

        // 更新头字段（凭证号不变）
        voucher.setVoucherDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : voucher.getVoucherDate());
        voucher.setFiscalYear(dto.getFiscalYear() != null ? dto.getFiscalYear() : voucher.getFiscalYear());
        voucher.setFiscalPeriod(dto.getFiscalPeriod() != null ? dto.getFiscalPeriod() : voucher.getFiscalPeriod());
        voucher.setVoucherType(StringUtils.hasText(dto.getVoucherType()) ? dto.getVoucherType() : voucher.getVoucherType());
        voucher.setSummary(dto.getSummary());
        voucher.setHandlerName(dto.getHandlerName());
        voucher.setDeptName(dto.getDeptName());
        voucher.setSourceNo(dto.getSourceNo());
        voucher.setAttachments(dto.getAttachments() != null ? dto.getAttachments() : voucher.getAttachments());
        voucher.setPrintCount(dto.getPrintCount() != null ? dto.getPrintCount() : voucher.getPrintCount());
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setRemark(dto.getRemark());
        voucherMapper.updateById(voucher);

        // 重建明细：先删后插
        voucherItemMapper.delete(new LambdaQueryWrapper<VoucherItem>().eq(VoucherItem::getVoucherId, id));
        for (VoucherItemDTO itemDTO : itemDTOs) {
            voucherItemMapper.insert(buildItem(id, itemDTO));
        }

        List<VoucherItem> items = voucherItemMapper.findByVoucherId(id);
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    public VoucherDTO getById(Long id) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw BusinessException.notFound("凭证不存在: " + id);
        }
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(id);
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    public VoucherDTO getByVoucherNo(String voucherNo) {
        Voucher voucher = voucherMapper.findByVoucherNo(voucherNo)
                .orElseThrow(() -> BusinessException.notFound("凭证不存在: " + voucherNo));
        List<VoucherItem> items = voucherItemMapper.findByVoucherId(voucher.getId());
        voucher.setItems(items);
        return toDTO(voucher);
    }

    @Override
    public IPage<VoucherDTO> list(VoucherQuery query, Page<VoucherDTO> page) {
        VoucherQuery q = query != null ? query : new VoucherQuery();
        LambdaQueryWrapper<Voucher> wrapper = buildWrapper(q);
        wrapper.orderByDesc(Voucher::getVoucherDate).orderByDesc(Voucher::getId);

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
            throw BusinessException.notFound("凭证不存在: " + id);
        }

        if (!"draft".equals(voucher.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的凭证才能审核，当前状态: " + voucher.getStatus());
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
            throw BusinessException.notFound("凭证不存在: " + id);
        }

        if (!"audited".equals(voucher.getStatus())) {
            throw BusinessException.badRequest("只有已审核状态的凭证才能过账，当前状态: " + voucher.getStatus());
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
            throw BusinessException.notFound("凭证不存在: " + id);
        }

        List<VoucherItem> originalItems = voucherItemMapper.findByVoucherId(id);

        // 创建冲销凭证
        Voucher reverseVoucher = new Voucher();
        reverseVoucher.setVoucherNo(generateVoucherNo());
        reverseVoucher.setVoucherDate(LocalDate.now());
        reverseVoucher.setFiscalYear(original.getFiscalYear());
        reverseVoucher.setFiscalPeriod(original.getFiscalPeriod());
        reverseVoucher.setVoucherType("system");
        reverseVoucher.setSummary("冲销凭证: " + reason);
        reverseVoucher.setSourceNo(original.getVoucherNo());
        reverseVoucher.setAttachments(0);
        reverseVoucher.setPrintCount(0);
        reverseVoucher.setPrepBy(original.getPostBy());
        reverseVoucher.setPrepAt(LocalDateTime.now());
        reverseVoucher.setStatus("posted");
        reverseVoucher.setTotalDebit(original.getTotalCredit());
        reverseVoucher.setTotalCredit(original.getTotalDebit());
        reverseVoucher.setRemark("冲销凭证: " + reason);

        voucherMapper.insert(reverseVoucher);

        // 创建冲销明细（借贷方向相反）
        for (VoucherItem item : originalItems) {
            VoucherItem reverseItem = new VoucherItem();
            reverseItem.setVoucherId(reverseVoucher.getId());
            reverseItem.setSummary("冲销: " + item.getSummary());
            reverseItem.setSubjectId(item.getSubjectId());
            reverseItem.setSubjectCode(item.getSubjectCode());
            reverseItem.setSubjectName(item.getSubjectName());
            reverseItem.setDetailSubject(item.getDetailSubject());
            // 借贷互换
            reverseItem.setDebitAmount(item.getCreditAmount());
            reverseItem.setCreditAmount(item.getDebitAmount());
            reverseItem.setSourceType(item.getSourceType());
            reverseItem.setSourceId(item.getSourceId());
            reverseItem.setSourceNo(item.getSourceNo());
            voucherItemMapper.insert(reverseItem);
        }

        // 冲销凭证立即过账到分类账
        List<VoucherItem> reverseItems = voucherItemMapper.findByVoucherId(reverseVoucher.getId());
        reverseVoucher.setItems(reverseItems);
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
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Voucher voucher = voucherMapper.selectById(id);
            if (voucher == null) {
                throw BusinessException.notFound("凭证不存在: " + id);
            }
            if (!"draft".equals(voucher.getStatus())) {
                throw BusinessException.badRequest("只有草稿状态的凭证才能删除，当前状态: " + voucher.getStatus() + ", voucherNo: " + voucher.getVoucherNo());
            }
        }
        voucherMapper.deleteBatchIds(ids);
        log.info("批量删除凭证: ids={}", ids);
    }

    @Override
    public List<VoucherDTO> exportList(VoucherQuery query) {
        VoucherQuery q = query != null ? query : new VoucherQuery();
        LambdaQueryWrapper<Voucher> wrapper = buildWrapper(q);
        wrapper.orderByDesc(Voucher::getVoucherDate).orderByDesc(Voucher::getId);

        List<Voucher> entities = voucherMapper.selectList(wrapper);
        return entities.stream().map(this::toDTOWithItems).collect(Collectors.toList());
    }

    /**
     * 生成 KJPZ-YYYYMMDD-序号 格式凭证编号
     */
    @Override
    public String generateVoucherNo() {
        String prefix = "KJPZ-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = voucherMapper.selectCount(new LambdaQueryWrapper<Voucher>().likeRight(Voucher::getVoucherNo, prefix));
        long seq = (count != null ? count : 0L) + 1;
        return prefix + String.format("%03d", seq);
    }

    @Override
    public String nextNo() {
        return generateVoucherNo();
    }

    // ======== 私有方法 ========

    private VoucherItem buildItem(Long voucherId, VoucherItemDTO dto) {
        VoucherItem item = new VoucherItem();
        item.setVoucherId(voucherId);
        item.setSummary(dto.getSummary());
        item.setSubjectId(dto.getSubjectId());
        item.setSubjectCode(dto.getSubjectCode());
        item.setSubjectName(dto.getSubjectName());
        item.setSubjectFullName(dto.getSubjectFullName());
        item.setDetailSubject(dto.getDetailSubject());
        item.setDebitAmount(dto.getDebitAmount() != null ? dto.getDebitAmount() : BigDecimal.ZERO);
        item.setCreditAmount(dto.getCreditAmount() != null ? dto.getCreditAmount() : BigDecimal.ZERO);
        item.setSourceType(dto.getSourceType());
        item.setSourceId(dto.getSourceId());
        item.setSourceNo(dto.getSourceNo());
        item.setReconcileFlag(dto.getReconcileFlag() != null ? dto.getReconcileFlag() : 0);
        item.setAuxUnit(dto.getAuxUnit());
        item.setAuxDept(dto.getAuxDept());
        item.setAuxStaff(dto.getAuxStaff());
        return item;
    }

    private LambdaQueryWrapper<Voucher> buildWrapper(VoucherQuery query) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) wrapper.like(Voucher::getVoucherNo, query.getKeyword());
        if (StringUtils.hasText(query.getSourceNo())) wrapper.like(Voucher::getSourceNo, query.getSourceNo());
        if (StringUtils.hasText(query.getVoucherType())) wrapper.eq(Voucher::getVoucherType, query.getVoucherType());
        if (StringUtils.hasText(query.getSummary())) wrapper.like(Voucher::getSummary, query.getSummary());
        if (StringUtils.hasText(query.getHandlerName())) wrapper.like(Voucher::getHandlerName, query.getHandlerName());
        if (StringUtils.hasText(query.getDeptName())) wrapper.like(Voucher::getDeptName, query.getDeptName());
        if (StringUtils.hasText(query.getPrepBy())) wrapper.like(Voucher::getPrepBy, query.getPrepBy());
        if (StringUtils.hasText(query.getVerifyBy())) {
            wrapper.and(w -> w.like(Voucher::getAuditBy, query.getVerifyBy())
                    .or().like(Voucher::getPostBy, query.getVerifyBy()));
        }
        if (StringUtils.hasText(query.getStatus())) wrapper.eq(Voucher::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getStartDate())) {
            wrapper.ge(Voucher::getVoucherDate, LocalDate.parse(query.getStartDate()));
        }
        if (StringUtils.hasText(query.getEndDate())) {
            wrapper.le(Voucher::getVoucherDate, LocalDate.parse(query.getEndDate()));
        }
        // 显示红冲：默认不显示已冲销(reversed)凭证；勾选后全部显示
        if (Boolean.FALSE.equals(query.getShowRed())) {
            wrapper.ne(Voucher::getStatus, "reversed");
        }
        // 科目过滤：联查分录取凭证ID
        if (query.getSubjectId() != null) {
            List<Long> voucherIds = voucherItemMapper.selectList(
                            new LambdaQueryWrapper<VoucherItem>()
                                    .eq(VoucherItem::getSubjectId, query.getSubjectId())
                                    .select(VoucherItem::getVoucherId))
                    .stream().map(VoucherItem::getVoucherId).distinct().collect(Collectors.toList());
            if (voucherIds.isEmpty()) {
                wrapper.eq(Voucher::getId, -1L);
            } else {
                wrapper.in(Voucher::getId, voucherIds);
            }
        }
        return wrapper;
    }

    // ======== DTO <-> Entity 转换 ========

    private VoucherDTO toDTO(Voucher entity) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(entity.getId());
        dto.setVoucherNo(entity.getVoucherNo());
        dto.setVoucherDate(entity.getVoucherDate());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setFiscalPeriod(entity.getFiscalPeriod());
        dto.setVoucherType(entity.getVoucherType());
        dto.setSummary(entity.getSummary());
        dto.setHandlerName(entity.getHandlerName());
        dto.setDeptName(entity.getDeptName());
        dto.setSourceNo(entity.getSourceNo());
        dto.setAttachments(entity.getAttachments());
        dto.setPrintCount(entity.getPrintCount());
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
        dto.setSubjectFullName(entity.getSubjectFullName());
        dto.setDetailSubject(entity.getDetailSubject());
        dto.setDebitAmount(entity.getDebitAmount());
        dto.setCreditAmount(entity.getCreditAmount());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceNo(entity.getSourceNo());
        dto.setRemark(entity.getRemark());
        dto.setReconcileFlag(entity.getReconcileFlag());
        dto.setAuxUnit(entity.getAuxUnit());
        dto.setAuxDept(entity.getAuxDept());
        dto.setAuxStaff(entity.getAuxStaff());
        return dto;
    }
}
