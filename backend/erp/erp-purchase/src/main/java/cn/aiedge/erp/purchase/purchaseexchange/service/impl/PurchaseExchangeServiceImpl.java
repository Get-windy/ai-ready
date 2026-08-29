package cn.aiedge.erp.purchase.purchaseexchange.service.impl;

import cn.aiedge.erp.purchase.purchaseexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.ExchangeApprovalRecordMapper;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeItemMapper;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeMapper;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import cn.dev33.satoken.stp.StpUtil;
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
import java.util.stream.Collectors;

@Service
public class PurchaseExchangeServiceImpl extends ServiceImpl<PurchaseExchangeMapper, PurchaseExchange> implements PurchaseExchangeService {

    @Autowired
    private PurchaseExchangeItemMapper purchaseExchangeItemMapper;

    @Autowired
    private ExchangeApprovalRecordMapper exchangeApprovalRecordMapper;

    @Override
    public Page<PurchaseExchange> pageList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                           String startDate, String endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();
        applyBaseFilters(wrapper, keyword, supplierId, status, exchangeType, startDate, endDate);
        wrapper.orderByDesc(PurchaseExchange::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<PurchaseExchange> pageListExtended(Map<String, Object> params, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();

        String keyword = getStr(params, "keyword");
        Long supplierId = getLong(params, "supplierId");
        Integer status = getInt(params, "status");
        Integer exchangeType = getInt(params, "exchangeType");
        String startDate = getStr(params, "startDate");
        String endDate = getStr(params, "endDate");
        String exchangeNo = getStr(params, "exchangeNo");
        String supplierName = getStr(params, "supplierName");
        String handlerName = getStr(params, "handlerName");
        String deptName = getStr(params, "deptName");
        String creatorName = getStr(params, "creatorName");
        String bookkeeperName = getStr(params, "bookkeeperName");
        String inWarehouseName = getStr(params, "inWarehouseName");
        String outWarehouseName = getStr(params, "outWarehouseName");
        String remark = getStr(params, "remark");
        String settleStatus = getStr(params, "settleStatus");

        Long inWarehouseId = getLong(params, "inWarehouseId");
        Long outWarehouseId = getLong(params, "outWarehouseId");
        Long handlerId = getLong(params, "handlerId");

        // 组合关键词：单号/供应商/经手人等
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseExchange::getExchangeNo, keyword)
                    .or().like(PurchaseExchange::getSupplierName, keyword)
                    .or().like(PurchaseExchange::getHandlerName, keyword));
        }
        wrapper.like(exchangeNo != null && !exchangeNo.isEmpty(), PurchaseExchange::getExchangeNo, exchangeNo)
                .like(supplierName != null && !supplierName.isEmpty(), PurchaseExchange::getSupplierName, supplierName)
                .like(handlerName != null && !handlerName.isEmpty(), PurchaseExchange::getHandlerName, handlerName)
                .like(deptName != null && !deptName.isEmpty(), PurchaseExchange::getDeptName, deptName)
                .like(creatorName != null && !creatorName.isEmpty(), PurchaseExchange::getCreatedByName, creatorName)
                .like(bookkeeperName != null && !bookkeeperName.isEmpty(), PurchaseExchange::getBookkeeperName, bookkeeperName)
                .like(inWarehouseName != null && !inWarehouseName.isEmpty(), PurchaseExchange::getInWarehouseName, inWarehouseName)
                .like(outWarehouseName != null && !outWarehouseName.isEmpty(), PurchaseExchange::getOutWarehouseName, outWarehouseName)
                .like(remark != null && !remark.isEmpty(), PurchaseExchange::getRemark, remark)
                .eq(settleStatus != null && !settleStatus.isEmpty(), PurchaseExchange::getSettleStatus, settleStatus)
                .eq(supplierId != null, PurchaseExchange::getSupplierId, supplierId)
                .eq(inWarehouseId != null, PurchaseExchange::getInWarehouseId, inWarehouseId)
                .eq(outWarehouseId != null, PurchaseExchange::getOutWarehouseId, outWarehouseId)
                .eq(handlerId != null, PurchaseExchange::getHandlerId, handlerId)
                .eq(status != null, PurchaseExchange::getStatus, status)
                .eq(exchangeType != null, PurchaseExchange::getExchangeType, exchangeType);

        applyDateRange(wrapper, startDate, endDate);
        wrapper.orderByDesc(PurchaseExchange::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    private void applyBaseFilters(LambdaQueryWrapper<PurchaseExchange> wrapper, String keyword, Long supplierId,
                                  Integer status, Integer exchangeType, String startDate, String endDate) {
        wrapper.like(keyword != null, PurchaseExchange::getExchangeNo, keyword)
                .eq(supplierId != null, PurchaseExchange::getSupplierId, supplierId)
                .eq(status != null, PurchaseExchange::getStatus, status)
                .eq(exchangeType != null, PurchaseExchange::getExchangeType, exchangeType);
        applyDateRange(wrapper, startDate, endDate);
    }

    private void applyDateRange(LambdaQueryWrapper<PurchaseExchange> wrapper, String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        wrapper.ge(start != null, PurchaseExchange::getExchangeDate, start)
                .le(end != null, PurchaseExchange::getExchangeDate, end);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String getStr(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null ? null : v.toString();
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null || v.toString().isEmpty()) return null;
        try { return Long.valueOf(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null || v.toString().isEmpty()) return null;
        try { return Integer.valueOf(v.toString()); } catch (Exception e) { return null; }
    }

    @Override
    public List<PurchaseExchange> exportList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                             String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();
        applyBaseFilters(wrapper, keyword, supplierId, status, exchangeType, startDate, endDate);
        wrapper.orderByDesc(PurchaseExchange::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public String generateExchangeNo() {
        String prefix = "CGHD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseExchange::getExchangeNo, prefix + "-" + dateStr)
                .eq(PurchaseExchange::getDeleted, 0)
                .orderByDesc(PurchaseExchange::getExchangeNo)
                .last("LIMIT 1");
        PurchaseExchange last = getOne(wrapper);
        int seq = 1;
        if (last != null && last.getExchangeNo() != null) {
            String lastNo = last.getExchangeNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange createExchange(PurchaseExchange exchange, List<PurchaseExchangeItem> items) {
        Long loginId = currentLoginId();
        String loginName = currentLoginName();

        exchange.setTenantId(1L);
        exchange.setExchangeNo(generateExchangeNo());
        exchange.setStatus(0);
        exchange.setCreatedBy(loginId);
        exchange.setCreatedByName(loginName);
        exchange.setCreateTime(LocalDateTime.now());
        exchange.setUpdateTime(LocalDateTime.now());
        this.save(exchange);

        persistItems(exchange, items);

        calculateTotals(exchange.getId());
        return this.getById(exchange.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange updateExchange(Long id, PurchaseExchange exchange, List<PurchaseExchangeItem> items) {
        PurchaseExchange existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (existing.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的换货单可以修改");
        }

        exchange.setId(id);
        exchange.setTenantId(existing.getTenantId() != null ? existing.getTenantId() : 1L);
        exchange.setCreatedBy(existing.getCreatedBy());
        exchange.setCreatedByName(existing.getCreatedByName());
        exchange.setExchangeNo(existing.getExchangeNo());
        exchange.setCreateTime(existing.getCreateTime());
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        // 删除并重建明细
        purchaseExchangeItemMapper.delete(
                new LambdaQueryWrapper<PurchaseExchangeItem>()
                        .eq(PurchaseExchangeItem::getExchangeId, id)
        );
        persistItems(exchange, items);

        calculateTotals(id);
        return this.getById(id);
    }

    private void persistItems(PurchaseExchange exchange, List<PurchaseExchangeItem> items) {
        if (items == null || items.isEmpty()) return;
        int line = 1;
        for (PurchaseExchangeItem item : items) {
            item.setId(null);
            item.setExchangeId(exchange.getId());
            item.setTenantId(exchange.getTenantId() != null ? exchange.getTenantId() : 1L);
            item.setWarehouseType(item.getWarehouseType() == null ? 1 : item.getWarehouseType());
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            purchaseExchangeItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange submitForApproval(Long id) {
        PurchaseExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的换货单可以提交审批");
        }

        exchange.setStatus(1);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        saveApprovalRecord(id, "submit", "提交审批", null);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange approve(Long id, Long approverId, String approvedByName, String remark) {
        PurchaseExchange exchange = this.getById(id);
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

        saveApprovalRecord(id, "approve", "审批通过", remark);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange reject(Long id, String remark) {
        PurchaseExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的换货单可以拒绝");
        }

        exchange.setStatus(5);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        saveApprovalRecord(id, "reject", "审批拒绝", remark);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange cancel(Long id, String reason) {
        PurchaseExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        if (exchange.getStatus() == 6) {
            throw new RuntimeException("换货单已取消");
        }

        exchange.setStatus(6);
        exchange.setRemark(reason);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        saveApprovalRecord(id, "cancel", "取消换货单", reason);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange complete(Long id) {
        PurchaseExchange exchange = this.getById(id);
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

        saveApprovalRecord(id, "complete", "完成换货", null);
        return exchange;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void print(Long id) {
        PurchaseExchange exchange = this.getById(id);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }
        int count = exchange.getPrintCount() == null ? 0 : exchange.getPrintCount();
        exchange.setPrintCount(count + 1);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchPrint(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            print(id);
        }
    }

    @Override
    public List<PurchaseExchangeItem> getItems(Long exchangeId) {
        return purchaseExchangeItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseExchangeItem>()
                        .eq(PurchaseExchangeItem::getExchangeId, exchangeId)
                        .orderByAsc(PurchaseExchangeItem::getId)
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
        PurchaseExchange exchange = this.getById(exchangeId);
        if (exchange == null) {
            throw new RuntimeException("换货单不存在");
        }

        List<PurchaseExchangeItem> items = getItems(exchangeId);
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

    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long exchangeId) {
        List<PurchaseExchangeItem> items = getItems(exchangeId);

        List<PurchaseExchangeItem> inItems = items.stream()
                .filter(i -> i.getWarehouseType() == null || i.getWarehouseType() == 1)
                .collect(Collectors.toList());
        List<PurchaseExchangeItem> outItems = items.stream()
                .filter(i -> i.getWarehouseType() != null && i.getWarehouseType() == 2)
                .collect(Collectors.toList());

        BigDecimal inQty = inItems.stream().map(this::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outQty = outItems.stream().map(this::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productAmount = items.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = items.stream().map(this::discountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = items.stream().map(this::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVolume = items.stream().map(this::volume).reduce(BigDecimal.ZERO, BigDecimal::add);

        PurchaseExchange exchange = this.getById(exchangeId);
        if (exchange == null) return;
        exchange.setInQuantityTotal(inQty);
        exchange.setOutQuantityTotal(outQty);
        exchange.setProductAmount(productAmount);
        exchange.setDiscountAmount(discountAmount);
        exchange.setTotalAmount(discountAmount);
        exchange.setTotalWeight(totalWeight);
        exchange.setTotalVolume(totalVolume);
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);
    }

    private BigDecimal qty(PurchaseExchangeItem i) {
        return i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO;
    }

    private BigDecimal amount(PurchaseExchangeItem i) {
        return i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO;
    }

    private BigDecimal discountAmount(PurchaseExchangeItem i) {
        return i.getDiscountAmount() != null ? i.getDiscountAmount() : BigDecimal.ZERO;
    }

    private BigDecimal weight(PurchaseExchangeItem i) {
        return i.getWeight() != null ? i.getWeight() : BigDecimal.ZERO;
    }

    private BigDecimal volume(PurchaseExchangeItem i) {
        return i.getVolume() != null ? i.getVolume() : BigDecimal.ZERO;
    }

    private void saveApprovalRecord(Long exchangeId, String action, String actionName, String remark) {
        ExchangeApprovalRecord record = new ExchangeApprovalRecord();
        record.setExchangeId(exchangeId);
        record.setAction(action);
        record.setActionName(actionName);
        record.setOperatorId(currentLoginId());
        record.setOperatorName(currentLoginName());
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        exchangeApprovalRecordMapper.insert(record);
    }

    /** 无会话时返回 null，避免匿名调用抛异常 */
    private Long currentLoginId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentLoginName() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "系统";
        }
    }
}
