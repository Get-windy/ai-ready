package cn.aiedge.erp.sale.util;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOutbound;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一销售单据转换工具类
 * 将不同类型的销售单据转换为统一的DTO格式
 */
public class UnifiedDocConverter {

    /**
     * 将销售订单列表转换为统一销售单据DTO列表
     *
     * @param orders 销售订单列表
     * @param docType 单据类型标识
     * @return 统一销售单据DTO列表
     */
    public static List<UnifiedSalesDocumentDTO> convertToUnifiedSalesDocumentDTOs(List<SaleOrder> orders, String docType) {
        List<UnifiedSalesDocumentDTO> result = new ArrayList<>();
        if (orders == null || orders.isEmpty()) {
            return result;
        }

        for (SaleOrder order : orders) {
            UnifiedSalesDocumentDTO dto = convertSaleOrderToUnifiedDTO(order, docType);
            result.add(dto);
        }

        return result;
    }

    /**
     * 将销售订单转换为统一销售单据DTO
     *
     * @param order 销售订单
     * @param docType 单据类型标识
     * @return 统一销售单据DTO
     */
    public static UnifiedSalesDocumentDTO convertSaleOrderToUnifiedDTO(SaleOrder order, String docType) {
        UnifiedSalesDocumentDTO dto = new UnifiedSalesDocumentDTO();

        // 基础字段映射
        dto.setId(order.getId());
        dto.setDocumentDate(order.getOrderDate() != null ? order.getOrderDate().atStartOfDay() : null);           // 1单据日期 - SaleOrder使用orderDate(LocalDate)，需要转换为LocalDateTime
        dto.setDocumentNo(order.getOrderNo());              // 2单据编号 - SaleOrder使用orderNo
        dto.setDocumentType(docType);                          // 3单据类型
        dto.setInboundWarehouse(null);   // 4入库仓库 - 销售订单无此字段
        dto.setOutboundWarehouse(order.getWarehouseName()); // 5出库仓库 - 销售订单对应发货仓库
        dto.setCustomerName(order.getCustomerName());          // 6客户
        dto.setCustomerCode(order.getCustomerCode());          // 7客户编号
        dto.setCustomerLevel(order.getCustomerLevel());        // 8客户级别
        dto.setReceiverName(order.getReceiverName());          // 9收货人
        dto.setReceiverPhone(order.getReceiverPhone());        // 10联系电话
        dto.setShippingAddress(order.getShippingAddress());    // 11收货地址
        dto.setExtNum1(order.getExtNum1());                    // 12表头自定义字段1(数字)
        dto.setExtNum2(order.getExtNum2());                    // 13表头自定义字段2(数字)
        dto.setExtText1(order.getExtText1());                  // 14表头自定义字段3(文本)
        dto.setExtText2(order.getExtText2());                  // 15表头自定义字段4(文本)
        dto.setExtText3(order.getExtText3());                  // 16表头自定义字段5(文本)
        dto.setBuyerRemark(order.getBuyerRemark());            // 17买家备注
        dto.setCustomerRemark(order.getCustomerRemark());      // 18客户备注
        dto.setSourceOrder(order.getOriginalOrderNo());        // 19来源订单 - 销售订单使用原始订单号
        dto.setSourceOrderDate(null);                          // 20来源订单日期 - 销售订单无此字段
        dto.setLogisticsCompany(order.getLogisticsCompany());  // 21物流公司
        dto.setTrackingNumber(order.getWaybillNo());           // 22运单号 - 销售订单使用waybillNo
        dto.setRegion(order.getRegion());                      // 23区域
        dto.setGenerationMethod(order.getGenerationMethod());  // 24产生方式
        dto.setHandlerName(order.getSalesmanName());           // 25经手人 - 销售订单使用salesmanName
        dto.setDepartmentName(order.getDeptName());            // 26部门 - 销售订单使用deptName
        dto.setSettlementStatus(order.getSettlementMethod());  // 27结算状态 - 销售订单使用结算方式
        dto.setSalesQuantity(order.getTotalQuantity());        // 28销售数量 - 销售订单使用totalQuantity
        dto.setAmount(order.getProductAmount());               // 29金额 - 销售订单使用productAmount
        dto.setDiscountedAmount(order.getBillAmount());        // 30折后金额 - 销售订单使用billAmount
        dto.setSalesRevenue(order.getBillAmount());            // 31销售收入 - 销售订单使用billAmount
        dto.setFreightPayer(null);                             // 32运费承担方 - 销售订单可能无此字段
        dto.setFreight(order.getShippingFee());                // 33运费 - 销售订单使用shippingFee
        dto.setOtherFee(order.getOtherFee());                  // 34其它费用
        dto.setRoundingAmount(null);                           // 35抹零金额 - 销售订单无此字段
        dto.setTotalAmount(order.getBillAmount());             // 36本单金额 - 销售订单使用billAmount
        dto.setPromoDiscount(order.getPromoDiscount());        // 37促销优惠
        dto.setCouponAmount(order.getCouponAmount());          // 38优惠券优惠
        dto.setDirectDiscount(order.getDirectDiscount());      // 39直接优惠
        dto.setPointsDeduction(null);                          // 40积分抵扣 - 销售订单可能无此字段
        dto.setCostAmount(null);                               // 41成本金额 - 销售订单无此字段
        dto.setGrossProfit(null);                              // 42毛利 - 销售订单无此字段
        dto.setSalesType(String.valueOf(order.getSaleType())); // 43销售类型 - 销售订单使用saleType(Integer)
        dto.setRemark(order.getRemark());                      // 44单据备注
        dto.setSummary(order.getSummary());                    // 45摘要
        dto.setAttachment(order.getAttachment());              // 46附件
        dto.setBookkeeperName(order.getCreatorName());         // 47记账人 - 销售订单使用creatorName作为制单人
        dto.setCreatorName(order.getCreatorName());            // 48制单人
        dto.setBookkeepingTime(order.getBookkeepingTime());    // 49记账时间
        dto.setCreateTime(order.getSubmitTime());              // 50制单时间 - 销售订单使用submitTime
        dto.setPrintCount(order.getPrintCount());              // 51打印次数

        // 其他字段映射
        dto.setStatus(order.getStatus());
        dto.setWarehouseId(order.getWarehouseId());
        dto.setCustomerId(order.getCustomerId());
        dto.setHandlerId(order.getSalesmanId());
        dto.setDepartmentId(order.getDeptId());
        dto.setSettlementMethod(order.getSettlementMethod());
        dto.setSourceOrderType(null);
        dto.setOrderAmount(order.getProductAmount());
        dto.setPaidAmount(order.getReceivedAmount());
        dto.setPendingAmount(null);
        dto.setAdvanceReceived(null);
        dto.setFavorableAmount(null);
        dto.setCodAmount(order.getCodAmount());
        dto.setCodStatus(null);
        dto.setDeliveryMethod(order.getDeliveryMethod());
        dto.setDeliveryDriver(order.getDriverName());
        dto.setLogisticsBranch(order.getLogisticsCompany());
        dto.setExpectedShipmentTime(order.getExpectedShipTime());
        dto.setActualShipmentTime(null);
        dto.setReceiptTime(null);
        dto.setApproverName(order.getAuditorName());
        dto.setApprovedTime(order.getAuditTime());
        dto.setCompletedTime(null);
        dto.setClosedTime(null);
        dto.setCancelledTime(null);
        dto.setCancelReason(null);
        dto.setCompletedBy(null);
        dto.setClosedBy(null);
        dto.setCancelledBy(null);
        dto.setSource(null);
        dto.setPriority(null);
        dto.setBusinessType(null);
        dto.setProjectId(null);
        dto.setProjectName(null);
        dto.setContractId(null);
        dto.setContractNo(null);
        dto.setTaxRate(null);
        dto.setTaxIncludedAmount(null);
        dto.setTaxExcludedAmount(null);
        dto.setTaxAmount(null);
        dto.setCurrency(null);
        dto.setExchangeRate(null);
        dto.setForeignCurrencyAmount(null);
        dto.setPaymentTerms(order.getSettlementMethod());
        dto.setPaymentDueDate(null);
        dto.setInvoiceNo(null);
        dto.setInvoiceDate(null);
        dto.setInvoiceStatus(null);
        dto.setInvoiceType(null);
        dto.setInvoiceTitle(null);
        dto.setTaxRegistrationNo(order.getTaxNo());
        dto.setInvoicerName(null);
        dto.setInvoiceTime(null);
        dto.setRemark2(null);
        dto.setRemark3(null);
        dto.setCustomField1(null);
        dto.setCustomField2(null);
        dto.setCustomField3(null);
        dto.setCustomField4(null);
        dto.setCustomField5(null);
        dto.setCustomNumField1(null);
        dto.setCustomNumField2(null);
        dto.setCustomNumField3(null);
        dto.setCustomNumField4(null);
        dto.setCustomNumField5(null);
        dto.setCustomDateField1(null);
        dto.setCustomDateField2(null);
        dto.setCustomDateField3(null);
        dto.setCustomDateField4(null);
        dto.setCustomDateField5(null);
        dto.setCustomBoolField1(null);
        dto.setCustomBoolField2(null);
        dto.setCustomBoolField3(null);
        dto.setCustomBoolField4(null);
        dto.setCustomBoolField5(null);
        dto.setCustomObjField1(null);
        dto.setCustomObjField2(null);
        dto.setCustomObjField3(null);
        dto.setCustomObjField4(null);
        dto.setCustomObjField5(null);

        return dto;
    }

