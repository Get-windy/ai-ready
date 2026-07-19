package cn.aiedge.erp.sale.saleexchange.service.impl;

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
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SaleExchangeServiceImpl extends ServiceImpl<SaleExchangeMapper, SaleExchange> implements SaleExchangeService, ISaleExchangeService {

    @Autowired
    private SaleExchangeItemMapper saleExchangeItemMapper;

    @Autowired
    private ExchangeApprovalRecordMapper exchangeApprovalRecordMapper;

    @Autowired
    private StockService stockService;

    @Override
    public Page<SaleExchange> pageList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                       String startDate, String endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();

        // 转换日期字符串为 LocalDateTime，避免 PostgreSQL 类型不匹配
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            } catch (Exception e) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            } catch (Exception e) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }
        }

        wrapper.like(keyword != null, SaleExchange::getExchangeNo, keyword)
                .eq(customerId != null, SaleExchange::getCustomerId, customerId)
                .eq(status != null, SaleExchange::getStatus, status)
                .eq(exchangeType != null, SaleExchange::getExchangeType, exchangeType)
                .ge(startDateTime != null, SaleExchange::getCreateTime, startDateTime)
                .le(endDateTime != null, SaleExchange::getCreateTime, endDateTime)
                .orderByDesc(SaleExchange::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleExchange> exportList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();

        // 转换日期字符串为 LocalDateTime
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            } catch (Exception e) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            } catch (Exception e) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }
        }

        wrapper.like(keyword != null, SaleExchange::getExchangeNo, keyword)
                .eq(customerId != null, SaleExchange::getCustomerId, customerId)
                .eq(status != null, SaleExchange::getStatus, status)
                .eq(exchangeType != null, SaleExchange::getExchangeType, exchangeType)
                .ge(startDateTime != null, SaleExchange::getCreateTime, startDateTime)
                .le(endDateTime != null, SaleExchange::getCreateTime, endDateTime)
                .orderByDesc(SaleExchange::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public String generateExchangeNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "SE" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange createExchange(SaleExchange exchange, List<SaleExchangeItem> items) {
        Long loginId = StpUtil.getLoginIdAsLong();
        String loginName = StpUtil.getLoginIdAsString();

        exchange.setTenantId(loginId);
        exchange.setExchangeNo(generateExchangeNo());
        exchange.setStatus(0);
        exchange.setCreatorId(loginId);
        exchange.setCreatorName(loginName);
        exchange.setCreateTime(LocalDateTime.now());
        this.save(exchange);

        if (items != null && !items.isEmpty()) {
            for (SaleExchangeItem item : items) {
                item.setExchangeId(exchange.getId());
                item.setTenantId(loginId);
                saleExchangeItemMapper.insert(item);
            }
        }

        calculateTotals(exchange.getId());

        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange updateExchange(Long id, SaleExchange exchange, List<SaleExchangeItem> items) {
        SaleExchange existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (existing.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的换货单可以修改");
        }

        exchange.setId(id);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        if (items != null) {
            // 删除原有明细
            saleExchangeItemMapper.delete(
                    new LambdaQueryWrapper<SaleExchangeItem>()
                            .eq(SaleExchangeItem::getExchangeId, id)
            );
            // 插入新明细
            for (SaleExchangeItem item : items) {
                item.setId(null);
                item.setExchangeId(id);
                saleExchangeItemMapper.insert(item);
            }
        }

        calculateTotals(id);

        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange submitForApproval(Long id) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的换货单可以提交审批");
        }

        exchange.setStatus(1);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 记录审批记录
        saveApprovalRecord(id, "submit", "提交审批", null);

        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange approve(Long id, Long approverId, String approvedByName, String remark) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的换货单可以审批");
        }

        exchange.setStatus(2);
        exchange.setApprovedBy(approverId);
        exchange.setApprovedByName(approvedByName);
        exchange.setApprovedTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 记录审批记录
        saveApprovalRecord(id, "approve", "审批通过", remark);

        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange reject(Long id, String remark) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的换货单可以拒绝");
        }

        exchange.setStatus(5);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 记录审批记录
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
        if (exchange.getStatus() == 4) {
            throw new RuntimeException("已完成的换货单不能取消");
        }
        if (exchange.getStatus() == 6) {
            throw new RuntimeException("换货单已取消");
        }

        exchange.setStatus(6);
        exchange.setRemark(reason);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 记录审批记录
        saveApprovalRecord(id, "cancel", "取消换货单", reason);

        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleExchange complete(Long id) {
        SaleExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 2) {
            throw new RuntimeException("只有已审批状态的换货单可以完成");
        }

        exchange.setStatus(4);
        exchange.setCompletedTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 记录审批记录
        saveApprovalRecord(id, "complete", "完成换货", null);

        return exchange;
    }

    @Override
    public List<SaleExchangeItem> getItems(Long exchangeId) {
        return saleExchangeItemMapper.selectList(
                new LambdaQueryWrapper<SaleExchangeItem>()
                        .eq(SaleExchangeItem::getExchangeId, exchangeId)
                        .orderByAsc(SaleExchangeItem::getId)
        );
    }

    @Override
    public List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId) {
        return exchangeApprovalRecordMapper.selectList(
                new LambdaQueryWrapper<ExchangeApprovalRecord>()
                        .eq(ExchangeApprovalRecord::getExchangeId, exchangeId)
                        .orderByAsc(ExchangeApprovalRecord::getId)
        );
    }

    @Override
    public Map<String, Object> getTracking(Long exchangeId) {
        SaleExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }

        List<SaleExchangeItem> items = getItems(exchangeId);
        List<ExchangeApprovalRecord> records = getApprovalRecords(exchangeId);

        // 构建时间线
        List<Map<String, Object>> timeline = new ArrayList<>();

        // 创建事件
        Map<String, Object> createEvent = new LinkedHashMap<>();
        createEvent.put("time", exchange.getCreateTime());
        createEvent.put("title", "创建换货单");
        createEvent.put("content", "换货单 " + exchange.getExchangeNo() + " 已创建");
        createEvent.put("status", "success");
        timeline.add(createEvent);

        // 审批记录事件
        for (ExchangeApprovalRecord record : records) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("time", record.getCreateTime());
            event.put("title", record.getActionName());
            event.put("content", record.getRemark() != null ? record.getRemark() : "");
            String status = "processing";
            if ("approve".equals(record.getAction())) {
                status = "success";
            } else if ("reject".equals(record.getAction())) {
                status = "error";
            } else if ("cancel".equals(record.getAction())) {
                status = "error";
            } else if ("complete".equals(record.getAction())) {
                status = "success";
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

    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long exchangeId) {
        List<SaleExchangeItem> items = getItems(exchangeId);

        // 计算入库和出库数量合计
        BigDecimal inQuantityTotal = items.stream()
                .filter(i -> i.getWarehouseType() != null && i.getWarehouseType() == 1)
                .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outQuantityTotal = items.stream()
                .filter(i -> i.getWarehouseType() != null && i.getWarehouseType() == 2)
                .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算金额合计
        BigDecimal productAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO;
                    BigDecimal price = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = items.stream()
                .map(i -> i.getDiscountAmount() != null ? i.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight = items.stream()
                .map(i -> i.getWeight() != null ? i.getWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVolume = items.stream()
                .map(i -> i.getVolume() != null ? i.getVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SaleExchange exchange = this.getById(exchangeId);
        exchange.setInQuantityTotal(inQuantityTotal);
        exchange.setOutQuantityTotal(outQuantityTotal);
        exchange.setProductAmount(productAmount);
        exchange.setDiscountAmount(discountAmount);
        exchange.setTotalAmount(productAmount.subtract(discountAmount));
        exchange.setTotalWeight(totalWeight);
        exchange.setTotalVolume(totalVolume);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
    }

    private void saveApprovalRecord(Long exchangeId, String action, String actionName, String remark) {
        ExchangeApprovalRecord record = new ExchangeApprovalRecord();
        record.setExchangeId(exchangeId);
        record.setAction(action);
        record.setActionName(actionName);
        record.setOperatorId(StpUtil.getLoginIdAsLong());
        record.setOperatorName(StpUtil.getLoginIdAsString());
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        exchangeApprovalRecordMapper.insert(record);
    }

    @Override
    public Page<SaleExchange> pageListExtended(Map<String, Object> params, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();

        // 日期范围
        String startDate = (String) params.get("startDate");
        String endDate = (String) params.get("endDate");
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            } catch (Exception e) {
                // ignore
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            } catch (Exception e) {
                // ignore
            }
        }

        // 关键词（单据编号）
        String keyword = (String) params.get("keyword");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SaleExchange::getExchangeNo, keyword);
        }

        // 客户名称
        String customerName = (String) params.get("customerName");
        if (customerName != null && !customerName.isEmpty()) {
            wrapper.like(SaleExchange::getCustomerName, customerName);
        }

        // 经手人
        String handlerName = (String) params.get("handlerName");
        if (handlerName != null && !handlerName.isEmpty()) {
            wrapper.like(SaleExchange::getHandlerName, handlerName);
        }

        // 部门
        String deptName = (String) params.get("deptName");
        if (deptName != null && !deptName.isEmpty()) {
            wrapper.like(SaleExchange::getDeptName, deptName);
        }

        // 入库仓库
        String inWarehouseName = (String) params.get("inWarehouseName");
        if (inWarehouseName != null && !inWarehouseName.isEmpty()) {
            wrapper.like(SaleExchange::getInWarehouseName, inWarehouseName);
        }

        // 出库仓库
        String outWarehouseName = (String) params.get("outWarehouseName");
        if (outWarehouseName != null && !outWarehouseName.isEmpty()) {
            wrapper.like(SaleExchange::getOutWarehouseName, outWarehouseName);
        }

        // 制单人
        String creatorName = (String) params.get("creatorName");
        if (creatorName != null && !creatorName.isEmpty()) {
            wrapper.like(SaleExchange::getCreatorName, creatorName);
        }

        // 记账人
        String bookkeeperName = (String) params.get("bookkeeperName");
        if (bookkeeperName != null && !bookkeeperName.isEmpty()) {
            wrapper.like(SaleExchange::getBookkeeperName, bookkeeperName);
        }

        // 单据状态
        Integer status = (Integer) params.get("status");
        if (status != null) {
            wrapper.eq(SaleExchange::getStatus, status);
        }

        // 结算状态
        String settleStatus = (String) params.get("settleStatus");
        if (settleStatus != null && !settleStatus.isEmpty()) {
            wrapper.eq(SaleExchange::getSettleStatus, settleStatus);
        }

        // 销售类型
        String salesType = (String) params.get("salesType");
        if (salesType != null && !salesType.isEmpty()) {
            wrapper.eq(SaleExchange::getSalesType, salesType);
        }

        // 日期范围过滤
        wrapper.ge(startDateTime != null, SaleExchange::getExchangeDate, startDateTime);
        wrapper.le(endDateTime != null, SaleExchange::getExchangeDate, endDateTime);
        wrapper.orderByDesc(SaleExchange::getCreateTime);

        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Long> ids, Long approverId, String approvedByName) {
        int count = 0;
        for (Long id : ids) {
            try {
                SaleExchange exchange = this.getById(id);
                if (exchange != null && exchange.getStatus() == 1) {
                    exchange.setStatus(2);
                    exchange.setApprovedBy(approverId);
                    exchange.setApprovedByName(approvedByName);
                    exchange.setApprovedTime(LocalDateTime.now());
                    exchange.setUpdateTime(LocalDateTime.now());
                    this.updateById(exchange);
                    saveApprovalRecord(id, "approve", "批量审批通过", null);
                    count++;
                }
            } catch (Exception e) {
                // 单条失败不影响其他
            }
        }
        return count;
    }

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
                // 单条失败不影响其他
            }
        }
    }

    @Override
    public List<SaleExchangeItem> getItemsByWarehouseType(Long exchangeId, Integer warehouseType) {
        return saleExchangeItemMapper.selectList(
                new LambdaQueryWrapper<SaleExchangeItem>()
                        .eq(SaleExchangeItem::getExchangeId, exchangeId)
                        .eq(SaleExchangeItem::getWarehouseType, warehouseType)
                        .orderByAsc(SaleExchangeItem::getId)
        );
    }

    /**
     * 处理库存变更（审核通过时调用）
     * 换入仓库增加库存，换出仓库减少库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void processInventoryChange(Long exchangeId) {
        SaleExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }

        List<SaleExchangeItem> items = getItems(exchangeId);

        // 处理换入仓库（增加库存）
        items.stream()
                .filter(item -> item.getWarehouseType() != null && item.getWarehouseType() == 1)
                .forEach(item -> {
                    boolean ok = stockService.increaseStock(
                            item.getProductId(), exchange.getInWarehouseId(), item.getQuantity());
                    if (!ok) {
                        throw new RuntimeException("换入仓库库存增加失败，商品ID: " + item.getProductId());
                    }
                });

        // 处理换出仓库（减少库存）
        items.stream()
                .filter(item -> item.getWarehouseType() != null && item.getWarehouseType() == 2)
                .forEach(item -> {
                    boolean ok = stockService.decreaseStock(
                            item.getProductId(), exchange.getOutWarehouseId(), item.getQuantity());
                    if (!ok) {
                        throw new RuntimeException("换出仓库库存不足，商品ID: " + item.getProductId());
                    }
                });
    }

    /**
     * 回滚库存变更（取消/反审核时调用）
     * 换入仓库减少库存，换出仓库增加库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackInventoryChange(Long exchangeId) {
        SaleExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }

        List<SaleExchangeItem> items = getItems(exchangeId);

        // 回滚换入仓库（减少库存）
        items.stream()
                .filter(item -> item.getWarehouseType() != null && item.getWarehouseType() == 1)
                .forEach(item -> {
                    boolean ok = stockService.decreaseStock(
                            item.getProductId(), exchange.getInWarehouseId(), item.getQuantity());
                    if (!ok) {
                        throw new RuntimeException("换入仓库库存回滚失败，商品ID: " + item.getProductId());
                    }
                });

        // 回滚换出仓库（增加库存）
        items.stream()
                .filter(item -> item.getWarehouseType() != null && item.getWarehouseType() == 2)
                .forEach(item -> {
                    boolean ok = stockService.increaseStock(
                            item.getProductId(), exchange.getOutWarehouseId(), item.getQuantity());
                    if (!ok) {
                        throw new RuntimeException("换出仓库库存回滚失败，商品ID: " + item.getProductId());
                    }
                });
    }
}
