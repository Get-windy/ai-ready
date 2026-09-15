package cn.aiedge.erp.sale.outbound.service.impl;

import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.crm.customer.service.CustomerCreditService;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.dto.VoucherItemDTO;
import cn.aiedge.erp.finance.service.VoucherService;
import cn.aiedge.erp.pricing.service.PriceEngineService;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationResult;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundQueryDTO;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.enums.OutboundStatus;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.service.integration.SalesAccountingService;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.service.ProductCategoryService;
import cn.aiedge.erp.stock.service.StockService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOutboundServiceImpl extends ServiceImpl<SaleOutboundMapper, SaleOutbound> implements SaleOutboundService {

    private final SaleOutboundItemMapper outboundItemMapper;
    private final ISaleOrderService saleOrderService;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final StockService stockService;
    private final PriceEngineService priceEngineService;
    private final CustomerCreditService customerCreditService;
    private final VoucherService voucherService;
    private final SalesAccountingService salesAccountingService;
    private final ProductMapper productMapper;
    private final ProductCategoryService productCategoryService;
    /** 库存变动唯一写入口在 WMS（InventoryChangeEvent → InventoryService），ERP 侧只发布请求 */
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public SaleOutbound getByOutboundNo(String outboundNo) {
        return lambdaQuery()
                .eq(SaleOutbound::getOutboundNo, outboundNo)
                .eq(SaleOutbound::getDeleted, 0)
                .one();
    }

    @Override
    public Page<SaleOutbound> pageList(SaleOutboundQueryDTO query) {
        LambdaQueryWrapper<SaleOutbound> wrapper = buildDocWrapper(query);
        wrapper.orderByDesc(SaleOutbound::getCreateTime);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    /**
     * 构建单据级查询条件（对标文档「按单据」40 项查询条件 + 按明细 Tab 的单据维度条件）。
     * <p>单一实现点：按单据分页 / 按明细分页 / 导出 均复用本方法，避免条件漂移。</p>
     */
    private LambdaQueryWrapper<SaleOutbound> buildDocWrapper(SaleOutboundQueryDTO q) {
        LambdaQueryWrapper<SaleOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOutbound::getDeleted, 0);
        if (q == null) {
            return wrapper;
        }
        // 1 日期
        if (StringUtils.isNotBlank(q.getDateStart())) {
            wrapper.ge(SaleOutbound::getOutboundDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.isNotBlank(q.getDateEnd())) {
            wrapper.le(SaleOutbound::getOutboundDate, LocalDate.parse(q.getDateEnd()));
        }
        // 2 单据编号
        if (StringUtils.isNotBlank(q.getOutboundNo())) {
            wrapper.like(SaleOutbound::getOutboundNo, q.getOutboundNo());
        }
        // 3 客户
        if (q.getCustomerId() != null) {
            wrapper.eq(SaleOutbound::getCustomerId, q.getCustomerId());
        }
        if (StringUtils.isNotBlank(q.getCustomerName())) {
            wrapper.like(SaleOutbound::getCustomerName, q.getCustomerName());
        }
        // 4 经手人 / 5 部门 / 6 仓库
        if (q.getSalesPersonId() != null) {
            wrapper.eq(SaleOutbound::getSalesPersonId, q.getSalesPersonId());
        }
        if (StringUtils.isNotBlank(q.getSalesPersonName())) {
            wrapper.like(SaleOutbound::getSalesPersonName, q.getSalesPersonName());
        }
        if (StringUtils.isNotBlank(q.getDepartmentName())) {
            wrapper.like(SaleOutbound::getDepartmentName, q.getDepartmentName());
        }
        if (q.getWarehouseId() != null) {
            wrapper.eq(SaleOutbound::getWarehouseId, q.getWarehouseId());
        }
        if (StringUtils.isNotBlank(q.getWarehouseName())) {
            wrapper.like(SaleOutbound::getWarehouseName, q.getWarehouseName());
        }
        // 7 单据状态 / 8 结算状态 / 9 结款方式
        if (q.getStatus() != null) {
            wrapper.eq(SaleOutbound::getStatus, q.getStatus());
        }
        if (StringUtils.isNotBlank(q.getSettlementStatus())) {
            wrapper.eq(SaleOutbound::getSettlementStatus, q.getSettlementStatus());
        }
        if (StringUtils.isNotBlank(q.getSettlementMethod())) {
            wrapper.eq(SaleOutbound::getSettlementMethod, q.getSettlementMethod());
        }
        // 10 来源订单
        if (StringUtils.isNotBlank(q.getSourceOrder())) {
            wrapper.like(SaleOutbound::getOrderNo, q.getSourceOrder());
        }
        // 11-13 备注/摘要
        if (StringUtils.isNotBlank(q.getRemark())) {
            wrapper.like(SaleOutbound::getRemark, q.getRemark());
        }
        if (StringUtils.isNotBlank(q.getBuyerRemark())) {
            wrapper.like(SaleOutbound::getBuyerRemark, q.getBuyerRemark());
        }
        if (StringUtils.isNotBlank(q.getSummary())) {
            wrapper.like(SaleOutbound::getSummary, q.getSummary());
        }
        // 14-16 制单人/审核人/记账人
        if (StringUtils.isNotBlank(q.getCreatorName())) {
            wrapper.like(SaleOutbound::getCreatorName, q.getCreatorName());
        }
        if (StringUtils.isNotBlank(q.getAuditorName())) {
            wrapper.like(SaleOutbound::getAuditorName, q.getAuditorName());
        }
        if (StringUtils.isNotBlank(q.getBookkeeperName())) {
            wrapper.like(SaleOutbound::getBookkeeperName, q.getBookkeeperName());
        }
        // 17-21 表头自定义字段
        if (q.getExtNum1() != null) {
            wrapper.eq(SaleOutbound::getExtNum1, q.getExtNum1());
        }
        if (q.getExtNum2() != null) {
            wrapper.eq(SaleOutbound::getExtNum2, q.getExtNum2());
        }
        if (StringUtils.isNotBlank(q.getExtText1())) {
            wrapper.like(SaleOutbound::getExtText1, q.getExtText1());
        }
        if (StringUtils.isNotBlank(q.getExtText2())) {
            wrapper.like(SaleOutbound::getExtText2, q.getExtText2());
        }
        if (StringUtils.isNotBlank(q.getExtText3())) {
            wrapper.like(SaleOutbound::getExtText3, q.getExtText3());
        }
        // 22-24 收货信息
        if (StringUtils.isNotBlank(q.getReceiverName())) {
            wrapper.like(SaleOutbound::getReceiverName, q.getReceiverName());
        }
        if (StringUtils.isNotBlank(q.getReceiverPhone())) {
            wrapper.like(SaleOutbound::getReceiverPhone, q.getReceiverPhone());
        }
        if (StringUtils.isNotBlank(q.getShippingAddress())) {
            wrapper.like(SaleOutbound::getShippingAddress, q.getShippingAddress());
        }
        // 25-26 物流公司/运单号
        if (StringUtils.isNotBlank(q.getLogisticsCompany())) {
            wrapper.like(SaleOutbound::getLogisticsCompany, q.getLogisticsCompany());
        }
        if (StringUtils.isNotBlank(q.getTrackingNumber())) {
            wrapper.and(w -> w.like(SaleOutbound::getTrackingNumber, q.getTrackingNumber())
                    .or().like(SaleOutbound::getWaybillNo, q.getTrackingNumber()));
        }
        if (StringUtils.isNotBlank(q.getLogisticsRemark())) {
            wrapper.like(SaleOutbound::getLogisticsRemark, q.getLogisticsRemark());
        }
        if (StringUtils.isNotBlank(q.getDeliveryDriver())) {
            wrapper.like(SaleOutbound::getDeliveryDriver, q.getDeliveryDriver());
        }
        // 27-30 收款账户 1-4
        if (StringUtils.isNotBlank(q.getPaymentAccount1())) {
            wrapper.like(SaleOutbound::getPaymentAccount1, q.getPaymentAccount1());
        }
        if (StringUtils.isNotBlank(q.getPaymentAccount2())) {
            wrapper.like(SaleOutbound::getPaymentAccount2, q.getPaymentAccount2());
        }
        if (StringUtils.isNotBlank(q.getPaymentAccount3())) {
            wrapper.like(SaleOutbound::getPaymentAccount3, q.getPaymentAccount3());
        }
        if (StringUtils.isNotBlank(q.getPaymentAccount4())) {
            wrapper.like(SaleOutbound::getPaymentAccount4, q.getPaymentAccount4());
        }
        // 31 区域 / 32 产生方式 / 33 销售类型 / 35 配送方式
        if (StringUtils.isNotBlank(q.getRegion())) {
            wrapper.like(SaleOutbound::getRegion, q.getRegion());
        }
        if (StringUtils.isNotBlank(q.getGenerationMethod())) {
            wrapper.like(SaleOutbound::getGenerationMethod, q.getGenerationMethod());
        }
        if (q.getOutboundType() != null) {
            wrapper.eq(SaleOutbound::getOutboundType, q.getOutboundType());
        }
        if (StringUtils.isNotBlank(q.getDeliveryMethod())) {
            wrapper.eq(SaleOutbound::getDeliveryMethod, q.getDeliveryMethod());
        }
        // 34 商品行属性（明细维度 → 先解析命中单据集合）
        if (StringUtils.isNotBlank(q.getProductAttribute())) {
            List<Long> matched = outboundItemMapper.selectList(new LambdaQueryWrapper<SaleOutboundItem>()
                            .eq(SaleOutboundItem::getDeleted, 0)
                            .eq(SaleOutboundItem::getProductAttribute, q.getProductAttribute())
                            .select(SaleOutboundItem::getOutboundId))
                    .stream().map(SaleOutboundItem::getOutboundId).distinct().toList();
            wrapper.in(SaleOutbound::getId, matched.isEmpty() ? List.of(-1L) : matched);
        }
        // 36 配送司机 / 37 打印次数 / 38 本单金额
        if (q.getPrintCount() != null) {
            wrapper.eq(SaleOutbound::getPrintCount, q.getPrintCount());
        }
        if (q.getTotalAmount() != null) {
            wrapper.eq(SaleOutbound::getTotalAmount, q.getTotalAmount());
        }
        // 39 显示红冲：默认隐藏红字（负数金额）单据
        if (!Boolean.TRUE.equals(q.getShowRed())) {
            wrapper.and(w -> w.isNull(SaleOutbound::getTotalAmount)
                    .or().ge(SaleOutbound::getTotalAmount, BigDecimal.ZERO));
        }
        // 40 仅显示异常记账单据：已完成但未生成凭证（记账时间为空）
        if (Boolean.TRUE.equals(q.getShowAbnormal())) {
            wrapper.eq(SaleOutbound::getStatus, OutboundStatus.COMPLETED.getCode())
                    .isNull(SaleOutbound::getBookkeepingTime);
        }
        // 发货查询固定项：配送状态 / 配送线路（由 DMS 配送任务反查到的出库单号集合做精确过滤）
        if (q.getOutboundNos() != null && !q.getOutboundNos().isEmpty()) {
            wrapper.in(SaleOutbound::getOutboundNo, q.getOutboundNos());
        }
        // 通用关键词（单据编号/来源订单/客户名）
        if (StringUtils.isNotBlank(q.getKeyword())) {
            String kw = q.getKeyword();
            wrapper.and(w -> w.like(SaleOutbound::getOutboundNo, kw)
                    .or().like(SaleOutbound::getOrderNo, kw)
                    .or().like(SaleOutbound::getCustomerName, kw));
        }
        if (q.getProductId() != null || StringUtils.isNotBlank(q.getCategoryId())) {
            // 按明细 Tab 的商品/分类筛选：命中的单据集合由明细侧决定
            // 此处不追加单据条件（由 pageDetail 在明细查询中处理）
            log.debug("按明细商品维度筛选由 pageDetail 处理: productId={}, categoryId={}",
                    q.getProductId(), q.getCategoryId());
        }
        return wrapper;
    }

    /** 商品分类树筛选：解析分类（含子孙）对应的商品 ID 集合；无匹配时返回空集合 */
    private List<Long> resolveProductIdsByCategory(String categoryId) {
        if (StringUtils.isBlank(categoryId) || "0".equals(categoryId)) {
            return null;
        }
        Long rootId;
        try {
            rootId = Long.valueOf(categoryId);
        } catch (NumberFormatException e) {
            return null;
        }
        List<ProductCategory> all = productCategoryService.list();
        java.util.Set<Long> categoryIds = new java.util.HashSet<>();
        categoryIds.add(rootId);
        boolean expanded = true;
        while (expanded) {
            expanded = false;
            for (ProductCategory c : all) {
                if (c.getParentId() != null && categoryIds.contains(c.getParentId()) && categoryIds.add(c.getId())) {
                    expanded = true;
                }
            }
        }
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getCategoryId, categoryIds)
                        .select(Product::getId));
        return products.stream().map(Product::getId).toList();
    }

    @Override
    public Page<Map<String, Object>> pageDetail(SaleOutboundQueryDTO query) {
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // ── 1. 单据维度筛选（40 项查询条件的单据侧）→ 命中单据 ID 集合 ──
        List<Long> docIds = list(buildDocWrapper(query).select(SaleOutbound::getId))
                .stream().map(SaleOutbound::getId).toList();
        if (docIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // ── 2. 明细维度筛选（按明细 Tab 专属条件） ──
        LambdaQueryWrapper<SaleOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SaleOutboundItem::getDeleted, 0);
        itemWrapper.in(SaleOutboundItem::getOutboundId, docIds);
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String kw = query.getKeyword();
            itemWrapper.and(w -> w.like(SaleOutboundItem::getProductName, kw)
                    .or().like(SaleOutboundItem::getProductCode, kw));
        }
        if (StringUtils.isNotBlank(query.getProductName())) {
            itemWrapper.and(w -> w.like(SaleOutboundItem::getProductName, query.getProductName())
                    .or().like(SaleOutboundItem::getProductCode, query.getProductName()));
        }
        if (query.getProductId() != null) {
            itemWrapper.eq(SaleOutboundItem::getProductId, query.getProductId());
        }
        if (StringUtils.isNotBlank(query.getProductAttribute())) {
            itemWrapper.eq(SaleOutboundItem::getProductAttribute, query.getProductAttribute());
        }
        if (StringUtils.isNotBlank(query.getItemRemark())) {
            itemWrapper.like(SaleOutboundItem::getRemark, query.getItemRemark());
        }
        if (Boolean.TRUE.equals(query.getGift())) {
            itemWrapper.eq(SaleOutboundItem::getGift, true);
        }
        // 促销商品：命中优惠折扣的商品行
        if (Boolean.TRUE.equals(query.getPromoProduct())) {
            itemWrapper.gt(SaleOutboundItem::getFavorableDiscountRate, BigDecimal.ZERO);
        }
        // 商品分类树：解析分类（含子孙）对应的商品
        List<Long> categoryProductIds = resolveProductIdsByCategory(query.getCategoryId());
        if (categoryProductIds != null) {
            itemWrapper.in(SaleOutboundItem::getProductId,
                    categoryProductIds.isEmpty() ? List.of(-1L) : categoryProductIds);
        }

        // ── 3. 分页（口径 = 明细行数，与对标一致） ──
        itemWrapper.orderByDesc(SaleOutboundItem::getOutboundId).orderByAsc(SaleOutboundItem::getLineNo);
        Page<SaleOutboundItem> itemPage = outboundItemMapper.selectPage(new Page<>(pageNum, pageSize), itemWrapper);

        List<SaleOutboundItem> filteredItems = itemPage.getRecords();
        List<Long> pageDocIds = filteredItems.stream().map(SaleOutboundItem::getOutboundId).distinct().toList();
        Map<Long, SaleOutbound> outboundMap = pageDocIds.isEmpty() ? Map.of()
                : listByIds(pageDocIds).stream().collect(Collectors.toMap(SaleOutbound::getId, o -> o));

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, itemPage.getTotal());
        List<Map<String, Object>> records = filteredItems.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            SaleOutbound outbound = outboundMap.get(item.getOutboundId());
            if (outbound != null) {
                // 单据信息
                map.put("outboundDate", outbound.getOutboundDate());
                map.put("outboundNo", outbound.getOutboundNo());
                map.put("orderNo", outbound.getOrderNo());
                map.put("status", outbound.getStatus());
                map.put("settlementStatus", outbound.getSettlementStatus());
                map.put("warehouseName", outbound.getWarehouseName());
                map.put("customerName", outbound.getCustomerName());
                map.put("customerCode", outbound.getCustomerCode());
                map.put("customerLevel", outbound.getCustomerLevel());
                map.put("receiverName", outbound.getReceiverName());
                map.put("receiverPhone", outbound.getReceiverPhone());
                map.put("shippingAddress", outbound.getShippingAddress());
                map.put("customerRemark", outbound.getCustomerRemark());
                map.put("salesPersonName", outbound.getSalesPersonName());
                map.put("departmentName", outbound.getDepartmentName());
                map.put("region", outbound.getRegion());
                map.put("location", outbound.getLocation());
                // 金额/计数
                map.put("totalAmount", outbound.getTotalAmount());
                map.put("settledAmount", outbound.getSettledAmount());
                map.put("totalQuantity", outbound.getTotalQuantity());
                // 表头自定义字段
                // 表头自定义字段前缀 header*，避免与表体自定义字段(extNum1..)键冲突
                map.put("headerExtNum1", outbound.getExtNum1());
                map.put("headerExtNum2", outbound.getExtNum2());
                map.put("headerExtNum3", outbound.getExtNum3());
                map.put("headerExtNum4", outbound.getExtNum4());
                map.put("headerExtNum5", outbound.getExtNum5());
                map.put("headerExtText1", outbound.getExtText1());
                map.put("headerExtText2", outbound.getExtText2());
                map.put("headerExtText3", outbound.getExtText3());
                map.put("headerExtText4", outbound.getExtText4());
                map.put("headerExtText5", outbound.getExtText5());
                // 表尾自定义字段
                map.put("footerExtText1", outbound.getFooterExtText1());
                map.put("footerExtText2", outbound.getFooterExtText2());
                // 系统字段
                map.put("creatorName", outbound.getCreatorName());
                map.put("bookkeeperName", outbound.getBookkeeperName());
                map.put("auditorName", outbound.getAuditorName());
                map.put("printCount", outbound.getPrintCount());
                // 单据备注用 docRemark，避免与明细备注(remark)键冲突
                map.put("docRemark", outbound.getRemark());
                map.put("buyerRemark", outbound.getBuyerRemark());
                map.put("logisticsRemark", outbound.getLogisticsRemark());
                map.put("summary", outbound.getSummary());
                map.put("generationMethod", outbound.getGenerationMethod());
                // 物流
                map.put("logisticsCompany", outbound.getLogisticsCompany());
                map.put("trackingNumber", outbound.getTrackingNumber());
                map.put("waybillNo", outbound.getWaybillNo());
                map.put("deliveryMethod", outbound.getDeliveryMethod());
                map.put("freightPayer", outbound.getFreightPayer());
                map.put("freight", outbound.getFreight());
                // 收款账户
                map.put("paymentAccount1", outbound.getPaymentAccount1());
                map.put("paymentAccount2", outbound.getPaymentAccount2());
                map.put("paymentAccount3", outbound.getPaymentAccount3());
                map.put("paymentAccount4", outbound.getPaymentAccount4());
                // 流程
                map.put("bookkeepingTime", outbound.getBookkeepingTime());
                map.put("printTime", outbound.getPrintTime());
                map.put("createTime", outbound.getCreateTime());
                // 附件：本系统销售出库单暂未提供单据附件存储，列对标保留、无附件时为空
                map.put("attachment", null);
            }
            // 明细字段（71列完整覆盖）
            map.put("productName", item.getProductName());
            map.put("productCode", item.getProductCode());
            map.put("specification", item.getProductSpec() != null ? item.getProductSpec() : item.getSpecification());
            map.put("model", item.getModel());
            map.put("origin", item.getOrigin());
            map.put("brand", item.getBrand());
            map.put("barcode", item.getBarcode());
            map.put("smallUnitBarcode", item.getSmallUnitBarcode());
            map.put("productUnit", item.getProductUnit());
            map.put("productAttribute", item.getProductAttribute());
            map.put("imageUrl", item.getImageUrl());
            map.put("quantity", item.getOutboundQuantity());
            map.put("unitPrice", item.getUnitPrice());
            map.put("lineAmount", item.getLineAmount());
            map.put("smallUnit", item.getSmallUnit());
            map.put("smallUnitPrice", item.getSmallUnitPrice());
            map.put("smallUnitQuantity", item.getSmallUnitQuantity());
            map.put("pieceQuantity", item.getPieceQuantity());
            map.put("bigPack", item.getBigPack());
            map.put("midPack", item.getMidPack());
            map.put("smallPack", item.getSmallPack());
            map.put("conversionRelation", item.getConversionRelation());
            map.put("conversionResult", item.getConversionResult());
            map.put("discountRate", item.getDiscountRate());
            map.put("discountedAmount", item.getDiscountedAmount());
            map.put("discountedPrice", item.getDiscountedPrice());
            map.put("favorableDiscountRate", item.getFavorableDiscountRate());
            map.put("favorableUnitPrice", item.getFavorableUnitPrice());
            map.put("favorableAmount", item.getFavorableAmount());
            map.put("costPrice", item.getCostPrice());
            map.put("costAmount", item.getCostAmount());
            map.put("grossProfit", item.getGrossProfit());
            map.put("retailPrice", item.getRetailPrice());
            map.put("wholesalePrice", item.getWholesalePrice());
            map.put("minSalePrice", item.getMinSalePrice());
            map.put("lastSalePrice", item.getLastSalePrice());
            map.put("lastSaleDate", item.getLastSaleDate());
            map.put("priceLevel1", item.getPriceLevel1());
            map.put("priceLevel2", item.getPriceLevel2());
            map.put("priceLevel3", item.getPriceLevel3());
            map.put("priceLevel4", item.getPriceLevel4());
            map.put("priceLevel5", item.getPriceLevel5());
            map.put("priceLevel6", item.getPriceLevel6());
            map.put("priceLevel7", item.getPriceLevel7());
            map.put("priceLevel8", item.getPriceLevel8());
            map.put("availableStock", item.getAvailableStock());
            map.put("bookStock", item.getBookStock());
            map.put("batchNo", item.getBatchNo());
            map.put("productionDate", item.getProductionDate());
            map.put("expiryDate", item.getExpiryDate());
            map.put("shelfLife", item.getShelfLife());
            map.put("volume", item.getVolume());
            map.put("weight", item.getWeight());
            map.put("gift", item.getGift() != null ? item.getGift() : false);
            map.put("giftItem", item.getGiftItem());
            map.put("exchangePoints", item.getExchangePoints());
            map.put("usedPoints", item.getUsedPoints());
            map.put("generatedPoints", item.getGeneratedPoints());
            map.put("boxNo", item.getBoxNo());
            map.put("customerTicket", item.getCustomerTicket());
            map.put("remark", item.getRemark());
            // 表体自定义字段（10个）
            map.put("extNum1", item.getExtNum1());
            map.put("extNum2", item.getExtNum2());
            map.put("extNum3", item.getExtNum3());
            map.put("extNum4", item.getExtNum4());
            map.put("extNum5", item.getExtNum5());
            map.put("extNum6", item.getExtNum6());
            map.put("extNum7", item.getExtNum7());
            map.put("extText1", item.getExtText1());
            map.put("extText2", item.getExtText2());
            map.put("extPartner", item.getExtPartner());
            map.put("extStaff", item.getExtStaff());
            map.put("extDept", item.getExtDept());
            return map;
        }).toList();
        result.setRecords(records);
        return result;
    }

    @Override
    public List<SaleOutbound> exportList(SaleOutboundQueryDTO query) {
        LambdaQueryWrapper<SaleOutbound> wrapper = buildDocWrapper(query);
        wrapper.orderByDesc(SaleOutbound::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateLogisticsRemark(List<Long> ids, String logisticsRemark) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        LambdaUpdateWrapper<SaleOutbound> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(SaleOutbound::getId, ids)
                .eq(SaleOutbound::getDeleted, 0)
                .set(SaleOutbound::getLogisticsRemark, logisticsRemark)
                .set(SaleOutbound::getUpdateTime, LocalDateTime.now());
        return baseMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound copyOutbound(Long sourceId) {
        SaleOutbound source = getById(sourceId);
        if (source == null) {
            throw new RuntimeException("出库单不存在: " + sourceId);
        }
        // 复制表头（清空ID和状态）
        SaleOutbound copy = new SaleOutbound();
        org.springframework.beans.BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setOutboundNo(generateOutboundNo());
        copy.setStatus(OutboundStatus.DRAFT.getCode());
        copy.setCreateTime(null);
        copy.setUpdateTime(null);
        copy.setCreateBy(StpUtil.getLoginIdAsLong());
        copy.setUpdateBy(null);
        copy.setVersionNo(0);
        copy.setPrintCount(0);
        copy.setPrintTime(null);
        copy.setApprovedBy(null);
        copy.setApprovedTime(null);
        copy.setApprovedNote(null);
        copy.setCompletedBy(null);
        copy.setCompletedTime(null);
        copy.setBookkeepingTime(null);
        save(copy);

        // 复制明细
        List<SaleOutboundItem> sourceItems = getItems(sourceId);
        if (sourceItems != null && !sourceItems.isEmpty()) {
            for (int i = 0; i < sourceItems.size(); i++) {
                SaleOutboundItem srcItem = sourceItems.get(i);
                SaleOutboundItem copyItem = new SaleOutboundItem();
                org.springframework.beans.BeanUtils.copyProperties(srcItem, copyItem);
                copyItem.setId(null);
                copyItem.setOutboundId(copy.getId());
                copyItem.setLineNo(i + 1);
                copyItem.setCreateTime(null);
                copyItem.setUpdateTime(null);
                copyItem.setPickingStatus(0);
                copyItem.setPackingStatus(0);
                copyItem.setPickingBy(null);
                copyItem.setPickingTime(null);
                copyItem.setPackingBy(null);
                copyItem.setPackingTime(null);
                outboundItemMapper.insert(copyItem);
            }
        }
        calculateTotals(copy.getId());
        return getById(copy.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importOutbound(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("导入文件不能为空");
        }
        String filename = file.getOriginalFilename();
        log.info("批量导入出库单，文件名={}, 大小={}", filename, file.getSize());

        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new RuntimeException("仅支持 .xlsx / .xls 格式的Excel文件");
        }

        int importedCount = 0;
        try (InputStream is = file.getInputStream();
             Workbook workbook = filename.endsWith(".xlsx")
                     ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new RuntimeException("Excel文件中没有找到工作表");
            }

            int rowCount = sheet.getPhysicalNumberOfRows();
            log.info("待解析行数={}", rowCount);

            // 从第2行开始读取（第1行为标题行）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    SaleOutbound outbound = new SaleOutbound();

                    // A列: 客户名称（必填）
                    String customerName = getCellStringValue(row.getCell(0));
                    if (StringUtils.isBlank(customerName)) continue;
                    outbound.setCustomerName(customerName);

                    // B列: 仓库名称
                    String warehouseName = getCellStringValue(row.getCell(1));
                    outbound.setWarehouseName(warehouseName);

                    // C列: 经手人
                    String salesPerson = getCellStringValue(row.getCell(2));
                    outbound.setSalesPersonName(salesPerson);

                    // D列: 单据日期
                    String dateStr = getCellStringValue(row.getCell(3));
                    if (StringUtils.isNotBlank(dateStr)) {
                        try {
                            outbound.setOutboundDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        } catch (Exception e) {
                            outbound.setOutboundDate(LocalDate.now());
                        }
                    } else {
                        outbound.setOutboundDate(LocalDate.now());
                    }

                    // E列: 商品数量合计
                    String qtyStr = getCellStringValue(row.getCell(4));
                    if (StringUtils.isNotBlank(qtyStr)) {
                        outbound.setTotalQuantity(new BigDecimal(qtyStr));
                    }

                    // F列: 商品金额合计
                    String amtStr = getCellStringValue(row.getCell(5));
                    if (StringUtils.isNotBlank(amtStr)) {
                        outbound.setTotalAmount(new BigDecimal(amtStr));
                    }

                    // G列: 收货人
                    outbound.setReceiverName(getCellStringValue(row.getCell(6)));

                    // H列: 联系电话
                    outbound.setReceiverPhone(getCellStringValue(row.getCell(7)));

                    // I列: 收货地址
                    outbound.setShippingAddress(getCellStringValue(row.getCell(8)));

                    // J列: 单据备注
                    outbound.setRemark(getCellStringValue(row.getCell(9)));

                    // 设置系统默认值
                    outbound.setOutboundNo(generateOutboundNo());
                    outbound.setStatus(OutboundStatus.DRAFT.getCode());
                    outbound.setCreateBy(StpUtil.getLoginIdAsLong());
                    outbound.setCreateTime(LocalDateTime.now());
                    outbound.setDeleted(0);
                    outbound.setVersionNo(0);

                    save(outbound);
                    importedCount++;
                } catch (Exception e) {
                    log.error("第{}行导入失败", i + 1, e);
                    // 继续执行后续行
                }
            }
        } catch (Exception e) {
            log.error("Excel解析失败", e);
            throw new RuntimeException("导入文件解析失败: " + e.getMessage());
        }

        log.info("批量导入完成，共导入{}条", importedCount);
        return importedCount;
    }

    /**
     * 读取单元格字符串值（支持文本、数字、日期等多种类型）
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf((long) cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }

    @Override
    public List<SaleOutbound> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<SaleOutbound> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateOutboundNo() {
        String prefix = "XSCK";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<SaleOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SaleOutbound::getOutboundNo, prefix + dateStr)
                .eq(SaleOutbound::getDeleted, 0)
                .orderByDesc(SaleOutbound::getOutboundNo)
                .last("LIMIT 1");
        SaleOutbound lastOutbound = getOne(wrapper);
        int seq = 1;
        if (lastOutbound != null) {
            String lastNo = lastOutbound.getOutboundNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound createOutbound(SaleOutbound outbound, List<SaleOutboundItem> items) {
        // 单号权威在后端号段：沿用前端从 /next-no 取到的号码；同号已存在或为空则重新分配
        if (StringUtils.isBlank(outbound.getOutboundNo())
                || lambdaQuery().eq(SaleOutbound::getOutboundNo, outbound.getOutboundNo()).count() > 0) {
            outbound.setOutboundNo(generateOutboundNo());
        }
        if (outbound.getStatus() == null) {
            outbound.setStatus(OutboundStatus.DRAFT.getCode());
        }
        if (outbound.getOutboundDate() == null) {
            outbound.setOutboundDate(LocalDate.now());
        }
        if (outbound.getOutboundType() == null) {
            outbound.setOutboundType(0);
        }
        // 来源：调用方未指定时视为电脑端录入（PC）
        if (StringUtils.isBlank(outbound.getSource())) {
            outbound.setSource("PC");
        }
        if (outbound.getTotalQuantity() == null) {
            outbound.setTotalQuantity(BigDecimal.ZERO);
        }
        if (outbound.getTotalAmount() == null) {
            outbound.setTotalAmount(BigDecimal.ZERO);
        }
        save(outbound);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                SaleOutboundItem item = items.get(i);
                item.setOutboundId(outbound.getId());
                item.setLineNo(i + 1);
                item.setTenantId(outbound.getTenantId());
                if (item.getOutboundQuantity() == null) {
                    item.setOutboundQuantity(item.getOrderQuantity() != null ? item.getOrderQuantity() : BigDecimal.ZERO);
                }
                item.setPendingQuantity(item.getOutboundQuantity());
                item.setPickingStatus(0);
                item.setPackingStatus(0);
                calculateItemAmounts(item);
                outboundItemMapper.insert(item);
            }
        }
        calculateTotals(outbound.getId());
        return getById(outbound.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound createFromOrder(Long orderId) {
        // 1. 查询销售订单
        SaleOrder order = saleOrderService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("销售订单不存在: " + orderId);
        }

        // 2. 查询订单明细
        List<SaleOrderItem> orderItems = saleOrderItemMapper.selectByOrderId(orderId);

        // 3. 复制表头数据
        SaleOutbound outbound = new SaleOutbound();
        outbound.setOrderId(orderId);
        outbound.setOrderNo(order.getOrderNo());
        outbound.setOutboundDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now());
        outbound.setOutboundType(1); // 1=正常销售
        outbound.setGenerationMethod("订单生成");
        outbound.setSource("PC");    // 订单生成出库由电脑端触发
        outbound.setSummary(order.getSummary());

        // 客户快照
        outbound.setCustomerId(order.getCustomerId());
        outbound.setCustomerName(order.getCustomerName());
        outbound.setCustomerCode(order.getCustomerCode());
        outbound.setCustomerLevel(order.getCustomerLevel());
        outbound.setCustomerRemark(order.getCustomerRemark());

        // 银行/税务
        outbound.setBankName(order.getBankName());
        outbound.setBankAccount(order.getBankAccount());
        outbound.setTaxNo(order.getTaxNo());

        // 仓库/经手人
        outbound.setWarehouseId(order.getWarehouseId());
        outbound.setWarehouseName(order.getWarehouseName());
        outbound.setSalesPersonId(order.getSalesmanId());
        outbound.setSalesPersonName(order.getSalesmanName());
        outbound.setDepartmentId(order.getDeptId());
        outbound.setDepartmentName(order.getDeptName());
        outbound.setRegion(order.getRegion());

        // 收货信息
        outbound.setReceiverName(order.getReceiverName());
        outbound.setReceiverPhone(order.getReceiverPhone());
        outbound.setShippingAddress(order.getShippingAddress());

        // 金额/费用
        outbound.setPromoDiscount(order.getPromoDiscount());
        outbound.setCouponAmount(order.getCouponAmount());
        outbound.setDirectDiscount(order.getDirectDiscount());
        outbound.setOtherFee(order.getOtherFee());
        outbound.setTotalWeight(order.getTotalWeight());
        outbound.setTotalVolume(order.getTotalVolume());

        // 结算/收款
        outbound.setSettlementMethod(order.getSettlementMethod());
        outbound.setDeliveryMethod(order.getDeliveryMethod());

        // 物流
        outbound.setLogisticsCompany(order.getLogisticsCompany());
        outbound.setFreightPayer(order.getFreightPayer());
        outbound.setFreight(order.getShippingFee());
        outbound.setWaybillNo(order.getWaybillNo());
        outbound.setCodAmount(order.getCodAmount());

        // 会员
        outbound.setMemberCardNo(order.getMemberCardNo());
        outbound.setPrevPoints(order.getPrevPoints());
        outbound.setPaymentDate(order.getPaymentDate());
        outbound.setReconciliationDate(order.getReconciliationDate());

        // 备注
        outbound.setRemark(order.getRemark());
        outbound.setBuyerRemark(order.getBuyerRemark());

        // 自定义字段
        outbound.setExtNum1(order.getExtNum1());
        outbound.setExtNum2(order.getExtNum2());
        outbound.setExtText1(order.getExtText1());
        outbound.setExtText2(order.getExtText2());
        outbound.setExtText3(order.getExtText3());
        outbound.setFooterExtText1(order.getFooterExtText1());
        outbound.setFooterExtText2(order.getFooterExtText2());

        // 4. 保存表头
        outbound.setOutboundNo(generateOutboundNo());
        outbound.setStatus(OutboundStatus.DRAFT.getCode());
        if (outbound.getTotalQuantity() == null) outbound.setTotalQuantity(BigDecimal.ZERO);
        if (outbound.getTotalAmount() == null) outbound.setTotalAmount(BigDecimal.ZERO);
        save(outbound);

        // 5. 复制明细
        List<SaleOutboundItem> items = new ArrayList<>();
        if (orderItems != null && !orderItems.isEmpty()) {
            for (int i = 0; i < orderItems.size(); i++) {
                SaleOrderItem oi = orderItems.get(i);
                SaleOutboundItem item = new SaleOutboundItem();
                item.setOutboundId(outbound.getId());
                item.setLineNo(i + 1);
                item.setTenantId(outbound.getTenantId());

                // 产品快照
                item.setOrderItemId(oi.getId());
                item.setProductId(oi.getProductId());
                item.setProductCode(oi.getProductCode());
                item.setProductName(oi.getProductName());
                item.setBarcode(oi.getBarcode());
                item.setSmallUnitBarcode(oi.getSmallUnitBarcode());
                item.setSpecification(oi.getSpecification());
                item.setProductSpec(oi.getSpecification());
                item.setModel(oi.getModel());
                item.setOrigin(oi.getOrigin());
                item.setBrand(oi.getBrand());
                item.setProductUnit(oi.getUnit());
                item.setProductAttribute(oi.getLineAttribute());

                // 数量
                item.setOrderQuantity(oi.getQuantity());
                item.setQuantity(oi.getQuantity());
                item.setOutboundQuantity(oi.getQuantity());
                item.setPendingQuantity(oi.getQuantity());
                item.setPieceQuantity(oi.getQuantity() != null ? oi.getQuantity() : BigDecimal.ZERO);
                item.setBigPack(oi.getBigPack());
                item.setMidPack(oi.getMidPack());
                item.setSmallPack(oi.getSmallPack());

                // 单位换算
                item.setConversionRelation(oi.getConversionRelation());
                item.setSmallUnit(oi.getSmallUnit());
                item.setSmallUnitQuantity(oi.getSmallUnitQuantity());

                // 价格
                item.setUnitPrice(oi.getUnitPrice());
                item.setSmallUnitPrice(oi.getSmallUnitPrice());
                item.setTaxRate(oi.getTaxRate());

                // 折扣
                item.setDiscountRate(oi.getDiscountRate());
                item.setDiscountedPrice(oi.getDiscountedUnitPrice());
                item.setFavorableUnitPrice(oi.getFavorableUnitPrice());

                // 成本/参考
                item.setCostPrice(oi.getCostPrice());
                item.setRetailPrice(oi.getRetailPrice());
                item.setWholesalePrice(oi.getWholesalePrice());
                item.setMinSalePrice(oi.getLowestPrice());
                item.setLastSalePrice(oi.getLatestSalePrice());
                item.setLastSaleDate(oi.getLatestSaleDate());

                // 库存快照
                item.setAvailableStock(oi.getAvailableStock());
                item.setBookStock(oi.getBookStock());

                // 批次/有效期
                item.setBatchNo(oi.getBatchCode());
                item.setProductionDate(oi.getProductionDate() != null ? oi.getProductionDate().atStartOfDay() : null);
                item.setExpiryDate(oi.getExpiryDate());
                item.setShelfLife(oi.getShelfLife());

                // 体积/重量
                item.setVolume(oi.getVolume());
                item.setWeight(oi.getWeight());

                // 赠品
                item.setGift(oi.getGift());

                // 调用完整金额计算链（含折扣、成本、毛利）
                calculateItemAmounts(item);

                // 拣货/打包初始状态
                item.setPickingStatus(0);
                item.setPackingStatus(0);

                outboundItemMapper.insert(item);
                items.add(item);
            }
        }

        // 6. 更新合计
        calculateTotals(outbound.getId());
        return getById(outbound.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound updateOutbound(Long outboundId, SaleOutbound outbound, List<SaleOutboundItem> items) {
        SaleOutbound existing = getById(outboundId);
        if (existing == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (existing.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以修改");
        }
        // 乐观锁并发控制（对标SAP/金蝶版本号机制）
        if (outbound.getVersionNo() != null && existing.getVersionNo() != null
                && !outbound.getVersionNo().equals(existing.getVersionNo())) {
            throw new RuntimeException("数据已被其他操作员修改，请刷新后重试");
        }
        outbound.setId(outboundId);
        outbound.setVersionNo(existing.getVersionNo() != null ? existing.getVersionNo() + 1 : 1);
        updateById(outbound);
        if (items != null) {
            List<SaleOutboundItem> existingItems = getItems(outboundId);
            for (SaleOutboundItem oldItem : existingItems) {
                outboundItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                SaleOutboundItem item = items.get(i);
                item.setOutboundId(outboundId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setOutboundQuantity(BigDecimal.ZERO);
                item.setPickingStatus(0);
                item.setPackingStatus(0);
                calculateItemAmounts(item);
                outboundItemMapper.insert(item);
            }
        }
        calculateTotals(outboundId);
        return getById(outboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound submitForApproval(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以提交审批");
        }

        // 信用额度检查（对标Odoo credit control / SAP credit management）
        if (outbound.getCustomerId() != null) {
            try {
                BigDecimal totalAmount = outbound.getTotalAmount() != null ? outbound.getTotalAmount() : BigDecimal.ZERO;
                boolean creditOk = customerCreditService.checkCreditAvailable(outbound.getCustomerId(), totalAmount);
                if (!creditOk) {
                    // 获取信用状态详情用于日志
                    Map<String, Object> creditStatus = customerCreditService.getCreditStatus(outbound.getCustomerId());
                    log.warn("客户信用额度不足，客户ID={}, 本单金额={}, 信用状态={}", outbound.getCustomerId(), totalAmount, creditStatus);
                    // 更新表头信用字段
                    BigDecimal creditLimit = customerCreditService.getCreditLimit(outbound.getCustomerId());
                    BigDecimal availableCredit = customerCreditService.getAvailableCredit(outbound.getCustomerId());
                    BigDecimal currentDebt = customerCreditService.getCurrentDebt(outbound.getCustomerId());
                    outbound.setCreditLimit(creditLimit);
                    outbound.setAvailableCredit(availableCredit);
                    outbound.setPrevArrears(currentDebt);
                    outbound.setCurrentArrears(currentDebt.add(totalAmount));
                    outbound.setArrearsBalance(creditLimit.subtract(currentDebt).subtract(totalAmount));
                    // 超信用时设置警告但不阻断（生产级系统通常允许配置是否阻断）
                    outbound.setRemark((outbound.getRemark() != null ? outbound.getRemark() + "；" : "") + "【信用警告】超出信用额度");
                    updateById(outbound);
                } else {
                    // 信用充足，更新信用字段
                    BigDecimal creditLimit = customerCreditService.getCreditLimit(outbound.getCustomerId());
                    BigDecimal availableCredit = customerCreditService.getAvailableCredit(outbound.getCustomerId());
                    BigDecimal currentDebt = customerCreditService.getCurrentDebt(outbound.getCustomerId());
                    outbound.setCreditLimit(creditLimit);
                    outbound.setAvailableCredit(availableCredit);
                    outbound.setPrevArrears(currentDebt);
                    outbound.setCurrentArrears(currentDebt);
                    outbound.setArrearsBalance(creditLimit.subtract(currentDebt));
                    updateById(outbound);
                }
            } catch (Exception e) {
                log.warn("信用检查异常，客户ID={}，继续提交: {}", outbound.getCustomerId(), e.getMessage());
            }
        }

        // 预收款扣减计算
        if (outbound.getAvailableAdvancePayment() != null && outbound.getAvailableAdvancePayment().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalAmount = outbound.getTotalAmount() != null ? outbound.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal usedAdvance = totalAmount.min(outbound.getAvailableAdvancePayment());
            outbound.setUsedAdvancePayment(usedAdvance);
            outbound.setAdvancePaymentBalance(outbound.getAvailableAdvancePayment().subtract(usedAdvance));
            updateById(outbound);
        }

        outbound.setStatus(OutboundStatus.PENDING_APPROVAL.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound approve(Long outboundId, Long approverId, String note) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的出库单可以审批");
        }
        outbound.setStatus(OutboundStatus.APPROVED.getCode());
        outbound.setApprovedBy(approverId);
        outbound.setApprovedTime(LocalDateTime.now());
        outbound.setApprovedNote(note);
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound reject(Long outboundId, String reason) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的出库单可以拒绝");
        }
        outbound.setStatus(OutboundStatus.DRAFT.getCode());
        outbound.setRemark(reason);
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound startPicking(Long outboundId, Long pickerId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的出库单可以开始拣货");
        }
        outbound.setStatus(OutboundStatus.PICKING.getCode());
        outbound.setPickingBy(pickerId);
        outbound.setPickingTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem pickItem(Long itemId, BigDecimal outboundQuantity, String batchNo) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.PICKING.getCode()) {
            throw new RuntimeException("只有拣货中状态的出库单可以处理明细");
        }
        item.setOutboundQuantity(outboundQuantity);
        item.setPendingQuantity(item.getOrderQuantity().subtract(outboundQuantity));
        item.setBatchNo(batchNo);
        item.setPickingStatus(1);
        item.setPickingBy(outbound.getPickingBy());
        item.setPickingTime(LocalDateTime.now());
        calculateItemAmounts(item);
        outboundItemMapper.updateById(item);
        calculateTotals(item.getOutboundId());
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound completePicking(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        outbound.setStatus(OutboundStatus.PICKED.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound startPacking(Long outboundId, Long packerId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PICKED.getCode()) {
            throw new RuntimeException("只有已拣货状态的出库单可以开始打包");
        }
        outbound.setStatus(OutboundStatus.PACKING.getCode());
        outbound.setPackingBy(packerId);
        outbound.setPackingTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem packItem(Long itemId) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.PACKING.getCode()) {
            throw new RuntimeException("只有打包中状态的出库单可以处理明细");
        }
        item.setPackingStatus(1);
        item.setPackingBy(outbound.getPackingBy());
        item.setPackingTime(LocalDateTime.now());
        outboundItemMapper.updateById(item);
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound completePacking(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        outbound.setStatus(OutboundStatus.PACKED.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound confirmFromWms(Long saleOrderId) {
        SaleOutbound outbound = createFromOrder(saleOrderId);
        if (outbound == null) {
            throw new RuntimeException("由销售订单[" + saleOrderId + "]创建出库单失败");
        }
        // WMS 已用 InventoryService.decrease 完成库存扣减（唯一扣减点），此处仅推进为已发货，不再扣减（避免双扣链路）
        outbound.setStatus(OutboundStatus.SHIPPED.getCode());
        outbound.setShippedTime(LocalDateTime.now());
        outbound.setActualShipTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound ship(Long outboundId, Long shipperId, String trackingNumber, String logisticsCompany) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PACKED.getCode()) {
            throw new RuntimeException("只有已打包状态的出库单可以发货");
        }
        outbound.setStatus(OutboundStatus.SHIPPED.getCode());
        outbound.setShippedBy(shipperId);
        outbound.setShippedTime(LocalDateTime.now());
        outbound.setActualShipTime(LocalDateTime.now());
        outbound.setTrackingNumber(trackingNumber);
        outbound.setLogisticsCompany(logisticsCompany);
        updateById(outbound);
        updateStock(outboundId);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound complete(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.SHIPPED.getCode()) {
            throw new RuntimeException("只有已发货状态的出库单可以完成");
        }
        outbound.setStatus(OutboundStatus.COMPLETED.getCode());
        outbound.setCompletedBy(outbound.getCreateBy());
        outbound.setCompletedTime(LocalDateTime.now());

        // 结算状态自动更新（对标Odoo/SAP：出库完成 → 默认"未结清"，全额收款则"已结清"）
        BigDecimal totalAmount = outbound.getTotalAmount() != null ? outbound.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal settledAmount = outbound.getSettledAmount() != null ? outbound.getSettledAmount() : BigDecimal.ZERO;
        if (settledAmount.compareTo(totalAmount) >= 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            outbound.setSettlementStatus("settled");
        } else if (settledAmount.compareTo(BigDecimal.ZERO) > 0) {
            outbound.setSettlementStatus("partial");
        } else {
            outbound.setSettlementStatus("unsettled");
        }

        updateById(outbound);

        // 源单履约跟踪：更新销售订单已出库数量（对标Odoo stock.picking → sale.order 回写）
        if (outbound.getOrderId() != null) {
            updateSourceOrderFulfillment(outbound.getOrderId(), outboundId);
        }

        // 自动生成会计凭证（对标SAP/金蝶的凭证自动生成）
        // 记账人/记账时间仅在凭证生成成功时写入；失败则留空，列表页「仅显示异常记账单据」据此可查
        boolean voucherPosted = false;
        try {
            generateAccountingVoucher(outbound);
            voucherPosted = true;
        } catch (Exception e) {
            log.error("自动生成会计凭证失败，出库单ID={}, 原因={}", outboundId, e.getMessage(), e);
            // 凭证生成失败不影响出库单完成状态
        }
        if (voucherPosted) {
            outbound.setBookkeeperName(outbound.getCreatorName());
            outbound.setBookkeepingTime(LocalDateTime.now());
            updateById(outbound);
        } else {
            log.warn("出库单 {} 记账异常（凭证未生成），可通过「仅显示异常记账单据」筛选", outbound.getOutboundNo());
        }

        // 业财直调：发货完成产生应收及收入凭证（对标Odoo invoice on delivery）
        if (outbound.getCustomerId() != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                salesAccountingService.createReceivableOnShipment(
                        outbound.getId(), outbound.getOutboundNo(),
                        String.valueOf(outbound.getCustomerId()), outbound.getCustomerName(),
                        totalAmount);
            } catch (Exception e) {
                log.error("销售发货自动记账失败，出库单ID={}, 原因={}", outboundId, e.getMessage(), e);
                // 记账失败不影响出库单完成状态
            }
        }

        // 更新客户信用欠款（对标Odoo/SAP信用管理）
        if (outbound.getCustomerId() != null) {
            try {
                customerCreditService.updateCustomerDebt(outbound.getCustomerId());
            } catch (Exception e) {
                log.warn("更新客户信用欠款失败，客户ID={}: {}", outbound.getCustomerId(), e.getMessage());
            }
        }

        return outbound;
    }

    /**
     * 更新源单（销售订单）履约数量（对标Odoo sale.order.line.qty_delivered回写）
     */
    private void updateSourceOrderFulfillment(Long orderId, Long outboundId) {
        try {
            List<SaleOutboundItem> outboundItems = getItems(outboundId);
            for (SaleOutboundItem outboundItem : outboundItems) {
                if (outboundItem.getOrderItemId() != null) {
                    // 查找该订单明细对应的所有出库明细合计已出库数量
                    BigDecimal totalDelivered = outboundItemMapper.sumOutboundQuantityByOutboundId(outboundId);
                    // 更新销售订单明细的已出库数量（对标Odoo sale.order.line.qty_delivered回写）
                    SaleOrderItem orderItem = saleOrderItemMapper.selectById(outboundItem.getOrderItemId());
                    if (orderItem != null) {
                        BigDecimal currentShipped = orderItem.getShippedQuantity() != null ? orderItem.getShippedQuantity() : BigDecimal.ZERO;
                        orderItem.setShippedQuantity(currentShipped.add(outboundItem.getOutboundQuantity()));
                        saleOrderItemMapper.updateById(orderItem);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("更新源单履约状态失败，订单ID={}: {}", orderId, e.getMessage());
        }
    }

    /**
     * 自动生成会计凭证（对标SAP/金蝶/用友的销售出库凭证模板）
     * 标准分录：
     *   借：应收账款（客户）  →  本单金额
     *   贷：主营业务收入       →  本单金额（不含税）
     * 如果有成本信息则同时结转成本：
     *   借：主营业务成本       →  成本金额
     *   贷：库存商品           →  成本金额
     */
    private void generateAccountingVoucher(SaleOutbound outbound) {
        BigDecimal totalAmount = outbound.getTotalAmount() != null ? outbound.getTotalAmount() : BigDecimal.ZERO;
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int fiscalYear = now.getYear();
        int fiscalPeriod = now.getMonthValue();

        // 构建凭证明细
        List<VoucherItemDTO> voucherItems = new ArrayList<>();

        // 1. 借：应收账款（科目代码待配置，这里用标准代码 1122）
        VoucherItemDTO debitReceivable = new VoucherItemDTO();
        debitReceivable.setSummary("销售出库 - " + outbound.getOutboundNo() + " " + outbound.getCustomerName());
        debitReceivable.setSubjectCode("1122");
        debitReceivable.setSubjectName("应收账款");
        debitReceivable.setDebitAmount(totalAmount);
        debitReceivable.setCreditAmount(BigDecimal.ZERO);
        debitReceivable.setSourceType("sale_outbound");
        debitReceivable.setSourceId(outbound.getId());
        debitReceivable.setSourceNo(outbound.getOutboundNo());
        voucherItems.add(debitReceivable);

        // 2. 贷：主营业务收入（科目代码 6001）
        VoucherItemDTO creditRevenue = new VoucherItemDTO();
        creditRevenue.setSummary("销售出库 - " + outbound.getOutboundNo());
        creditRevenue.setSubjectCode("6001");
        creditRevenue.setSubjectName("主营业务收入");
        creditRevenue.setDebitAmount(BigDecimal.ZERO);
        creditRevenue.setCreditAmount(totalAmount);
        creditRevenue.setSourceType("sale_outbound");
        creditRevenue.setSourceId(outbound.getId());
        creditRevenue.setSourceNo(outbound.getOutboundNo());
        voucherItems.add(creditRevenue);

        // 3. 结转成本（如果有成本数据）
        List<SaleOutboundItem> items = getItems(outbound.getId());
        BigDecimal totalCost = items.stream()
                .map(i -> i.getCostAmount() != null ? i.getCostAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            // 借：主营业务成本（科目代码 6401）
            VoucherItemDTO debitCost = new VoucherItemDTO();
            debitCost.setSummary("结转成本 - " + outbound.getOutboundNo());
            debitCost.setSubjectCode("6401");
            debitCost.setSubjectName("主营业务成本");
            debitCost.setDebitAmount(totalCost);
            debitCost.setCreditAmount(BigDecimal.ZERO);
            debitCost.setSourceType("sale_outbound");
            debitCost.setSourceId(outbound.getId());
            debitCost.setSourceNo(outbound.getOutboundNo());
            voucherItems.add(debitCost);

            // 贷：库存商品（科目代码 1405）
            VoucherItemDTO creditStock = new VoucherItemDTO();
            creditStock.setSummary("结转成本 - " + outbound.getOutboundNo());
            creditStock.setSubjectCode("1405");
            creditStock.setSubjectName("库存商品");
            creditStock.setDebitAmount(BigDecimal.ZERO);
            creditStock.setCreditAmount(totalCost);
            creditStock.setSourceType("sale_outbound");
            creditStock.setSourceId(outbound.getId());
            creditStock.setSourceNo(outbound.getOutboundNo());
            voucherItems.add(creditStock);
        }

        // 创建凭证
        VoucherDTO voucher = new VoucherDTO();
        voucher.setVoucherDate(LocalDate.now());
        voucher.setFiscalYear(fiscalYear);
        voucher.setFiscalPeriod(fiscalPeriod);
        voucher.setAttachments(0);
        voucher.setPrepBy(outbound.getCreatorName());
        voucher.setPrepAt(now);
        voucher.setStatus("draft");
        voucher.setTotalDebit(totalAmount.add(totalCost));
        voucher.setTotalCredit(totalAmount.add(totalCost));
        voucher.setRemark("销售出库自动生成 - " + outbound.getOutboundNo());
        voucher.setItems(voucherItems);

        VoucherDTO created = voucherService.create(voucher);
        log.info("销售出库凭证自动生成成功，凭证ID={}, 凭证号={}, 出库单={}",
                created.getId(), created.getVoucherNo(), outbound.getOutboundNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound cancel(Long outboundId, String reason) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }

        // 已完成的出库单：需要红冲处理（反向库存 + 红字凭证）
        if (outbound.getStatus() == OutboundStatus.COMPLETED.getCode()) {
            // 反向库存操作（对标Odoo: 退货入库增加库存）
            try {
                reverseStock(outboundId);
            } catch (Exception e) {
                log.error("取消完成状态出库单时回滚库存失败，出库单ID={}: {}", outboundId, e.getMessage());
                throw new RuntimeException("库存回滚异常，取消操作无法完成", e);
            }
            // 生成红字冲销凭证（对标SAP/金蝶：取消完成出库单需红冲凭证）
            try {
                VoucherDTO createdVoucher = generateReverseVoucher(outbound);
                log.info("红字冲销凭证生成成功，凭证ID={}", createdVoucher.getId());
            } catch (Exception e) {
                log.warn("取消完成状态出库单时生成红冲凭证失败，出库单ID={}: {}", outboundId, e.getMessage());
                // 凭证生成失败不影响取消操作
            }
            // 更新客户信用欠款
            if (outbound.getCustomerId() != null) {
                try {
                    customerCreditService.updateCustomerDebt(outbound.getCustomerId());
                } catch (Exception e) {
                    log.warn("更新客户信用欠款失败，客户ID={}: {}", outbound.getCustomerId(), e.getMessage());
                }
            }
        } else if (outbound.getStatus() == OutboundStatus.SHIPPED.getCode()) {
            // 已发货但未完成：回滚库存
            try {
                reverseStock(outboundId);
            } catch (Exception e) {
                log.error("取消发货状态出库单时回滚库存失败，出库单ID={}: {}", outboundId, e.getMessage());
                throw new RuntimeException("库存回滚异常，取消操作无法完成", e);
            }
        }

        // 如果出库单有对应的销售订单，按原履约数量回退
        if (outbound.getOrderId() != null) {
            try {
                reverseSourceOrderFulfillment(outbound.getOrderId(), outboundId);
            } catch (Exception e) {
                log.warn("回退源单履约数量失败，订单ID={}: {}", outbound.getOrderId(), e.getMessage());
            }
        }

        outbound.setStatus(OutboundStatus.CANCELLED.getCode());
        outbound.setRemark(reason);
        updateById(outbound);
        return outbound;
    }

    /**
     * 反向库存操作（取消出库时回滚，对标Odoo stock return / 金蝶红字出库单）
     */
    private void reverseStock(Long outboundId) {
        List<SaleOutboundItem> items = getItems(outboundId);
        SaleOutbound outbound = getById(outboundId);
        Long warehouseId = outbound.getWarehouseId();
        if (warehouseId == null) {
            log.warn("出库单 {} 未指定仓库，取消未产生库存回滚", outbound.getOutboundNo());
            return;
        }
        for (SaleOutboundItem item : items) {
            BigDecimal qty = item.getOutboundQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            // 与出库扣减对称：同样只发布变动请求，由 WMS InventoryService 统一过账
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.INCREASE, item.getProductId(), warehouseId,
                    item.getWarehouseLocationId() == null ? null : item.getWarehouseLocationId().longValue(),
                    item.getBatchNo(), qty,
                    "SALE_OUTBOUND_CANCEL", outbound.getId(), outbound.getOutboundNo(),
                    outbound.getShippedBy() != null ? outbound.getShippedBy() : outbound.getCreateBy(),
                    outbound.getCreatorName()));
            log.info("取消出库触发库存回冲: outboundNo={}, productId={}, qty={}",
                    outbound.getOutboundNo(), item.getProductId(), qty);
        }
    }

    /**
     * 回退源单履约数量（取消出库时回退已出库数量）
     */
    private void reverseSourceOrderFulfillment(Long orderId, Long outboundId) {
        try {
            List<SaleOutboundItem> outboundItems = getItems(outboundId);
            for (SaleOutboundItem outboundItem : outboundItems) {
                if (outboundItem.getOrderItemId() != null) {
                    SaleOrderItem orderItem = saleOrderItemMapper.selectById(outboundItem.getOrderItemId());
                    if (orderItem != null) {
                        BigDecimal currentShipped = orderItem.getShippedQuantity() != null ? orderItem.getShippedQuantity() : BigDecimal.ZERO;
                        BigDecimal newShipped = currentShipped.subtract(outboundItem.getOutboundQuantity());
                        if (newShipped.compareTo(BigDecimal.ZERO) < 0) {
                            newShipped = BigDecimal.ZERO;
                        }
                        orderItem.setShippedQuantity(newShipped);
                        saleOrderItemMapper.updateById(orderItem);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("回退源单履约数量失败，订单ID={}: {}", orderId, e.getMessage());
        }
    }

    /**
     * 生成红字冲销凭证（对标SAP/金蝶：取消已完成出库单的会计冲销）
     * 标准红字分录（与原凭证相反方向）：
     *   借：主营业务收入（红字）
     *   贷：应收账款（红字）
     */
    private VoucherDTO generateReverseVoucher(SaleOutbound outbound) {
        BigDecimal totalAmount = outbound.getTotalAmount() != null ? outbound.getTotalAmount() : BigDecimal.ZERO;
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        List<VoucherItemDTO> voucherItems = new ArrayList<>();
        // 红字冲销：方向与原凭证相反（收入冲红）
        VoucherItemDTO debitRevenue = new VoucherItemDTO();
        debitRevenue.setSummary("取消出库红冲 - " + outbound.getOutboundNo());
        debitRevenue.setSubjectCode("6001");
        debitRevenue.setSubjectName("主营业务收入");
        debitRevenue.setDebitAmount(totalAmount);
        debitRevenue.setCreditAmount(BigDecimal.ZERO);
        debitRevenue.setSourceType("sale_outbound_cancel");
        debitRevenue.setSourceId(outbound.getId());
        debitRevenue.setSourceNo(outbound.getOutboundNo());
        voucherItems.add(debitRevenue);

        VoucherItemDTO creditReceivable = new VoucherItemDTO();
        creditReceivable.setSummary("取消出库红冲 - " + outbound.getOutboundNo());
        creditReceivable.setSubjectCode("1122");
        creditReceivable.setSubjectName("应收账款");
        creditReceivable.setDebitAmount(BigDecimal.ZERO);
        creditReceivable.setCreditAmount(totalAmount);
        creditReceivable.setSourceType("sale_outbound_cancel");
        creditReceivable.setSourceId(outbound.getId());
        creditReceivable.setSourceNo(outbound.getOutboundNo());
        voucherItems.add(creditReceivable);

        VoucherDTO voucher = new VoucherDTO();
        voucher.setVoucherDate(LocalDate.now());
        voucher.setFiscalYear(now.getYear());
        voucher.setFiscalPeriod(now.getMonthValue());
        voucher.setAttachments(0);
        voucher.setPrepBy(outbound.getCreatorName());
        voucher.setPrepAt(now);
        voucher.setStatus("draft");
        voucher.setTotalDebit(totalAmount);
        voucher.setTotalCredit(totalAmount);
        voucher.setRemark("取消出库自动红冲 - " + outbound.getOutboundNo());
        voucher.setItems(voucherItems);
        return voucherService.create(voucher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long outboundId) {
        BigDecimal totalQuantity = outboundItemMapper.sumOutboundQuantityByOutboundId(outboundId);
        BigDecimal totalAmount = outboundItemMapper.sumLineAmountByOutboundId(outboundId);
        SaleOutbound outbound = getById(outboundId);
        outbound.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        outbound.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        updateById(outbound);
    }

    @Override
    public List<SaleOutboundItem> getItems(Long outboundId) {
        return outboundItemMapper.selectByOutboundId(outboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem addItem(Long outboundId, SaleOutboundItem item) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以添加明细");
        }
        List<SaleOutboundItem> existingItems = getItems(outboundId);
        item.setOutboundId(outboundId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(outbound.getTenantId());
        item.setPendingQuantity(item.getOrderQuantity());
        item.setOutboundQuantity(BigDecimal.ZERO);
        item.setPickingStatus(0);
        item.setPackingStatus(0);
        calculateItemAmounts(item);
        outboundItemMapper.insert(item);
        calculateTotals(outboundId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem updateItem(Long itemId, SaleOutboundItem item) {
        SaleOutboundItem existing = outboundItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(existing.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以修改明细");
        }
        item.setId(itemId);
        item.setPendingQuantity(item.getOrderQuantity());
        calculateItemAmounts(item);
        outboundItemMapper.updateById(item);
        calculateTotals(existing.getOutboundId());
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以删除明细");
        }
        outboundItemMapper.deleteById(itemId);
        calculateTotals(item.getOutboundId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculateItemPrice(Long customerId, Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (customerId == null || productId == null) {
                result.put("success", false);
                result.put("message", "客户和商品不能为空");
                return result;
            }
            // 使用定价引擎计算最优价格（对标Odoo pricelist / SAP条件定价）
            PriceCalculationRequest request = new PriceCalculationRequest();
            request.setProductId(String.valueOf(productId));
            request.setCustomerId(String.valueOf(customerId));
            request.setQuantity(quantity != null ? quantity.intValue() : 1);
            request.setBasePrice(unitPrice);
            PriceCalculationResult priceResult = priceEngineService.getOptimalPrice(request);
            if (priceResult != null && priceResult.getSuccess() != null && priceResult.getSuccess()) {
                result.put("success", true);
                result.put("unitPrice", priceResult.getUnitPrice() != null ? priceResult.getUnitPrice() : unitPrice);
                result.put("finalPrice", priceResult.getFinalPrice());
                result.put("basePrice", priceResult.getBasePrice());
                result.put("discountedPrice", priceResult.getDiscountedPrice());
                result.put("totalDiscountRate", priceResult.getTotalDiscountRate());
                result.put("totalDiscountAmount", priceResult.getTotalDiscountAmount());
                result.put("costPrice", priceResult.getCostPrice());
                result.put("grossProfitMargin", priceResult.getGrossProfitMargin());
                result.put("strategyName", priceResult.getPrimaryPricingStrategyName());
                result.put("message", "价格计算完成");
            } else {
                result.put("success", true);
                result.put("unitPrice", unitPrice);
                result.put("finalPrice", unitPrice);
                result.put("message", "定价引擎暂不可用，使用原价");
            }
        } catch (Exception e) {
            log.warn("价格计算异常: customerId={}, productId={}: {}", customerId, productId, e.getMessage());
            result.put("success", true);
            result.put("unitPrice", unitPrice);
            result.put("finalPrice", unitPrice);
            result.put("message", "价格计算异常，使用原价");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在: " + outboundId);
        }
        Long warehouseId = outbound.getWarehouseId();
        if (warehouseId == null) {
            throw new RuntimeException("出库单未指定发货仓库，无法过账库存");
        }
        List<SaleOutboundItem> items = getItems(outboundId);
        boolean anyPosted = false;
        for (SaleOutboundItem item : items) {
            BigDecimal qty = item.getOutboundQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            // 库存唯一写入口是 WMS：ERP 侧只发布变动请求，由 InventoryChangeEventListener →
            // InventoryService.decrease 统一过账（双写 wms_inventory + 镜像 erp_stock），严禁 ERP 直写 erp_stock。
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.DECREASE, item.getProductId(), warehouseId,
                    item.getWarehouseLocationId() == null ? null : item.getWarehouseLocationId().longValue(),
                    item.getBatchNo(), qty,
                    "SALE_OUTBOUND", outbound.getId(), outbound.getOutboundNo(),
                    outbound.getShippedBy() != null ? outbound.getShippedBy() : outbound.getCreateBy(),
                    outbound.getCreatorName()));
            log.info("销售出库触发库存出账: outboundNo={}, productId={}, qty={}",
                    outbound.getOutboundNo(), item.getProductId(), qty);
            anyPosted = true;
        }
        if (!anyPosted) {
            log.warn("出库单 {} 无有效出库明细，未产生库存变动", outbound.getOutboundNo());
        }
        refreshStockSnapshot(warehouseId, items);
    }

    /** 回填明细行库存快照（可用库存/账面库存），供列表与明细页展示 */
    private void refreshStockSnapshot(Long warehouseId, List<SaleOutboundItem> items) {
        for (SaleOutboundItem item : items) {
            if (item.getProductId() == null) {
                continue;
            }
            try {
                Stock stock = stockService.getStockDetail(item.getProductId(), warehouseId);
                if (stock != null) {
                    item.setAvailableStock(stock.getAvailableQuantity() != null ? stock.getAvailableQuantity() : BigDecimal.ZERO);
                    item.setBookStock(stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO);
                    outboundItemMapper.updateById(item);
                }
            } catch (Exception e) {
                log.warn("回填库存快照失败: 产品ID={}, 仓库ID={}, 原因={}", item.getProductId(), warehouseId, e.getMessage());
            }
        }
    }

    /**
     * 计算明细行金额（完整价格计算链，对标Odoo/SAP/金蝶/用友）
     * 链1: lineAmount = quantity × unitPrice（原价金额）
     * 链2: discountedAmount = lineAmount × (1 - discountRate/100)（折扣后金额）
     * 链3: discountedPrice = discountedAmount / quantity（折扣后单价）
     * 链4: favorableAmount = discountedAmount × (1 - favorableDiscountRate/100)（优惠后金额）
     * 链5: favorableUnitPrice = favorableAmount / quantity（优惠后单价）
     * 链6: costAmount = quantity × costPrice（成本金额）
     * 链7: grossProfit = favorableAmount - costAmount（参考毛利）
     * 链8: smallUnitPrice / smallUnitQuantity / conversionResult（单位换算）
     */
    private void calculateItemAmounts(SaleOutboundItem item) {
        BigDecimal quantity = item.getOutboundQuantity() != null ? item.getOutboundQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        // 链1: 原价金额 = 数量 × 单价
        BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setLineAmount(lineAmount);

        // 链2: 折扣计算
        BigDecimal discountRate = item.getDiscountRate() != null ? item.getDiscountRate() : BigDecimal.ZERO;
        BigDecimal discountedAmount;
        BigDecimal discountedPrice;
        if (discountRate.compareTo(BigDecimal.ZERO) > 0 && discountRate.compareTo(BigDecimal.valueOf(100)) < 0) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(discountRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            discountedAmount = lineAmount.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
            discountedPrice = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? discountedAmount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } else {
            discountedAmount = lineAmount;
            discountedPrice = unitPrice;
        }
        item.setDiscountedAmount(discountedAmount);
        item.setDiscountedPrice(discountedPrice);

        // 链3: 优惠折扣计算（二级折扣）
        BigDecimal favorableDiscountRate = item.getFavorableDiscountRate() != null ? item.getFavorableDiscountRate() : BigDecimal.ZERO;
        BigDecimal favorableAmount;
        BigDecimal favorableUnitPrice;
        if (favorableDiscountRate.compareTo(BigDecimal.ZERO) > 0 && favorableDiscountRate.compareTo(BigDecimal.valueOf(100)) < 0) {
            BigDecimal favFactor = BigDecimal.ONE.subtract(favorableDiscountRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            favorableAmount = discountedAmount.multiply(favFactor).setScale(2, RoundingMode.HALF_UP);
            favorableUnitPrice = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? favorableAmount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } else {
            favorableAmount = discountedAmount;
            favorableUnitPrice = discountedPrice;
        }
        item.setFavorableAmount(favorableAmount);
        item.setFavorableUnitPrice(favorableUnitPrice);

        // 链4: 成本与毛利计算
        BigDecimal costPrice = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
        BigDecimal costAmount = quantity.multiply(costPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfit = favorableAmount.subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
        item.setCostAmount(costAmount);
        item.setGrossProfit(grossProfit);

        // 链5: 小单位换算（对标金蝶/用友的辅助单位换算）
        BigDecimal smallUnitQuantity = item.getSmallUnitQuantity();
        if (smallUnitQuantity != null && smallUnitQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal smallUnitPrice = favorableAmount.divide(smallUnitQuantity, 4, RoundingMode.HALF_UP);
            item.setSmallUnitPrice(smallUnitPrice);
        }

        // 链6: 换算结果
        String conversionRelation = item.getConversionRelation();
        if (conversionRelation != null && !conversionRelation.isEmpty() && quantity.compareTo(BigDecimal.ZERO) > 0) {
            try {
                // 换算关系格式如 "1箱=12盒" 或 "1:12"
                BigDecimal conversionResult = calculateConversion(quantity, conversionRelation);
                item.setConversionResult(conversionResult);
            } catch (Exception e) {
                log.debug("换算计算失败: {}", conversionRelation);
            }
        }
    }

    /**
     * 计算单位换算结果
     * 支持格式: "1箱=12盒" / "1:12" / "12"
     */
    private BigDecimal calculateConversion(BigDecimal quantity, String conversionRelation) {
        if (conversionRelation == null || conversionRelation.isEmpty()) return quantity;
        try {
            // 尝试提取换算比例数字
            String ratio = conversionRelation.replaceAll("[^0-9.]", "");
            if (!ratio.isEmpty()) {
                BigDecimal factor = new BigDecimal(ratio);
                if (factor.compareTo(BigDecimal.ZERO) > 0) {
                    return quantity.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return quantity;
    }
}