    /**
     * 将销售出库单列表转换为统一销售单据DTO列表
     *
     * @param outbounds 销售出库单列表
     * @param docType 单据类型标识
     * @return 统一销售单据DTO列表
     */
    public static List<UnifiedSalesDocumentDTO> convertSaleOutboundToUnifiedDTOs(List<SaleOutbound> outbounds, String docType) {
        List<UnifiedSalesDocumentDTO> result = new ArrayList<>();
        if (outbounds == null || outbounds.isEmpty()) {
            return result;
        }

        for (SaleOutbound outbound : outbounds) {
            UnifiedSalesDocumentDTO dto = convertSaleOutboundToUnifiedDTO(outbound, docType);
            result.add(dto);
        }

        return result;
    }

    /**
     * 将销售出库单转换为统一销售单据DTO
     *
     * @param outbound 销售出库单
     * @param docType 单据类型标识
     * @return 统一销售单据DTO
     */
    public static UnifiedSalesDocumentDTO convertSaleOutboundToUnifiedDTO(SaleOutbound outbound, String docType) {
        UnifiedSalesDocumentDTO dto = new UnifiedSalesDocumentDTO();

        // 基础字段映射
        dto.setId(outbound.getId());
        dto.setDocumentDate(outbound.getDocumentDate());           // 1单据日期
        dto.setDocumentNo(outbound.getDocumentNo());              // 2单据编号
        dto.setDocumentType(docType);                             // 3单据类型
        dto.setInboundWarehouse(outbound.getInboundWarehouse());   // 4入库仓库
        dto.setOutboundWarehouse(outbound.getOutboundWarehouse()); // 5出库仓库
        dto.setCustomerName(outbound.getCustomerName());          // 6客户
        dto.setCustomerCode(outbound.getCustomerCode());          // 7客户编号
        dto.setCustomerLevel(outbound.getCustomerLevel());        // 8客户级别
        dto.setReceiverName(outbound.getReceiverName());          // 9收货人
        dto.setReceiverPhone(outbound.getReceiverPhone());        // 10联系电话
        dto.setShippingAddress(outbound.getShippingAddress());    // 11收货地址
        dto.setExtNum1(outbound.getExtNum1());                   // 12表头自定义字段1(数字)
        dto.setExtNum2(outbound.getExtNum2());                   // 13表头自定义字段2(数字)
        dto.setExtText1(outbound.getExtText1());                 // 14表头自定义字段3(文本)
        dto.setExtText2(outbound.getExtText2());                 // 15表头自定义字段4(文本)
        dto.setExtText3(outbound.getExtText3());                 // 16表头自定义字段5(文本)
        dto.setBuyerRemark(outbound.getBuyerRemark());           // 17买家备注
        dto.setCustomerRemark(outbound.getCustomerRemark());     // 18客户备注
        dto.setSourceOrder(outbound.getSourceOrder());           // 19来源订单
        dto.setSourceOrderDate(outbound.getSourceOrderDate());   // 20来源订单日期
        dto.setLogisticsCompany(outbound.getLogisticsCompany()); // 21物流公司
        dto.setTrackingNumber(outbound.getTrackingNumber());     // 22运单号
        dto.setRegion(outbound.getRegion());                     // 23区域
        dto.setGenerationMethod(outbound.getGenerationMethod()); // 24产生方式
        dto.setHandlerName(outbound.getHandlerName());           // 25经手人
        dto.setDepartmentName(outbound.getDepartmentName());     // 26部门
        dto.setSettlementStatus(outbound.getSettlementStatus()); // 27结算状态
        dto.setSalesQuantity(outbound.getSalesQuantity());       // 28销售数量
        dto.setAmount(outbound.getAmount());                     // 29金额
        dto.setDiscountedAmount(outbound.getDiscountedAmount()); // 30折后金额
        dto.setSalesRevenue(outbound.getSalesRevenue());         // 31销售收入
        dto.setFreightPayer(outbound.getFreightPayer());         // 32运费承担方
        dto.setFreight(outbound.getFreight());                   // 33运费
        dto.setOtherFee(outbound.getOtherFee());                 // 34其它费用
        dto.setRoundingAmount(outbound.getRoundingAmount());     // 35抹零金额
        dto.setTotalAmount(outbound.getTotalAmount());           // 36本单金额
        dto.setPromoDiscount(outbound.getPromoDiscount());       // 37促销优惠
        dto.setCouponAmount(outbound.getCouponAmount());         // 38优惠券优惠
        dto.setDirectDiscount(outbound.getDirectDiscount());     // 39直接优惠
        dto.setPointsDeduction(outbound.getPointsDeduction());   // 40积分抵扣
        dto.setCostAmount(outbound.getCostAmount());             // 41成本金额
        dto.setGrossProfit(outbound.getGrossProfit());           // 42毛利
        dto.setSalesType(outbound.getSalesType());               // 43销售类型
        dto.setRemark(outbound.getRemark());                     // 44单据备注
        dto.setSummary(outbound.getSummary());                   // 45摘要
        dto.setAttachment(outbound.getAttachment());             // 46附件
        dto.setBookkeeperName(outbound.getBookkeeperName());     // 47记账人
        dto.setCreatorName(outbound.getCreatorName());           // 48制单人
        dto.setBookkeepingTime(outbound.getBookkeepingTime());   // 49记账时间
        dto.setCreateTime(outbound.getCreateTime());             // 50制单时间
        dto.setPrintCount(outbound.getPrintCount());             // 51打印次数

        // 其他字段映射
        dto.setStatus(outbound.getStatus());
        dto.setWarehouseId(outbound.getWarehouseId());
        dto.setCustomerId(outbound.getCustomerId());
        dto.setHandlerId(outbound.getHandlerId());
        dto.setDepartmentId(outbound.getDepartmentId());
        dto.setSettlementMethod(outbound.getSettlementMethod());
        dto.setSourceOrderType(outbound.getSourceOrderType());
        dto.setOrderAmount(outbound.getOrderAmount());
        dto.setPaidAmount(outbound.getPaidAmount());
        dto.setPendingAmount(outbound.getPendingAmount());
        dto.setAdvanceReceived(outbound.getAdvanceReceived());
        dto.setFavorableAmount(outbound.getFavorableAmount());
        dto.setCodAmount(outbound.getCodAmount());
        dto.setCodStatus(outbound.getCodStatus());
        dto.setDeliveryMethod(outbound.getDeliveryMethod());
        dto.setDeliveryDriver(outbound.getDeliveryDriver());
        dto.setLogisticsBranch(outbound.getLogisticsBranch());
        dto.setExpectedShipmentTime(outbound.getExpectedShipmentTime());
        dto.setActualShipmentTime(outbound.getActualShipmentTime());
        dto.setReceiptTime(outbound.getReceiptTime());
        dto.setApproverName(outbound.getApproverName());
        dto.setApprovedTime(outbound.getApprovedTime());
        dto.setCompletedTime(outbound.getCompletedTime());
        dto.setClosedTime(outbound.getClosedTime());
        dto.setCancelledTime(outbound.getCancelledTime());
        dto.setCancelReason(outbound.getCancelReason());
        dto.setCompletedBy(outbound.getCompletedBy());
        dto.setClosedBy(outbound.getClosedBy());
        dto.setCancelledBy(outbound.getCancelledBy());
        dto.setSource(outbound.getSource());
        dto.setPriority(outbound.getPriority());
        dto.setBusinessType(outbound.getBusinessType());
        dto.setProjectId(outbound.getProjectId());
        dto.setProjectName(outbound.getProjectName());
        dto.setContractId(outbound.getContractId());
        dto.setContractNo(outbound.getContractNo());
        dto.setTaxRate(outbound.getTaxRate());
        dto.setTaxIncludedAmount(outbound.getTaxIncludedAmount());
        dto.setTaxExcludedAmount(outbound.getTaxExcludedAmount());
        dto.setTaxAmount(outbound.getTaxAmount());
        dto.setCurrency(outbound.getCurrency());
        dto.setExchangeRate(outbound.getExchangeRate());
        dto.setForeignCurrencyAmount(outbound.getForeignCurrencyAmount());
        dto.setPaymentTerms(outbound.getPaymentTerms());
        dto.setPaymentDueDate(outbound.getPaymentDueDate());
        dto.setInvoiceNo(outbound.getInvoiceNo());
        dto.setInvoiceDate(outbound.getInvoiceDate());
        dto.setInvoiceStatus(outbound.getInvoiceStatus());
        dto.setInvoiceType(outbound.getInvoiceType());
        dto.setInvoiceTitle(outbound.getInvoiceTitle());
        dto.setTaxRegistrationNo(outbound.getTaxRegistrationNo());
        dto.setInvoicerName(outbound.getInvoicerName());
        dto.setInvoiceTime(outbound.getInvoiceTime());
        dto.setRemark2(outbound.getRemark2());
        dto.setRemark3(outbound.getRemark3());
        dto.setCustomField1(outbound.getCustomField1());
        dto.setCustomField2(outbound.getCustomField2());
        dto.setCustomField3(outbound.getCustomField3());
        dto.setCustomField4(outbound.getCustomField4());
        dto.setCustomField5(outbound.getCustomField5());
        dto.setCustomNumField1(outbound.getCustomNumField1());
        dto.setCustomNumField2(outbound.getCustomNumField2());
        dto.setCustomNumField3(outbound.getCustomNumField3());
        dto.setCustomNumField4(outbound.getCustomNumField4());
        dto.setCustomNumField5(outbound.getCustomNumField5());
        dto.setCustomDateField1(outbound.getCustomDateField1());
        dto.setCustomDateField2(outbound.getCustomDateField2());
        dto.setCustomDateField3(outbound.getCustomDateField3());
        dto.setCustomDateField4(outbound.getCustomDateField4());
        dto.setCustomDateField5(outbound.getCustomDateField5());
        dto.setCustomBoolField1(outbound.getCustomBoolField1());
        dto.setCustomBoolField2(outbound.getCustomBoolField2());
        dto.setCustomBoolField3(outbound.getCustomBoolField3());
        dto.setCustomBoolField4(outbound.getCustomBoolField4());
        dto.setCustomBoolField5(outbound.getCustomBoolField5());
        dto.setCustomObjField1(outbound.getCustomObjField1());
        dto.setCustomObjField2(outbound.getCustomObjField2());
        dto.setCustomObjField3(outbound.getCustomObjField3());
        dto.setCustomObjField4(outbound.getCustomObjField4());
        dto.setCustomObjField5(outbound.getCustomObjField5());

        return dto;
    }

