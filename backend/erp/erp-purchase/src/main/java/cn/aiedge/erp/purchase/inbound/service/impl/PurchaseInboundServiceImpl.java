package cn.aiedge.erp.purchase.inbound.service.impl;

import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.inbound.dto.PurchaseInboundQuery;
import org.springframework.context.ApplicationEventPublisher;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import cn.aiedge.erp.purchase.inbound.enums.InboundStatus;
import cn.aiedge.erp.purchase.inbound.mapper.InboundNameLookupMapper;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundItemMapper;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundMapper;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.service.integration.PurchaseAccountingService;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.quality.service.QualityInspectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseInboundServiceImpl extends ServiceImpl<PurchaseInboundMapper, PurchaseInbound> implements PurchaseInboundService {

    private final PurchaseInboundItemMapper inboundItemMapper;
    private final StockService stockService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PurchaseAccountingService purchaseAccountingService;
    private final QualityInspectionService qualityInspectionService;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InboundNameLookupMapper nameLookupMapper;

    @Override
    public PurchaseInbound getByInboundNo(String inboundNo) {
        return lambdaQuery()
                .eq(PurchaseInbound::getInboundNo, inboundNo)
                .eq(PurchaseInbound::getDeleted, 0)
                .one();
    }

    @Override
    public Page<PurchaseInbound> pageList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseInbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseInbound::getInboundNo, keyword)
                    .or().like(PurchaseInbound::getOrderNo, keyword)
                    .or().like(PurchaseInbound::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseInbound::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseInbound::getOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseInbound::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseInbound::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseInbound::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<PurchaseInbound> pageList(PurchaseInboundQuery q) {
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseInbound::getDeleted, 0);
        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(PurchaseInbound::getInboundNo, q.getKeyword())
                    .or().like(PurchaseInbound::getOrderNo, q.getKeyword())
                    .or().like(PurchaseInbound::getSupplierName, q.getKeyword()));
        }
        wrapper.like(hasText(q.getInboundNo()), PurchaseInbound::getInboundNo, q.getInboundNo());
        wrapper.like(hasText(q.getSupplierName()), PurchaseInbound::getSupplierName, q.getSupplierName());
        wrapper.like(hasText(q.getOrderNo()), PurchaseInbound::getOrderNo, q.getOrderNo());
        wrapper.like(hasText(q.getPurchaserName()), PurchaseInbound::getPurchaserName, q.getPurchaserName());
        wrapper.like(hasText(q.getDepartmentName()), PurchaseInbound::getDepartmentName, q.getDepartmentName());
        wrapper.like(hasText(q.getCreateByName()), PurchaseInbound::getCreateByName, q.getCreateByName());
        wrapper.like(hasText(q.getPosterName()), PurchaseInbound::getPosterName, q.getPosterName());
        wrapper.like(hasText(q.getApprovedByName()), PurchaseInbound::getApprovedByName, q.getApprovedByName());
        wrapper.like(hasText(q.getWarehouseName()), PurchaseInbound::getWarehouseName, q.getWarehouseName());
        wrapper.like(hasText(q.getRemark()), PurchaseInbound::getRemark, q.getRemark());
        wrapper.like(hasText(q.getExtText1()), PurchaseInbound::getExtText1, q.getExtText1());
        wrapper.like(hasText(q.getExtText2()), PurchaseInbound::getExtText2, q.getExtText2());
        wrapper.like(hasText(q.getExtText3()), PurchaseInbound::getExtText3, q.getExtText3());
        wrapper.eq(q.getStatus() != null, PurchaseInbound::getStatus, q.getStatus());
        wrapper.eq(q.getSettleStatus() != null, PurchaseInbound::getSettleStatus, q.getSettleStatus());
        wrapper.eq(q.getExtNum1() != null, PurchaseInbound::getExtNum1, q.getExtNum1());
        wrapper.eq(q.getExtNum2() != null, PurchaseInbound::getExtNum2, q.getExtNum2());
        wrapper.eq(q.getPrintCount() != null, PurchaseInbound::getPrintCount, q.getPrintCount());
        wrapper.eq(q.getSupplierId() != null, PurchaseInbound::getSupplierId, q.getSupplierId());
        wrapper.eq(q.getOrderId() != null, PurchaseInbound::getOrderId, q.getOrderId());
        wrapper.eq(q.getWarehouseId() != null, PurchaseInbound::getWarehouseId, q.getWarehouseId());
        wrapper.ge(q.getStartDate() != null, PurchaseInbound::getInboundDate, q.getStartDate());
        wrapper.le(q.getEndDate() != null, PurchaseInbound::getInboundDate, q.getEndDate());
        wrapper.orderByDesc(PurchaseInbound::getCreateTime);
        return page(new Page<>(q.getPageNum(), q.getPageSize()), wrapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public List<PurchaseInbound> exportList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status) {
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseInbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseInbound::getInboundNo, keyword)
                    .or().like(PurchaseInbound::getOrderNo, keyword)
                    .or().like(PurchaseInbound::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseInbound::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseInbound::getOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseInbound::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseInbound::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseInbound::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<PurchaseInbound> listBySupplierId(Long supplierId) {
        return baseMapper.selectBySupplierId(supplierId);
    }

    @Override
    public List<PurchaseInbound> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateInboundNo() {
        String prefix = "PI";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseInbound::getInboundNo, prefix + dateStr)
                .eq(PurchaseInbound::getDeleted, 0)
                .orderByDesc(PurchaseInbound::getInboundNo)
                .last("LIMIT 1");
        PurchaseInbound lastInbound = getOne(wrapper);
        int seq = 1;
        if (lastInbound != null) {
            String lastNo = lastInbound.getInboundNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound createInbound(PurchaseInbound inbound, List<PurchaseInboundItem> items) {
        inbound.setInboundNo(generateInboundNo());
        inbound.setStatus(InboundStatus.DRAFT.getCode());
        inbound.setInboundDate(LocalDate.now());
        inbound.setInboundType(1);
        inbound.setTotalQuantity(BigDecimal.ZERO);
        inbound.setTotalAmount(BigDecimal.ZERO);
        inbound.setTaxAmount(BigDecimal.ZERO);
        inbound.setTotalAmountWithTax(BigDecimal.ZERO);
        save(inbound);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                PurchaseInboundItem item = items.get(i);
                item.setInboundId(inbound.getId());
                item.setLineNo(i + 1);
                item.setTenantId(inbound.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setInboundQuantity(BigDecimal.ZERO);
                calculateItemAmounts(item);
                inboundItemMapper.insert(item);
            }
        }
        calculateTotals(inbound.getId());
        return getById(inbound.getId());
    }

    /**
     * 由采购订单生成采购入库单（收货动作）
     *
     * <p>原实现只建了「空头」入库单（无明细、无待收数量），收货工作台点完收货得到的是一张
     * 没有商品行的草稿单，必须人工再录一遍——本次补齐为**真实收货**：</p>
     * <ol>
     *   <li>带出单据头快照（订单号/供应商/仓库/经手人/部门，取值口径与《采购单据查询》一致）；</li>
     *   <li>带出**待收**明细：仅取「订货数量 − 已收数量 &gt; 0」的行，入库数量口径 = 本次待收数量；</li>
     *   <li>入库单落 `DRAFT`，后续在《采购入库单》确认收货/质检/记账（库存写入仍走既有链路，不重复实现）。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound createFromOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("采购订单不存在: " + orderId);
        }
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderId(orderId);
        inbound.setOrderNo(order.getOrderNo());
        inbound.setSupplierId(order.getSupplierId());
        inbound.setSupplierName(nameLookupMapper.findSupplierNameByOrder(orderId));
        inbound.setWarehouseId(order.getWarehouseId() != null ? order.getWarehouseId() : 1L);
        inbound.setWarehouseName(nameLookupMapper.findWarehouseName(inbound.getWarehouseId()));
        inbound.setPurchaserId(order.getPurchaserId());
        if (order.getPurchaserId() != null) {
            inbound.setPurchaserName(nameLookupMapper.findUserName(order.getPurchaserId()));
        }
        inbound.setDepartmentId(order.getDeptId());
        if (order.getDeptId() != null) {
            inbound.setDepartmentName(nameLookupMapper.findDeptName(order.getDeptId()));
        }
        inbound.setInboundDate(LocalDate.now());
        inbound.setInboundType(1);

        // 待收明细：订货数量 - 已收数量 > 0 才带出（已全部收完的行不再生成）
        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        List<PurchaseInboundItem> items = new ArrayList<>();
        for (PurchaseOrderItem oi : orderItems) {
            BigDecimal ordered = oi.getQuantity() == null ? BigDecimal.ZERO : oi.getQuantity();
            // 已收口径取 received_quantity（《采购明细查询》同口径；received_quantity_detail 为历史死列，全项目无写入方）
            BigDecimal received = oi.getReceivedQuantity() == null ? BigDecimal.ZERO : oi.getReceivedQuantity();
            BigDecimal pending = ordered.subtract(received);
            if (pending.signum() <= 0) {
                continue;
            }
            PurchaseInboundItem item = new PurchaseInboundItem();
            item.setProductId(oi.getProductId());
            item.setProductCode(oi.getProductCode());
            item.setProductName(oi.getProductName());
            item.setProductSpec(oi.getSpecification());
            item.setProductUnit(oi.getUnit());
            item.setOrderItemId(oi.getId());
            item.setOrderQuantity(pending);
            item.setUnitPrice(oi.getUnitPrice());
            item.setTaxRate(oi.getTaxRate() == null ? BigDecimal.ZERO : oi.getTaxRate());
            item.setRemark(oi.getRemark());
            items.add(item);
        }
        log.info("由采购订单生成入库单: orderId={}, orderNo={}, 待收明细 {} 行", orderId, order.getOrderNo(), items.size());
        return createInbound(inbound, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importOrders(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new cn.aiedge.common.exception.BusinessException("导入文件不能为空");
        }
        int count = 0;
        try (var inputStream = file.getInputStream()) {
            var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                var supplierCell = row.getCell(0);
                var warehouseCell = row.getCell(1);
                var productCell = row.getCell(2);
                var qtyCell = row.getCell(3);
                var priceCell = row.getCell(4);
                if (productCell == null || productCell.getStringCellValue().trim().isEmpty()) continue;
                PurchaseInbound inbound = new PurchaseInbound();
                if (supplierCell != null) inbound.setSupplierName(supplierCell.getStringCellValue());
                if (warehouseCell != null) inbound.setWarehouseName(warehouseCell.getStringCellValue());
                inbound.setTenantId(1L);
                PurchaseInboundItem item = new PurchaseInboundItem();
                item.setProductName(productCell.getStringCellValue());
                item.setOrderQuantity(qtyCell != null ? BigDecimal.valueOf(qtyCell.getNumericCellValue()) : BigDecimal.ZERO);
                item.setUnitPrice(priceCell != null ? BigDecimal.valueOf(priceCell.getNumericCellValue()) : BigDecimal.ZERO);
                item.setTaxRate(BigDecimal.ZERO);
                try {
                    createInbound(inbound, List.of(item));
                    count++;
                } catch (Exception e) {
                    log.warn("导入第{}行失败: {}", i, e.getMessage());
                }
            }
            workbook.close();
        } catch (cn.aiedge.common.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new cn.aiedge.common.exception.BusinessException("导入文件解析失败: " + e.getMessage());
        }
        return count;
    }

    @Override
    public void batchPrint(List<Long> ids, String template) {
        if (ids == null || ids.isEmpty()) {
            throw new cn.aiedge.common.exception.BusinessException("请选择要打印的入库单");
        }
        for (Long id : ids) {
            PurchaseInbound inbound = baseMapper.selectById(id);
            if (inbound == null) continue;
            int pc = (inbound.getPrintCount() != null ? inbound.getPrintCount() : 0) + 1;
            PurchaseInbound upd = new PurchaseInbound();
            upd.setId(id);
            upd.setPrintCount(pc);
            baseMapper.updateById(upd);
        }
        log.info("批量打印 {} 条入库单，模板: {}", ids.size(), template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound updateInbound(Long inboundId, PurchaseInbound inbound, List<PurchaseInboundItem> items) {
        PurchaseInbound existing = getById(inboundId);
        if (existing == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (existing.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以修改");
        }
        inbound.setId(inboundId);
        updateById(inbound);
        if (items != null) {
            List<PurchaseInboundItem> existingItems = getItems(inboundId);
            for (PurchaseInboundItem oldItem : existingItems) {
                inboundItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                PurchaseInboundItem item = items.get(i);
                item.setInboundId(inboundId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setInboundQuantity(BigDecimal.ZERO);
                calculateItemAmounts(item);
                inboundItemMapper.insert(item);
            }
        }
        calculateTotals(inboundId);
        return getById(inboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound submitForApproval(Long inboundId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以提交审批");
        }
        inbound.setStatus(InboundStatus.PENDING_APPROVAL.getCode());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound approve(Long inboundId, Long approverId, String note) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的入库单可以审批");
        }
        inbound.setStatus(InboundStatus.APPROVED.getCode());
        inbound.setApprovedBy(approverId);
        inbound.setApprovedTime(LocalDateTime.now());
        inbound.setApprovedNote(note);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound reject(Long inboundId, String reason) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的入库单可以拒绝");
        }
        inbound.setStatus(InboundStatus.DRAFT.getCode());
        inbound.setRemark(reason);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound receive(Long inboundId, Long receiverId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的入库单可以收货");
        }
        inbound.setStatus(InboundStatus.RECEIVED.getCode());
        inbound.setReceivedBy(receiverId);
        inbound.setReceivedTime(LocalDateTime.now());
        inbound.setActualArrivalTime(LocalDateTime.now());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem receiveItem(Long itemId, BigDecimal inboundQuantity, String qualityNote) {
        PurchaseInboundItem item = inboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(item.getInboundId());
        if (inbound.getStatus() != InboundStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的入库单可以处理明细");
        }
        item.setInboundQuantity(inboundQuantity);
        item.setPendingQuantity(item.getOrderQuantity().subtract(inboundQuantity));
        item.setQualityNote(qualityNote);
        calculateItemAmounts(item);
        inboundItemMapper.updateById(item);
        calculateTotals(item.getInboundId());
        return inboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound qualityCheck(Long inboundId, Long checkerId, String result) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的入库单可以质检");
        }
        inbound.setStatus(InboundStatus.QUALITY_CHECKED.getCode());
        inbound.setQualityCheckedBy(checkerId);
        inbound.setQualityCheckedTime(LocalDateTime.now());
        inbound.setQualityCheckResult(result);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound confirmWarehouse(Long inboundId, Long confirmerId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.QUALITY_CHECKED.getCode()) {
            throw new RuntimeException("只有已质检状态的入库单可以确认入库");
        }
        // 未检不入库门禁(强制开启)：来源采购订单质检未通过(PASS/CONCESSION)则拦截
        if (StringUtils.hasText(inbound.getOrderNo())
                && !qualityInspectionService.hasPassed("PURCHASE_ORDER", inbound.getOrderNo(), null)) {
            throw new RuntimeException("该采购订单未质检通过，禁止入库，请先完成质检");
        }
        inbound.setStatus(InboundStatus.WAREHOUSE_CONFIRMED.getCode());
        inbound.setWarehouseConfirmedBy(confirmerId);
        inbound.setWarehouseConfirmedTime(LocalDateTime.now());
        updateById(inbound);
        updateStock(inboundId);

        // 业财直调：收货入库完成产生应付及库存凭证（对标Odoo bill on receipt）
        BigDecimal payableAmount = inbound.getTotalAmountWithTax() != null
                ? inbound.getTotalAmountWithTax()
                : (inbound.getTotalAmount() != null ? inbound.getTotalAmount() : BigDecimal.ZERO);
        if (inbound.getSupplierId() != null && payableAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                purchaseAccountingService.createPayableOnReceipt(
                        inbound.getId(), inbound.getInboundNo(),
                        String.valueOf(inbound.getSupplierId()), inbound.getSupplierName(),
                        payableAmount);
            } catch (Exception e) {
                log.error("采购收货自动记账失败，入库单ID={}, 原因={}", inboundId, e.getMessage(), e);
                // 记账失败不影响入库确认
            }
        }
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound complete(Long inboundId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.WAREHOUSE_CONFIRMED.getCode()) {
            throw new RuntimeException("只有已入库状态的入库单可以完成");
        }
        inbound.setStatus(InboundStatus.COMPLETED.getCode());
        inbound.setCompletedBy(inbound.getCreateBy());
        inbound.setCompletedTime(LocalDateTime.now());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound cancel(Long inboundId, String reason) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() == InboundStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的入库单不能取消");
        }
        // 已确认入库的单据库存已回写，取消前必须先回冲，避免库存虚增
        if (inbound.getStatus() >= InboundStatus.WAREHOUSE_CONFIRMED.getCode()) {
            reverseStock(inboundId);
        }
        inbound.setStatus(InboundStatus.CANCELLED.getCode());
        inbound.setRemark(reason);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long inboundId) {
        BigDecimal totalQuantity = inboundItemMapper.sumInboundQuantityByInboundId(inboundId);
        BigDecimal totalAmount = inboundItemMapper.sumLineAmountByInboundId(inboundId);
        BigDecimal taxAmount = inboundItemMapper.sumTaxAmountByInboundId(inboundId);
        BigDecimal totalAmountWithTax = inboundItemMapper.sumLineTotalByInboundId(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        inbound.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        inbound.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        inbound.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        inbound.setTotalAmountWithTax(totalAmountWithTax != null ? totalAmountWithTax : BigDecimal.ZERO);
        updateById(inbound);
    }

    @Override
    public List<PurchaseInboundItem> getItems(Long inboundId) {
        return inboundItemMapper.selectByInboundId(inboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem addItem(Long inboundId, PurchaseInboundItem item) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以添加明细");
        }
        List<PurchaseInboundItem> existingItems = getItems(inboundId);
        item.setInboundId(inboundId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(inbound.getTenantId());
        item.setPendingQuantity(item.getOrderQuantity());
        item.setInboundQuantity(BigDecimal.ZERO);
        calculateItemAmounts(item);
        inboundItemMapper.insert(item);
        calculateTotals(inboundId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem updateItem(Long itemId, PurchaseInboundItem item) {
        PurchaseInboundItem existing = inboundItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(existing.getInboundId());
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以修改明细");
        }
        item.setId(itemId);
        item.setPendingQuantity(item.getOrderQuantity());
        calculateItemAmounts(item);
        inboundItemMapper.updateById(item);
        calculateTotals(existing.getInboundId());
        return inboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        PurchaseInboundItem item = inboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(item.getInboundId());
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以删除明细");
        }
        inboundItemMapper.deleteById(itemId);
        calculateTotals(item.getInboundId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long inboundId) {
        List<PurchaseInboundItem> items = getItems(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        Long warehouseId = inbound.getWarehouseId();
        if (warehouseId == null) {
            throw new RuntimeException("入库单未指定仓库，无法回写库存");
        }
        for (PurchaseInboundItem item : items) {
            BigDecimal qty = item.getInboundQuantity();
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0 && item.getProductId() != null) {
                // TODO-P0 库存收敛：仓库入库改由 WMS InventoryService 统一过账（唯一写入口），不再直写 erp_stock
                applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                        InventoryChangeEvent.ChangeType.INCREASE, item.getProductId(), warehouseId, null,
                        null, qty, "PURCHASE_INBOUND", inboundId, inbound.getInboundNo(), null, null));
                log.info("采购入库触发库存入账事件: 入库单ID={}, 产品ID={}, 数量={}", inboundId, item.getProductId(), qty);
            }
        }
    }

    /**
     * 取消已入库单据时的库存回冲（按已入库数量扣减）
     */
    private void reverseStock(Long inboundId) {
        List<PurchaseInboundItem> items = getItems(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        Long warehouseId = inbound.getWarehouseId();
        for (PurchaseInboundItem item : items) {
            BigDecimal qty = item.getInboundQuantity();
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0 && item.getProductId() != null && warehouseId != null) {
                // 与 updateStock 对称：反向回冲同样走 WMS 唯一写入口（发事件），不再直写 erp_stock。
                // 此前正向走 WMS 双写、反向只写 ERP 单轨，每次「取消入库」都会制造一次单向漂移（2026-09-20 修复）
                applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                        InventoryChangeEvent.ChangeType.DECREASE, item.getProductId(), warehouseId, null,
                        null, qty, "PURCHASE_INBOUND_CANCEL", inboundId, inbound.getInboundNo(), null, null));
                log.info("采购入库取消回冲库存(WMS 过账): 入库单ID={}, 产品ID={}, 仓库ID={}, 数量={}",
                        inboundId, item.getProductId(), warehouseId, qty);
            }
        }
    }

    private void calculateItemAmounts(PurchaseInboundItem item) {
        BigDecimal quantity = item.getInboundQuantity() != null ? item.getInboundQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setLineAmount(lineAmount);
        BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = lineAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setTaxAmount(taxAmount);
        BigDecimal lineTotal = lineAmount.add(taxAmount);
        item.setLineTotal(lineTotal);
    }
}