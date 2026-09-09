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
        // 应收来源：仅销售出库单 + 销售退货单（排除销售订单与换货单，换货单表缺应收核销列）
        boolean skipOrder = Boolean.TRUE.equals(queryDTO.getReceivableOnly());
        boolean skipExchange = Boolean.TRUE.equals(queryDTO.getReceivableOnly());
        long orderCount = 0, outboundCount = 0, returnCount = 0, exchangeCount = 0;
        if (!skipOrder && (docType == null || docType.isEmpty() || "SALE_ORDER".equals(docType))) {
            orderCount = saleOrderService.count(buildSaleOrderQueryWrapper(queryDTO));
        }
        if (docType == null || docType.isEmpty() || "OUTBOUND".equals(docType)) {
            outboundCount = saleOutboundService.count(buildSaleOutboundQueryWrapper(queryDTO));
        }
        if (docType == null || docType.isEmpty() || "RETURN".equals(docType)) {
            returnCount = saleReturnDocService.count(buildSaleReturnDocQueryWrapper(queryDTO));
        }
        if (!skipExchange && (docType == null || docType.isEmpty() || "EXCHANGE".equals(docType))) {
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

        // 销售订单（应收来源过滤时跳过）
        if (!skipOrder && orderCount > 0 && offset < globalEnd) {
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

        // 销售换货单（应收来源过滤时跳过）
        if (!skipExchange && exchangeCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
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
        addCommonQueryConditions(wrapper, queryDTO, "SALE_ORDER");

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
        addCommonQueryConditions(wrapper, queryDTO, "OUTBOUND");

        // SaleOutbound默认日期字段是outbound_date
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("outbound_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("outbound_date", endTs);
                }
            }
        }

        // 销售出库单特有查询条件
        if (queryDTO.getOutboundTypes() != null && !queryDTO.getOutboundTypes().isEmpty()) {
            wrapper.in("outbound_type", queryDTO.getOutboundTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("total_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("total_amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        return wrapper;
    }

    /**
     * 构建销售退货单查询条件
     */
    private QueryWrapper<SaleReturnDoc> buildSaleReturnDocQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleReturnDoc> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO, "RETURN");

        // SaleReturnDoc默认日期字段是order_date
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

        // 销售退货单特有查询条件
        if (queryDTO.getReturnTypes() != null && !queryDTO.getReturnTypes().isEmpty()) {
            wrapper.in("return_type", queryDTO.getReturnTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("total_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("total_amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        return wrapper;
    }

    /**
     * 构建销售换货单查询条件
     */
    private QueryWrapper<SaleExchange> buildSaleExchangeQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleExchange> wrapper = new QueryWrapper<>();

        // 通用查询条件
        addCommonQueryConditions(wrapper, queryDTO, "EXCHANGE");

        // SaleExchange默认日期字段是exchange_date
        String dateType = queryDTO.getDateType();
        if (dateType == null || dateType.isEmpty()) {
            if (queryDTO.getStartDate() != null && !queryDTO.getStartDate().isEmpty()) {
                java.sql.Timestamp startTs = toTimestamp(queryDTO.getStartDate());
                if (startTs != null) {
                    wrapper.ge("exchange_date", startTs);
                }
            }
            if (queryDTO.getEndDate() != null && !queryDTO.getEndDate().isEmpty()) {
                java.sql.Timestamp endTs = toTimestamp(queryDTO.getEndDate());
                if (endTs != null) {
                    wrapper.le("exchange_date", endTs);
                }
            }
        }

        // 销售换货单特有查询条件
        if (queryDTO.getExchangeTypes() != null && !queryDTO.getExchangeTypes().isEmpty()) {
            wrapper.in("exchange_type", queryDTO.getExchangeTypes());
        }

        // 金额范围查询
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("total_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("total_amount", queryDTO.getMaxAmount());
        }

        // 客户相关查询
        if (queryDTO.getCustomerName() != null && !queryDTO.getCustomerName().isEmpty()) {
            wrapper.like("customer_name", queryDTO.getCustomerName());
        }
        if (queryDTO.getCustomerCode() != null && !queryDTO.getCustomerCode().isEmpty()) {
            wrapper.eq("customer_code", queryDTO.getCustomerCode());
        }

        return wrapper;
    }

    /**
     * 添加通用查询条件（按单据类型映射正确的数据库列名）
     *
     * <p>销售订单/出库单/退货单/换货单的表结构列名不同（如单据编号、经手人、日期列），
     * 故此处不再使用统一的列名字面量，而是按 {@code docType} 解析正确的列名；
     * 若某类型不存在对应列则跳过该条件，避免 PostgreSQL 报 "column does not exist"。
     */
    private void addCommonQueryConditions(QueryWrapper<?> wrapper, UnifiedSalesDocQueryDTO queryDTO, String docType) {
        // 单据编号
        addLike(wrapper, col("docNo", docType), queryDTO.getDocumentNo());
        // 客户名称/编号
        addLike(wrapper, "customer_name", queryDTO.getCustomerName());
        addEq(wrapper, col("customerCode", docType), queryDTO.getCustomerCode());
        // 经手人
        addLike(wrapper, col("handler", docType), queryDTO.getHandlerName());
        // 收货人/联系电话/收货地址
        addLike(wrapper, col("receiver", docType), queryDTO.getReceiverName());
        addLike(wrapper, col("receiverPhone", docType), queryDTO.getReceiverPhone());
        addLike(wrapper, col("shippingAddress", docType), queryDTO.getShippingAddress());
        // 部门
        addLike(wrapper, col("dept", docType), queryDTO.getDepartmentName());
        // 制单人/记账人
        addLike(wrapper, col("creator", docType), queryDTO.getCreatorName());
        addLike(wrapper, col("bookkeeper", docType), queryDTO.getBookkeeperName());
        // 结算状态
        addEq(wrapper, col("settlementStatus", docType), queryDTO.getSettlementStatus());
        // 来源订单
        addLike(wrapper, col("sourceOrder", docType), queryDTO.getSourceOrder());
        // 产生方式
        addEq(wrapper, col("generationMethod", docType), queryDTO.getGenerationMethod());
        // 销售类型
        addEq(wrapper, col("salesType", docType), queryDTO.getSalesType());
        // 单据备注/买家备注
        addLike(wrapper, "remark", queryDTO.getRemark());
        addLike(wrapper, col("buyerRemark", docType), queryDTO.getBuyerRemark());
        // 物流公司/运单号/区域
        addLike(wrapper, col("logistics", docType), queryDTO.getLogisticsCompany());
        addLike(wrapper, col("tracking", docType), queryDTO.getTrackingNumber());
        addLike(wrapper, col("region", docType), queryDTO.getRegion());
        // 本单金额范围
        if (queryDTO.getMinTotalAmount() != null) {
            wrapper.ge(col("totalAmount", docType), queryDTO.getMinTotalAmount());
        }
        if (queryDTO.getMaxTotalAmount() != null) {
            wrapper.le(col("totalAmount", docType), queryDTO.getMaxTotalAmount());
        }
        // 来源
        addEq(wrapper, col("source", docType), queryDTO.getSource());
        // 状态查询
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }
        // 红冲过滤：默认不显示红冲单据（status=3为红冲状态）
        if (!Boolean.TRUE.equals(queryDTO.getShowRed())) {
            wrapper.ne("status", 3);
        }
        // 仓库查询：前端传仓库名称文本，用模糊匹配
        if (queryDTO.getWarehouseName() != null && !queryDTO.getWarehouseName().isEmpty()) {
            wrapper.like(col("warehouse", docType), queryDTO.getWarehouseName());
        }
    }

    /**
     * 解析某个逻辑字段在指定单据类型下的数据库列名；类型无该列时返回 null（调用方跳过）。
     */
    private String col(String logical, String docType) {
        switch (logical) {
            case "docNo":
                switch (docType) {
                    case "OUTBOUND": return "outbound_no";
                    case "RETURN": return "return_doc_no";
                    case "EXCHANGE": return "exchange_no";
                    default: return "order_no";
                }
            case "handler":
                switch (docType) {
                    case "OUTBOUND": return "sales_person_name";
                    case "SALE_ORDER": return "salesman_name";
                    case "EXCHANGE": return null;
                    default: return "handler_name";
                }
            case "receiver":
                return "EXCHANGE".equals(docType) ? null : "receiver_name";
            case "receiverPhone":
                return "EXCHANGE".equals(docType) ? null : "receiver_phone";
            case "shippingAddress":
                return "EXCHANGE".equals(docType) ? null : "shipping_address";
            case "dept":
                if ("EXCHANGE".equals(docType)) return null;
                return "OUTBOUND".equals(docType) ? "department_name" : "dept_name";
            case "creator":
                return "EXCHANGE".equals(docType) ? null : "creator_name";
            case "bookkeeper":
                return ("RETURN".equals(docType) || "EXCHANGE".equals(docType)) ? null : "bookkeeper_name";
            case "settlementStatus":
                if ("OUTBOUND".equals(docType)) return "settlement_status";
                return ("EXCHANGE".equals(docType) || "SALE_ORDER".equals(docType)) ? null : "settle_status";
            case "customerCode":
                return "EXCHANGE".equals(docType) ? null : "customer_code";
            case "sourceOrder":
                switch (docType) {
                    case "OUTBOUND": return "order_no";
                    case "RETURN": return "source_order";
                    case "EXCHANGE": return null;
                    default: return "original_order_no";
                }
            case "generationMethod":
                if ("RETURN".equals(docType)) return "generate_type";
                return "EXCHANGE".equals(docType) ? null : "generation_method";
            case "salesType":
                return "OUTBOUND".equals(docType) ? "outbound_type" : "sales_type";
            case "buyerRemark":
                return "EXCHANGE".equals(docType) ? null : "buyer_remark";
            case "logistics":
                return "EXCHANGE".equals(docType) ? null : "logistics_company";
            case "tracking":
                switch (docType) {
                    case "OUTBOUND": return "tracking_number";
                    case "RETURN": return "waybill_no";
                    case "EXCHANGE": return null;
                    default: return "waybill_no";
                }
            case "region":
                return "EXCHANGE".equals(docType) ? null : "region";
            case "totalAmount":
                return "SALE_ORDER".equals(docType) ? "bill_amount" : "total_amount";
            case "source":
                return "source";
            case "warehouse":
                return "EXCHANGE".equals(docType) ? "in_warehouse_name" : "warehouse_name";
            default:
                return null;
        }
    }

    private void addLike(QueryWrapper<?> wrapper, String column, String val) {
        if (column != null && val != null && !val.isEmpty()) {
            wrapper.like(column, val);
        }
    }

    private void addEq(QueryWrapper<?> wrapper, String column, String val) {
        if (column != null && val != null && !val.isEmpty()) {
            wrapper.eq(column, val);
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