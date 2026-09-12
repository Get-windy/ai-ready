package cn.aiedge.erp.sale.salereturn.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnMapper;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
import cn.aiedge.erp.sale.service.integration.SalesAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SaleReturnServiceImpl extends ServiceImpl<SaleReturnMapper, SaleReturn> implements SaleReturnService {

    @Autowired
    private SaleReturnItemMapper saleReturnItemMapper;

    /** 库存唯一写入口：发布库存变动事件，由 WMS InventoryService 统一过账（ERP 侧不直写 erp_stock） */
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /** 业财集成：审核通过生成退货冲销凭证 */
    @Autowired
    private SalesAccountingService salesAccountingService;

    /** 取真实姓名（昵称优先）用于制单人/审核人快照 */
    @Autowired
    private cn.aiedge.base.mapper.SysUserMapper sysUserMapper;

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
        // 明细维度条件（商品/明细备注/商品分类）：按明细表 EXISTS 下推，避免空转参数
        if (productName != null && !productName.isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_item i "
                    + "WHERE i.return_id = erp_sale_return.id AND i.deleted = 0 "
                    + "AND i.product_name LIKE CONCAT('%', {0}, '%'))", productName);
        }
        if (itemRemark != null && !itemRemark.isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_item i "
                    + "WHERE i.return_id = erp_sale_return.id AND i.deleted = 0 "
                    + "AND i.item_remark LIKE CONCAT('%', {0}, '%'))", itemRemark);
        }
        if (categoryId != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_item i "
                    + "JOIN erp_product p ON p.id = i.product_id "
                    + "WHERE i.return_id = erp_sale_return.id AND i.deleted = 0 AND p.category_id = {0})", categoryId);
        }
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
        String prefix = "XSTHSQD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String lastNo = this.baseMapper.selectLastReturnNo(prefix);
        int seq = 1;
        if (lastNo != null && lastNo.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("历史单号后缀非数字，号段从1重新开始: " + lastNo);
            }
        }
        return prefix + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn createReturn(SaleReturn returnOrder) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 租户从 Sa-Token Session 解析（StpUtil.getExtra 需 sa-token-jwt 插件，此处不可用）；
        // 上下文不可用时置空，由多租户拦截器在 insert 时统一注入
        returnOrder.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
        // 单号来自后端号段 /next-no，前端原样传入则保留；未传时自动生成
        if (returnOrder.getReturnNo() == null || returnOrder.getReturnNo().isBlank()) {
            returnOrder.setReturnNo(generateReturnNo());
        }
        // 保存草稿(0) / 直接提交审批(1)；进入审核中时记录提交人与提交时间
        Integer status = returnOrder.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            status = 0;
        }
        returnOrder.setStatus(status);
        if (status == 1) {
            returnOrder.setSubmitBy(userId);
            returnOrder.setSubmitTime(LocalDateTime.now());
        }
        returnOrder.setCreateTime(LocalDateTime.now());
        returnOrder.setCreateBy(userId);
        if (returnOrder.getCreatorName() == null || returnOrder.getCreatorName().isBlank()) {
            returnOrder.setCreatorName(currentUserName());
        }
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

        // 返回落库后的完整单据（含明细与汇总金额），避免调用方拿到未回填的对象
        return this.getByIdWithItems(returnOrder.getId());
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
        // 草稿编辑后直接提交：记录提交人与提交时间
        if (returnOrder.getStatus() != null && returnOrder.getStatus() == 1) {
            returnOrder.setSubmitBy(StpUtil.getLoginIdAsLong());
            returnOrder.setSubmitTime(LocalDateTime.now());
        }
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

        return this.getByIdWithItems(returnOrder.getId());
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
        // 审核人/审核时间快照（列表"审核人/审核时间"列的真实来源）
        returnOrder.setAuditorId(StpUtil.getLoginIdAsLong());
        returnOrder.setAuditorName(currentUserName());
        returnOrder.setAuditTime(LocalDateTime.now());
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);

        // 审核通过即真实业务闭环：退货入库回写库存 + 生成冲销凭证
        // 幂等：bookkeepingTime 已存在说明已过账，跳过（重复审核已被状态机拦截）
        if (returnOrder.getBookkeepingTime() == null) {
            applyStockIncrease(returnOrder);
            String voucherNo = salesAccountingService.createSaleReturnVoucher(
                    returnOrder.getId(), returnOrder.getReturnNo(), returnOrder.getCustomerId(),
                    returnOrder.getCustomerName(), returnOrder.getTotalAmount(), calcCostAmount(returnOrder.getId()));
            if (voucherNo != null && !voucherNo.isEmpty()) {
                returnOrder.setBookkeepingTime(LocalDateTime.now());
                this.updateById(returnOrder);
            }
        }
        return returnOrder;
    }

    /**
     * 审核通过后回写库存（退货入库增加）。
     * <p>发布 {@link InventoryChangeEvent}，由 WMS {@code InventoryService} 统一过账——
     * 库存唯一写入口，ERP 侧不直写 erp_stock。</p>
     */
    private void applyStockIncrease(SaleReturn returnOrder) {
        if (returnOrder.getWarehouseId() == null) {
            throw new RuntimeException("退货申请单未指定入库仓库，无法回写库存");
        }
        List<SaleReturnItem> items = getItems(returnOrder.getId());
        boolean anyApplied = false;
        for (SaleReturnItem item : items) {
            BigDecimal qty = item.getReturnQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.INCREASE, item.getProductId(), returnOrder.getWarehouseId(), null,
                    null, qty, "SALE_RETURN_APPLY", returnOrder.getId(), returnOrder.getReturnNo(),
                    returnOrder.getHandlerId(), returnOrder.getHandlerName()));
            log.info("销售退货审核触发库存入账: returnNo=" + returnOrder.getReturnNo()
                    + ", productId=" + item.getProductId() + ", qty=" + qty);
            anyApplied = true;
        }
        if (!anyApplied) {
            throw new RuntimeException("退货申请单无有效退货明细，无法回写库存");
        }
    }

    /**
     * 取消已记账单据时冲回库存（与 {@link #applyStockIncrease} 对称）
     */
    private void applyStockDecrease(SaleReturn returnOrder) {
        if (returnOrder.getWarehouseId() == null) {
            return;
        }
        for (SaleReturnItem item : getItems(returnOrder.getId())) {
            BigDecimal qty = item.getReturnQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.DECREASE, item.getProductId(), returnOrder.getWarehouseId(), null,
                    null, qty, "SALE_RETURN_APPLY_CANCEL", returnOrder.getId(), returnOrder.getReturnNo(),
                    returnOrder.getHandlerId(), returnOrder.getHandlerName()));
            log.info("销售退货取消触发库存回冲: returnNo=" + returnOrder.getReturnNo()
                    + ", productId=" + item.getProductId() + ", qty=" + qty);
        }
    }

    /** 退货成本合计（明细参考成本金额之和），无成本数据时返回 null */
    private BigDecimal calcCostAmount(Long returnId) {
        BigDecimal cost = getItems(returnId).stream()
                .map(SaleReturnItem::getRefCostAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cost.signum() > 0 ? cost : null;
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
        if (returnOrder.getStatus() == 4) {
            throw BusinessException.badRequest("已取消的退货申请单不能重复取消");
        }

        // 已记账单据取消：冲回库存 + 生成反向凭证，保持账实一致
        if (returnOrder.getBookkeepingTime() != null) {
            applyStockDecrease(returnOrder);
            salesAccountingService.createSaleReturnReverseVoucher(
                    returnOrder.getId(), returnOrder.getReturnNo(), returnOrder.getCustomerId(),
                    returnOrder.getCustomerName(), returnOrder.getTotalAmount(), calcCostAmount(returnOrder.getId()));
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
            Boolean isGift, String auditTime, Long categoryId) {

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
        // 商品分类树过滤：按明细商品所属分类下推
        if (categoryId != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_product p "
                    + "WHERE p.id = i.product_id AND p.category_id = {0})", categoryId);
        }

        // 排序
        Page<Map<String, Object>> raw = this.baseMapper.selectPageDetail(page, wrapper);
        // MyBatis 对 Map 结果不做驼峰转换（map-underscore-to-camel-case 仅作用于实体映射），
        // 这里统一转为驼峰键，保证前端按明细列（productName/returnQuantity…）可直接取值
        Page<Map<String, Object>> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(SaleReturnServiceImpl::toCamelKeys).toList());
        return result;
    }

    /** 当前登录用户显示名（昵称优先，回退用户名/登录ID），用于制单人-审核人快照 */
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

    /** 将 Map 结果的下划线键转为驼峰键（只处理含下划线的键） */
    private static Map<String, Object> toCamelKeys(Map<String, Object> row) {
        Map<String, Object> converted = new java.util.LinkedHashMap<>(row.size());
        row.forEach((key, value) -> converted.put(toCamel(key), value));
        return converted;
    }

    private static String toCamel(String name) {
        if (name == null || name.indexOf('_') < 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name.length());
        boolean upperNext = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upperNext = true;
                continue;
            }
            sb.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        return sb.toString();
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
