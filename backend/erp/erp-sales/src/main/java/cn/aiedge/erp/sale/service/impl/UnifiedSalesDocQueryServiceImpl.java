package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocQueryDTO;
import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDocItem;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocItemMapper;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeItemMapper;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
import cn.aiedge.erp.sale.service.ISaleExchangeService;
import cn.aiedge.erp.sale.service.UnifiedSalesDocQueryService;
import cn.aiedge.erp.sale.util.UnifiedDocConverter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Autowired
    private SaleOrderItemMapper saleOrderItemMapper;

    @Autowired
    private SaleOutboundItemMapper saleOutboundItemMapper;

    @Autowired
    private SaleReturnDocItemMapper saleReturnDocItemMapper;

    @Autowired
    private SaleExchangeItemMapper saleExchangeItemMapper;

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
        if (!skipOrder && orderCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + orderCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), orderCount - skip);
                if (take > 0) {
                    // MyBatis-Plus 分页语义：Page(current, size) → LIMIT size OFFSET (current-1)*size
                    // 排序加在 Page 上而非 wrapper：wrapper 的 orderBy 会被 count 查询带上，
                    // PostgreSQL 下 `SELECT COUNT(*) ... ORDER BY 非分组列` 会直接报错。
                    Page<SaleOrder> typePage = saleOrderService.page(
                            buildTypePage(skip, take, "order_date"),
                            buildSaleOrderQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertToUnifiedSalesDocumentDTOs(typePage.getRecords(), "SALE_ORDER");
                    fillCostAndProfit(converted, "SALE_ORDER");
                    pageRecords.addAll(converted);
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
                            buildTypePage(skip, take, "outbound_date"),
                            buildSaleOutboundQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleOutboundToUnifiedDTOs(typePage.getRecords(), "OUTBOUND");
                    fillCostAndProfit(converted, "OUTBOUND");
                    pageRecords.addAll(converted);
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
                            buildTypePage(skip, take, "order_date"),
                            buildSaleReturnDocQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleReturnDocToUnifiedDTOs(typePage.getRecords(), "RETURN");
                    fillCostAndProfit(converted, "RETURN");
                    pageRecords.addAll(converted);
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
                            buildTypePage(skip, take, "exchange_date"),
                            buildSaleExchangeQueryWrapper(queryDTO));
                    List<UnifiedSalesDocumentDTO> converted =
                            UnifiedDocConverter.convertSaleExchangeToUnifiedDTOs(typePage.getRecords(), "EXCHANGE");
                    fillCostAndProfit(converted, "EXCHANGE");
                    pageRecords.addAll(converted);
                }
            }
        }

        resultPage.setRecords(pageRecords);
        return resultPage;
    }

    /**
     * 构造类型分页对象：LIMIT take OFFSET skip，并按单据日期倒序（同日按 ID 倒序）。
     * 排序放在 Page 上，避免 ORDER BY 出现在 COUNT 查询里。
     */
    private <T> Page<T> buildTypePage(long skip, long take, String dateColumn) {
        Page<T> page = new Page<>(skip + 1, take);
        page.addOrder(OrderItem.desc(dateColumn));
        page.addOrder(OrderItem.desc("id"));
        return page;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 成本金额 / 毛利：按单据 ID 批量聚合明细（避免逐行 N+1 查询）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 回填成本金额与毛利。
     *
     * <p>成本金额 = 明细成本金额之和；毛利优先取明细毛利之和（订单 / 出库单有该列），
     * 明细无毛利列的类型（退货单 / 换货单）按「本单金额 - 成本金额」计算。
     * 明细无成本数据时保持 null，前端展示为空，不以 0 冒充。</p>
     */
    private void fillCostAndProfit(List<UnifiedSalesDocumentDTO> list, String docType) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream()
                .map(UnifiedSalesDocumentDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal[]> agg = aggregateItemCost(docType, ids);
        for (UnifiedSalesDocumentDTO dto : list) {
            BigDecimal[] pair = agg.get(dto.getId());
            BigDecimal cost = pair != null ? pair[0] : null;
            BigDecimal profit = pair != null ? pair[1] : null;
            dto.setCostAmount(cost);
            if (profit == null && cost != null && dto.getTotalAmount() != null) {
                profit = dto.getTotalAmount().subtract(cost);
            }
            dto.setGrossProfit(profit);
        }
    }

    /** 按单据类型聚合明细成本 / 毛利，返回 billId → [成本金额, 毛利] */
    private Map<Long, BigDecimal[]> aggregateItemCost(String docType, List<Long> ids) {
        Map<Long, BigDecimal[]> result = new HashMap<>();
        List<Map<String, Object>> rows;
        String billKey;
        boolean hasProfit;

        switch (docType) {
            case "SALE_ORDER": {
                QueryWrapper<SaleOrderItem> qw = new QueryWrapper<>();
                qw.select("order_id", "SUM(cost_amount) AS cost_amount", "SUM(gross_profit) AS gross_profit")
                        .in("order_id", ids)
                        .groupBy("order_id");
                rows = saleOrderItemMapper.selectMaps(qw);
                billKey = "order_id";
                hasProfit = true;
                break;
            }
            case "OUTBOUND": {
                QueryWrapper<SaleOutboundItem> qw = new QueryWrapper<>();
                qw.select("outbound_id", "SUM(cost_amount) AS cost_amount", "SUM(gross_profit) AS gross_profit")
                        .in("outbound_id", ids)
                        .groupBy("outbound_id");
                rows = saleOutboundItemMapper.selectMaps(qw);
                billKey = "outbound_id";
                hasProfit = true;
                break;
            }
            case "RETURN": {
                QueryWrapper<SaleReturnDocItem> qw = new QueryWrapper<>();
                qw.select("return_doc_id", "SUM(ref_cost_amount) AS cost_amount")
                        .in("return_doc_id", ids)
                        .groupBy("return_doc_id");
                rows = saleReturnDocItemMapper.selectMaps(qw);
                billKey = "return_doc_id";
                hasProfit = false;
                break;
            }
            case "EXCHANGE": {
                QueryWrapper<SaleExchangeItem> qw = new QueryWrapper<>();
                qw.select("exchange_id", "SUM(cost_amount) AS cost_amount")
                        .in("exchange_id", ids)
                        .groupBy("exchange_id");
                rows = saleExchangeItemMapper.selectMaps(qw);
                billKey = "exchange_id";
                hasProfit = false;
                break;
            }
            default:
                return result;
        }

        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Long billId = toLong(pick(row, billKey));
            if (billId == null) {
                continue;
            }
            BigDecimal cost = toBigDecimal(pick(row, "cost_amount"));
            BigDecimal profit = hasProfit ? toBigDecimal(pick(row, "gross_profit")) : null;
            result.put(billId, new BigDecimal[]{cost, profit});
        }
        return result;
    }

    /** 兼容不同驱动 / 驼峰映射下的 Map 键名差异 */
    private Object pick(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String normalized = key.replace("_", "");
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)
                    || e.getKey().replace("_", "").equalsIgnoreCase(normalized)) {
                return e.getValue();
            }
        }
        return null;
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 构建销售订单查询条件
     */
    private QueryWrapper<SaleOrder> buildSaleOrderQueryWrapper(UnifiedSalesDocQueryDTO queryDTO) {
        QueryWrapper<SaleOrder> wrapper = new QueryWrapper<>();

        // 通用查询条件（含日期类型 / 日期区间 / 自定义字段 / 商品行属性）
        addCommonQueryConditions(wrapper, queryDTO, "SALE_ORDER");

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

        // 通用查询条件（含日期类型 / 日期区间 / 自定义字段 / 商品行属性）
        addCommonQueryConditions(wrapper, queryDTO, "OUTBOUND");

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

        // 通用查询条件（含日期类型 / 日期区间 / 自定义字段 / 商品行属性）
        addCommonQueryConditions(wrapper, queryDTO, "RETURN");

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

        // 通用查询条件（含日期类型 / 日期区间 / 自定义字段 / 商品行属性）
        addCommonQueryConditions(wrapper, queryDTO, "EXCHANGE");

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
        // 日期类型 + 日期区间（列名按单据类型解析，类型无该列时跳过）
        addDateRange(wrapper, dateColumn(queryDTO.getDateType(), docType), queryDTO.getStartDate(), queryDTO.getEndDate());
        // 来源订单日期区间
        addDateRange(wrapper, col("sourceOrderDate", docType),
                queryDTO.getSourceOrderStartDate(), queryDTO.getSourceOrderEndDate());
        // 商品行属性（明细维度，用 EXISTS 避免表头行重复）
        addProductAttribute(wrapper, queryDTO.getProductAttribute(), docType);

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
        // 表头自定义字段（数字区间 / 文本）
        addRange(wrapper, col("extNum1", docType), queryDTO.getExtNum1Min(), queryDTO.getExtNum1Max());
        addRange(wrapper, col("extNum2", docType), queryDTO.getExtNum2Min(), queryDTO.getExtNum2Max());
        addLike(wrapper, col("extText1", docType), queryDTO.getExtText1());
        addLike(wrapper, col("extText2", docType), queryDTO.getExtText2());
        addLike(wrapper, col("extText3", docType), queryDTO.getExtText3());
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
        // 显示红冲：默认隐藏「已取消」单据。
        // 各单据类型的状态码不同（无统一 status 语义），按类型映射，避免误过滤正常单据
        // （此前统一用 status<>3，导致销售订单「部分发货(3)」被错误隐藏）。
        if (!Boolean.TRUE.equals(queryDTO.getShowRed())) {
            switch (docType) {
                case "SALE_ORDER":
                    wrapper.ne("status", 6);   // 6=已取消
                    break;
                case "OUTBOUND":
                    wrapper.ne("status", 12);  // 12=已取消
                    break;
                case "RETURN":
                    wrapper.ne("status", 4);   // 4=已取消
                    break;
                case "EXCHANGE":
                    wrapper.ne("status", 6);   // 6=已取消
                    break;
                default:
                    break;
            }
        }
        // 仓库：ID 精确匹配，名称模糊匹配
        if (queryDTO.getWarehouseId() != null) {
            String whIdCol = col("warehouseId", docType);
            if (whIdCol != null) {
                wrapper.eq(whIdCol, queryDTO.getWarehouseId());
            }
        }
        addLike(wrapper, col("warehouse", docType), queryDTO.getWarehouseName());
        // 仅统计车辆库：口径与「销售明细查询」一致（仓库名称包含「车辆」）
        if (Boolean.TRUE.equals(queryDTO.getOnlyVehicleWarehouse())) {
            String whCol = col("warehouse", docType);
            if (whCol != null) {
                wrapper.like(whCol, "车辆");
            }
        }
    }

    /**
     * 日期类型 → 数据库日期列。
     * 指定类型在该单据表无对应列时返回 null（调用方跳过，避免 PG 列不存在）。
     */
    private String dateColumn(String dateType, String docType) {
        String type = (dateType == null || dateType.isEmpty()) ? "documentDate" : dateType;
        switch (type) {
            case "createTime":
                return "create_time";
            case "bookkeepingTime":
                return "bookkeeping_time";
            case "sourceOrderDate":
                return col("sourceOrderDate", docType);
            case "documentDate":
            default:
                switch (docType) {
                    case "OUTBOUND":
                        return "outbound_date";
                    case "EXCHANGE":
                        return "exchange_date";
                    default:
                        return "order_date";
                }
        }
    }

    /** 日期区间条件（起止均含当天；结束日期补到 23:59:59） */
    private void addDateRange(QueryWrapper<?> wrapper, String column, String startDate, String endDate) {
        if (column == null) {
            return;
        }
        if (hasText(startDate)) {
            java.sql.Timestamp startTs = toTimestamp(startDate);
            if (startTs != null) {
                wrapper.ge(column, startTs);
            }
        }
        if (hasText(endDate)) {
            java.sql.Timestamp endTs = toTimestampEnd(endDate);
            if (endTs != null) {
                wrapper.le(column, endTs);
            }
        }
    }

    /** 数值区间条件 */
    private void addRange(QueryWrapper<?> wrapper, String column, BigDecimal min, BigDecimal max) {
        if (column == null) {
            return;
        }
        if (min != null) {
            wrapper.ge(column, min);
        }
        if (max != null) {
            wrapper.le(column, max);
        }
    }

    /**
     * 商品行属性筛选（明细维度）：用 EXISTS 子查询，避免 JOIN 造成表头记录重复。
     * 各单据明细的属性列名不同：订单 line_attribute / 出库 product_attribute / 退货与换货 product_line_attr。
     */
    private void addProductAttribute(QueryWrapper<?> wrapper, String value, String docType) {
        if (!hasText(value)) {
            return;
        }
        switch (docType) {
            case "SALE_ORDER":
                wrapper.exists("SELECT 1 FROM erp_sale_order_item i WHERE i.order_id = erp_sale_order.id AND i.line_attribute = {0}", value);
                break;
            case "OUTBOUND":
                wrapper.exists("SELECT 1 FROM erp_sale_outbound_item i WHERE i.outbound_id = erp_sale_outbound.id AND i.product_attribute = {0}", value);
                break;
            case "RETURN":
                wrapper.exists("SELECT 1 FROM erp_sale_return_doc_item i WHERE i.return_doc_id = erp_sale_return_doc.id AND i.product_line_attr = {0}", value);
                break;
            case "EXCHANGE":
                wrapper.exists("SELECT 1 FROM erp_sale_exchange_item i WHERE i.exchange_id = erp_sale_exchange.id AND i.product_line_attr = {0}", value);
                break;
            default:
                break;
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isEmpty();
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
            case "warehouseId":
                return "EXCHANGE".equals(docType) ? "in_warehouse_id" : "warehouse_id";
            case "sourceOrderDate":
                // 四类销售单据的表头均无「来源订单日期」列（该日期在来源单据上），不参与过滤
                return null;
            case "extNum1":
                return "ext_num1";
            case "extNum2":
                return "ext_num2";
            case "extText1":
                return "ext_text1";
            case "extText2":
                return "ext_text2";
            case "extText3":
                return "ext_text3";
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
     * 将日期字符串（结束日期）转为当天 23:59:59 的 Timestamp，保证区间包含当天。
     */
    private java.sql.Timestamp toTimestampEnd(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        String s = dateString.trim();
        try {
            if (s.length() <= 10) {
                return java.sql.Timestamp.valueOf(LocalDate.parse(s).atTime(23, 59, 59));
            }
            return toTimestamp(s);
        } catch (Exception e) {
            return toTimestamp(s);
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