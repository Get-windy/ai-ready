package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocQueryDTO;
import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
import cn.aiedge.erp.sale.service.ISaleExchangeService;
import cn.aiedge.erp.sale.service.UnifiedSalesDocQueryService;
import cn.aiedge.erp.sale.util.UnifiedDocConverter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一销售单据查询服务实现
 * 整合销售订单、销售出库单、销售退货单、销售换货单的查询
 */
@Service
public class UnifiedSalesDocQueryServiceImpl implements UnifiedSalesDocQueryService {

    @Autowired
    private ISaleOrderService saleOrderService;

    @Autowired
    private SaleOutboundService saleOutboundService;

    @Autowired
    private ISaleReturnDocService saleReturnDocService;

    @Autowired
    private ISaleExchangeService saleExchangeService;

    @Override
    public Page<UnifiedSalesDocumentDTO> unifiedPage(UnifiedSalesDocQueryDTO queryDTO) {
        Page<UnifiedSalesDocumentDTO> resultPage = new Page<>();
        resultPage.setCurrent(queryDTO.getCurrent());
        resultPage.setSize(queryDTO.getSize());

        long currentPage = queryDTO.getCurrent();
        long pageSize = queryDTO.getSize();
        String docType = queryDTO.getDocumentType();

        // Step 1: 获取各类型的记录数（使用SQL COUNT，不加载数据）
        long orderCount = 0, outboundCount = 0, returnCount = 0, exchangeCount = 0;
        if (docType == null || docType.isEmpty() || "SALE_ORDER".equals(docType)) {
            orderCount = saleOrderService.count(buildSaleOrderQueryWrapper(queryDTO));
        }
        if (docType == null || docType.isEmpty() || "OUTBOUND".equals(docType)) {
            outboundCount = saleOutboundService.count(buildSaleOutboundQueryWrapper(queryDTO));
        }
        if (docType == null || docType.isEmpty() || "RETURN".equals(docType)) {
            returnCount = saleReturnDocService.count(buildSaleReturnDocQueryWrapper(queryDTO));
        }
        if (docType == null || docType.isEmpty() || "EXCHANGE".equals(docType)) {
            exchangeCount = saleExchangeService.count(buildSaleExchangeQueryWrapper(queryDTO));
        }

        long totalCount = orderCount + outboundCount + returnCount + exchangeCount;
        resultPage.setTotal(totalCount);
        resultPage.setPages((totalCount + pageSize - 1) / pageSize);

        if (totalCount == 0) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // Step 2: 计算全局偏移，确定每种类型需要查询的范围
        long globalStart = (currentPage - 1) * pageSize;
        long globalEnd = globalStart + pageSize;

        List<UnifiedSalesDocumentDTO> pageRecords = new ArrayList<>();
        long offset = 0;

        // 销售订单
        if (orderCount > 0 && offset < globalEnd) {
            long typeEnd = offset + orderCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), orderCount - skip);
                if (take > 0) {
                    Page<SaleOrder> typePage = saleOrderService.page(
                            new Page<>(skip / pageSize + 1, pageSize),
                            buildSaleOrderQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertToUnifiedSalesDocumentDTOs(typePage.getRecords(), "SALE_ORDER");
                    // 处理skip偏移不在页边界的情况
                    int startIdx = (int)(skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) {
                        pageRecords.addAll(converted.subList(0, takeCount));
                    }
                }
            }
            offset = typeEnd;
        }

        // 销售出库单
        if (outboundCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + outboundCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), outboundCount - skip);
                if (take > 0) {
                    Page<SaleOutbound> typePage = saleOutboundService.page(
                            new Page<>(skip / pageSize + 1, pageSize),
                            buildSaleOutboundQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleOutboundToUnifiedDTOs(typePage.getRecords(), "OUTBOUND");
                    int startIdx = (int)(skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) {
                        pageRecords.addAll(converted.subList(0, takeCount));
                    }
                }
            }
            offset = typeEnd;
        }

        // 销售退货单
        if (returnCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + returnCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), returnCount - skip);
                if (take > 0) {
                    Page<SaleReturnDoc> typePage = saleReturnDocService.page(
                            new Page<>(skip / pageSize + 1, pageSize),
                            buildSaleReturnDocQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleReturnDocToUnifiedDTOs(typePage.getRecords(), "RETURN");
                    int startIdx = (int)(skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) {
                        pageRecords.addAll(converted.subList(0, takeCount));
                    }
                }
            }
            offset = typeEnd;
        }

        // 销售换货单
        if (exchangeCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + exchangeCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), exchangeCount - skip);
                if (take > 0) {
                    Page<SaleExchange> typePage = saleExchangeService.page(
                            new Page<>(skip / pageSize + 1, pageSize),
                            buildSaleExchangeQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleExchangeToUnifiedDTOs(typePage.getRecords(), "EXCHANGE");
                    int startIdx = (int)(skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) {
                        pageRecords.addAll(converted.subList(0, takeCount));
                    }
                }
            }
        }

        resultPage.setRecords(pageRecords);
        return resultPage;
    }

    /**
     * 构建销售订单查询条件
     */
    private QueryWrapper<SaleOrder> buildSaleOrderQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleOrder> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO);

        // SaleOrder默认日期字段是order_date（非document_date）
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("order_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("order_date", endTs);
                }
            }
        }

        // 销售订单特有查询条件
        if (queryDTO.getOrderTypes() != null && !queryDTO.getOrderTypes().isEmpty()) {
            wrapper.in("sale_type", queryDTO.getOrderTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("product_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("product_amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        // 经手人相关查询
        if (queryDTO.getHandlerName() != null && !queryDTO.getHandlerName().isEmpty()) {
            wrapper.like("salesman_name", queryDTO.getHandlerName());
        }

        return wrapper;
    }

    /**
     * 构建销售出库单查询条件
     */
    private QueryWrapper<SaleOutbound> buildSaleOutboundQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleOutbound> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO);

        // SaleOutbound默认日期字段是document_date
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("document_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("document_date", endTs);
                }
            }
        }

        // 销售出库单特有查询条件
        if (queryDTO.getOutboundTypes() != null && !queryDTO.getOutboundTypes().isEmpty()) {
            wrapper.in("document_type", queryDTO.getOutboundTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        // 经手人相关查询
        if (queryDTO.getHandlerName() != null && !queryDTO.getHandlerName().isEmpty()) {
            wrapper.like("handler_name", queryDTO.getHandlerName());
        }

        return wrapper;
    }

    /**
     * 构建销售退货单查询条件
     */
    private QueryWrapper<SaleReturnDoc> buildSaleReturnDocQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleReturnDoc> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO);

        // SaleReturnDoc默认日期字段
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("document_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("document_date", endTs);
                }
            }
        }

        // 销售退货单特有查询条件
        if (queryDTO.getReturnTypes() != null && !queryDTO.getReturnTypes().isEmpty()) {
            wrapper.in("document_type", queryDTO.getReturnTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        // 经手人相关查询
        if (queryDTO.getHandlerName() != null && !queryDTO.getHandlerName().isEmpty()) {
            wrapper.like("handler_name", queryDTO.getHandlerName());
        }

        return wrapper;
    }

    /**
     * 构建销售换货单查询条件
     */
    private QueryWrapper<SaleExchange> buildSaleExchangeQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleExchange> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO);

        // SaleExchange默认日期字段
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("document_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("document_date", endTs);
                }
            }
        }

        // 销售换货单特有查询条件
        if (queryDTO.getExchangeTypes() != null && !queryDTO.getExchangeTypes().isEmpty()) {
            wrapper.in("document_type", queryDTO.getExchangeTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        // 经手人相关查询
        if (queryDTO.getHandlerName() != null && !queryDTO.getHandlerName().isEmpty()) {
            wrapper.like("handler_name", queryDTO.getHandlerName());
        }

        return wrapper;
    }

    /**
     * 添加通用查询条件
     */
    private void addCommonQueryConditions(QueryWrapper<?> wrapper, UnifiedSalesDocQueryDTO queryDTO) {
        // 单据编号查询
        if (queryDTO.getDocumentNo() != null && !queryDTO.getDocumentNo().isEmpty()) {
            wrapper.like("document_no", queryDTO.getDocumentNo());
        }

        // 日期范围由各单据类型的build方法自行处理（SaleOrder→order_date，其他→document_date）

        // 收货人查询
        if (queryDTO.getReceiverName() != null && !queryDTO.getReceiverName().isEmpty()) {
            wrapper.like("receiver_name", queryDTO.getReceiverName());
        }

        // 联系电话查询
        if (queryDTO.getReceiverPhone() != null && !queryDTO.getReceiverPhone().isEmpty()) {
            wrapper.like("receiver_phone", queryDTO.getReceiverPhone());
        }

        // 收货地址查询
        if (queryDTO.getShippingAddress() != null && !queryDTO.getShippingAddress().isEmpty()) {
            wrapper.like("shipping_address", queryDTO.getShippingAddress());
        }

        // 部门查询
        if (queryDTO.getDepartmentName() != null && !queryDTO.getDepartmentName().isEmpty()) {
            wrapper.like("department_name", queryDTO.getDepartmentName());
        }

        // 制单人查询
        if (queryDTO.getCreatorName() != null && !queryDTO.getCreatorName().isEmpty()) {
            wrapper.like("creator_name", queryDTO.getCreatorName());
        }

        // 记账人查询
        if (queryDTO.getBookkeeperName() != null && !queryDTO.getBookkeeperName().isEmpty()) {
            wrapper.like("bookkeeper_name", queryDTO.getBookkeeperName());
        }

        // 结算状态查询
        if (queryDTO.getSettlementStatus() != null && !queryDTO.getSettlementStatus().isEmpty()) {
            wrapper.eq("settlement_status", queryDTO.getSettlementStatus());
        }

        // 来源订单查询
        if (queryDTO.getSourceOrder() != null && !queryDTO.getSourceOrder().isEmpty()) {
            wrapper.like("source_order", queryDTO.getSourceOrder());
        }

        // 来源订单日期范围查询
        if (queryDTO.getSourceOrderStartDate() != null && !queryDTO.getSourceOrderStartDate().isEmpty()) {
            java.sql.Timestamp startTs = toTimestamp(queryDTO.getSourceOrderStartDate());
            if (startTs != null) {
                wrapper.ge("source_order_date", startTs);
            }
        }
        if (queryDTO.getSourceOrderEndDate() != null && !queryDTO.getSourceOrderEndDate().isEmpty()) {
            java.sql.Timestamp endTs = toTimestamp(queryDTO.getSourceOrderEndDate());
            if (endTs != null) {
                wrapper.le("source_order_date", endTs);
            }
        }

        // 产生方式查询
        if (queryDTO.getGenerationMethod() != null && !queryDTO.getGenerationMethod().isEmpty()) {
            wrapper.eq("generation_method", queryDTO.getGenerationMethod());
        }

        // 销售类型查询
        if (queryDTO.getSalesType() != null && !queryDTO.getSalesType().isEmpty()) {
            wrapper.eq("sales_type", queryDTO.getSalesType());
        }

        // 单据备注查询
        if (queryDTO.getRemark() != null && !queryDTO.getRemark().isEmpty()) {
            wrapper.like("remark", queryDTO.getRemark());
        }

        // 买家备注查询
        if (queryDTO.getBuyerRemark() != null && !queryDTO.getBuyerRemark().isEmpty()) {
            wrapper.like("buyer_remark", queryDTO.getBuyerRemark());
        }

        // 自定义字段查询
        if (queryDTO.getExtNum1Min() != null || queryDTO.getExtNum1Max() != null) {
            if (queryDTO.getExtNum1Min() != null) {
                wrapper.ge("ext_num1", queryDTO.getExtNum1Min());
            }
            if (queryDTO.getExtNum1Max() != null) {
                wrapper.le("ext_num1", queryDTO.getExtNum1Max());
            }
        }

        if (queryDTO.getExtNum2Min() != null || queryDTO.getExtNum2Max() != null) {
            if (queryDTO.getExtNum2Min() != null) {
                wrapper.ge("ext_num2", queryDTO.getExtNum2Min());
            }
            if (queryDTO.getExtNum2Max() != null) {
                wrapper.le("ext_num2", queryDTO.getExtNum2Max());
            }
        }

        if (queryDTO.getExtText1() != null && !queryDTO.getExtText1().isEmpty()) {
            wrapper.like("ext_text1", queryDTO.getExtText1());
        }

        if (queryDTO.getExtText2() != null && !queryDTO.getExtText2().isEmpty()) {
            wrapper.like("ext_text2", queryDTO.getExtText2());
        }

        if (queryDTO.getExtText3() != null && !queryDTO.getExtText3().isEmpty()) {
            wrapper.like("ext_text3", queryDTO.getExtText3());
        }

        // 物流公司查询
        if (queryDTO.getLogisticsCompany() != null && !queryDTO.getLogisticsCompany().isEmpty()) {
            wrapper.like("logistics_company", queryDTO.getLogisticsCompany());
        }

        // 运单号查询
        if (queryDTO.getTrackingNumber() != null && !queryDTO.getTrackingNumber().isEmpty()) {
            wrapper.like("tracking_number", queryDTO.getTrackingNumber());
        }

        // 区域查询
        if (queryDTO.getRegion() != null && !queryDTO.getRegion().isEmpty()) {
            wrapper.like("region", queryDTO.getRegion());
        }

        // 本单金额范围查询
        if (queryDTO.getMinTotalAmount() != null) {
            wrapper.ge("total_amount", queryDTO.getMinTotalAmount());
        }
        if (queryDTO.getMaxTotalAmount() != null) {
            wrapper.le("total_amount", queryDTO.getMaxTotalAmount());
        }

        // 来源查询
        if (queryDTO.getSource() != null && !queryDTO.getSource().isEmpty()) {
            wrapper.eq("source", queryDTO.getSource());
        }

        // 状态查询
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }

        // 仅统计车辆库
        if (Boolean.TRUE.equals(queryDTO.getOnlyVehicleWarehouse())) {
            wrapper.eq("warehouse_type", "VEHICLE");
        }

        // 红冲过滤：默认不显示红冲单据（status=3为红冲状态）
        if (!Boolean.TRUE.equals(queryDTO.getShowRed())) {
            wrapper.ne("status", 3);
        }

        // 仓库查询：前端传仓库名称文本，用模糊匹配
        if (queryDTO.getWarehouseName() != null && !queryDTO.getWarehouseName().isEmpty()) {
            wrapper.and(w -> w.like("warehouse_name", queryDTO.getWarehouseName())
                    .or().like("outbound_warehouse", queryDTO.getWarehouseName())
                    .or().like("inbound_warehouse", queryDTO.getWarehouseName()));
        }
    }

    /**
     * 根据dateType解析对应的数据库列名
     */
    private String resolveDateColumn(String dateType) {
        switch (dateType) {
            case "createTime":
                return "create_time";
            case "bookkeepingTime":
                return "bookkeeping_time";
            case "sourceOrderDate":
                return "source_order_date";
            case "documentDate":
            default:
                return "document_date";
        }
    }

    /**
     * 将日期字符串转为 java.sql.Timestamp，避免 MyBatis-Plus 将 String 作为 VARCHAR 传给 PostgreSQL
     * 导致 "timestamp >= character varying" 类型不匹配错误
     */
    private java.sql.Timestamp toTimestamp(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            // 去掉可能的前后空格
            dateString = dateString.trim();

            // 检查日期字符串长度，确保是有效格式
            if (dateString.length() < 8) {
                System.out.println("日期格式太短: " + dateString + ", 将跳过日期过滤");
                return null;
            }

            // 尝试解析为 "yyyy-MM-dd" 格式
            LocalDate localDate = LocalDate.parse(dateString);
            return java.sql.Timestamp.valueOf(localDate.atStartOfDay());
        } catch (Exception e1) {
            try {
                // 尝试解析为 "yyyy-MM-dd HH:mm:ss" 格式
                LocalDateTime localDateTime = LocalDateTime.parse(dateString);
                return java.sql.Timestamp.valueOf(localDateTime);
            } catch (Exception e2) {
                // 尝试解析为 "yyyy-MM-dd HH:mm" 格式
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateString + ":00");
                    return java.sql.Timestamp.valueOf(localDateTime);
                } catch (Exception e3) {
                    // 如果所有格式都不匹配，记录日志并返回null
                    System.out.println("日期格式解析失败: " + dateString + ", 将跳过日期过滤. 原始异常: " + e3.getMessage());
                    return null;
                }
            }
        }
    }
}