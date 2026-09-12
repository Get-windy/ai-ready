package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import cn.aiedge.erp.sale.service.SalesDetailQueryService;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.mapper.ProductCategoryMapper;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售明细查询服务实现
 * 两阶段查询：先查明细表过滤商品级条件，再查表头过滤单据级条件，最后合并96列结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDetailQueryServiceImpl implements SalesDetailQueryService {

    private final SaleOutboundMapper outboundMapper;
    private final SaleOutboundItemMapper outboundItemMapper;
    /** 来源订单主数据（来源订单日期 / 所属行业类别） */
    private final SaleOrderMapper saleOrderMapper;
    /** 往来单位主数据（客户默认经手人） */
    private final PartyMapper partyMapper;
    /** 商品主数据（左侧分类树过滤） */
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public Page<Map<String, Object>> pageDetail(SalesDetailQueryDTO dto) {
        int pageNum = dto.getCurrent().intValue();
        int pageSize = dto.getSize().intValue();

        // 一行一明细：分页粒度 = 明细行（total 为明细行数，与表格逐行展示一致）
        List<Map<String, Object>> rows = queryRows(dto);
        int total = rows.size();
        int from = Math.min(Math.max(pageNum - 1, 0) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, total);
        result.setRecords(new ArrayList<>(rows.subList(from, to)));
        return result;
    }

    @Override
    public List<Map<String, Object>> listDetail(SalesDetailQueryDTO dto) {
        return queryRows(dto);
    }

    /**
     * 明细级两阶段查询 + 96 列组装（分页/导出共用同一口径）
     * <p>Phase A 明细过滤 → Phase B 表头过滤 → 主数据派生列回填 → 按单据日期倒序逐行输出</p>
     */
    private List<Map<String, Object>> queryRows(SalesDetailQueryDTO dto) {
        // ═══ Phase A: 查询明细表，应用明细级过滤 ═══
        LambdaQueryWrapper<SaleOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SaleOutboundItem::getDeleted, 0);
        applyItemFilters(itemWrapper, dto);
        List<SaleOutboundItem> matchedItems = outboundItemMapper.selectList(itemWrapper);
        if (matchedItems.isEmpty()) {
            return List.of();
        }
        Set<Long> candidateIds = matchedItems.stream()
                .map(SaleOutboundItem::getOutboundId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // ═══ Phase B: 查询表头，应用表头级过滤 ═══
        LambdaQueryWrapper<SaleOutbound> headerWrapper = new LambdaQueryWrapper<>();
        headerWrapper.eq(SaleOutbound::getDeleted, 0);
        headerWrapper.in(SaleOutbound::getId, candidateIds);
        applyHeaderFilters(headerWrapper, dto);
        List<SaleOutbound> headers = outboundMapper.selectList(headerWrapper);
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<Long, SaleOutbound> headerMap = headers.stream()
                .collect(Collectors.toMap(SaleOutbound::getId, o -> o, (a, b) -> a));

        // ═══ Phase C: 主数据派生列回填 + 逐行组装 ═══
        Map<Long, LocalDate> orderDateById = orderDateByOrderId(headers);
        Map<String, LocalDate> orderDateByNo = orderDateByOrderNo(headers);
        Map<Long, String> handlerByCustomer = defaultHandlerByCustomer(headers);

        List<SaleOutboundItem> rows = matchedItems.stream()
                .filter(i -> headerMap.containsKey(i.getOutboundId()))
                .sorted(rowComparator(headerMap))
                .toList();

        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (SaleOutboundItem item : rows) {
            SaleOutbound ob = headerMap.get(item.getOutboundId());
            LocalDate sourceOrderDate = null;
            if (ob.getOrderId() != null) {
                sourceOrderDate = orderDateById.get(ob.getOrderId());
            }
            if (sourceOrderDate == null && StringUtils.hasText(ob.getOrderNo())) {
                sourceOrderDate = orderDateByNo.get(ob.getOrderNo());
            }
            result.add(buildRowMap(ob, item, sourceOrderDate,
                    ob.getCustomerId() != null ? handlerByCustomer.get(ob.getCustomerId()) : null));
        }
        return result;
    }

    /** 明细行排序：单据日期倒序 → 制单时间倒序 → 单据ID倒序 → 行号升序 */
    private Comparator<SaleOutboundItem> rowComparator(Map<Long, SaleOutbound> headerMap) {
        return Comparator
                .comparing((SaleOutboundItem i) -> headerMap.get(i.getOutboundId()).getOutboundDate(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(i -> headerMap.get(i.getOutboundId()).getCreateTime(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(i -> headerMap.get(i.getOutboundId()).getId(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SaleOutboundItem::getLineNo,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** 来源订单日期：erp_sale_order.order_date，按订单ID批量回填 */
    private Map<Long, LocalDate> orderDateByOrderId(List<SaleOutbound> headers) {
        List<Long> ids = headers.stream().map(SaleOutbound::getOrderId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return saleOrderMapper.selectList(new LambdaQueryWrapper<SaleOrder>()
                        .select(SaleOrder::getId, SaleOrder::getOrderDate)
                        .in(SaleOrder::getId, ids))
                .stream()
                .filter(o -> o.getId() != null && o.getOrderDate() != null)
                .collect(Collectors.toMap(SaleOrder::getId, SaleOrder::getOrderDate, (a, b) -> a));
    }

    /** 来源订单日期：兼容仅有 order_no 手工来源的历史单据 */
    private Map<String, LocalDate> orderDateByOrderNo(List<SaleOutbound> headers) {
        List<String> nos = headers.stream().map(SaleOutbound::getOrderNo)
                .filter(StringUtils::hasText).distinct().toList();
        if (nos.isEmpty()) {
            return Map.of();
        }
        return saleOrderMapper.selectList(new LambdaQueryWrapper<SaleOrder>()
                        .select(SaleOrder::getOrderNo, SaleOrder::getOrderDate)
                        .in(SaleOrder::getOrderNo, nos))
                .stream()
                .filter(o -> StringUtils.hasText(o.getOrderNo()) && o.getOrderDate() != null)
                .collect(Collectors.toMap(SaleOrder::getOrderNo, SaleOrder::getOrderDate, (a, b) -> a));
    }

    /** 默认经手人：客户主数据 biz_party.default_handler_name，按客户ID批量回填 */
    private Map<Long, String> defaultHandlerByCustomer(List<SaleOutbound> headers) {
        List<Long> customerIds = headers.stream().map(SaleOutbound::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return partyMapper.selectList(new LambdaQueryWrapper<Party>()
                        .select(Party::getId, Party::getDefaultHandlerName)
                        .in(Party::getId, customerIds))
                .stream()
                .filter(p -> p.getId() != null && StringUtils.hasText(p.getDefaultHandlerName()))
                .collect(Collectors.toMap(Party::getId, Party::getDefaultHandlerName, (a, b) -> a));
    }

    /**
     * 最近成交价实时聚合（只读）
     * 从销售出库明细中按 product+往来单位 分组，取最近一次的成交价/折扣/日期。
     * 注意：本方法不含任何写操作；价格点的新增/修改/删除由 SalePriceTrackService 维护台账表。
     */
    @Override
    public Page<Map<String, Object>> pageRecentPriceAgg(SalesDetailQueryDTO dto) {
        int pageNum = dto.getCurrent().intValue();
        int pageSize = dto.getSize().intValue();

        LambdaQueryWrapper<SaleOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.select(
            SaleOutboundItem::getId,
            SaleOutboundItem::getOutboundId,
            SaleOutboundItem::getProductId,
            SaleOutboundItem::getProductCode,
            SaleOutboundItem::getProductName,
            SaleOutboundItem::getBarcode,
            SaleOutboundItem::getSpecification,
            SaleOutboundItem::getModel,
            SaleOutboundItem::getOrigin,
            SaleOutboundItem::getProductUnit,
            SaleOutboundItem::getUnitPrice,
            SaleOutboundItem::getDiscountRate
        );
        itemWrapper.eq(SaleOutboundItem::getDeleted, 0);
        applyItemFilters(itemWrapper, dto);
        itemWrapper.orderByDesc(SaleOutboundItem::getOutboundId);

        List<SaleOutboundItem> items = outboundItemMapper.selectList(itemWrapper);
        List<Long> outboundIds = items.stream().map(SaleOutboundItem::getOutboundId).distinct().toList();
        if (outboundIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        LambdaQueryWrapper<SaleOutbound> headerWrapper = new LambdaQueryWrapper<>();
        headerWrapper.select(
            SaleOutbound::getId,
            SaleOutbound::getCustomerId,
            SaleOutbound::getCustomerCode,
            SaleOutbound::getCustomerName,
            SaleOutbound::getOutboundDate,
            SaleOutbound::getUpdateTime
        );
        headerWrapper.in(SaleOutbound::getId, outboundIds);
        headerWrapper.eq(SaleOutbound::getDeleted, 0);
        applyHeaderFilters(headerWrapper, dto);
        List<SaleOutbound> outbounds = outboundMapper.selectList(headerWrapper);
        Map<Long, SaleOutbound> outboundMap = outbounds.stream()
                .collect(java.util.stream.Collectors.toMap(SaleOutbound::getId, o -> o, (a, b) -> a));

        // 按 productId + 往来单位 分组，保留最近一条
        Map<String, Map<String, Object>> aggMap = new LinkedHashMap<>();
        for (SaleOutboundItem item : items) {
            SaleOutbound o = outboundMap.get(item.getOutboundId());
            if (o == null || o.getCustomerId() == null) continue;
            String key = item.getProductId() + "_" + o.getCustomerId();
            if (aggMap.containsKey(key)) continue; // 已保留最近一条（按 outbound_id 倒序）
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", item.getProductId());
            row.put("productCode", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("itemCode", item.getProductCode());
            row.put("barcode", item.getBarcode());
            row.put("specification", item.getSpecification());
            row.put("model", item.getModel());
            row.put("origin", item.getOrigin());
            row.put("unit", item.getProductUnit() != null ? item.getProductUnit() : "");
            row.put("partnerId", o.getCustomerId());
            row.put("partnerCode", o.getCustomerCode());
            row.put("partnerName", o.getCustomerName());
            row.put("recentPrice", item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
            row.put("recentDiscountRate", item.getDiscountRate() != null ? item.getDiscountRate() : BigDecimal.ZERO);
            row.put("recentSaleDate", o.getOutboundDate() != null ? o.getOutboundDate().toString() : "");
            row.put("lastModifyTime", o.getUpdateTime() != null ? o.getUpdateTime().toString() : "");
            aggMap.put(key, row);
        }

        List<Map<String, Object>> all = new java.util.ArrayList<>(aggMap.values());
        int total = all.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, total);
        result.setRecords(all.subList(from, to));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 明细级过滤（作用于 erp_sale_outbound_item 表）
    // ═══════════════════════════════════════════════════════════════════
    private void applyItemFilters(LambdaQueryWrapper<SaleOutboundItem> w, SalesDetailQueryDTO dto) {
        // 商品分类（左侧分类树，含子分类）
        if (dto.getCategoryId() != null) {
            List<Long> categoryIds = expandCategoryIds(dto.getCategoryId());
            List<Long> productIds = categoryIds.isEmpty() ? List.of()
                    : productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .select(Product::getId)
                            .in(Product::getCategoryId, categoryIds))
                    .stream().map(Product::getId).filter(Objects::nonNull).toList();
            if (productIds.isEmpty()) {
                w.apply("1 = 0");
            } else {
                w.in(SaleOutboundItem::getProductId, productIds);
            }
        }
        // 商品名称/货号/条码
        if (hasText(dto.getProductName())) {
            w.like(SaleOutboundItem::getProductName, dto.getProductName());
        }
        if (hasText(dto.getProductCode())) {
            w.like(SaleOutboundItem::getProductCode, dto.getProductCode());
        }
        if (hasText(dto.getBarcode())) {
            w.like(SaleOutboundItem::getBarcode, dto.getBarcode());
        }
        // 品牌
        if (hasText(dto.getBrand())) {
            w.like(SaleOutboundItem::getBrand, dto.getBrand());
        }
        // 商品行属性
        if (hasText(dto.getProductAttribute())) {
            w.like(SaleOutboundItem::getProductAttribute, dto.getProductAttribute());
        }
        // 是否赠品
        if (dto.getIsGift() != null) {
            w.eq(SaleOutboundItem::getGift, dto.getIsGift());
        }
        // 是否促销商品（giftItem 字段标记促销品）
        if (dto.getIsPromoProduct() != null && dto.getIsPromoProduct()) {
            w.eq(SaleOutboundItem::getGiftItem, true);
        }
        // 价格范围
        if (dto.getMinPrice() != null) {
            w.ge(SaleOutboundItem::getUnitPrice, dto.getMinPrice());
        }
        if (dto.getMaxPrice() != null) {
            w.le(SaleOutboundItem::getUnitPrice, dto.getMaxPrice());
        }
        // 明细备注
        if (hasText(dto.getItemRemark())) {
            w.like(SaleOutboundItem::getRemark, dto.getItemRemark());
        }
        // 表体自定义字段（数字范围 1-7）
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum1, dto.getItemExtNum1Min(), dto.getItemExtNum1Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum2, dto.getItemExtNum2Min(), dto.getItemExtNum2Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum3, dto.getItemExtNum3Min(), dto.getItemExtNum3Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum4, dto.getItemExtNum4Min(), dto.getItemExtNum4Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum5, dto.getItemExtNum5Min(), dto.getItemExtNum5Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum6, dto.getItemExtNum6Min(), dto.getItemExtNum6Max());
        applyItemExtNumFilter(w, SaleOutboundItem::getExtNum7, dto.getItemExtNum7Min(), dto.getItemExtNum7Max());
        // 表体自定义字段（文本 1-2）
        if (hasText(dto.getItemExtText1())) {
            w.like(SaleOutboundItem::getExtText1, dto.getItemExtText1());
        }
        if (hasText(dto.getItemExtText2())) {
            w.like(SaleOutboundItem::getExtText2, dto.getItemExtText2());
        }
        // 表体自定义字段（往来单位/职员/部门）
        if (dto.getItemExtPartner() != null) {
            w.eq(SaleOutboundItem::getExtPartner, dto.getItemExtPartner());
        }
        if (dto.getItemExtStaff() != null) {
            w.eq(SaleOutboundItem::getExtStaff, dto.getItemExtStaff());
        }
        if (dto.getItemExtDept() != null) {
            w.eq(SaleOutboundItem::getExtDept, dto.getItemExtDept());
        }
    }

    /** 商品分类及其全部子孙分类ID（左侧分类树选中父节点时需覆盖子节点商品） */
    private List<Long> expandCategoryIds(Long rootCategoryId) {
        List<ProductCategory> all = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().select(ProductCategory::getId, ProductCategory::getParentId));
        Set<Long> result = new LinkedHashSet<>();
        result.add(rootCategoryId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ProductCategory c : all) {
                if (c.getParentId() != null && result.contains(c.getParentId()) && result.add(c.getId())) {
                    changed = true;
                }
            }
        }
        return new ArrayList<>(result);
    }

    private void applyItemExtNumFilter(LambdaQueryWrapper<SaleOutboundItem> w,
                                        com.baomidou.mybatisplus.core.toolkit.support.SFunction<SaleOutboundItem, BigDecimal> field,
                                        BigDecimal min, BigDecimal max) {
        if (min != null) w.ge(field, min);
        if (max != null) w.le(field, max);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 表头级过滤（作用于 erp_sale_outbound 表）
    // ═══════════════════════════════════════════════════════════════════
    private void applyHeaderFilters(LambdaQueryWrapper<SaleOutbound> w, SalesDetailQueryDTO dto) {
        // 日期范围（根据dateType选择不同字段）
        applyDateFilter(w, dto);

        // 单据编号
        if (hasText(dto.getDocumentNo())) {
            w.like(SaleOutbound::getOutboundNo, dto.getDocumentNo());
        }
        // 单据类型（erp_sale_outbound.outbound_type：0销售出库/1换货出库/2调拨出库/3其他出库）
        if (hasText(dto.getDocumentType())) {
            Integer documentType = parseIntSafe(dto.getDocumentType());
            if (documentType != null) {
                w.eq(SaleOutbound::getOutboundType, documentType);
            }
        }
        // 来源（erp_sale_outbound.source：PC/MOBILE/API/IMPORT）
        if (hasText(dto.getSource())) {
            w.eq(SaleOutbound::getSource, dto.getSource().toUpperCase());
        }
        // 所属行业类别（溯源：来源销售订单 erp_sale_order.industry_category）
        if (hasText(dto.getIndustryCategory())) {
            List<String> matchedOrderNos = saleOrderMapper.selectList(new LambdaQueryWrapper<SaleOrder>()
                            .select(SaleOrder::getOrderNo)
                            .like(SaleOrder::getIndustryCategory, dto.getIndustryCategory()))
                    .stream()
                    .map(SaleOrder::getOrderNo)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (matchedOrderNos.isEmpty()) {
                w.apply("1 = 0");
            } else {
                w.in(SaleOutbound::getOrderNo, matchedOrderNos);
            }
        }
        // 默认经手人（溯源：客户主数据 biz_party.default_handler_name）
        if (hasText(dto.getDefaultHandlerName())) {
            List<Long> matchedCustomerIds = partyMapper.selectList(new LambdaQueryWrapper<Party>()
                            .select(Party::getId)
                            .like(Party::getDefaultHandlerName, dto.getDefaultHandlerName()))
                    .stream()
                    .map(Party::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (matchedCustomerIds.isEmpty()) {
                w.apply("1 = 0");
            } else {
                w.in(SaleOutbound::getCustomerId, matchedCustomerIds);
            }
        }
        // 仓库
        if (hasText(dto.getWarehouseName())) {
            w.like(SaleOutbound::getWarehouseName, dto.getWarehouseName());
        }
        // 客户
        if (hasText(dto.getCustomerName())) {
            w.like(SaleOutbound::getCustomerName, dto.getCustomerName());
        }
        if (hasText(dto.getCustomerCode())) {
            w.like(SaleOutbound::getCustomerCode, dto.getCustomerCode());
        }
        // 收货信息
        if (hasText(dto.getReceiverName())) {
            w.like(SaleOutbound::getReceiverName, dto.getReceiverName());
        }
        if (hasText(dto.getReceiverPhone())) {
            w.like(SaleOutbound::getReceiverPhone, dto.getReceiverPhone());
        }
        if (hasText(dto.getShippingAddress())) {
            w.like(SaleOutbound::getShippingAddress, dto.getShippingAddress());
        }
        // 经手人/部门/制单人/记账人
        if (hasText(dto.getHandlerName())) {
            w.like(SaleOutbound::getSalesPersonName, dto.getHandlerName());
        }
        if (hasText(dto.getDepartmentName())) {
            w.like(SaleOutbound::getDepartmentName, dto.getDepartmentName());
        }
        if (hasText(dto.getCreatorName())) {
            w.like(SaleOutbound::getCreatorName, dto.getCreatorName());
        }
        if (hasText(dto.getBookkeeperName())) {
            w.like(SaleOutbound::getBookkeeperName, dto.getBookkeeperName());
        }
        // 结算状态
        if (hasText(dto.getSettlementStatus())) {
            w.eq(SaleOutbound::getSettlementStatus, dto.getSettlementStatus());
        }
        // 来源订单
        if (hasText(dto.getSourceOrder())) {
            w.like(SaleOutbound::getOrderNo, dto.getSourceOrder());
        }
        if (Boolean.TRUE.equals(dto.getHasSourceOrder())) {
            w.isNotNull(SaleOutbound::getOrderNo);
            w.ne(SaleOutbound::getOrderNo, "");
        }
        // 产生方式
        if (hasText(dto.getGenerationMethod())) {
            w.eq(SaleOutbound::getGenerationMethod, dto.getGenerationMethod());
        }
        // 销售类型（对应outboundType字段：0正常销售/1换货/2调拨/3其他）
        if (hasText(dto.getSalesType())) {
            Integer salesTypeInt = parseIntSafe(dto.getSalesType());
            if (salesTypeInt != null) w.eq(SaleOutbound::getOutboundType, salesTypeInt);
        }
        // 物流
        if (hasText(dto.getLogisticsCompany())) {
            w.like(SaleOutbound::getLogisticsCompany, dto.getLogisticsCompany());
        }
        if (hasText(dto.getTrackingNumber())) {
            w.like(SaleOutbound::getTrackingNumber, dto.getTrackingNumber());
        }
        if (hasText(dto.getDeliveryMethod())) {
            w.eq(SaleOutbound::getDeliveryMethod, dto.getDeliveryMethod());
        }
        if (hasText(dto.getDeliveryDriver())) {
            w.like(SaleOutbound::getDeliveryDriver, dto.getDeliveryDriver());
        }
        // 区域
        if (hasText(dto.getRegion())) {
            w.like(SaleOutbound::getRegion, dto.getRegion());
        }
        // 备注
        if (hasText(dto.getRemark())) {
            w.like(SaleOutbound::getRemark, dto.getRemark());
        }
        if (hasText(dto.getBuyerRemark())) {
            w.like(SaleOutbound::getBuyerRemark, dto.getBuyerRemark());
        }
        // 结算完成时间
        if (hasText(dto.getSettlementTimeStart())) {
            w.ge(SaleOutbound::getCompletedTime, parseLocalDateToStartOfDay(dto.getSettlementTimeStart()));
        }
        if (hasText(dto.getSettlementTimeEnd())) {
            w.le(SaleOutbound::getCompletedTime, parseLocalDateToEndOfDay(dto.getSettlementTimeEnd()));
        }
        // 显示红冲（默认不显示，取消/红冲状态的单据）
        if (!Boolean.TRUE.equals(dto.getShowRed())) {
            // status=12 表示已取消/红冲，排除
            w.ne(SaleOutbound::getStatus, 12);
        }
        // 仅统计车辆库
        if (Boolean.TRUE.equals(dto.getOnlyVehicleWarehouse())) {
            w.like(SaleOutbound::getWarehouseName, "车辆");
        }
    }

    private void applyDateFilter(LambdaQueryWrapper<SaleOutbound> w, SalesDetailQueryDTO dto) {
        String startDateStr = dto.getStartDate();
        String endDateStr = dto.getEndDate();

        if (!hasText(startDateStr) && !hasText(endDateStr)) return;

        String dateType = dto.getDateType() != null ? dto.getDateType() : "documentDate";
        LocalDate start = parseLocalDateSafely(startDateStr);
        LocalDate end = parseLocalDateSafely(endDateStr);

        if (start == null && end == null) return; // 如果日期解析失败，则跳过日期过滤

        switch (dateType) {
            case "createTime":
                if (start != null) w.ge(SaleOutbound::getCreateTime, start.atStartOfDay());
                if (end != null) w.le(SaleOutbound::getCreateTime, end.atTime(23, 59, 59));
                break;
            case "bookkeepingTime":
                if (start != null) w.ge(SaleOutbound::getBookkeepingTime, start.atStartOfDay());
                if (end != null) w.le(SaleOutbound::getBookkeepingTime, end.atTime(23, 59, 59));
                break;
            case "settlementTime":
                if (start != null) w.ge(SaleOutbound::getCompletedTime, start.atStartOfDay());
                if (end != null) w.le(SaleOutbound::getCompletedTime, end.atTime(23, 59, 59));
                break;
            default: // documentDate
                if (start != null) w.ge(SaleOutbound::getOutboundDate, start);
                if (end != null) w.le(SaleOutbound::getOutboundDate, end);
                break;
        }
    }

    private LocalDate parseLocalDateSafely(String dateStr) {
        if (!hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("无效的日期格式: {}, 将跳过日期过滤", dateStr, e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 构建96列结果行
    // ═══════════════════════════════════════════════════════════════════
    private Map<String, Object> buildRowMap(SaleOutbound ob, SaleOutboundItem item,
                                            LocalDate sourceOrderDate, String defaultHandlerName) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (ob != null) {
            // ── 表头字段（列1-27） ──
            map.put("docDate", ob.getOutboundDate());                          // 1 单据日期
            map.put("docNo", ob.getOutboundNo());                               // 2 单据编号
            map.put("docType", mapDocType(ob.getOutboundType()));               // 3 单据类型
            map.put("warehouseName", ob.getWarehouseName());                    // 4 仓库
            map.put("customerName", ob.getCustomerName());                      // 5 客户
            map.put("customerCode", ob.getCustomerCode());                      // 6 客户编号
            map.put("customerLevel", ob.getCustomerLevel());                    // 7 客户级别
            map.put("receiverName", ob.getReceiverName());                      // 8 收货人
            map.put("receiverPhone", ob.getReceiverPhone());                    // 9 联系电话
            map.put("shippingAddress", ob.getShippingAddress());                // 10 收货地址
            map.put("custExtText1", ob.getExtText1());                          // 11 客户自定义字段1
            map.put("custExtText2", ob.getExtText2());                          // 12 客户自定义字段2
            map.put("custExtText3", ob.getExtText3());                          // 13 客户自定义字段3
            map.put("custExtText4", ob.getExtText4());                          // 14 客户自定义字段4
            map.put("custExtText5", ob.getExtText5());                          // 15 客户自定义字段5
            map.put("logisticsCompany", ob.getLogisticsCompany());              // 16 物流公司
            map.put("trackingNumber", ob.getTrackingNumber());                  // 17 运单号
            map.put("region", ob.getRegion());                                  // 18 区域
            map.put("buyerRemark", ob.getBuyerRemark());                        // 19 买家备注
            map.put("customerRemark", ob.getCustomerRemark());                  // 20 客户备注
            map.put("sourceOrder", ob.getOrderNo());                            // 21 来源订单
            map.put("sourceOrderDate", sourceOrderDate);                        // 22 来源订单日期（销售订单 order_date）
            map.put("generationMethod", ob.getGenerationMethod());              // 23 产生方式
            map.put("handlerName", ob.getSalesPersonName());                    // 24 经手人
            map.put("departmentName", ob.getDepartmentName());                  // 25 部门
            map.put("defaultHandlerName", defaultHandlerName);                  // 26 默认经手人（客户主数据）
            map.put("settlementStatus", settlementStatusLabel(ob.getSettlementStatus())); // 27 结算状态

            // ── 表头系统字段（列89-96） ──
            map.put("remark", ob.getRemark());                                  // 89 单据备注
            map.put("bookkeeperName", ob.getBookkeeperName());                  // 90 记账人
            map.put("creatorName", ob.getCreatorName());                        // 91 制单人
            map.put("bookkeepingTime", ob.getBookkeepingTime());                // 92 记账时间
            map.put("createTime", ob.getCreateTime());                          // 93 制单时间
            map.put("settlementCompleteTime", ob.getCompletedTime());           // 94 结算完成时间
            map.put("printCount", ob.getPrintCount());                          // 95 打印次数
            map.put("deliveryDriver", ob.getDeliveryDriver());                  // 96 配送司机

            // ── 部分明细列需要从表头取的字段 ──
            map.put("deliveryMethod", deliveryMethodLabel(ob.getDeliveryMethod())); // 35 配送方式
            map.put("salesType", mapSaleType(ob.getOutboundType()));             // 86 销售类型
        } else {
            map.put("deliveryMethod", null);
            map.put("salesType", null);
        }

        // ── 明细字段（列28-88） ──
        map.put("productName", item.getProductName());                          // 28 商品名称
        map.put("productCode", item.getProductCode());                          // 29 货号
        map.put("barcode", item.getBarcode());                                  // 30 条码
        map.put("smallUnitBarcode", item.getSmallUnitBarcode());                // 31 小单位条码
        map.put("specification",                                               // 32 规格
                item.getProductSpec() != null ? item.getProductSpec() : item.getSpecification());
        map.put("model", item.getModel());                                      // 33 型号
        map.put("origin", item.getOrigin());                                    // 34 产地
        // 35 deliveryMethod 已在上面从表头取
        map.put("brand", item.getBrand());                                      // 36 品牌

        // 表体自定义字段 1-7（数字）(37-43)
        map.put("itemExtNum1", item.getExtNum1());                              // 37
        map.put("itemExtNum2", item.getExtNum2());                              // 38
        map.put("itemExtNum3", item.getExtNum3());                              // 39
        map.put("itemExtNum4", item.getExtNum4());                              // 40
        map.put("itemExtNum5", item.getExtNum5());                              // 41
        map.put("itemExtNum6", item.getExtNum6());                              // 42
        map.put("itemExtNum7", item.getExtNum7());                              // 43

        // 表体自定义字段（文本/往来/职员/部门）(40-46)
        // 对应文档列：40-41 = 表体自定义4-5(文本)，44-46 = 表体自定义8(往来单位)/9(职员)/10(部门)
        map.put("itemExtText1", item.getExtText1());                            // 40 表体自定义4(文本)
        map.put("itemExtText2", item.getExtText2());                            // 41 表体自定义5(文本)
        map.put("itemExtPartner", item.getExtPartner());                        // 44 表体自定义8(往来单位)
        map.put("itemExtStaff", item.getExtStaff());                            // 45 表体自定义9(职员)
        map.put("itemExtDept", item.getExtDept());                              // 46 表体自定义10(部门)

        // 数量/包装 (47-58)
        map.put("salesQuantity", item.getOutboundQuantity());                   // 47 销售数量
        map.put("salesQuantityUnit", item.getProductUnit());                    // 48 销售数量单位
        map.put("commonUnitQuantity", item.getConversionResult());              // 49 销售常用单位数量
        map.put("commonUnit", item.getConversionRelation());                    // 50 销售常用单位
        map.put("batchBarcode", item.getBatchNo());                             // 51 批次条码
        map.put("productionDate", item.getProductionDate());                    // 52 生产日期
        map.put("expiryDate", item.getExpiryDate());                            // 53 到期日期
        map.put("bigPack", item.getBigPack());                                  // 54 大包装
        map.put("midPack", item.getMidPack());                                  // 55 中包装
        map.put("smallPack", item.getSmallPack());                              // 56 小包装
        map.put("smallUnit", item.getSmallUnit());                              // 57 小单位
        map.put("smallUnitQuantity", item.getSmallUnitQuantity());              // 58 小单位数量

        // 价格等级 (59-66)
        map.put("priceLevel1", item.getPriceLevel1());                          // 59 餐饮店
        map.put("priceLevel2", item.getPriceLevel2());                          // 60 食堂团餐
        map.put("priceLevel3", item.getPriceLevel3());                          // 61 自助vip
        map.put("priceLevel4", item.getPriceLevel4());                          // 62 大团餐
        map.put("priceLevel5", item.getPriceLevel5());                          // 63 特价客户
        map.put("priceLevel6", item.getPriceLevel6());                          // 64 外围餐饮店
        map.put("priceLevel7", item.getPriceLevel7());                          // 65 重点vip01
        map.put("priceLevel8", item.getPriceLevel8());                          // 66 连锁vip

        // 定价 (67-80)
        map.put("unitPrice", item.getUnitPrice());                              // 67 单价
        map.put("smallUnitPrice", item.getSmallUnitPrice());                    // 68 小单位单价
        map.put("amount", item.getLineAmount());                                // 69 金额
        map.put("discountRate", item.getDiscountRate());                        // 70 折扣(%)
        map.put("discountedPrice", item.getDiscountedPrice());                  // 71 折后单价
        map.put("discountedAmount", item.getDiscountedAmount());                // 72 折后金额
        map.put("favorableDiscountRate", item.getFavorableDiscountRate());      // 73 优惠折扣
        map.put("favorableUnitPrice", item.getFavorableUnitPrice());            // 74 优惠后单价
        map.put("favorableAmount", item.getFavorableAmount());                  // 75 优惠后金额
        map.put("salesRevenue", item.getFavorableAmount());                     // 76 销售收入 (= 优惠后金额)
        map.put("costPrice", item.getCostPrice());                              // 77 成本单价
        map.put("costAmount", item.getCostAmount());                            // 78 成本金额
        map.put("grossProfit", item.getGrossProfit());                          // 79 毛利
        map.put("grossProfitRate", calcGrossProfitRate(item));                  // 80 毛利率(%)

        // 参考/物理 (81-85)
        map.put("wholesalePrice", item.getWholesalePrice());                    // 81 批发价
        map.put("retailPrice", item.getRetailPrice());                          // 82 零售价
        map.put("minSalePrice", item.getMinSalePrice());                        // 83 最低售价
        map.put("weight", item.getWeight());                                    // 84 重量(kg)
        map.put("volume", item.getVolume());                                    // 85 体积(m³)

        // 类型属性 (86-88)
        // 86 salesType 已在上面从表头取
        map.put("productAttribute", item.getProductAttribute());                // 87 商品行属性
        map.put("itemRemark", item.getRemark());                                // 88 明细备注

        return map;
    }

    /**
     * 毛利率计算 = 毛利 / 优惠后金额 * 100（优惠后金额 > 0 时）
     */
    private BigDecimal calcGrossProfitRate(SaleOutboundItem item) {
        BigDecimal favorableAmount = item.getFavorableAmount();
        BigDecimal grossProfit = item.getGrossProfit();
        if (favorableAmount == null || grossProfit == null) return null;
        if (favorableAmount.compareTo(BigDecimal.ZERO) == 0) return null;
        return grossProfit.multiply(new BigDecimal("100"))
                .divide(favorableAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * 单据类型映射（erp_sale_outbound.outbound_type）
     * 本页数据源为销售出库单，故不存在「退货出库」（退货单在 erp_sale_return_doc）。
     */
    private String mapDocType(Integer outboundType) {
        if (outboundType == null) return "销售出库";
        return switch (outboundType) {
            case 0 -> "销售出库";
            case 1 -> "换货出库";
            case 2 -> "调拨出库";
            case 3 -> "其他出库";
            default -> "销售出库";
        };
    }

    /**
     * 销售类型映射（与销售出库单列表口径一致：0正常销售/1换货/2调拨/3其他）
     */
    private String mapSaleType(Integer outboundType) {
        if (outboundType == null) return "正常销售";
        return switch (outboundType) {
            case 0 -> "正常销售";
            case 1 -> "换货";
            case 2 -> "调拨";
            case 3 -> "其他";
            default -> "正常销售";
        };
    }

    /** 结算状态：落库为 unsettled/partial/settled，展示为中文 */
    private String settlementStatusLabel(String status) {
        if (!hasText(status)) return "";
        return switch (status.toLowerCase()) {
            case "unsettled" -> "未结算";
            case "partial", "partial_paid" -> "部分结算";
            case "settled", "paid" -> "已结算";
            default -> status;
        };
    }

    /** 配送方式：落库为 delivery/self/logistics/express，展示为中文 */
    private String deliveryMethodLabel(String method) {
        if (!hasText(method)) return "";
        return switch (method.toLowerCase()) {
            case "delivery" -> "配送";
            case "self" -> "自提";
            case "logistics" -> "物流";
            case "express" -> "快递";
            default -> method;
        };
    }

    private LocalDateTime parseLocalDateToStartOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atStartOfDay();
        } catch (Exception e) {
            log.warn("无效的日期格式: {}, 将跳过日期过滤", dateString, e);
            return null;
        }
    }

    private LocalDateTime parseLocalDateToEndOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atTime(23, 59, 59);
        } catch (Exception e) {
            log.warn("无效的日期格式: {}, 将跳过日期过滤", dateString, e);
            return null;
        }
    }

    private boolean hasText(String s) {
        return StringUtils.hasText(s);
    }

    private Integer parseIntSafe(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的整数查询参数: {}", value);
            return null;
        }
    }
}
