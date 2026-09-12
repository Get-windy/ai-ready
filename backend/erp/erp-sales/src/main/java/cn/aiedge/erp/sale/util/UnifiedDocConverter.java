package cn.aiedge.erp.sale.util;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
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
        dto.setSourceOrder(order.getSourceOrder() != null ? order.getSourceOrder() : order.getOriginalOrderNo()); // 19来源订单 - 优先 source_order，回退原始订单号
        dto.setSourceOrderDate(null);                          // 20来源订单日期 - 销售订单无此字段
        dto.setLogisticsCompany(order.getLogisticsCompany());  // 21物流公司
        dto.setTrackingNumber(order.getWaybillNo());           // 22运单号 - 销售订单使用waybillNo
        dto.setRegion(order.getRegion());                      // 23区域
        dto.setGenerationMethod(order.getGenerationMethod());  // 24产生方式
        dto.setHandlerName(order.getSalesmanName());           // 25经手人 - 销售订单使用salesmanName
        dto.setDepartmentName(order.getDeptName());            // 26部门 - 销售订单使用deptName
        dto.setSettlementStatus(deriveSettlementStatus(order.getBillAmount(), order.getSettledAmount(), null)); // 27结算状态 - 由「本单金额 / 已结算金额」派生（订单无结算状态列）
        dto.setSalesQuantity(order.getTotalQuantity());        // 28销售数量 - 销售订单使用totalQuantity
        dto.setAmount(order.getProductAmount());               // 29金额 - 销售订单使用productAmount
        dto.setDiscountedAmount(order.getBillAmount());        // 30折后金额 - 销售订单使用billAmount
        dto.setSalesRevenue(order.getBillAmount());            // 31销售收入 - 销售订单使用billAmount
        dto.setFreightPayer(order.getFreightPayer());          // 32运费承担方
        dto.setFreight(order.getShippingFee());                // 33运费 - 销售订单使用shippingFee
        dto.setOtherFee(order.getOtherFee());                  // 34其它费用
        dto.setRoundingAmount(null);                           // 35抹零金额 - 销售订单无此字段（不适用）
        dto.setTotalAmount(order.getBillAmount());             // 36本单金额 - 销售订单使用billAmount
        dto.setPromoDiscount(order.getPromoDiscount());        // 37促销优惠
        dto.setCouponAmount(order.getCouponAmount());          // 38优惠券优惠
        dto.setDirectDiscount(order.getDirectDiscount());      // 39直接优惠
        dto.setPointsDeduction(order.getUsedPoints());         // 40积分抵扣 - 销售订单使用usedPoints
        dto.setCostAmount(null);                               // 41成本金额 - 由 Service 按明细 cost_amount 聚合回填
        dto.setGrossProfit(null);                              // 42毛利 - 由 Service 按明细 gross_profit 聚合回填
        dto.setSalesType(String.valueOf(order.getSaleType())); // 43销售类型 - 销售订单使用saleType(Integer)
        dto.setRemark(order.getRemark());                      // 44单据备注
        dto.setSummary(order.getSummary());                    // 45摘要
        dto.setAttachment(order.getAttachment());              // 46附件
        dto.setBookkeeperName(null);                           // 47记账人 - 销售订单无记账人字段（记账人只在过账后记录，订单不落库）
        dto.setCreatorName(order.getCreatorName());            // 48制单人
        dto.setBookkeepingTime(order.getBookkeepingTime());    // 49记账时间
        dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime() : order.getSubmitTime()); // 50制单时间 - 优先 createTime，回退提交时间
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
     * <p>基于现行实体 {@link cn.aiedge.erp.sale.outbound.entity.SaleOutbound}（表 erp_sale_outbound），
     * 新实体没有的字段统一置 null。</p>
     *
     * @param outbound 销售出库单
     * @param docType 单据类型标识
     * @return 统一销售单据DTO
     */
    public static UnifiedSalesDocumentDTO convertSaleOutboundToUnifiedDTO(SaleOutbound outbound, String docType) {
        UnifiedSalesDocumentDTO dto = new UnifiedSalesDocumentDTO();

        // 基础字段映射
        dto.setId(outbound.getId());
        dto.setDocumentDate(outbound.getOutboundDate() != null ? outbound.getOutboundDate().atStartOfDay() : null); // 1单据日期 → outboundDate(LocalDate)转LocalDateTime
        dto.setDocumentNo(outbound.getOutboundNo());           // 2单据编号 → outboundNo
        dto.setDocumentType(docType);                          // 3单据类型
        dto.setInboundWarehouse(null);                         // 4入库仓库 - 新实体无此字段
        dto.setOutboundWarehouse(outbound.getWarehouseName()); // 5出库仓库 → warehouseName
        dto.setCustomerName(outbound.getCustomerName());       // 6客户
        dto.setCustomerCode(outbound.getCustomerCode());       // 7客户编号
        dto.setCustomerLevel(outbound.getCustomerLevel());     // 8客户级别
        dto.setReceiverName(outbound.getReceiverName());       // 9收货人
        dto.setReceiverPhone(outbound.getReceiverPhone());     // 10联系电话
        dto.setShippingAddress(outbound.getShippingAddress()); // 11收货地址
        dto.setExtNum1(outbound.getExtNum1());                 // 12表头自定义字段1(数字)
        dto.setExtNum2(outbound.getExtNum2());                 // 13表头自定义字段2(数字)
        dto.setExtText1(outbound.getExtText1());               // 14表头自定义字段3(文本)
        dto.setExtText2(outbound.getExtText2());               // 15表头自定义字段4(文本)
        dto.setExtText3(outbound.getExtText3());               // 16表头自定义字段5(文本)
        dto.setBuyerRemark(outbound.getBuyerRemark());         // 17买家备注
        dto.setCustomerRemark(outbound.getCustomerRemark());   // 18客户备注
        dto.setSourceOrder(outbound.getOrderNo());             // 19来源订单 → orderNo
        dto.setSourceOrderDate(null);                          // 20来源订单日期 - 新实体无此字段
        dto.setLogisticsCompany(outbound.getLogisticsCompany()); // 21物流公司
        dto.setTrackingNumber(outbound.getTrackingNumber());   // 22运单号
        dto.setRegion(outbound.getRegion());                   // 23区域
        dto.setGenerationMethod(outbound.getGenerationMethod()); // 24产生方式
        dto.setHandlerName(outbound.getSalesPersonName());     // 25经手人 → salesPersonName
        dto.setDepartmentName(outbound.getDepartmentName());   // 26部门
        dto.setSettlementStatus(deriveSettlementStatus(outbound.getTotalAmount(), outbound.getSettledAmount(), outbound.getSettlementStatus())); // 27结算状态 - 实体列优先，为空时按已结算金额派生
        dto.setSalesQuantity(outbound.getTotalQuantity());     // 28销售数量 → totalQuantity
        dto.setAmount(outbound.getTotalAmount());              // 29金额 → totalAmount
        dto.setDiscountedAmount(null);                         // 30折后金额 - 新实体无此字段
        dto.setSalesRevenue(null);                             // 31销售收入 - 新实体无此字段
        dto.setFreightPayer(outbound.getFreightPayer());       // 32运费承担方
        dto.setFreight(outbound.getFreight());                 // 33运费
        dto.setOtherFee(outbound.getOtherFee());               // 34其它费用
        dto.setRoundingAmount(outbound.getRoundingAmount());   // 35抹零金额
        dto.setTotalAmount(outbound.getTotalAmount());         // 36本单金额
        dto.setPromoDiscount(outbound.getPromoDiscount());     // 37促销优惠
        dto.setCouponAmount(outbound.getCouponAmount());       // 38优惠券优惠
        dto.setDirectDiscount(outbound.getDirectDiscount());   // 39直接优惠
        dto.setPointsDeduction(outbound.getMemberUsedPoints()); // 40积分抵扣 → memberUsedPoints
        dto.setCostAmount(null);                               // 41成本金额 - 由 Service 按明细 cost_amount 聚合回填
        dto.setGrossProfit(null);                              // 42毛利 - 由 Service 按明细 gross_profit 聚合回填
        dto.setSalesType(outbound.getOutboundType() != null ? String.valueOf(outbound.getOutboundType()) : null); // 43销售类型 → outboundType(Integer)
        dto.setRemark(outbound.getRemark());                   // 44单据备注
        dto.setSummary(outbound.getSummary());                 // 45摘要
        dto.setAttachment(null);                               // 46附件 - 新实体无此字段
        dto.setBookkeeperName(outbound.getBookkeeperName());   // 47记账人
        dto.setCreatorName(outbound.getCreatorName());         // 48制单人
        dto.setBookkeepingTime(outbound.getBookkeepingTime()); // 49记账时间
        dto.setCreateTime(outbound.getCreateTime());           // 50制单时间
        dto.setPrintCount(outbound.getPrintCount());           // 51打印次数

        // 其他字段映射（仅映射新实体中存在的字段，不存在的设为null）
        dto.setStatus(outbound.getStatus());
        dto.setWarehouseId(outbound.getWarehouseId());
        dto.setCustomerId(outbound.getCustomerId());
        dto.setHandlerId(outbound.getSalesPersonId());         // → salesPersonId
        dto.setDepartmentId(outbound.getDepartmentId());
        dto.setSettlementMethod(outbound.getSettlementMethod());
        dto.setSourceOrderType(null);                          // 新实体无此字段
        dto.setOrderAmount(null);                              // 新实体无此字段
        dto.setPaidAmount(outbound.getSettledAmount());        // → settledAmount
        dto.setPendingAmount(null);                            // 新实体无此字段
        dto.setAdvanceReceived(null);                          // 新实体无此字段（表无 advance 列）
        dto.setFavorableAmount(null);                          // 新实体无此字段
        dto.setCodAmount(outbound.getCodAmount());
        dto.setCodStatus(null);                                // 新实体无此字段
        dto.setDeliveryMethod(outbound.getDeliveryMethod());
        dto.setDeliveryDriver(outbound.getDeliveryDriver());
        dto.setLogisticsBranch(outbound.getLogisticsBranch());
        dto.setExpectedShipmentTime(outbound.getExpectedShipTime()); // → expectedShipTime
        dto.setActualShipmentTime(outbound.getActualShipTime());     // → actualShipTime
        dto.setReceiptTime(null);                              // 新实体无此字段
        dto.setApproverName(outbound.getAuditorName());        // → auditorName
        dto.setApprovedTime(outbound.getApprovedTime());
        dto.setCompletedTime(outbound.getCompletedTime());
        dto.setClosedTime(null);                               // 新实体无此字段
        dto.setCancelledTime(null);                            // 新实体无此字段
        dto.setCancelReason(null);                             // 新实体无此字段
        dto.setCompletedBy(outbound.getCompletedBy() != null ? String.valueOf(outbound.getCompletedBy()) : null); // Long → String
        dto.setClosedBy(null);                                 // 新实体无此字段
        dto.setCancelledBy(null);                              // 新实体无此字段
        dto.setSource(null);                                   // 新实体无此字段
        dto.setPriority(null);                                 // 新实体无此字段
        dto.setBusinessType(null);                             // 新实体无此字段
        dto.setProjectId(null);                                // 新实体无此字段
        dto.setProjectName(null);                              // 新实体无此字段
        dto.setContractId(null);                               // 新实体无此字段
        dto.setContractNo(null);                               // 新实体无此字段
        dto.setTaxRate(null);                                  // 新实体表头无此字段
        dto.setTaxIncludedAmount(null);                        // 新实体无此字段
        dto.setTaxExcludedAmount(null);                        // 新实体无此字段
        dto.setTaxAmount(null);                                // 新实体无此字段
        dto.setCurrency(null);                                 // 新实体无此字段
        dto.setExchangeRate(null);                             // 新实体无此字段
        dto.setForeignCurrencyAmount(null);                    // 新实体无此字段
        dto.setPaymentTerms(null);                             // 新实体无此字段
        dto.setPaymentDueDate(null);                           // 新实体无此字段
        dto.setInvoiceNo(null);                                // 新实体无此字段
        dto.setInvoiceDate(null);                              // 新实体无此字段
        dto.setInvoiceStatus(null);                            // 新实体无此字段
        dto.setInvoiceType(null);                              // 新实体无此字段
        dto.setInvoiceTitle(null);                             // 新实体无此字段
        dto.setTaxRegistrationNo(outbound.getTaxNo());         // → taxNo
        dto.setInvoicerName(null);                             // 新实体无此字段
        dto.setInvoiceTime(null);                              // 新实体无此字段
        dto.setRemark2(null);                                  // 新实体无此字段
        dto.setRemark3(null);                                  // 新实体无此字段
        dto.setCustomField1(null);                             // 新实体无此字段
        dto.setCustomField2(null);
        dto.setCustomField3(null);
        dto.setCustomField4(null);
        dto.setCustomField5(null);
        dto.setCustomNumField1(null);                          // 新实体无此字段
        dto.setCustomNumField2(null);
        dto.setCustomNumField3(null);
        dto.setCustomNumField4(null);
        dto.setCustomNumField5(null);
        dto.setCustomDateField1(null);                         // 新实体无此字段
        dto.setCustomDateField2(null);
        dto.setCustomDateField3(null);
        dto.setCustomDateField4(null);
        dto.setCustomDateField5(null);
        dto.setCustomBoolField1(null);                         // 新实体无此字段
        dto.setCustomBoolField2(null);
        dto.setCustomBoolField3(null);
        dto.setCustomBoolField4(null);
        dto.setCustomBoolField5(null);
        dto.setCustomObjField1(null);                          // 新实体无此字段
        dto.setCustomObjField2(null);
        dto.setCustomObjField3(null);
        dto.setCustomObjField4(null);
        dto.setCustomObjField5(null);

        // ═══ 应收核销字段（按单收款） ═══
        dto.setSettledAmount(outbound.getSettledAmount());      // 已结算金额
        dto.setUnsettledAmount(calcUnsettled(outbound.getTotalAmount(), outbound.getSettledAmount())); // 未结算金额
        dto.setPendingApproveAmount(null);                      // 待审金额
        dto.setSettlementUnit(outbound.getSettlementMethod());  // 结算单位
        dto.setDriverName(outbound.getDeliveryDriver());        // 司机名称
        dto.setReceiptDate(outbound.getPaymentDate() != null ? outbound.getPaymentDate().atStartOfDay() : null); // 收款日期
        dto.setReconciliationDate(outbound.getReconciliationDate() != null ? outbound.getReconciliationDate().atStartOfDay() : null); // 对账日期
        dto.setDynamicPayTerm(null);                            // 动态收款期限
        dto.setFixedTerms(null);                                // 固定账期
        dto.setSettlePeriod(outbound.getSettlementMethod());    // 结算期
        dto.setOverdueDays(null);                               // 超期天数
        dto.setReconcile(null);                                 // 对账标记
        dto.setLoanNote(null);                                  // 欠条领取
        dto.setLastReconcileTime(null);                         // 最后对账标记时间
        dto.setLastReconcileBy(null);                           // 最后对账标记人

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
        dto.setSettlementStatus(deriveSettlementStatus(returnDoc.getTotalAmount(), returnDoc.getSettledAmount(), returnDoc.getSettleStatus())); // settleStatus 优先，为空时按已结算金额派生
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
        dto.setPointsDeduction(returnDoc.getMemberUsedPoints());   // memberUsedPoints
        dto.setCostAmount(null);                                   // 由 Service 按明细 ref_cost_amount 聚合回填
        dto.setGrossProfit(null);                                  // 由 Service 回填（本单金额 - 成本金额）
        dto.setSalesType(returnDoc.getSalesType());
        dto.setRemark(returnDoc.getRemark());
        dto.setSummary(returnDoc.getSummary());
        dto.setAttachment(returnDoc.getAttachment());
        dto.setBookkeeperName(returnDoc.getBookkeeperName());      // 记账人（过账后记录）
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

        // ═══ 应收核销字段（按单收款） ═══
        dto.setSettledAmount(returnDoc.getSettledAmount());      // 已结算金额
        dto.setUnsettledAmount(calcUnsettled(returnDoc.getTotalAmount(), returnDoc.getSettledAmount())); // 未结算金额
        dto.setPendingApproveAmount(null);                       // 待审金额
        dto.setSettlementUnit(null);                             // 结算单位（退货单无结算方式字段）
        dto.setDriverName(returnDoc.getDriverName());            // 司机名称
        dto.setReceiptDate(returnDoc.getPaymentDate());          // 收款日期
        dto.setReconciliationDate(returnDoc.getReconciliationDate()); // 对账日期
        dto.setDynamicPayTerm(null);                             // 动态收款期限
        dto.setFixedTerms(null);                                 // 固定账期
        dto.setSettlePeriod(null);                               // 结算期
        dto.setOverdueDays(null);                                // 超期天数
        dto.setReconcile(null);                                  // 对账标记
        dto.setLoanNote(null);                                   // 欠条领取
        dto.setLastReconcileTime(null);                          // 最后对账标记时间
        dto.setLastReconcileBy(null);                            // 最后对账标记人

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
        dto.setSettlementStatus(deriveSettlementStatus(exchange.getTotalAmount(), exchange.getSettledAmount(), exchange.getSettleStatus())); // settleStatus 优先，为空时按已结算金额派生
        dto.setSalesQuantity(exchange.getInQuantityTotal());       // inQuantityTotal
        dto.setAmount(exchange.getProductAmount());                // productAmount
        dto.setDiscountedAmount(exchange.getTotalAmount());        // totalAmount（换货以本单金额为结算金额）
        dto.setSalesRevenue(null);                                 // 销售收入 - 换货单不适用
        dto.setFreightPayer(null);                                 // 运费承担方 - 换货单不适用
        dto.setFreight(null);                                      // 运费 - 换货单不适用
        dto.setOtherFee(null);                                     // 其它费用 - 换货单不适用
        dto.setRoundingAmount(null);                               // 抹零金额 - 换货单不适用
        dto.setTotalAmount(exchange.getTotalAmount());
        dto.setPromoDiscount(null);                                // 促销优惠 - 换货单不适用
        dto.setCouponAmount(null);                                 // 优惠券优惠 - 换货单不适用
        dto.setDirectDiscount(exchange.getDiscountAmount());       // 直接优惠 - 换货单折扣金额
        dto.setPointsDeduction(null);                              // 积分抵扣 - 换货单不适用
        dto.setCostAmount(null);                                   // 由 Service 按明细 cost_amount 聚合回填
        dto.setGrossProfit(null);                                  // 由 Service 回填（本单金额 - 成本金额）
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

        // ═══ 应收核销字段（按单收款） ═══
        dto.setSettledAmount(exchange.getSettledAmount());            // 已结算金额
        dto.setUnsettledAmount(calcUnsettled(exchange.getTotalAmount(), exchange.getSettledAmount())); // 未结算金额
        dto.setPendingApproveAmount(null);                            // 待审金额
        dto.setSettlementUnit(null);                                  // 结算单位（换货单无结算方式字段）
        dto.setDriverName(null);                                      // 司机名称
        dto.setReceiptDate(null);                                     // 收款日期
        dto.setReconciliationDate(null);                              // 对账日期
        dto.setDynamicPayTerm(null);                                  // 动态收款期限
        dto.setFixedTerms(null);                                      // 固定账期
        dto.setSettlePeriod(null);                                    // 结算期
        dto.setOverdueDays(null);                                     // 超期天数
        dto.setReconcile(null);                                       // 对账标记
        dto.setLoanNote(null);                                        // 欠条领取
        dto.setLastReconcileTime(null);                               // 最后对账标记时间
        dto.setLastReconcileBy(null);                                 // 最后对账标记人

        return dto;
    }

    /**
     * 计算未结算金额 = 本单金额 - 已结算金额
     * 金额缺失时按 0 处理，保证金额口径（本单金额=已结算+待审+未结算）恒等。
     */
    private static java.math.BigDecimal calcUnsettled(java.math.BigDecimal totalAmount, java.math.BigDecimal settledAmount) {
        java.math.BigDecimal total = totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO;
        java.math.BigDecimal settled = settledAmount != null ? settledAmount : java.math.BigDecimal.ZERO;
        return total.subtract(settled);
    }

    /**
     * 结算状态口径：实体自身列优先；列缺失时由「本单金额 / 已结算金额」派生，
     * 取值与前端「结算状态」下拉（未结算 / 部分结算 / 已结算）一致。
     */
    private static String deriveSettlementStatus(java.math.BigDecimal totalAmount,
                                                 java.math.BigDecimal settledAmount,
                                                 String entityValue) {
        if (entityValue != null && !entityValue.isEmpty()) {
            return entityValue;
        }
        java.math.BigDecimal total = totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO;
        if (total.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return null;
        }
        java.math.BigDecimal settled = settledAmount != null ? settledAmount : java.math.BigDecimal.ZERO;
        if (settled.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        return settled.compareTo(total) >= 0 ? "PAID" : "PARTIAL_PAID";
    }
}