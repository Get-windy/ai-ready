package cn.aiedge.erp.purchase.purchaseexchange.service.impl;

import cn.aiedge.erp.purchase.purchaseexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.ExchangeApprovalRecordMapper;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeItemMapper;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeMapper;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
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
public class PurchaseExchangeServiceImpl extends ServiceImpl<PurchaseExchangeMapper, PurchaseExchange> implements PurchaseExchangeService {

    @Autowired
    private PurchaseExchangeItemMapper purchaseExchangeItemMapper;

    @Autowired
    private ExchangeApprovalRecordMapper exchangeApprovalRecordMapper;

    @Override
    public Page<PurchaseExchange> pageList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                           String startDate, String endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();

        // 转换日期字符串为 LocalDateTime，避免 PostgreSQL 类型不匹配
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            } catch (Exception e) {
                // 如果解析失败，尝试其他格式
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

        wrapper.like(keyword != null, PurchaseExchange::getExchangeNo, keyword)
                .eq(supplierId != null, PurchaseExchange::getSupplierId, supplierId)
                .eq(status != null, PurchaseExchange::getStatus, status)
                .eq(exchangeType != null, PurchaseExchange::getExchangeType, exchangeType)
                .ge(startDateTime != null, PurchaseExchange::getCreateTime, startDateTime)
                .le(endDateTime != null, PurchaseExchange::getCreateTime, endDateTime)
                .orderByDesc(PurchaseExchange::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<PurchaseExchange> exportList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                             String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseExchange> wrapper = new LambdaQueryWrapper<>();

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

        wrapper.like(keyword != null, PurchaseExchange::getExchangeNo, keyword)
                .eq(supplierId != null, PurchaseExchange::getSupplierId, supplierId)
                .eq(status != null, PurchaseExchange::getStatus, status)
                .eq(exchangeType != null, PurchaseExchange::getExchangeType, exchangeType)
                .ge(startDateTime != null, PurchaseExchange::getCreateTime, startDateTime)
                .le(endDateTime != null, PurchaseExchange::getCreateTime, endDateTime)
                .orderByDesc(PurchaseExchange::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public String generateExchangeNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "EX" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseExchange createExchange(PurchaseExchange exchange, List<PurchaseExchangeItem> items) {
        Long loginId = StpUtil.getLoginIdAsLong();
        String loginName = StpUtil.getLoginIdAsString();

        exchange.setTenantId(loginId);
        exchange.setExchangeNo(generateExchangeNo());
        exchange.setStatus(0);
        exchange.setCreatedBy(loginId);
        exchange.setCreatedByName(loginName);
        exchange.setCreateTime(LocalDateTime.now());
        this.save(exchange);

        if (items != null && !items.isEmpty()) {
            for (PurchaseExchangeItem item : items) {
                item.setExchangeId(exchange.getId());
                purchaseExchangeItemMapper.insert(item);
            }
        }

        calculateTotals(exchange.getId());

        return exchange;
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
        exchange.setUpdateTime(LocalDateTime.now());
        this.updateById(exchange);

        if (items != null) {
            // 删除原有明细
            purchaseExchangeItemMapper.delete(
                    new LambdaQueryWrapper<PurchaseExchangeItem>()
                            .eq(PurchaseExchangeItem::getExchangeId, id)
            );
            // 插入新明细
            for (PurchaseExchangeItem item : items) {
                item.setId(null);
                item.setExchangeId(id);
                purchaseExchangeItemMapper.insert(item);
            }
        }

        calculateTotals(id);

        return this.getById(id);
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

        // 记录审批记录
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

        // 记录审批记录
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

        // 记录审批记录
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

        // 记录审批记录
        saveApprovalRecord(id, "complete", "完成换货", null);

        return exchange;
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
        List<PurchaseExchangeItem> items = getItems(exchangeId);

        BigDecimal totalAmount = items.stream()
                .map(i -> i.getExchangeQuantity().multiply(i.getExchangePrice() != null ? i.getExchangePrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PurchaseExchange exchange = this.getById(exchangeId);
        exchange.setTotalAmount(totalAmount);
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
}