    /**
     * 将销售退货单列表转换为统一销售单据DTO列表
     *
     * @param returns 销售退货单列表
     * @param docType 单据类型标识
     * @return 统一销售单据DTO列表
     */
    public static List<UnifiedSalesDocumentDTO> convertSaleReturnDocToUnifiedDTOs(List<SaleReturnDoc> returns, String docType) {
        List<UnifiedSalesDocumentDTO> result = new ArrayList<>();
        if (returns == null || returns.isEmpty()) {
            return result;
        }

        for (SaleReturnDoc returnDoc : returns) {
            UnifiedSalesDocumentDTO dto = convertSaleReturnDocToUnifiedDTO(returnDoc, docType);
            result.add(dto);
        }

        return result;
    }

    /**
     * 将销售退货单转换为统一销售单据DTO
     *
     * @param returnDoc 销售退货单
     * @param docType 单据类型标识
     * @return 统一销售单据DTO
     */
    public static UnifiedSalesDocumentDTO convertSaleReturnDocToUnifiedDTO(SaleReturnDoc returnDoc, String docType) {
        UnifiedSalesDocumentDTO dto = new UnifiedSalesDocumentDTO();

        // 基础字段映射（新实体字段名）
        dto.setId(returnDoc.getId());
        dto.setDocumentDate(returnDoc.getOrderDate());             // 单据日期 → orderDate
        dto.setDocumentNo(returnDoc.getReturnDocNo());             // 单据编号 → returnDocNo
        dto.setDocumentType(docType);
        dto.setInboundWarehouse(returnDoc.getWarehouseName());     // 仓库 → warehouseName
        dto.setOutboundWarehouse(null);                            // 新实体无此字段
        dto.setCustomerName(returnDoc.getCustomerName());
        dto.setCustomerCode(returnDoc.getCustomerCode());
        dto.setCustomerLevel(returnDoc.getCustomerLevel());
        dto.setReceiverName(returnDoc.getReceiverName());
        dto.setReceiverPhone(returnDoc.getReceiverPhone());
        dto.setShippingAddress(returnDoc.getShippingAddress());
        dto.setExtNum1(returnDoc.getExtNum1());
        dto.setExtNum2(returnDoc.getExtNum2());
        dto.setExtText1(returnDoc.getExtText1());
        dto.setExtText2(returnDoc.getExtText2());
        dto.setExtText3(returnDoc.getExtText3());
        dto.setBuyerRemark(returnDoc.getBuyerRemark());
        dto.setCustomerRemark(returnDoc.getCustomerRemark());
        dto.setSourceOrder(returnDoc.getSourceOrder());
        dto.setSourceOrderDate(null);                                // 新实体无此字段
        dto.setLogisticsCompany(returnDoc.getLogisticsCompany());
        dto.setTrackingNumber(returnDoc.getWaybillNo());             // waybillNo
        dto.setRegion(returnDoc.getRegion());
        dto.setGenerationMethod(returnDoc.getGenerateType());        // generateType
        dto.setHandlerName(returnDoc.getHandlerName());
        dto.setDepartmentName(returnDoc.getDeptName());              // deptName
        dto.setSettlementStatus(returnDoc.getSettleStatus());      // settleStatus
        dto.setSalesQuantity(returnDoc.getTotalQuantity());        // totalQuantity
        dto.setAmount(returnDoc.getProductAmount());               // productAmount
        dto.setDiscountedAmount(returnDoc.getDiscountAmount());    // discountAmount
        dto.setSalesRevenue(null);
        dto.setFreightPayer(returnDoc.getFreightPayer());
        dto.setFreight(returnDoc.getShippingFee());                // shippingFee
        dto.setOtherFee(returnDoc.getOtherFee());
        dto.setRoundingAmount(null);                               // 新实体无此字段
        dto.setTotalAmount(returnDoc.getTotalAmount());
        dto.setPromoDiscount(returnDoc.getPromoDiscount());        // promoDiscount
        dto.setCouponAmount(returnDoc.getCouponAmount());          // couponAmount
        dto.setDirectDiscount(returnDoc.getDirectDiscount());      // directDiscount
        dto.setPointsDeduction(null);
        dto.setCostAmount(null);                                   // 新实体无此字段
        dto.setGrossProfit(null);                                  // 新实体无此字段
        dto.setSalesType(returnDoc.getSalesType());
        dto.setRemark(returnDoc.getRemark());
        dto.setSummary(returnDoc.getSummary());
        dto.setAttachment(returnDoc.getAttachment());
        dto.setBookkeeperName(null);                               // 新实体无此字段
        dto.setCreatorName(returnDoc.getCreatorName());
        dto.setBookkeepingTime(returnDoc.getBookkeepingTime());
        dto.setCreateTime(returnDoc.getCreateTime());
        dto.setPrintCount(returnDoc.getPrintCount());

        // 其他字段映射（仅映射实体中存在的字段，不存在的设为null）
        dto.setStatus(returnDoc.getStatus());
        dto.setWarehouseId(returnDoc.getWarehouseId());
        dto.setCustomerId(returnDoc.getCustomerId());
        dto.setHandlerId(returnDoc.getHandlerId());
        dto.setDepartmentId(returnDoc.getDeptId());
        dto.setSettlementMethod(null);                             // 新实体无settleMethod
        dto.setSourceOrderType(null);                              // 新实体无此字段
        dto.setOrderAmount(returnDoc.getProductAmount());
        dto.setPaidAmount(returnDoc.getSettledAmount());           // settledAmount
        dto.setPendingAmount(null);                                // 新实体无unsettledAmount
        dto.setAdvanceReceived(returnDoc.getPrevAdvance());
        dto.setFavorableAmount(returnDoc.getDiscountAmount());
        dto.setCodAmount(returnDoc.getCodAmount());
        dto.setCodStatus(null);                                    // 新实体无此字段
        dto.setDeliveryMethod(returnDoc.getDeliveryMethod());
        dto.setDeliveryDriver(returnDoc.getDriverName());          // driverName
        dto.setLogisticsBranch(null);                              // 新实体无此字段
        dto.setExpectedShipmentTime(null);
        dto.setActualShipmentTime(null);
        dto.setReceiptTime(null);
        dto.setApproverName(returnDoc.getAuditorName());           // auditorName
        dto.setApprovedTime(returnDoc.getApprovedTime());
        dto.setCompletedTime(null);                                // 新实体无此字段
        dto.setClosedTime(null);                                   // 新实体无此字段
        dto.setCancelledTime(null);                                // 新实体无此字段
        dto.setCancelReason(null);                                 // 新实体无此字段
        dto.setCompletedBy(null);                                  // 新实体无此字段
        dto.setClosedBy(null);                                     // 新实体无此字段
        dto.setCancelledBy(null);                                  // 新实体无此字段
        dto.setSource(null);                                       // 新实体无此字段
        dto.setPriority(null);                                     // 新实体无此字段
        dto.setBusinessType(null);                                 // 新实体无此字段
        dto.setProjectId(null);                                    // 新实体无此字段
        dto.setProjectName(null);                                  // 新实体无此字段
        dto.setContractId(null);                                   // 新实体无此字段
        dto.setContractNo(null);                                   // 新实体无此字段
        dto.setTaxRate(null);                                      // 新实体无此字段
        dto.setTaxIncludedAmount(null);                            // 新实体无此字段
        dto.setTaxExcludedAmount(null);                            // 新实体无此字段
        dto.setTaxAmount(null);                                    // 新实体无此字段
        dto.setCurrency(null);                                     // 新实体无此字段
        dto.setExchangeRate(null);                                 // 新实体无此字段
        dto.setForeignCurrencyAmount(null);                        // 新实体无此字段
        dto.setPaymentTerms(null);                                 // 新实体无此字段
        dto.setPaymentDueDate(null);                               // 新实体无此字段
        dto.setInvoiceNo(null);                                    // 新实体无此字段
        dto.setInvoiceDate(null);                                  // 新实体无此字段
        dto.setInvoiceStatus(null);                                // 新实体无此字段
        dto.setInvoiceType(null);                                  // 新实体无此字段
        dto.setInvoiceTitle(null);                                 // 新实体无此字段
        dto.setTaxRegistrationNo(returnDoc.getTaxNo());            // taxNo
        dto.setInvoicerName(null);                                 // 新实体无此字段
        dto.setInvoiceTime(null);                                  // 新实体无此字段
        dto.setRemark2(null);                                      // 新实体无此字段
        dto.setRemark3(null);                                      // 新实体无此字段
        dto.setCustomField1(null);                                 // 新实体无此字段
        dto.setCustomField2(null);
        dto.setCustomField3(null);
        dto.setCustomField4(null);
        dto.setCustomField5(null);
        dto.setCustomNumField1(null);                              // 新实体无此字段
        dto.setCustomNumField2(null);
        dto.setCustomNumField3(null);
        dto.setCustomNumField4(null);
        dto.setCustomNumField5(null);
        dto.setCustomDateField1(null);                             // 新实体无此字段
        dto.setCustomDateField2(null);
        dto.setCustomDateField3(null);
        dto.setCustomDateField4(null);
        dto.setCustomDateField5(null);
        dto.setCustomBoolField1(null);                             // 新实体无此字段
        dto.setCustomBoolField2(null);
        dto.setCustomBoolField3(null);
        dto.setCustomBoolField4(null);
        dto.setCustomBoolField5(null);
        dto.setCustomObjField1(null);                              // 新实体无此字段
        dto.setCustomObjField2(null);
        dto.setCustomObjField3(null);
        dto.setCustomObjField4(null);
        dto.setCustomObjField5(null);

        return dto;
    }

