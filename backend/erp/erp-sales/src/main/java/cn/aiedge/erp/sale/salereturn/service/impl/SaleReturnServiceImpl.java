package cn.aiedge.erp.sale.salereturn.service.impl;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnMapper;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class SaleReturnServiceImpl extends ServiceImpl<SaleReturnMapper, SaleReturn> implements SaleReturnService {

    @Autowired
    private SaleReturnItemMapper saleReturnItemMapper;

    @Override
    public SaleReturn getByReturnNo(String returnNo) {
        return this.lambdaQuery()
                .eq(SaleReturn::getReturnNo, returnNo)
                .one();
    }

    @Override
    public SaleReturn getByIdWithItems(Long id) {
        SaleReturn returnOrder = this.getById(id);
        if (returnOrder != null) {
            List<SaleReturnItem> items = getItems(id);
            returnOrder.setItems(items);
        }
        return returnOrder;
    }

    @Override
    public Page<SaleReturn> pageList(int pageNum, int pageSize, String keyword, String customerName,
                                     String handlerName, String deptName, String warehouseName,
                                     String productName, String itemRemark, Integer status,
                                     String generateType, String settleStatus, Integer printCount,
                                     String startDate, String endDate, Long categoryId,
                                     String creatorName, String auditorName, String submitBy,
                                     String productLineAttr, String remark, String summary,
                                     String deliveryMethod, Integer extNum1, Integer extNum2,
                                     String extText1, String extText2, String extText3,
                                     String contactName, String contactPhone, String contactAddress,
                                     String auditTime, String salesType) {
        // 日期字符串 → LocalDateTime（对标 SaleExchange 模块的正确实现）
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            startDateTime = parseLocalDateToStartOfDay(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            endDateTime = parseLocalDateToEndOfDay(endDate);
        }
        LocalDateTime auditDateTime = null;
        if (auditTime != null && !auditTime.isEmpty()) {
            auditDateTime = parseLocalDateToStartOfDay(auditTime);
        }

        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturn::getReturnNo, keyword)
                .like(customerName != null, SaleReturn::getCustomerName, customerName)
                .like(handlerName != null, SaleReturn::getHandlerName, handlerName)
                .like(deptName != null, SaleReturn::getDeptName, deptName)
                .like(warehouseName != null, SaleReturn::getWarehouseName, warehouseName)
                .eq(status != null, SaleReturn::getStatus, status)
                .eq(generateType != null, SaleReturn::getGenerateType, generateType)
                .eq(settleStatus != null, SaleReturn::getSettleStatus, settleStatus)
                .ge(printCount != null, SaleReturn::getPrintCount, printCount)
                .ge(startDateTime != null, SaleReturn::getOrderDate, startDateTime)
                .le(endDateTime != null, SaleReturn::getOrderDate, endDateTime)
                .like(creatorName != null, SaleReturn::getCreatorName, creatorName)
                .like(auditorName != null, SaleReturn::getAuditorName, auditorName)
                .eq(submitBy != null, SaleReturn::getSubmitBy, submitBy)
                .eq(productLineAttr != null, SaleReturn::getProductLineAttr, productLineAttr)
                .like(remark != null, SaleReturn::getRemark, remark)
                .like(summary != null, SaleReturn::getSummary, summary)
                .eq(deliveryMethod != null, SaleReturn::getDeliveryMethod, deliveryMethod)
                .eq(extNum1 != null, SaleReturn::getExtNum1, extNum1)
                .eq(extNum2 != null, SaleReturn::getExtNum2, extNum2)
                .like(extText1 != null, SaleReturn::getExtText1, extText1)
                .like(extText2 != null, SaleReturn::getExtText2, extText2)
                .like(extText3 != null, SaleReturn::getExtText3, extText3)
                .like(contactName != null, SaleReturn::getContactName, contactName)
                .like(contactPhone != null, SaleReturn::getContactPhone, contactPhone)
                .like(contactAddress != null, SaleReturn::getContactAddress, contactAddress)
                .ge(auditDateTime != null, SaleReturn::getAuditTime, auditDateTime)
                .eq(salesType != null, SaleReturn::getSalesType, salesType)
                .orderByDesc(SaleReturn::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleReturn> exportList(String keyword, String customerName, Integer status) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturn::getReturnNo, keyword)
                .like(customerName != null, SaleReturn::getCustomerName, customerName)
                .eq(status != null, SaleReturn::getStatus, status)
                .orderByDesc(SaleReturn::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Long> ids, String note) {
        int count = 0;
        for (Long id : ids) {
            try {
                approve(id, note);
                count++;
            } catch (RuntimeException e) {
                log.warn("批量审批跳过ID " + id + ": " + e.getMessage());
            }
        }
        return count;
    }

    @Override
    public List<SaleReturn> listByCustomerId(Long customerId) {
        return this.lambdaQuery()
                .eq(SaleReturn::getCustomerId, customerId)
                .orderByDesc(SaleReturn::getCreateTime)
                .list();
    }

    @Override
    public String generateReturnNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "XSTHSQD-" + dateStr + "-" + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn createReturn(SaleReturn returnOrder) {
        Long userId = StpUtil.getLoginIdAsLong();

        returnOrder.setTenantId(StpUtil.getExtra("tenantId") != null ?
                Long.parseLong(StpUtil.getExtra("tenantId").toString()) : 0L);
        returnOrder.setReturnNo(generateReturnNo());
        returnOrder.setStatus(0);
        returnOrder.setCreateTime(LocalDateTime.now());
        returnOrder.setCreateBy(userId);
        this.save(returnOrder);

        // 保存明细
        if (returnOrder.getItems() != null && !returnOrder.getItems().isEmpty()) {
            for (int i = 0; i < returnOrder.getItems().size(); i++) {
                SaleReturnItem item = returnOrder.getItems().get(i);
                item.setTenantId(returnOrder.getTenantId());
                item.setReturnId(returnOrder.getId());
                item.setLineNo(i + 1);
                item.setCreateTime(LocalDateTime.now());
                item.setCreateBy(userId);
                saleReturnItemMapper.insert(item);
            }
        }

        calculateTotals(returnOrder.getId());

        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn updateReturn(SaleReturn returnOrder) {
        SaleReturn existing = this.getById(returnOrder.getId());
        if (existing == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (existing.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货申请单可以编辑");
        }

        returnOrder.setUpdateTime(LocalDateTime.now());
        returnOrder.setUpdateBy(StpUtil.getLoginIdAsLong());
        this.updateById(returnOrder);

        // 更新明细：先删除旧的，再插入新的
        saleReturnItemMapper.delete(
                new LambdaQueryWrapper<SaleReturnItem>()
                        .eq(SaleReturnItem::getReturnId, returnOrder.getId())
        );

        if (returnOrder.getItems() != null && !returnOrder.getItems().isEmpty()) {
            for (int i = 0; i < returnOrder.getItems().size(); i++) {
                SaleReturnItem item = returnOrder.getItems().get(i);
                item.setId(null);
                item.setTenantId(existing.getTenantId());
                item.setReturnId(returnOrder.getId());
                item.setLineNo(i + 1);
                item.setCreateTime(LocalDateTime.now());
                item.setCreateBy(StpUtil.getLoginIdAsLong());
                saleReturnItemMapper.insert(item);
            }
        }

        calculateTotals(returnOrder.getId());

        return this.getById(returnOrder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn submitForApproval(Long returnId) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货申请单可以提交审批");
        }

        returnOrder.setStatus(1);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn approve(Long returnId, String note) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货申请单可以审批");
        }

        returnOrder.setStatus(2);
        returnOrder.setApprovedBy(StpUtil.getLoginIdAsLong());
        returnOrder.setApprovedTime(LocalDateTime.now());
        returnOrder.setApprovedNote(note);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn reject(Long returnId, String reason) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货申请单可以拒绝");
        }

        returnOrder.setStatus(0);
        returnOrder.setApprovedNote(reason);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn complete(Long returnId) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() != 2) {
            throw new RuntimeException("只有已审批状态的退货申请单可以完成");
        }

        returnOrder.setStatus(3);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn cancel(Long returnId, String reason) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() == 3) {
            throw new RuntimeException("已完成的退货申请单不能取消");
        }

        returnOrder.setStatus(4);
        returnOrder.setRemark(reason);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    public List<SaleReturnItem> getItems(Long returnId) {
        return saleReturnItemMapper.selectList(
                new LambdaQueryWrapper<SaleReturnItem>()
                        .eq(SaleReturnItem::getReturnId, returnId)
                        .orderByAsc(SaleReturnItem::getLineNo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem addItem(Long returnId, SaleReturnItem item) {
        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货申请单不存在");
        }
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货申请单可以添加明细");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        item.setTenantId(returnOrder.getTenantId());
        item.setReturnId(returnId);
        item.setCreateTime(LocalDateTime.now());
        item.setCreateBy(userId);
        saleReturnItemMapper.insert(item);

        calculateTotals(returnId);

        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem updateItem(Long itemId, SaleReturnItem item) {
        SaleReturnItem existing = saleReturnItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturn returnOrder = this.getById(existing.getReturnId());
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货申请单可以修改明细");
        }

        existing.setProductId(item.getProductId());
        existing.setProductName(item.getProductName());
        existing.setProductCode(item.getProductCode());
        existing.setReturnQuantity(item.getReturnQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        existing.setReason(item.getReason());
        existing.setRemark(item.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        saleReturnItemMapper.updateById(existing);

        calculateTotals(existing.getReturnId());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        SaleReturnItem item = saleReturnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturn returnOrder = this.getById(item.getReturnId());
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货申请单可以删除明细");
        }

        saleReturnItemMapper.deleteById(itemId);

        calculateTotals(item.getReturnId());
    }

    @Override
    public Page<Map<String, Object>> pageDetail(
            String keyword, Long customerId, Long warehouseId, Integer status,
            String returnNo, String productName, Long handlerId, String deptName,
            String settleStatus, String salesType, String productLineAttr,
            String itemRemark, String startDate, String endDate,
            int pageNum, int pageSize,
            String creatorName, String auditorName, String remark,
            Boolean isGift, String auditTime) {

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SaleReturn> wrapper = new QueryWrapper<>();

        // 日期字符串 → LocalDateTime（与 pageList 保持一致，避免 PG 类型比较错误）
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            startDateTime = parseLocalDateToStartOfDay(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            endDateTime = parseLocalDateToEndOfDay(endDate);
        }
        LocalDateTime auditDateTime = null;
        if (auditTime != null && !auditTime.isEmpty()) {
            auditDateTime = parseLocalDateToStartOfDay(auditTime);
        }

        // 主表条件
        wrapper.eq("r.deleted", 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("r.return_no", keyword);
        }
        if (customerId != null) {
            wrapper.eq("r.customer_id", customerId);
        }
        if (warehouseId != null) {
            wrapper.eq("r.warehouse_id", warehouseId);
        }
        if (status != null) {
            wrapper.eq("r.status", status);
        }
        if (returnNo != null && !returnNo.isEmpty()) {
            wrapper.like("r.return_no", returnNo);
        }
        if (handlerId != null) {
            wrapper.eq("r.handler_id", handlerId);
        }
        if (deptName != null && !deptName.isEmpty()) {
            wrapper.like("r.dept_name", deptName);
        }
        if (settleStatus != null && !settleStatus.isEmpty()) {
            wrapper.eq("r.settle_status", settleStatus);
        }
        if (salesType != null && !salesType.isEmpty()) {
            wrapper.eq("r.sales_type", salesType);
        }
        if (startDateTime != null) {
            wrapper.ge("r.order_date", startDateTime);
        }
        if (endDateTime != null) {
            wrapper.le("r.order_date", endDateTime);
        }
        if (creatorName != null && !creatorName.isEmpty()) {
            wrapper.like("r.creator_name", creatorName);
        }
        if (auditorName != null && !auditorName.isEmpty()) {
            wrapper.like("r.auditor_name", auditorName);
        }
        if (remark != null && !remark.isEmpty()) {
            wrapper.like("r.remark", remark);
        }
        if (auditDateTime != null) {
            wrapper.ge("r.audit_time", auditDateTime);
        }

        // 明细表条件
        if (productName != null && !productName.isEmpty()) {
            wrapper.like("i.product_name", productName);
        }
        if (productLineAttr != null && !productLineAttr.isEmpty()) {
            wrapper.eq("i.product_line_attr", productLineAttr);
        }
        if (itemRemark != null && !itemRemark.isEmpty()) {
            wrapper.like("i.item_remark", itemRemark);
        }
        if (isGift != null) {
            wrapper.eq("i.is_gift", isGift);
        }

        // 排序
        return this.baseMapper.selectPageDetail(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnId) {
        List<SaleReturnItem> items = getItems(returnId);

        BigDecimal totalQuantity = items.stream()
                .map(SaleReturnItem::getReturnQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = i.getReturnQuantity() != null ? i.getReturnQuantity() : BigDecimal.ZERO;
                    BigDecimal price = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SaleReturn returnOrder = this.getById(returnId);
        if (returnOrder != null) {
            returnOrder.setTotalQuantity(totalQuantity);
            returnOrder.setTotalAmount(totalAmount);
            returnOrder.setUpdateTime(LocalDateTime.now());
            this.updateById(returnOrder);
        }
    }

    private LocalDateTime parseLocalDateToStartOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected format: yyyy-MM-dd");
        }
    }

    private LocalDateTime parseLocalDateToEndOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atTime(23, 59, 59);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected format: yyyy-MM-dd");
        }
    }
}
