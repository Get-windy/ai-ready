package cn.aiedge.erp.finance.reconciliation.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.reconciliation.entity.Reconciliation;
import cn.aiedge.erp.finance.reconciliation.entity.ReconciliationItem;
import cn.aiedge.erp.finance.reconciliation.mapper.ReconciliationMapper;
import cn.aiedge.erp.finance.reconciliation.mapper.ReconciliationItemMapper;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationCreateRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationItemCreateRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationUpdateRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationQueryRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationVO;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationItemVO;
import cn.aiedge.erp.finance.reconciliation.service.IReconciliationService;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustSaveDTO;
import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjustItem;
import cn.aiedge.erp.finance.arapadjust.service.ArApAdjustService;
import cn.aiedge.erp.finance.model.entity.Receivable;
import cn.aiedge.erp.finance.model.entity.Payable;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.mapper.ReceivableMapper;
import cn.aiedge.erp.finance.mapper.PayableMapper;
import cn.aiedge.erp.finance.mapper.FinanceAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对账记录服务实现
 */
@Service
@Transactional
public class ReconciliationServiceImpl extends ServiceImpl<ReconciliationMapper, Reconciliation> implements IReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    /** 应收/应付查询的非坏账、非核销状态 */
    private static final List<String> OPEN_STATUSES = Arrays.asList("normal", "overdue");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationItemMapper reconciliationItemMapper;
    private final ReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final FinanceAccountMapper financeAccountMapper;
    private final ArApAdjustService arApAdjustService;

    public ReconciliationServiceImpl(ReconciliationMapper reconciliationMapper, ReconciliationItemMapper reconciliationItemMapper,
                                     ReceivableMapper receivableMapper, PayableMapper payableMapper,
                                     FinanceAccountMapper financeAccountMapper, ArApAdjustService arApAdjustService) {
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationItemMapper = reconciliationItemMapper;
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
        this.financeAccountMapper = financeAccountMapper;
        this.arApAdjustService = arApAdjustService;
    }

    @Override
    public Long createReconciliation(ReconciliationCreateRequest request) {
        Reconciliation reconciliation = new Reconciliation();
        BeanUtils.copyProperties(request, reconciliation);
        
        // 设置编号
        reconciliation.setReconciliationNo("REC-" + System.currentTimeMillis());
        
        // 计算差异
        BigDecimal difference = request.getSystemBalance().subtract(request.getActualBalance());
        reconciliation.setDifference(difference.abs());
        
        // 设置状态
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            reconciliation.setStatus(1); // 已对账
        } else {
            reconciliation.setStatus(2); // 有差异
        }
        
        // 设置对账日期
        if (reconciliation.getStatus() == 1) {
            reconciliation.setReconciliationDate(LocalDate.now());
        }
        
        // 设置租户ID和创建信息
        reconciliation.setTenantId(getCurrentTenantId());
        reconciliation.setCreateBy(getCurrentUser());
        reconciliation.setCreateTime(LocalDateTime.now());
        
        reconciliationMapper.insert(reconciliation);
        
        // 创建对账明细项
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ReconciliationItemCreateRequest itemRequest : request.getItems()) {
                createReconciliationItem(reconciliation.getId(), itemRequest);
            }
        }
        
        return reconciliation.getId();
    }

    @Override
    public void updateReconciliation(ReconciliationUpdateRequest request) {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setId(request.getId());
        reconciliation.setDifferenceReason(request.getDifferenceReason());
        reconciliation.setHandlerId(request.getHandlerId());
        reconciliation.setHandlerName(request.getHandlerName());
        reconciliation.setRemark(request.getRemark());
        
        reconciliationMapper.updateById(reconciliation);
    }

    @Override
    public Page<ReconciliationVO> pageReconciliations(ReconciliationQueryRequest request) {
        LambdaQueryWrapper<Reconciliation> wrapper = Wrappers.lambdaQuery(Reconciliation.class)
                .like(request.getReconciliationNo() != null, Reconciliation::getReconciliationNo, request.getReconciliationNo())
                .eq(request.getReconciliationType() != null, Reconciliation::getReconciliationType, request.getReconciliationType())
                .eq(request.getTargetId() != null, Reconciliation::getTargetId, request.getTargetId())
                .like(request.getTargetName() != null, Reconciliation::getTargetName, request.getTargetName())
                .eq(request.getStatus() != null, Reconciliation::getStatus, request.getStatus())
                .ge(request.getStartDateStart() != null, Reconciliation::getStartDate, request.getStartDateStart())
                .le(request.getStartDateEnd() != null, Reconciliation::getStartDate, request.getStartDateEnd())
                .ge(request.getEndDateStart() != null, Reconciliation::getEndDate, request.getEndDateStart())
                .le(request.getEndDateEnd() != null, Reconciliation::getEndDate, request.getEndDateEnd())
                .eq(Reconciliation::getTenantId, getCurrentTenantId())
                .orderByDesc(Reconciliation::getCreateTime);

        Page<Reconciliation> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Reconciliation> resultPage = reconciliationMapper.selectPage(page, wrapper);

        Page<ReconciliationVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ReconciliationVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ReconciliationVO getReconciliationById(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            return null;
        }
        return convertToVO(reconciliation);
    }

    @Override
    public void deleteReconciliation(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation != null && reconciliation.getTenantId().equals(getCurrentTenantId())) {
            // 删除关联的对账明细项
            LambdaQueryWrapper<ReconciliationItem> itemWrapper = Wrappers.lambdaQuery(ReconciliationItem.class)
                    .eq(ReconciliationItem::getReconciliationId, id);
            reconciliationItemMapper.delete(itemWrapper);
            
            // 删除对账记录
            reconciliationMapper.deleteById(id);
        }
    }

    @Override
    public void reconcile(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            throw BusinessException.notFound("对账记录不存在");
        }
        
        // 计算差异
        BigDecimal difference = reconciliation.getSystemBalance().subtract(reconciliation.getActualBalance());
        
        // 更新状态
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            reconciliation.setStatus(1); // 已对账
            reconciliation.setReconciliationDate(LocalDate.now());
        } else {
            reconciliation.setStatus(2); // 有差异
        }
        
        reconciliation.setDifference(difference.abs());
        
        reconciliationMapper.updateById(reconciliation);
    }

    @Override
    public void handleDifference(Long id, String differenceReason) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            throw BusinessException.notFound("对账记录不存在");
        }

        // P1 差异闭环：客户/供应商差异下推《应收应付调整》（记账经凭证，调整应收/应付余额）
        BigDecimal signedDiff = nvl(reconciliation.getSystemBalance()).subtract(nvl(reconciliation.getActualBalance()));
        if ("CUSTOMER".equals(reconciliation.getReconciliationType())
                || "SUPPLIER".equals(reconciliation.getReconciliationType())) {
            if (signedDiff.compareTo(ZERO) != 0) {
                pushDownAdjust(reconciliation, signedDiff);
            }
        } else if (signedDiff.compareTo(ZERO) != 0) {
            // 银行差异：不涉及应收/应付，仅登记原因结案（避免误用调整单）
            log.warn("[对账管理] 银行对账差异，不生成应收应付调整单: id={}", id);
        }

        // 结案：登记差异原因 + 处理人
        reconciliation.setDifferenceReason(differenceReason);
        reconciliation.setHandlerId(getCurrentUser());
        reconciliation.setHandlerName(getCurrentUserName());
        reconciliation.setStatus(1); // 差异已处理，状态设为已对账
        reconciliation.setReconciliationDate(LocalDate.now());

        reconciliationMapper.updateById(reconciliation);
    }

    /**
     * 按对账类型与差异符号决定调整方向并下推《应收应付调整》。
     *  客户：系统应收 > 实际确认 → 应收减少(2)；系统应收 < 实际确认 → 应收增加(1)。
     *  供应商：系统应付 > 实际确认 → 应付减少(4)；系统应付 < 实际确认 → 应付增加(3)。
     */
    private void pushDownAdjust(Reconciliation reconciliation, BigDecimal signedDiff) {
        boolean isCustomer = "CUSTOMER".equals(reconciliation.getReconciliationType());
        boolean positive = signedDiff.compareTo(ZERO) > 0;
        Integer direction;
        if (isCustomer) {
            direction = positive ? 2 : 1; // 应收减少 / 应收增加
        } else {
            direction = positive ? 4 : 3; // 应付减少 / 应付增加
        }
        String partnerType = isCustomer ? "customer" : "supplier";

        ArApAdjustSaveDTO dto = new ArApAdjustSaveDTO();
        dto.setDocDate(reconciliation.getReconciliationDate() != null ? reconciliation.getReconciliationDate() : LocalDate.now());
        dto.setDirection(direction);
        dto.setPartnerType(partnerType);
        dto.setPartnerId(reconciliation.getTargetId());
        dto.setPartnerName(reconciliation.getTargetName());
        dto.setSummary("对账差异处理 - " + reconciliation.getReconciliationNo());
        dto.setRemark(reconciliation.getRemark());

        ArApAdjustItem item = new ArApAdjustItem();
        item.setSubjectCode("1901"); // 待处理财产损溢
        item.setSubjectName("待处理财产损溢");
        item.setAmount(signedDiff.abs());
        item.setRemark("对账差异 - " + reconciliation.getReconciliationNo());
        dto.setItems(java.util.Collections.singletonList(item));

        cn.aiedge.erp.finance.arapadjust.entity.ArApAdjust adjust = arApAdjustService.saveDraft(dto);
        cn.aiedge.erp.finance.arapadjust.entity.ArApAdjust posted =
                arApAdjustService.confirm(adjust.getId(), getCurrentUserId(), getCurrentUserName());
        String downPushNo = (posted != null && posted.getDocNo() != null) ? posted.getDocNo() : adjust.getDocNo();
        reconciliation.setRemark(appendRemark(reconciliation.getRemark(), "下推调整单:" + downPushNo));
        log.info("[对账管理] 差异下推应收应付调整成功: reconNo={}, adjustNo={}", reconciliation.getReconciliationNo(), downPushNo);
    }

    @Override
    public BigDecimal getBalance(String reconciliationType, Long targetId) {
        if (targetId == null) {
            return ZERO;
        }
        if ("BANK".equals(reconciliationType)) {
            FinanceAccount account = financeAccountMapper.selectById(targetId);
            return account != null ? nvl(account.getBalance()) : ZERO;
        } else if ("CUSTOMER".equals(reconciliationType)) {
            List<Receivable> list = receivableMapper.selectList(new LambdaQueryWrapper<Receivable>()
                    .eq(Receivable::getCustomerId, String.valueOf(targetId))
                    .in(Receivable::getStatus, OPEN_STATUSES));
            return list.stream().map(r -> nvl(r.getRemainingAmount())).reduce(ZERO, BigDecimal::add);
        } else if ("SUPPLIER".equals(reconciliationType)) {
            List<Payable> list = payableMapper.selectList(new LambdaQueryWrapper<Payable>()
                    .eq(Payable::getSupplierId, String.valueOf(targetId))
                    .in(Payable::getStatus, OPEN_STATUSES));
            return list.stream().map(p -> nvl(p.getRemainingAmount())).reduce(ZERO, BigDecimal::add);
        }
        return ZERO;
    }

    private ReconciliationVO convertToVO(Reconciliation reconciliation) {
        ReconciliationVO vo = new ReconciliationVO();
        BeanUtils.copyProperties(reconciliation, vo);
        
        // 获取对账明细项
        LambdaQueryWrapper<ReconciliationItem> itemWrapper = Wrappers.lambdaQuery(ReconciliationItem.class)
                .eq(ReconciliationItem::getReconciliationId, reconciliation.getId())
                .orderByAsc(ReconciliationItem::getCreateTime);
        
        List<ReconciliationItem> items = reconciliationItemMapper.selectList(itemWrapper);
        List<ReconciliationItemVO> itemVOs = items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
        
        vo.setItems(itemVOs);
        
        return vo;
    }

    private ReconciliationItemVO convertItemToVO(ReconciliationItem item) {
        ReconciliationItemVO vo = new ReconciliationItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private void createReconciliationItem(Long reconciliationId, ReconciliationItemCreateRequest request) {
        ReconciliationItem item = new ReconciliationItem();
        BeanUtils.copyProperties(request, item);
        
        item.setReconciliationId(reconciliationId);
        item.setTenantId(getCurrentTenantId());
        item.setCreateBy(getCurrentUser());
        item.setCreateTime(LocalDateTime.now());
        
        // 计算差异
        BigDecimal difference = request.getSystemAmount().subtract(request.getActualAmount());
        item.setDifference(difference.abs());
        
        // 设置是否匹配
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            item.setMatched(true);
        } else {
            item.setMatched(false);
        }
        
        reconciliationItemMapper.insert(item);
    }

    private Long getCurrentTenantId() {
        // 获取当前租户ID，这里需要根据实际的租户管理实现来获取
        return 1L; // 临时实现，实际项目中需要正确获取租户ID
    }

    private String getCurrentUser() {
        // 获取当前用户名
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString();
        }
        return "system";
    }

    private String getCurrentUserName() {
        // 获取当前用户名字
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString(); // 实际项目中可能需要从用户服务获取真实姓名
        }
        return "系统";
    }

    private Long getCurrentUserId() {
        if (StpUtil.isLogin()) {
            try {
                return Long.valueOf(StpUtil.getLoginIdAsString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private String appendRemark(String existing, String add) {
        if (existing == null || existing.isEmpty()) {
            return add;
        }
        return existing + "；" + add;
    }

    @Override
    public java.util.Map<String, Object> getStats() {
        Long tenantId = getCurrentTenantId();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 银行对账待处理数（status=0 且 reconciliationType='BANK'）
        long bankPending = this.count(Wrappers.<Reconciliation>lambdaQuery()
                .eq(Reconciliation::getTenantId, tenantId)
                .eq(Reconciliation::getReconciliationType, "BANK")
                .eq(Reconciliation::getStatus, 0));

        // 客户对账待处理数
        long customerPending = this.count(Wrappers.<Reconciliation>lambdaQuery()
                .eq(Reconciliation::getTenantId, tenantId)
                .eq(Reconciliation::getReconciliationType, "CUSTOMER")
                .eq(Reconciliation::getStatus, 0));

        // 供应商对账待处理数
        long supplierPending = this.count(Wrappers.<Reconciliation>lambdaQuery()
                .eq(Reconciliation::getTenantId, tenantId)
                .eq(Reconciliation::getReconciliationType, "SUPPLIER")
                .eq(Reconciliation::getStatus, 0));

        // 有差异的对账记录数（status=2）
        long differenceCount = this.count(Wrappers.<Reconciliation>lambdaQuery()
                .eq(Reconciliation::getTenantId, tenantId)
                .eq(Reconciliation::getStatus, 2));

        stats.put("bankPending", bankPending);
        stats.put("customerPending", customerPending);
        stats.put("supplierPending", supplierPending);
        stats.put("differenceCount", differenceCount);
        stats.put("totalPending", bankPending + customerPending + supplierPending);

        return stats;
    }
}
