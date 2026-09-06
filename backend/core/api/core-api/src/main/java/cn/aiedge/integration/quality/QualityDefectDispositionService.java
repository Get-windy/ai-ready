package cn.aiedge.integration.quality;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.service.BatchNumberService;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import cn.aiedge.erp.stock.service.StockDamageService;
import cn.aiedge.quality.entity.QualityInspection;
import cn.aiedge.quality.event.QualityDefectHandledEvent;
import cn.aiedge.quality.service.QualityDefectHandleService;
import cn.aiedge.quality.service.QualityInspectionService;
import cn.aiedge.wms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

/**
 * 缺陷处置联动编排（P1）。
 * 消费 {@link QualityDefectHandledEvent}（缺陷记录 handle 后发布，事件自包含产品/批次/仓库定位信息），
 * 按处置方式联动下游：RETURN→生成采购退货单（供应商/仓库经质检单→采购订单反查），
 * SCRAP→生成报损单并记账出库+冻结不合格批次；成功后回填缺陷记录下游单号。
 * 依赖方向：core-base 不依赖 erp/wms，本类位于 core-api（聚合全部依赖模块）承担跨模块编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityDefectDispositionService {

    private final QualityInspectionService inspectionService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseReturnService purchaseReturnService;
    private final StockDamageService stockDamageService;
    private final InventoryService inventoryService;
    private final QualityDefectHandleService defectHandleService;
    private final PlatformTransactionManager transactionManager;
    private final BatchNumberService batchNumberService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onHandled(QualityDefectHandledEvent ev) {
        if (ev == null) {
            return;
        }
        if ("RETURN".equalsIgnoreCase(ev.getHandleType())) {
            handleReturn(ev);
        } else if ("SCRAP".equalsIgnoreCase(ev.getHandleType())) {
            handleScrap(ev);
        }
    }

    private void handleReturn(QualityDefectHandledEvent ev) {
        QualityInspection insp = ev.getInspectionId() != null
                ? inspectionService.get(ev.getInspectionId()) : null;
        Long supplierId = null;
        String supplierName = null;
        Long whId = ev.getWarehouseId();
        String whName = ev.getWarehouseName();
        Long poId = null;
        String poNo = ev.getBizNo();
        if (insp != null) {
            poId = insp.getBizId();
            poNo = insp.getBizNo();
            if ("PURCHASE_ORDER".equals(insp.getBizType()) && insp.getBizId() != null) {
                PurchaseOrder po = purchaseOrderService.getById(insp.getBizId());
                if (po != null) {
                    supplierId = po.getSupplierId();
                    if (whId == null) {
                        whId = po.getWarehouseId();
                    }
                }
            }
        }
        if (supplierId == null || whId == null) {
            throw new IllegalArgumentException("无法联动采购退货：来源单据缺少供应商或仓库信息");
        }
        PurchaseReturn doc = new PurchaseReturn();
        doc.setSupplierId(supplierId);
        doc.setSupplierName(supplierName);
        doc.setWarehouseId(whId);
        doc.setWarehouseName(whName);
        doc.setPurchaseOrderId(poId);
        doc.setPurchaseOrderNo(poNo);
        doc.setReturnType(1);
        doc.setReturnTypeDesc("质量退货");
        doc.setReturnDate(LocalDate.now());
        PurchaseReturnItem item = new PurchaseReturnItem();
        item.setProductId(ev.getProductId());
        item.setBatchNo(ev.getBatchNo());
        item.setReturnQuantity(ev.getHandleQuantity());
        PurchaseReturn created = purchaseReturnService.createReturn(doc, Collections.singletonList(item));
        Long uid = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        purchaseReturnService.submitForApproval(created.getId());
        purchaseReturnService.approve(created.getId(), uid, "质检不合格处理自动退货");
        purchaseReturnService.complete(created.getId());
        markBatchDefective(ev, uid, operatorName);
        defectHandleService.linkDownstream(ev.getDefectId(), created.getReturnNo(), null);
    }

    private void handleScrap(QualityDefectHandledEvent ev) {
        Long whId = ev.getWarehouseId();
        if (whId == null) {
            throw new IllegalArgumentException("无法联动报损：来源单据缺少仓库信息");
        }
        Long uid = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        StockDamage doc = new StockDamage();
        doc.setWarehouseId(whId);
        doc.setWarehouseName(ev.getWarehouseName());
        doc.setHandlerId(uid);
        doc.setHandlerName(operatorName);
        doc.setDamageDate(LocalDate.now());
        doc.setDamageCause(1);
        StockDamageItem item = new StockDamageItem();
        item.setProductId(ev.getProductId());
        item.setBatchNo(ev.getBatchNo());
        item.setQuantity(ev.getHandleQuantity());
        item.setUnitCost(BigDecimal.ZERO);
        StockDamage created = stockDamageService.createStockDamage(doc, Collections.singletonList(item));
        stockDamageService.execute(created.getId());
        if (ev.getProductId() != null && ev.getBatchNo() != null) {
            // freeze 走 WMS 库位轨，且在原事务内抛异常会污染外层 handle 事务(rollback-only)。
            // 用 REQUIRES_NEW 独立事务执行冻结，失败仅回滚冻结自身，不影响报损落单。
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            try {
                tt.execute(status -> {
                    inventoryService.freeze(ev.getProductId(), whId, null, ev.getBatchNo(),
                            ev.getHandleQuantity(), "QUALITY_DEFECT", "QUALITY_DEFECT",
                            ev.getDefectId(), uid, operatorName);
                    return null;
                });
            } catch (Exception ex) {
                // 该批次若只存在于 ERP 轨(erp_stock)、未走 WMS 库位轨(wms_inventory)则无可冻行，
                // 容错跳过，不阻断报损落单（报损链已闭环）。
                log.warn("缺陷[{}] SCRAP 冻结批次跳过(仅ERP轨库存或无库存行): {}", ev.getDefectId(), ex.getMessage());
            }
        }
        markBatchDefective(ev, uid, operatorName);
        defectHandleService.linkDownstream(ev.getDefectId(), null, created.getDamageNo());
    }

    /**
     * 不合格批次隔离标记：将关联批次在 batch_number 主数据标记为 DEFECTIVE（不合格），
     * 补齐报告 P1「冻结/隔离不合格批次」。批次主数据无该批次或标记失败均容错跳过，不阻断处置落单。
     */
    private void markBatchDefective(QualityDefectHandledEvent ev, Long uid, String operatorName) {
        log.info("缺陷[{}] 批次隔离标记开始: productId={}, batchNo={}, warehouseId={}",
                ev.getDefectId(), ev.getProductId(), ev.getBatchNo(), ev.getWarehouseId());
        if (ev.getProductId() == null || ev.getBatchNo() == null) {
            log.warn("缺陷[{}] 批次隔离缺少产品/批次，跳过", ev.getDefectId());
            return;
        }
        try {
            BatchNumber bn = batchNumberService.lambdaQuery()
                    .eq(BatchNumber::getProductId, ev.getProductId())
                    .eq(BatchNumber::getBatchNo, ev.getBatchNo())
                    .last("limit 1")
                    .one();
            if (bn != null) {
                log.info("缺陷[{}] 批次隔离命中 batchId={}", ev.getDefectId(), bn.getId());
                batchNumberService.qualityInspection(bn.getId(), "DEFECTIVE",
                        uid != null ? String.valueOf(uid) : null, operatorName);
                log.info("缺陷[{}] 批次隔离已置 DEFECTIVE", ev.getDefectId());
            } else {
                log.warn("缺陷[{}] 批次隔离未命中 batch_number 记录", ev.getDefectId());
            }
        } catch (Exception ex) {
            log.warn("缺陷[{}] 批次隔离标记跳过(异常): {}", ev.getDefectId(), ex.getMessage());
        }
    }
}
