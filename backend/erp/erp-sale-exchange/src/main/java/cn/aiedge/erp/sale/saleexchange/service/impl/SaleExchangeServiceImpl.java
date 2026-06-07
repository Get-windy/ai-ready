package cn.aiedge.erp.sale.saleexchange.service.impl;

import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.mapper.ExchangeApprovalRecordMapper;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeItemMapper;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeMapper;
import cn.aiedge.erp.sale.saleexchange.service.SaleExchangeService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SaleExchangeServiceImpl extends ServiceImpl<SaleExchangeMapper, SaleExchange> implements SaleExchangeService {

    @Autowired
    private SaleExchangeItemMapper saleExchangeItemMapper;

    @Autowired
    private ExchangeApprovalRecordMapper exchangeApprovalRecordMapper;

    @Override
    public Page<SaleExchange> pageList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                       String startDate, String endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleExchange::getExchangeNo, keyword)
                .eq(customerId != null, SaleExchange::getCustomerId, customerId)
                .eq(status != null, SaleExchange::getStatus, status)
                .eq(exchangeType != null, SaleExchange::getExchangeType, exchangeType)
                .ge(startDate != null, SaleExchange::getCreateTime, startDate)
                .le(endDate != null, SaleExchange::getCreateTime, endDate)
                .orderByDesc(SaleExchange::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleExchange> exportList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<SaleExchange> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleExchange::getExchangeNo, keyword)
                .eq(customerId != null, SaleExchange::getCustomerId, customerId)
                .eq(status != null, SaleExchange::getStatus, status)
                .eq(exchangeType != null, SaleExchange::getExchangeType, exchangeType)
                .ge(startDate != null, SaleExchange::getCreateTime, startDate)
                .le(endDate != null, SaleExchange::getCreateTime, endDate)
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
        exchange.setCreatedBy(loginId);
        exchange.setCreatedByName(loginName);
        exchange.setCreateTime(LocalDateTime.now());
        this.save(exchange);

        if (items != null && !items.isEmpty()) {
            for (SaleExchangeItem item : items) {
                item.setExchangeId(exchange.getId());
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

        BigDecimal totalAmount = items.stream()
                .map(i -> i.getExchangeQuantity().multiply(i.getExchangePrice() != null ? i.getExchangePrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SaleExchange exchange = this.getById(exchangeId);
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