    /**
     * 将销售换货单列表转换为统一销售单据DTO列表
     *
     * @param exchanges 销售换货单列表
     * @param docType 单据类型标识
     * @return 统一销售单据DTO列表
     */
    public static List<UnifiedSalesDocumentDTO> convertSaleExchangeToUnifiedDTOs(List<SaleExchange> exchanges, String docType) {
        List<UnifiedSalesDocumentDTO> result = new ArrayList<>();
        if (exchanges == null || exchanges.isEmpty()) {
            return result;
        }

        for (SaleExchange exchange : exchanges) {
            UnifiedSalesDocumentDTO dto = convertSaleExchangeToUnifiedDTO(exchange, docType);
            result.add(dto);
        }

        return result;
    }

    /**
     * 将销售换货单转换为统一销售单据DTO
     *
     * @param exchange 销售换货单
     * @param docType 单据类型标识
     * @return 统一销售单据DTO
     */
    public static UnifiedSalesDocumentDTO convertSaleExchangeToUnifiedDTO(SaleExchange exchange, String docType) {
        UnifiedSalesDocumentDTO dto = new UnifiedSalesDocumentDTO();

        // 基础字段映射（新实体字段名）
        dto.setId(exchange.getId());
        dto.setDocumentDate(exchange.getExchangeDate());           // exchangeDate
        dto.setDocumentNo(exchange.getExchangeNo());              // exchangeNo
        dto.setDocumentType(docType);
        dto.setInboundWarehouse(exchange.getInWarehouseName());    // inWarehouseName
        dto.setOutboundWarehouse(exchange.getOutWarehouseName());  // outWarehouseName
        dto.setCustomerName(exchange.getCustomerName());
        dto.setCustomerCode(exchange.getCustomerCode());
        dto.setCustomerLevel(exchange.getCustomerLevel());
        dto.setReceiverName(null);
        dto.setReceiverPhone(null);
        dto.setShippingAddress(null);
        dto.setExtNum1(exchange.getExtNum1());
        dto.setExtNum2(exchange.getExtNum2());
        dto.setExtText1(exchange.getExtText1());
        dto.setExtText2(exchange.getExtText2());
        dto.setExtText3(exchange.getExtText3());
        dto.setBuyerRemark(null);
        dto.setCustomerRemark(null);
        dto.setSourceOrder(exchange.getOriginalOrderNo());
        dto.setSourceOrderDate(null);
        dto.setLogisticsCompany(null);
        dto.setTrackingNumber(null);
        dto.setRegion(null);
        dto.setGenerationMethod(null);
        dto.setHandlerName(exchange.getHandlerName());
        dto.setDepartmentName(exchange.getDeptName());
        dto.setSettlementStatus(exchange.getSettleStatus());       // settleStatus
        dto.setSalesQuantity(exchange.getInQuantityTotal());       // inQuantityTotal
        dto.setAmount(exchange.getProductAmount());                // productAmount
        dto.setDiscountedAmount(exchange.getTotalAmount());        // totalAmount
        dto.setSalesRevenue(null);
        dto.setFreightPayer(null);
        dto.setFreight(null);
        dto.setOtherFee(null);
        dto.setRoundingAmount(null);
        dto.setTotalAmount(exchange.getTotalAmount());
        dto.setPromoDiscount(null);
        dto.setCouponAmount(null);
        dto.setDirectDiscount(null);
        dto.setPointsDeduction(null);
        dto.setCostAmount(null);
        dto.setGrossProfit(null);
        dto.setSalesType(exchange.getSalesType());
        dto.setRemark(exchange.getRemark());
        dto.setSummary(exchange.getSummary());
        dto.setAttachment(exchange.getAttachment());
        dto.setBookkeeperName(exchange.getBookkeeperName());
        dto.setCreatorName(exchange.getCreatorName());
        dto.setBookkeepingTime(exchange.getBookkeepingTime());
        dto.setCreateTime(exchange.getCreateTime());
        dto.setPrintCount(exchange.getPrintCount());

        // 其他字段映射（仅映射实体中存在的字段，不存在的设为null）
        dto.setStatus(exchange.getStatus());
        dto.setWarehouseId(exchange.getInWarehouseId());
        dto.setCustomerId(exchange.getCustomerId());
        dto.setHandlerId(exchange.getHandlerId());
        dto.setDepartmentId(exchange.getDeptId());
        dto.setSettlementMethod(null);                             // 新实体无此字段
        dto.setSourceOrderType(null);                              // 新实体无此字段
        dto.setOrderAmount(exchange.getProductAmount());
        dto.setPaidAmount(exchange.getSettledAmount());            // settledAmount
        dto.setPendingAmount(null);                                // 新实体无此字段
        dto.setAdvanceReceived(exchange.getPrevAdvance());
        dto.setFavorableAmount(exchange.getDiscountAmount());
        dto.setCodAmount(null);                                    // 新实体无此字段
        dto.setCodStatus(null);                                    // 新实体无此字段
        dto.setDeliveryMethod(null);                               // 新实体无此字段
        dto.setDeliveryDriver(null);                               // 新实体无此字段
        dto.setLogisticsBranch(null);                              // 新实体无此字段
        dto.setExpectedShipmentTime(null);
        dto.setActualShipmentTime(null);
        dto.setReceiptTime(null);
        dto.setApproverName(exchange.getApprovedByName());         // approvedByName
        dto.setApprovedTime(exchange.getApprovedTime());
        dto.setCompletedTime(exchange.getCompletedTime());         // completedTime存在
        dto.setClosedTime(null);                                   // 新实体无此字段
        dto.setCancelledTime(null);                                // 新实体无此字段
        dto.setCancelReason(null);                                 // 新实体无此字段
        dto.setCompletedBy(null);                                  // 新实体无此字段
        dto.setClosedBy(null);                                     // 新实体无此字段
        dto.setCancelledBy(null);                                  // 新实体无此字段
        dto.setSource(null);                                       // 新实体无此字段
        dto.setPriority(null);                                     // 新实体无此字段
        dto.setBusinessType(null);                                 // 新实体无此字段
        dto.setProjectId(null);                                    // 新实体无此字段
        dto.setProjectName(null);                                  // 新实体无此字段
        dto.setContractId(null);                                   // 新实体无此字段
        dto.setContractNo(null);                                   // 新实体无此字段
        dto.setTaxRate(null);                                      // 新实体无此字段
        dto.setTaxIncludedAmount(null);                            // 新实体无此字段
        dto.setTaxExcludedAmount(null);                            // 新实体无此字段
        dto.setTaxAmount(null);                                    // 新实体无此字段
        dto.setCurrency(null);                                     // 新实体无此字段
        dto.setExchangeRate(null);                                 // 新实体无此字段
        dto.setForeignCurrencyAmount(null);                        // 新实体无此字段
        dto.setPaymentTerms(null);                                 // 新实体无此字段
        dto.setPaymentDueDate(null);                               // 新实体无此字段
        dto.setInvoiceNo(null);                                    // 新实体无此字段
        dto.setInvoiceDate(null);                                  // 新实体无此字段
        dto.setInvoiceStatus(null);                                // 新实体无此字段
        dto.setInvoiceType(null);                                  // 新实体无此字段
        dto.setInvoiceTitle(null);                                 // 新实体无此字段
        dto.setTaxRegistrationNo(exchange.getTaxNo());             // taxNo
        dto.setInvoicerName(null);                                 // 新实体无此字段
        dto.setInvoiceTime(null);                                  // 新实体无此字段
        dto.setRemark2(null);                                      // 新实体无此字段
        dto.setRemark3(null);                                      // 新实体无此字段
        dto.setCustomField1(null);                                 // 新实体无此字段
        dto.setCustomField2(null);
        dto.setCustomField3(null);
        dto.setCustomField4(null);
        dto.setCustomField5(null);
        dto.setCustomNumField1(null);                              // 新实体无此字段
        dto.setCustomNumField2(null);
        dto.setCustomNumField3(null);
        dto.setCustomNumField4(null);
        dto.setCustomNumField5(null);
        dto.setCustomDateField1(null);                             // 新实体无此字段
        dto.setCustomDateField2(null);
        dto.setCustomDateField3(null);
        dto.setCustomDateField4(null);
        dto.setCustomDateField5(null);
        dto.setCustomBoolField1(null);                             // 新实体无此字段
        dto.setCustomBoolField2(null);
        dto.setCustomBoolField3(null);
        dto.setCustomBoolField4(null);
        dto.setCustomBoolField5(null);
        dto.setCustomObjField1(null);                              // 新实体无此字段
        dto.setCustomObjField2(null);
        dto.setCustomObjField3(null);
        dto.setCustomObjField4(null);
        dto.setCustomObjField5(null);

        return dto;
    }
}