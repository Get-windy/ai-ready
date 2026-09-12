package cn.aiedge.erp.sale.saleexchange.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.sale.saleexchange.dto.SaleExchangeQuery;
import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.mapper.ExchangeApprovalRecordMapper;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeItemMapper;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeMapper;
import cn.aiedge.erp.sale.saleexchange.service.SaleExchangeService;
import cn.aiedge.erp.sale.service.ISaleExchangeService;
import cn.aiedge.erp.stock.service.StockService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * 销售换货单服务实现。
 *
 * <p>状态字典（前后端统一）：0 草稿 / 1 待审核 / 2 已审核 / 4 已完成 / 5 已拒绝 / 6 已取消。
 * 换货单的库存语义：审核通过时换入仓库 +数量、换出仓库 −数量；取消时反向回滚。</p>
 */
@Slf4j
@Service
public class SaleExchangeServiceImpl extends ServiceImpl<SaleExchangeMapper, SaleExchange>
        implements SaleExchangeService, ISaleExchangeService {

    /** 已过账（库存已增减）的状态集合：已审核、已完成 */
    private static final Set<Integer> POSTED_STATUS = Set.of(2, 4);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SaleExchangeItemMapper saleExchangeItemMapper;

    @Autowired
    private ExchangeApprovalRecordMapper exchangeApprovalRecordMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private cn.aiedge.base.mapper.SysUserMapper sysUserMapper;

    // ══════════════════════════════════════════════════════════
    // 查询
    // ══════════════════════════════════════════════════════════

    @Override
    public Page<SaleExchange> pageListExtended(SaleExchangeQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();

        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();
        applyQuery(wrapper, query);
        wrapper.orderByDesc(SaleExchange::getExchangeDate).orderByDesc(SaleExchange::getId);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleExchange> exportList(SaleExchangeQuery query) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();
        applyQuery(wrapper, query);
        wrapper.orderByDesc(SaleExchange::getExchangeDate).orderByDesc(SaleExchange::getId);
        return this.baseMapper.selectList(wrapper);
    }

    private void applyQuery(LambdaQueryWrapper<SaleExchange> wrapper, SaleExchangeQuery q) {
        LocalDateTime start = parseStart(q.getStartDate());
        LocalDateTime end = parseEnd(q.getEndDate());
        wrapper.ge(start != null, SaleExchange::getExchangeDate, start);
        wrapper.le(end != null, SaleExchange::getExchangeDate, end);

        wrapper.like(hasText(q.getExchangeNo()), SaleExchange::getExchangeNo, q.getExchangeNo());
        wrapper.like(hasText(q.getCustomerName()), SaleExchange::getCustomerName, q.getCustomerName());
        wrapper.like(hasText(q.getHandlerName()), SaleExchange::getHandlerName, q.getHandlerName());
        wrapper.like(hasText(q.getDeptName()), SaleExchange::getDeptName, q.getDeptName());
        wrapper.like(hasText(q.getCreatorName()), SaleExchange::getCreatorName, q.getCreatorName());
        wrapper.like(hasText(q.getBookkeeperName()), SaleExchange::getBookkeeperName, q.getBookkeeperName());
        wrapper.like(hasText(q.getInWarehouseName()), SaleExchange::getInWarehouseName, q.getInWarehouseName());
        wrapper.like(hasText(q.getOutWarehouseName()), SaleExchange::getOutWarehouseName, q.getOutWarehouseName());
        wrapper.eq(q.getStatus() != null, SaleExchange::getStatus, q.getStatus());
        wrapper.eq(hasText(q.getSettleStatus()), SaleExchange::getSettleStatus, q.getSettleStatus());
        wrapper.eq(hasText(q.getSalesType()), SaleExchange::getSalesType, q.getSalesType());
        wrapper.like(hasText(q.getRemark()), SaleExchange::getRemark, q.getRemark());
        wrapper.eq(q.getExtNum1() != null, SaleExchange::getExtNum1, q.getExtNum1());
        wrapper.eq(q.getExtNum2() != null, SaleExchange::getExtNum2, q.getExtNum2());
        wrapper.like(hasText(q.getExtText1()), SaleExchange::getExtText1, q.getExtText1());
        wrapper.like(hasText(q.getExtText2()), SaleExchange::getExtText2, q.getExtText2());
        wrapper.like(hasText(q.getExtText3()), SaleExchange::getExtText3, q.getExtText3());

        // 显示红冲：默认隐藏已取消单据；勾选「显示红冲」或在状态里显式筛选已取消时不再排除
        boolean explicitCancelledFilter = q.getStatus() != null && q.getStatus() == 6;
        if (!Boolean.TRUE.equals(q.getShowRedFlush()) && !explicitCancelledFilter) {
            wrapper.ne(SaleExchange::getStatus, 6);
        }

        // 商品行属性是明细级属性：先取命中明细所属单据，再回主表过滤
        if (hasText(q.getProductLineAttr())) {
            List<SaleExchangeItem> hits = saleExchangeItemMapper.selectList(
                    new LambdaQueryWrapper<SaleExchangeItem>()
                            .select(SaleExchangeItem::getExchangeId)
                            .eq(SaleExchangeItem::getProductLineAttr, q.getProductLineAttr()));
            Set<Long> exchangeIds = new HashSet<>();
            for (SaleExchangeItem hit : hits) {
                if (hit.getExchangeId() != null) {
                    exchangeIds.add(hit.getExchangeId());
                }
            }
            if (exchangeIds.isEmpty()) {
                // 命中为空时用恒假条件，避免返回全表
                wrapper.eq(SaleExchange::getId, -1L);
            } else {
                wrapper.in(SaleExchange::getId, exchangeIds);
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // 单号号段
    // ══════════════════════════════════════════════════════════

    @Override
    public String generateExchangeNo() {
        String prefix = "XSHHD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String lastNo = this.baseMapper.selectLastExchangeNo(prefix);
        int seq = 1;
        if (lastNo != null && lastNo.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("历史单号后缀非数字，号段从1重新开始: {}", lastNo);
            }
        }
        return prefix + String.format("%04d", seq);
    }

    // ══════════════════════════════════════════════════════════
    // 增删改
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange createExchange(SaleExchange exchange) {
        Long userId = StpUtil.getLoginIdAsLong();
        exchange.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());

        // 单号来自后端号段 /next-no：前端原样传入则保留，未传时自动生成
        if (!hasText(exchange.getExchangeNo())) {
            exchange.setExchangeNo(generateExchangeNo());
        }

        Integer status = exchange.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            status = 0;
        }
        exchange.setStatus(status);

        exchange.setCreatorId(userId);
        if (!hasText(exchange.getCreatorName())) {
            exchange.setCreatorName(currentUserName());
        }
        exchange.setCreateTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        if (exchange.getPrintCount() == null) {
            exchange.setPrintCount(0);
        }
        if (!hasText(exchange.getSettleStatus())) {
            exchange.setSettleStatus("unsettled");
        }
        this.save(exchange);

        saveItems(exchange.getId(), exchange.getItems(), exchange.getTenantId());
        calculateTotals(exchange.getId());

        return this.getById(exchange.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange updateExchange(Long id, SaleExchange exchange) {
        SaleExchange existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的换货单可以修改");
        }

        exchange.setId(id);
        // 单号与制单信息不接受前端改写
        exchange.setExchangeNo(existing.getExchangeNo());
        exchange.setCreatorId(existing.getCreatorId());
        exchange.setCreatorName(existing.getCreatorName());
        exchange.setCreateTime(existing.getCreateTime());
        exchange.setTenantId(existing.getTenantId());
        exchange.setStatus(0);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        if (exchange.getItems() != null) {
            // 明细整体替换（@TableLogic 逻辑删除，保留历史轨迹）
            saleExchangeItemMapper.delete(new LambdaQueryWrapper<SaleExchangeItem>()
                    .eq(SaleExchangeItem::getExchangeId, id));
            saveItems(id, exchange.getItems(), existing.getTenantId());
        }

        calculateTotals(id);

        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteExchange(Long id) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            return false;
        }
        if (exchange.getStatus() != null && POSTED_STATUS.contains(exchange.getStatus())) {
            throw new RuntimeException("已审核/已完成的换货单不能删除，请先取消");
        }
        // 已取消单据的库存已在 cancel() 中回滚，此处仅级联删除明细
        saleExchangeItemMapper.delete(new LambdaQueryWrapper<SaleExchangeItem>()
                .eq(SaleExchangeItem::getExchangeId, id));
        return this.removeById(id);
    }

    // ══════════════════════════════════════════════════════════
    // 审批流
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange submitForApproval(Long id) {
        SaleExchange exchange = requireStatus(id, 0, "只有草稿状态的换货单可以提交审批");
        exchange.setStatus(1);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
        saveApprovalRecord(id, "submit", "提交审批", null);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange approve(Long id, Long approverId, String approvedByName, String remark) {
        SaleExchange exchange = requireStatus(id, 1, "只有待审批状态的换货单可以审批");
        exchange.setStatus(2);
        exchange.setApprovedBy(approverId);
        exchange.setApprovedByName(hasText(approvedByName) ? approvedByName : currentUserName());
        exchange.setApprovedTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // P0 红线：审核通过必须真实过账（换入 +、换出 −）
        processInventoryChange(exchange);

        saveApprovalRecord(id, "approve", "审批通过", remark);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Long> ids, Long approverId, String approvedByName) {
        int count = 0;
        for (Long id : ids) {
            try {
                approve(id, approverId, approvedByName, "批量审批通过");
                count++;
            } catch (Exception e) {
                log.warn("批量审核跳过 ID {}: {}", id, e.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange reject(Long id, String remark) {
        SaleExchange exchange = requireStatus(id, 1, "只有待审批状态的换货单可以拒绝");
        exchange.setStatus(5);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
        saveApprovalRecord(id, "reject", "审批拒绝", remark);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange cancel(Long id, String reason) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != null && exchange.getStatus() == 6) {
            throw new RuntimeException("换货单已取消");
        }

        // 已过账单据取消前必须回滚库存（换入 −、换出 +）
        rollbackIfPosted(exchange);

        exchange.setStatus(6);
        exchange.setRemark(reason);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        saveApprovalRecord(id, "cancel", "取消换货单", reason);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange complete(Long id) {
        SaleExchange exchange = requireStatus(id, 2, "只有已审核状态的换货单可以完成");
        exchange.setStatus(4);
        exchange.setCompletedTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
        saveApprovalRecord(id, "complete", "完成换货", null);
        return exchange;
    }

    // ══════════════════════════════════════════════════════════
    // 库存闭环
    // ══════════════════════════════════════════════════════════

    /**
     * 过账：换入仓库增加库存，换出仓库减少库存（库存不足时整单回滚）。
     */
    private void processInventoryChange(SaleExchange exchange) {
        List<SaleExchangeItem> items = getItems(exchange.getId());
        if (items.isEmpty()) {
            return;
        }
        if (exchange.getInWarehouseId() == null || exchange.getOutWarehouseId() == null) {
            throw new RuntimeException("换货单缺少换入/换出仓库，无法过账");
        }

        for (SaleExchangeItem item : items) {
            if (item.getProductId() == null || isZero(item.getQuantity())) {
                continue;
            }
            boolean ok;
            if (isInType(item)) {
                ok = stockService.increaseStock(item.getProductId(), exchange.getInWarehouseId(), item.getQuantity());
            } else {
                ok = stockService.decreaseStock(item.getProductId(), exchange.getOutWarehouseId(), item.getQuantity());
            }
            if (!ok) {
                throw new RuntimeException("库存过账失败，商品ID: " + item.getProductId()
                        + "（换出仓库库存不足或仓库无效）");
            }
        }
    }

    /**
     * 回滚：已过账单据（已审核/已完成）取消或删除时反向冲销库存。
     */
    private void rollbackIfPosted(SaleExchange exchange) {
        if (exchange.getStatus() == null || !POSTED_STATUS.contains(exchange.getStatus())) {
            return;
        }
        List<SaleExchangeItem> items = getItems(exchange.getId());
        for (SaleExchangeItem item : items) {
            if (item.getProductId() == null || isZero(item.getQuantity())) {
                continue;
            }
            boolean ok;
            if (isInType(item)) {
                ok = stockService.decreaseStock(item.getProductId(), exchange.getInWarehouseId(), item.getQuantity());
            } else {
                ok = stockService.increaseStock(item.getProductId(), exchange.getOutWarehouseId(), item.getQuantity());
            }
            if (!ok) {
                throw new RuntimeException("库存回滚失败，商品ID: " + item.getProductId());
            }
        }
    }

    private boolean isInType(SaleExchangeItem item) {
        return item.getWarehouseType() != null && item.getWarehouseType() == 1;
    }

    private boolean isZero(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    // ══════════════════════════════════════════════════════════
    // 明细与合计
    // ══════════════════════════════════════════════════════════

    private void saveItems(Long exchangeId, List<SaleExchangeItem> items, Long tenantId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SaleExchangeItem item : items) {
            if (item.getProductId() == null && !hasText(item.getProductName())) {
                continue;
            }
            item.setId(null);
            item.setExchangeId(exchangeId);
            item.setTenantId(tenantId);
            if (item.getWarehouseType() == null) {
                item.setWarehouseType(1);
            }
            fillItemDerived(item);
            saleExchangeItemMapper.insert(item);
        }
    }

    /** 明细派生字段回填：金额 / 折后单价 / 折后金额 / 换算数量，保证落库口径完整 */
    private void fillItemDerived(SaleExchangeItem item) {
        BigDecimal qty = nz(item.getQuantity());
        BigDecimal price = nz(item.getUnitPrice());
        if (item.getAmount() == null) {
            item.setAmount(qty.multiply(price));
        }
        BigDecimal discount = item.getDiscount() == null ? new BigDecimal("100") : item.getDiscount();
        if (item.getDiscountPrice() == null) {
            item.setDiscountPrice(price.multiply(discount).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        }
        if (item.getDiscountAmount() == null) {
            item.setDiscountAmount(qty.multiply(item.getDiscountPrice()));
        }
        if (item.getCostAmount() == null) {
            item.setCostAmount(qty.multiply(nz(item.getCostPrice())));
        }
    }

    /**
     * 汇总主表合计字段。
     *
     * <p>口径：金额 = Σ(数量×单价)；折后金额 = Σ明细折后金额；本单金额 = 折后金额。
     * 数量/物理属性按换入、换出分仓汇总，另合计重量体积。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long exchangeId) {
        List<SaleExchangeItem> items = getItems(exchangeId);

        BigDecimal inQty = BigDecimal.ZERO;
        BigDecimal outQty = BigDecimal.ZERO;
        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;

        for (SaleExchangeItem item : items) {
            BigDecimal qty = nz(item.getQuantity());
            BigDecimal lineAmount = item.getAmount() != null ? item.getAmount() : qty.multiply(nz(item.getUnitPrice()));
            BigDecimal lineDiscount = item.getDiscountAmount() != null ? item.getDiscountAmount() : lineAmount;

            if (isInType(item)) {
                inQty = inQty.add(qty);
            } else if (item.getWarehouseType() != null && item.getWarehouseType() == 2) {
                outQty = outQty.add(qty);
            }
            productAmount = productAmount.add(lineAmount);
            discountAmount = discountAmount.add(lineDiscount);
            weight = weight.add(nz(item.getWeight()));
            volume = volume.add(nz(item.getVolume()));
        }

        SaleExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            return;
        }
        exchange.setInQuantityTotal(inQty);
        exchange.setOutQuantityTotal(outQty);
        exchange.setProductAmount(productAmount);
        exchange.setDiscountAmount(discountAmount);
        exchange.setTotalAmount(discountAmount);
        exchange.setTotalWeight(weight);
        exchange.setTotalVolume(volume);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
    }

    // ══════════════════════════════════════════════════════════
    // 明细 / 审批记录 / 跟踪
    // ══════════════════════════════════════════════════════════

    @Override
    public List<SaleExchangeItem> getItems(Long exchangeId) {
        return saleExchangeItemMapper.selectList(new LambdaQueryWrapper<SaleExchangeItem>()
                .eq(SaleExchangeItem::getExchangeId, exchangeId)
                .orderByAsc(SaleExchangeItem::getId));
    }

    @Override
    public List<SaleExchangeItem> getItemsByWarehouseType(Long exchangeId, Integer warehouseType) {
        return saleExchangeItemMapper.selectList(new LambdaQueryWrapper<SaleExchangeItem>()
                .eq(SaleExchangeItem::getExchangeId, exchangeId)
                .eq(SaleExchangeItem::getWarehouseType, warehouseType)
                .orderByAsc(SaleExchangeItem::getId));
    }

    @Override
    public List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId) {
        return exchangeApprovalRecordMapper.selectList(new LambdaQueryWrapper<ExchangeApprovalRecord>()
                .eq(ExchangeApprovalRecord::getExchangeId, exchangeId)
                .orderByAsc(ExchangeApprovalRecord::getId));
    }

    @Override
    public Map<String, Object> getTracking(Long exchangeId) {
        SaleExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }

        List<SaleExchangeItem> items = getItems(exchangeId);
        List<ExchangeApprovalRecord> records = getApprovalRecords(exchangeId);

        List<Map<String, Object>> timeline = new ArrayList<>();
        Map<String, Object> createEvent = new LinkedHashMap<>();
        createEvent.put("time", exchange.getCreateTime());
        createEvent.put("title", "创建换货单");
        createEvent.put("content", "换货单 " + exchange.getExchangeNo() + " 已创建");
        createEvent.put("status", "success");
        timeline.add(createEvent);

        for (ExchangeApprovalRecord record : records) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("time", record.getCreateTime());
            event.put("title", record.getActionName());
            event.put("content", record.getRemark() != null ? record.getRemark() : "");
            String status = "processing";
            if ("approve".equals(record.getAction()) || "complete".equals(record.getAction())) {
                status = "success";
            } else if ("reject".equals(record.getAction()) || "cancel".equals(record.getAction())) {
                status = "error";
            }
            event.put("status", status);
            timeline.add(event);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exchange", exchange);
        result.put("items", items);
        result.put("approvalRecords", records);
        result.put("timeline", timeline);
        return result;
    }

    // ══════════════════════════════════════════════════════════
    // 打印
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void print(Long id) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        Integer printCount = exchange.getPrintCount();
        exchange.setPrintCount(printCount == null ? 1 : printCount + 1);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchPrint(List<Long> ids) {
        for (Long id : ids) {
            try {
                print(id);
            } catch (Exception e) {
                log.warn("批量打印跳过 ID {}: {}", id, e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Excel 导出（真实 xlsx，列与列表页 32 列一致）
    // ══════════════════════════════════════════════════════════

    @Override
    public byte[] exportExcel(SaleExchangeQuery query) {
        List<SaleExchange> list = exportList(query);

        // 序号、列名、取值函数（与开发文档「表格列字段」32 列顺序一致）
        List<String> headers = Arrays.asList(
                "单据日期", "单据编号", "单据状态", "入库仓库", "出库仓库", "客户编号", "客户", "经手人",
                "部门", "入库数量", "出库数量", "本单金额", "金额", "折后金额", "已结金额", "结算状态",
                "重量(kg)", "体积(m³)", "销售类型", "单据备注", "表头自定义字段1(数字)", "表头自定义字段2(数字)",
                "表头自定义字段3(文本)", "表头自定义字段4(文本)", "表头自定义字段5(文本)", "摘要", "附件",
                "制单人", "记账人", "记账时间", "制单时间", "打印次数");

        List<Function<SaleExchange, Object>> getters = Arrays.asList(
                e -> fmtDate(e.getExchangeDate()), SaleExchange::getExchangeNo, e -> statusText(e.getStatus()),
                SaleExchange::getInWarehouseName, SaleExchange::getOutWarehouseName, SaleExchange::getCustomerCode,
                SaleExchange::getCustomerName, SaleExchange::getHandlerName, SaleExchange::getDeptName,
                SaleExchange::getInQuantityTotal, SaleExchange::getOutQuantityTotal, SaleExchange::getTotalAmount,
                SaleExchange::getProductAmount, SaleExchange::getDiscountAmount, SaleExchange::getSettledAmount,
                e -> settleStatusText(e.getSettleStatus()), SaleExchange::getTotalWeight, SaleExchange::getTotalVolume,
                SaleExchange::getSalesType, SaleExchange::getRemark, SaleExchange::getExtNum1,
                SaleExchange::getExtNum2, SaleExchange::getExtText1, SaleExchange::getExtText2,
                SaleExchange::getExtText3, SaleExchange::getSummary, SaleExchange::getAttachment,
                SaleExchange::getCreatorName, SaleExchange::getBookkeeperName,
                e -> fmtDateTime(e.getBookkeepingTime()), e -> fmtDateTime(e.getCreateTime()),
                SaleExchange::getPrintCount);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("销售换货单");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
                sheet.setColumnWidth(i, 4200);
            }
            for (int r = 0; r < list.size(); r++) {
                Row row = sheet.createRow(r + 1);
                SaleExchange exchange = list.get(r);
                for (int c = 0; c < getters.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object value = getters.get(c).apply(exchange);
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number num) {
                        cell.setCellValue(num.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // 辅助
    // ══════════════════════════════════════════════════════════

    private SaleExchange requireStatus(Long id, int expected, String message) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() == null || exchange.getStatus() != expected) {
            throw new RuntimeException(message);
        }
        return exchange;
    }

    private void saveApprovalRecord(Long exchangeId, String action, String actionName, String remark) {
        ExchangeApprovalRecord record = new ExchangeApprovalRecord();
        record.setExchangeId(exchangeId);
        record.setAction(action);
        record.setActionName(actionName);
        record.setOperatorId(StpUtil.getLoginIdAsLong());
        record.setOperatorName(currentUserName());
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        exchangeApprovalRecordMapper.insert(record);
    }

    private String currentUserName() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            cn.aiedge.base.entity.SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().isBlank()) {
                    return user.getNickname();
                }
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    return user.getUsername();
                }
            }
            return String.valueOf(userId);
        } catch (Exception e) {
            return StpUtil.getLoginIdAsString();
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static LocalDateTime parseStart(String date) {
        if (!hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date.substring(0, 10)).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseEnd(String date) {
        if (!hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date.substring(0, 10)).atTime(23, 59, 59);
        } catch (Exception e) {
            return null;
        }
    }

    private static String fmtDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE_FMT);
    }

    private static String fmtDateTime(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATETIME_FMT);
    }

    private static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待审核";
            case 2 -> "已审核";
            case 4 -> "已完成";
            case 5 -> "已拒绝";
            case 6 -> "已取消";
            default -> "未知";
        };
    }

    private static String settleStatusText(String settleStatus) {
        if (settleStatus == null) {
            return "";
        }
        return switch (settleStatus) {
            case "unsettled" -> "未结算";
            case "partial" -> "部分结算";
            case "settled" -> "已结算";
            default -> settleStatus;
        };
    }
}
