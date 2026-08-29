package cn.aiedge.erp.purchase.util;

import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocumentDTO;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一采购单据转换工具类
 * 将采购入库单/退货单/换货单转换为统一DTO格式。
 *
 * <p>金额口径：
 * <ul>
 *   <li>金额(amount) = 主表 total_amount</li>
 *   <li>折后金额(discountedAmount) = 主表 total_amount（本系统"本单金额"即折后口径）</li>
 *   <li>优惠后金额(favorableAmount) = total_amount - discount_amount（null 安全）</li>
 *   <li>税额(taxAmount) = 主表 tax_amount</li>
 *   <li>价税合计(totalAmountWithTax) = 主表 total_amount_with_tax</li>
 *   <li>本单金额(totalAmount) = 主表 total_amount_with_tax（含税口径）</li>
 *   <li>优惠金额(discountAmount) = 主表 discount_amount</li>
 * </ul>
 * 供应商编号/联系人等字段，入库单缺失时由 Service 层经 SupplierSnapshotMapper 补全。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public final class UnifiedPurchaseConverter {

    private UnifiedPurchaseConverter() {}

    // ═══ 采购入库单 → DTO ═══
    public static List<UnifiedPurchaseDocumentDTO> convertInboundList(List<PurchaseInbound> list) {
        List<UnifiedPurchaseDocumentDTO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) return result;
        for (PurchaseInbound e : list) {
            UnifiedPurchaseDocumentDTO dto = new UnifiedPurchaseDocumentDTO();
            dto.setId(e.getId());
            dto.setDocumentType("INBOUND");
            dto.setDocumentDate(e.getInboundDate());
            dto.setDocumentNo(e.getInboundNo());
            dto.setInboundWarehouse(e.getWarehouseName());
            dto.setOutboundWarehouse(null);
            dto.setSupplierName(e.getSupplierName());
            // supplierCode/contactName/contactPhone/contactAddress/supplierRemark 由 Service 补全
            dto.setSourceOrder(e.getOrderNo());
            dto.setHandlerName(e.getPurchaserName());
            dto.setDepartmentName(e.getDepartmentName());
            dto.setSettlementStatus(settleStatusToText(e.getSettleStatus()));
            dto.setPurchaseQuantity(e.getTotalQuantity());
            dto.setAmount(e.getTotalAmount());
            dto.setDiscountedAmount(e.getTotalAmount());
            dto.setFavorableAmount(sub(e.getTotalAmount(), e.getDiscountAmount()));
            dto.setTaxAmount(e.getTaxAmount());
            dto.setTotalAmountWithTax(e.getTotalAmountWithTax());
            dto.setTotalAmount(e.getTotalAmountWithTax());
            dto.setFee(e.getFee());
            dto.setDiscountAmount(e.getDiscountAmount());
            dto.setExtNum1(e.getExtNum1());
            dto.setExtNum2(e.getExtNum2());
            dto.setExtText1(e.getExtText1());
            dto.setExtText2(e.getExtText2());
            dto.setExtText3(e.getExtText3());
            dto.setRemark(e.getRemark());
            dto.setSummary(e.getSummary());
            dto.setAttachment(e.getAttachment());
            dto.setBookkeeperName(e.getPosterName());
            dto.setCreatorName(e.getCreateByName());
            dto.setBookkeepingTime(e.getPostTime());
            dto.setCreateTime(e.getCreateTime());
            dto.setPrintCount(e.getPrintCount());
            dto.setStatus(e.getStatus());
            dto.setStatusDesc(statusText(e.getStatus()));
            dto.setSupplierId(e.getSupplierId());
            dto.setWarehouseId(e.getWarehouseId());
            dto.setHandlerId(e.getPurchaserId());
            dto.setDepartmentId(e.getDepartmentId());
            dto.setWeight(e.getWeight());
            dto.setVolume(e.getVolume());
            result.add(dto);
        }
        return result;
    }

    // ═══ 采购退货单 → DTO ═══
    public static List<UnifiedPurchaseDocumentDTO> convertReturnList(List<PurchaseReturn> list) {
        List<UnifiedPurchaseDocumentDTO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) return result;
        for (PurchaseReturn e : list) {
            UnifiedPurchaseDocumentDTO dto = new UnifiedPurchaseDocumentDTO();
            dto.setId(e.getId());
            dto.setDocumentType("RETURN");
            dto.setDocumentDate(e.getReturnDate());
            dto.setDocumentNo(e.getReturnNo());
            dto.setInboundWarehouse(null);
            dto.setOutboundWarehouse(e.getWarehouseName());
            dto.setSupplierName(e.getSupplierName());
            dto.setSupplierCode(e.getSupplierNo());
            dto.setContactName(e.getContactName());
            dto.setContactPhone(e.getContactPhone());
            dto.setContactAddress(e.getContactAddress());
            dto.setSupplierRemark(e.getSupplierRemark());
            dto.setSourceOrder(e.getPurchaseOrderNo());
            dto.setHandlerName(e.getPurchaserName());
            dto.setDepartmentName(e.getDepartmentName());
            dto.setSettlementStatus(settleStatusToText(e.getSettleStatus()));
            dto.setPurchaseQuantity(e.getTotalQuantity());
            dto.setAmount(e.getTotalAmount());
            dto.setDiscountedAmount(e.getTotalAmount());
            dto.setFavorableAmount(sub(e.getTotalAmount(), e.getDiscountAmount()));
            dto.setTaxAmount(e.getTaxAmount());
            dto.setTotalAmountWithTax(e.getTotalAmountWithTax());
            dto.setTotalAmount(e.getTotalAmountWithTax());
            dto.setFee(null);
            dto.setDiscountAmount(e.getDiscountAmount());
            dto.setExtNum1(e.getExtNum1());
            dto.setExtNum2(e.getExtNum2());
            dto.setExtText1(e.getExtText1());
            dto.setExtText2(e.getExtText2());
            dto.setExtText3(e.getExtText3());
            dto.setRemark(e.getRemark());
            dto.setSummary(e.getSummary());
            dto.setAttachment(e.getAttachment());
            dto.setBookkeeperName(e.getPosterName());
            dto.setCreatorName(e.getCreateByName());
            dto.setBookkeepingTime(e.getPostTime());
            dto.setCreateTime(e.getCreateTime());
            dto.setPrintCount(e.getPrintCount());
            dto.setStatus(e.getStatus());
            dto.setStatusDesc(statusText(e.getStatus()));
            dto.setSupplierId(e.getSupplierId());
            dto.setWarehouseId(e.getWarehouseId());
            dto.setHandlerId(e.getPurchaserId());
            dto.setDepartmentId(e.getDepartmentId());
            dto.setWeight(e.getWeight());
            dto.setVolume(e.getVolume());
            result.add(dto);
        }
        return result;
    }

    // ═══ 采购换货单 → DTO ═══
    public static List<UnifiedPurchaseDocumentDTO> convertExchangeList(List<PurchaseExchange> list) {
        List<UnifiedPurchaseDocumentDTO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) return result;
        for (PurchaseExchange e : list) {
            UnifiedPurchaseDocumentDTO dto = new UnifiedPurchaseDocumentDTO();
            dto.setId(e.getId());
            dto.setDocumentType("EXCHANGE");
            dto.setDocumentDate(e.getExchangeDate());
            dto.setDocumentNo(e.getExchangeNo());
            dto.setInboundWarehouse(e.getInWarehouseName());
            dto.setOutboundWarehouse(e.getOutWarehouseName());
            dto.setSupplierName(e.getSupplierName());
            dto.setSupplierCode(e.getSupplierCode());
            dto.setContactName(e.getContactName());
            dto.setContactPhone(e.getContactPhone());
            dto.setContactAddress(e.getContactAddress());
            dto.setSupplierRemark(e.getSupplierRemark());
            dto.setSourceOrder(e.getOriginalOrderNo());
            dto.setHandlerName(e.getHandlerName());
            dto.setDepartmentName(e.getDeptName());
            dto.setSettlementStatus(e.getSettleStatus());
            dto.setPurchaseQuantity(e.getOutQuantityTotal() != null ? e.getOutQuantityTotal() : e.getInQuantityTotal());
            dto.setAmount(e.getProductAmount());
            dto.setDiscountedAmount(e.getProductAmount());
            dto.setFavorableAmount(sub(e.getProductAmount(), e.getDiscountAmount()));
            dto.setTaxAmount(null);
            dto.setTotalAmountWithTax(e.getTotalAmount());
            dto.setTotalAmount(e.getTotalAmount());
            dto.setFee(null);
            dto.setDiscountAmount(e.getDiscountAmount());
            dto.setExtNum1(e.getExtNum1());
            dto.setExtNum2(e.getExtNum2());
            dto.setExtText1(e.getExtText1());
            dto.setExtText2(e.getExtText2());
            dto.setExtText3(e.getExtText3());
            dto.setRemark(e.getRemark());
            dto.setSummary(e.getSummary());
            dto.setAttachment(e.getAttachment());
            dto.setBookkeeperName(e.getBookkeeperName());
            dto.setCreatorName(e.getCreatedByName());
            dto.setBookkeepingTime(e.getBookkeepingTime());
            dto.setCreateTime(e.getCreateTime());
            dto.setPrintCount(e.getPrintCount());
            dto.setStatus(e.getStatus());
            dto.setStatusDesc(statusText(e.getStatus()));
            dto.setSupplierId(e.getSupplierId());
            dto.setWarehouseId(e.getInWarehouseId());
            dto.setOutWarehouseId(e.getOutWarehouseId());
            dto.setHandlerId(e.getHandlerId());
            dto.setDepartmentId(e.getDeptId());
            dto.setWeight(e.getTotalWeight());
            dto.setVolume(e.getTotalVolume());
            result.add(dto);
        }
        return result;
    }

    // ═══ 工具 ═══
    /** null 安全减法 */
    private static BigDecimal sub(BigDecimal a, BigDecimal b) {
        if (a == null) return null;
        return b == null ? a : a.subtract(b);
    }

    /** 结算状态 Integer(0/1) → 文本；String 直接返回（换货单） */
    private static String settleStatusToText(Integer status) {
        if (status == null) return "";
        return status == 1 ? "已结算" : "未结算";
    }

    /** 单据状态数字 → 文本 */
    private static String statusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审批";
            case 2: return "已审批";
            case 3: return "已下达";
            case 4: return "执行中";
            case 5: return "部分入库";
            case 6: return "已完成";
            case 7: return "已取消";
            default: return String.valueOf(status);
        }
    }
}
