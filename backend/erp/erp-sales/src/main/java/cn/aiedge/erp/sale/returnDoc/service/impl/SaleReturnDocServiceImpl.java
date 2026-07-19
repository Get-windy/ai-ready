package cn.aiedge.erp.sale.returnDoc.service.impl;

import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDocItem;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocItemMapper;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocMapper;
import cn.aiedge.erp.sale.returnDoc.service.SaleReturnDocService;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
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

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SaleReturnDocServiceImpl extends ServiceImpl<SaleReturnDocMapper, SaleReturnDoc> implements SaleReturnDocService, ISaleReturnDocService {

    @Autowired
    private SaleReturnDocItemMapper saleReturnDocItemMapper;

    @Autowired
    private SaleReturnService saleReturnService;

    @Override
    public SaleReturnDoc getByReturnDocNo(String returnDocNo) {
        return this.lambdaQuery()
                .eq(SaleReturnDoc::getReturnDocNo, returnDocNo)
                .one();
    }

    @Override
    public SaleReturnDoc getByIdWithItems(Long id) {
        SaleReturnDoc returnDoc = this.getById(id);
        if (returnDoc != null) {
            List<SaleReturnDocItem> items = getItems(id);
            returnDoc.setItems(items);
        }
        return returnDoc;
    }

    @Override
    public Page<SaleReturnDoc> pageList(int pageNum, int pageSize, String keyword, String customerName,
                                         String handlerName, String deptName, String warehouseName,
                                         String productName, String itemRemark, Integer status,
                                         String generateType, String settleStatus, Integer printCount,
                                         String startDate, String endDate, Long categoryId,
                                         String creatorName, String auditorName, String submitBy,
                                         String productLineAttr, String remark, String summary,
                                         String deliveryMethod, Integer extNum1, Integer extNum2,
                                         String extText1, String extText2, String extText3,
                                         String contactName, String contactPhone, String contactAddress,
                                         String auditTime, String salesType, String receiverName,
                                         String logisticsCompany, String waybillNo, String region,
                                         Boolean showRedFlush) {
        LambdaQueryWrapper<SaleReturnDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturnDoc::getReturnDocNo, keyword)
                .like(customerName != null, SaleReturnDoc::getCustomerName, customerName)
                .like(handlerName != null, SaleReturnDoc::getHandlerName, handlerName)
                .like(deptName != null, SaleReturnDoc::getDeptName, deptName)
                .like(warehouseName != null, SaleReturnDoc::getWarehouseName, warehouseName)
                .eq(status != null, SaleReturnDoc::getStatus, status)
                .eq(generateType != null, SaleReturnDoc::getGenerateType, generateType)
                .eq(settleStatus != null, SaleReturnDoc::getSettleStatus, settleStatus)
                .ge(printCount != null, SaleReturnDoc::getPrintCount, printCount)
                .ge(startDate != null && !startDate.isEmpty(), SaleReturnDoc::getOrderDate, parseLocalDate(startDate))
                .le(endDate != null && !endDate.isEmpty(), SaleReturnDoc::getOrderDate, parseLocalDate(endDate))
                .like(creatorName != null, SaleReturnDoc::getCreatorName, creatorName)
                .like(auditorName != null, SaleReturnDoc::getAuditorName, auditorName)
                .eq(submitBy != null, SaleReturnDoc::getSubmitBy, submitBy)
                .eq(productLineAttr != null, SaleReturnDoc::getProductLineAttr, productLineAttr)
                .like(remark != null, SaleReturnDoc::getRemark, remark)
                .like(summary != null, SaleReturnDoc::getSummary, summary)
                .eq(deliveryMethod != null, SaleReturnDoc::getDeliveryMethod, deliveryMethod)
                .eq(extNum1 != null, SaleReturnDoc::getExtNum1, extNum1)
                .eq(extNum2 != null, SaleReturnDoc::getExtNum2, extNum2)
                .like(extText1 != null, SaleReturnDoc::getExtText1, extText1)
                .like(extText2 != null, SaleReturnDoc::getExtText2, extText2)
                .like(extText3 != null, SaleReturnDoc::getExtText3, extText3)
                .like(contactName != null, SaleReturnDoc::getContactName, contactName)
                .like(contactPhone != null, SaleReturnDoc::getContactPhone, contactPhone)
                .like(contactAddress != null, SaleReturnDoc::getContactAddress, contactAddress)
                .ge(auditTime != null && !auditTime.isEmpty(), SaleReturnDoc::getAuditTime, parseLocalDateTime(auditTime))
                .eq(salesType != null, SaleReturnDoc::getSalesType, salesType)
                .like(receiverName != null, SaleReturnDoc::getReceiverName, receiverName)
                .like(logisticsCompany != null, SaleReturnDoc::getLogisticsCompany, logisticsCompany)
                .like(waybillNo != null, SaleReturnDoc::getWaybillNo, waybillNo)
                .eq(region != null, SaleReturnDoc::getRegion, region)
                .orderByDesc(SaleReturnDoc::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleReturnDoc> exportList(String keyword, String customerName, Integer status) {
        LambdaQueryWrapper<SaleReturnDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturnDoc::getReturnDocNo, keyword)
                .like(customerName != null, SaleReturnDoc::getCustomerName, customerName)
                .eq(status != null, SaleReturnDoc::getStatus, status)
                .orderByDesc(SaleReturnDoc::getCreateTime);
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
    public List<SaleReturnDoc> listByCustomerId(Long customerId) {
        return this.lambdaQuery()
                .eq(SaleReturnDoc::getCustomerId, customerId)
                .orderByDesc(SaleReturnDoc::getCreateTime)
                .list();
    }

    @Override
    public String generateReturnDocNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "XSTHD-" + dateStr + "-" + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc createReturnDoc(SaleReturnDoc returnDoc) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 校验退货数量不超过退货申请数量
        if (returnDoc.getReturnApplyId() != null) {
            validateReturnApplyQuantities(returnDoc.getReturnApplyId(), returnDoc.getItems(), null);
        }

        returnDoc.setTenantId(StpUtil.getExtra("tenantId") != null ?
                Long.parseLong(StpUtil.getExtra("tenantId").toString()) : 0L);
        returnDoc.setReturnDocNo(generateReturnDocNo());
        returnDoc.setStatus(0);
        returnDoc.setCreateTime(LocalDateTime.now());
        returnDoc.setCreateBy(userId);
        this.save(returnDoc);

        // 保存明细
        if (returnDoc.getItems() != null && !returnDoc.getItems().isEmpty()) {
            for (int i = 0; i < returnDoc.getItems().size(); i++) {
                SaleReturnDocItem item = returnDoc.getItems().get(i);
                item.setTenantId(returnDoc.getTenantId());
                item.setReturnDocId(returnDoc.getId());
                item.setLineNo(i + 1);
                item.setCreateTime(LocalDateTime.now());
                item.setCreateBy(userId);
                saleReturnDocItemMapper.insert(item);
            }
        }

        calculateTotals(returnDoc.getId());

        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc updateReturnDoc(SaleReturnDoc returnDoc) {
        SaleReturnDoc existing = this.getById(returnDoc.getId());
        if (existing == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (existing.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以编辑");
        }

        // 校验退货数量不超过退货申请数量
        Long returnApplyId = returnDoc.getReturnApplyId() != null ? returnDoc.getReturnApplyId() : existing.getReturnApplyId();
        if (returnApplyId != null) {
            validateReturnApplyQuantities(returnApplyId, returnDoc.getItems(), returnDoc.getId());
        }

        returnDoc.setUpdateTime(LocalDateTime.now());
        returnDoc.setUpdateBy(StpUtil.getLoginIdAsLong());
        this.updateById(returnDoc);

        // 更新明细：先删除旧的，再插入新的
        saleReturnDocItemMapper.delete(
                new LambdaQueryWrapper<SaleReturnDocItem>()
                        .eq(SaleReturnDocItem::getReturnDocId, returnDoc.getId())
        );

        if (returnDoc.getItems() != null && !returnDoc.getItems().isEmpty()) {
            for (int i = 0; i < returnDoc.getItems().size(); i++) {
                SaleReturnDocItem item = returnDoc.getItems().get(i);
                item.setId(null);
                item.setTenantId(existing.getTenantId());
                item.setReturnDocId(returnDoc.getId());
                item.setLineNo(i + 1);
                item.setCreateTime(LocalDateTime.now());
                item.setCreateBy(StpUtil.getLoginIdAsLong());
                saleReturnDocItemMapper.insert(item);
            }
        }

        calculateTotals(returnDoc.getId());

        return this.getById(returnDoc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc submitForApproval(Long returnDocId) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以提交审批");
        }

        returnDoc.setStatus(1);
        returnDoc.setSubmitBy(StpUtil.getLoginIdAsLong());
        returnDoc.setSubmitTime(LocalDateTime.now());
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);
        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc approve(Long returnDocId, String note) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货单可以审批");
        }

        returnDoc.setStatus(2);
        returnDoc.setApprovedBy(StpUtil.getLoginIdAsLong());
        returnDoc.setApprovedTime(LocalDateTime.now());
        returnDoc.setApprovedNote(note);
        returnDoc.setAuditor(StpUtil.getExtra("nickname") != null ?
                StpUtil.getExtra("nickname").toString() : null);
        returnDoc.setAuditorId(StpUtil.getLoginIdAsLong());
        returnDoc.setAuditorName(StpUtil.getExtra("nickname") != null ?
                StpUtil.getExtra("nickname").toString() : null);
        returnDoc.setAuditTime(LocalDateTime.now());
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);

        // 审批通过后回写退货申请单状态
        writebackReturnApplyStatus(returnDoc.getReturnApplyId());

        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc reject(Long returnDocId, String reason) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货单可以拒绝");
        }

        returnDoc.setStatus(0);
        returnDoc.setApprovedNote(reason);
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);
        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc complete(Long returnDocId) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() != 2) {
            throw new RuntimeException("只有已审批状态的退货单可以完成");
        }

        returnDoc.setStatus(3);
        returnDoc.setBookkeepingTime(LocalDateTime.now());
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);
        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc cancel(Long returnDocId, String reason) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() == 3) {
            throw new RuntimeException("已完成的退货单不能取消");
        }

        returnDoc.setStatus(4);
        returnDoc.setRemark(reason);
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);
        return returnDoc;
    }

    @Override
    public List<SaleReturnDocItem> getItems(Long returnDocId) {
        return saleReturnDocItemMapper.selectList(
                new LambdaQueryWrapper<SaleReturnDocItem>()
                        .eq(SaleReturnDocItem::getReturnDocId, returnDocId)
                        .orderByAsc(SaleReturnDocItem::getLineNo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDocItem addItem(Long returnDocId, SaleReturnDocItem item) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnDoc.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以添加明细");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        item.setTenantId(returnDoc.getTenantId());
        item.setReturnDocId(returnDocId);
        item.setCreateTime(LocalDateTime.now());
        item.setCreateBy(userId);
        saleReturnDocItemMapper.insert(item);

        calculateTotals(returnDocId);

        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDocItem updateItem(Long itemId, SaleReturnDocItem item) {
        SaleReturnDocItem existing = saleReturnDocItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturnDoc returnDoc = this.getById(existing.getReturnDocId());
        if (returnDoc.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以修改明细");
        }

        existing.setProductId(item.getProductId());
        existing.setProductName(item.getProductName());
        existing.setProductCode(item.getProductCode());
        existing.setReturnQuantity(item.getReturnQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        existing.setLineAmount(item.getLineAmount());
        existing.setDiscountRate(item.getDiscountRate());
        existing.setDiscountedPrice(item.getDiscountedPrice());
        existing.setDiscountedAmount(item.getDiscountedAmount());
        existing.setWeight(item.getWeight());
        existing.setVolume(item.getVolume());
        existing.setIsGift(item.getIsGift());
        existing.setProductLineAttr(item.getProductLineAttr());
        existing.setRemark(item.getRemark());
        existing.setItemRemark(item.getItemRemark());
        existing.setUpdateTime(LocalDateTime.now());
        saleReturnDocItemMapper.updateById(existing);

        calculateTotals(existing.getReturnDocId());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        SaleReturnDocItem item = saleReturnDocItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturnDoc returnDoc = this.getById(item.getReturnDocId());
        if (returnDoc.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以删除明细");
        }

        saleReturnDocItemMapper.deleteById(itemId);

        calculateTotals(item.getReturnDocId());
    }

    @Override
    public Page<Map<String, Object>> pageDetail(
            String keyword, Long customerId, Long warehouseId, Integer status,
            String returnDocNo, String productName, Long handlerId, String deptName,
            String settleStatus, String salesType, String productLineAttr,
            String itemRemark, String startDate, String endDate,
            int pageNum, int pageSize,
            String creatorName, String auditorName, String remark,
            Boolean isGift, String auditTime) {

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SaleReturnDoc> wrapper = new QueryWrapper<>();

        // 主表条件
        wrapper.eq("r.deleted", 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("r.return_doc_no", keyword);
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
        if (returnDocNo != null && !returnDocNo.isEmpty()) {
            wrapper.like("r.return_doc_no", returnDocNo);
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
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("r.order_date", parseLocalDate(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("r.order_date", parseLocalDate(endDate));
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
        if (auditTime != null && !auditTime.isEmpty()) {
            wrapper.ge("r.audit_time", parseLocalDateTime(auditTime));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnDocId) {
        List<SaleReturnDocItem> items = getItems(returnDocId);

        BigDecimal totalQuantity = items.stream()
                .map(SaleReturnDocItem::getReturnQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = i.getReturnQuantity() != null ? i.getReturnQuantity() : BigDecimal.ZERO;
                    BigDecimal price = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight = items.stream()
                .map(SaleReturnDocItem::getWeight)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVolume = items.stream()
                .map(SaleReturnDocItem::getVolume)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountBillAmount = items.stream()
                .map(i -> {
                    if (i.getDiscountedAmount() != null && i.getDiscountedAmount().compareTo(BigDecimal.ZERO) != 0) {
                        return i.getDiscountedAmount();
                    }
                    // 如果没有折后金额，用数量×折后单价
                    BigDecimal qty = i.getReturnQuantity() != null ? i.getReturnQuantity() : BigDecimal.ZERO;
                    BigDecimal discountedPrice = i.getDiscountedPrice() != null ? i.getDiscountedPrice() : i.getUnitPrice();
                    return qty.multiply(discountedPrice != null ? discountedPrice : BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc != null) {
            returnDoc.setTotalQuantity(totalQuantity);
            returnDoc.setReturnQuantityTotal(totalQuantity);
            returnDoc.setTotalAmount(totalAmount);
            returnDoc.setProductAmount(totalAmount);
            returnDoc.setBillAmount(totalAmount);
            returnDoc.setDiscountBillAmount(discountBillAmount);
            returnDoc.setTotalWeight(totalWeight);
            returnDoc.setTotalVolume(totalVolume);
            returnDoc.setProductLineCount(items.size());
            returnDoc.setUpdateTime(LocalDateTime.now());
            this.updateById(returnDoc);
        }
    }

    private java.sql.Timestamp parseLocalDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return java.sql.Timestamp.valueOf(LocalDate.parse(dateString).atStartOfDay());
        } catch (DateTimeParseException e) {
            try {
                return java.sql.Timestamp.valueOf(LocalDateTime.parse(dateString));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected format: yyyy-MM-dd or yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    private java.sql.Timestamp parseLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        try {
            return java.sql.Timestamp.valueOf(LocalDateTime.parse(dateTimeString));
        } catch (DateTimeParseException e) {
            try {
                return java.sql.Timestamp.valueOf(LocalDate.parse(dateTimeString).atStartOfDay());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid datetime format: " + dateTimeString + ". Expected format: yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    /**
     * 校验退货单明细数量不超过退货申请单数量
     * 当退货单关联了退货申请时调用
     */
    private void validateReturnApplyQuantities(Long returnApplyId, List<SaleReturnDocItem> docItems, Long excludeDocId) {
        if (returnApplyId == null || docItems == null || docItems.isEmpty()) {
            return;
        }

        SaleReturn returnApply = saleReturnService.getById(returnApplyId);
        if (returnApply == null) {
            throw new RuntimeException("关联的退货申请单不存在，ID: " + returnApplyId);
        }

        // 获取退货申请单明细
        List<SaleReturnItem> applyItems = saleReturnService.getItems(returnApplyId);
        if (applyItems == null || applyItems.isEmpty()) {
            return;
        }

        // 查询同一退货申请已关联的其他退货单明细数量（排除当前单据）
        LambdaQueryWrapper<SaleReturnDoc> existDocWrapper = new LambdaQueryWrapper<>();
        existDocWrapper.eq(SaleReturnDoc::getReturnApplyId, returnApplyId)
                .ne(excludeDocId != null, SaleReturnDoc::getId, excludeDocId);
        List<SaleReturnDoc> existDocs = this.list(existDocWrapper);

        // 统计已退货数量（按productId）
        Map<Long, BigDecimal> alreadyReturned = new java.util.HashMap<>();
        for (SaleReturnDoc existDoc : existDocs) {
            List<SaleReturnDocItem> existItems = getItems(existDoc.getId());
            for (SaleReturnDocItem item : existItems) {
                if (item.getProductId() != null && item.getReturnQuantity() != null) {
                    alreadyReturned.merge(item.getProductId(), item.getReturnQuantity(), BigDecimal::add);
                }
            }
        }

        // 校验每个明细行
        for (SaleReturnDocItem docItem : docItems) {
            if (docItem.getProductId() == null || docItem.getReturnQuantity() == null) {
                continue;
            }

            // 在退货申请明细中找到对应产品
            SaleReturnItem applyItem = applyItems.stream()
                    .filter(ai -> ai.getProductId() != null && ai.getProductId().equals(docItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (applyItem == null || applyItem.getReturnQuantity() == null) {
                continue; // 申请单中没有该产品行，跳过
            }

            BigDecimal appliedQty = applyItem.getReturnQuantity();
            BigDecimal alreadyReturnedQty = alreadyReturned.getOrDefault(docItem.getProductId(), BigDecimal.ZERO);
            BigDecimal newReturnQty = docItem.getReturnQuantity();
            BigDecimal totalReturned = alreadyReturnedQty.add(newReturnQty);

            if (totalReturned.compareTo(appliedQty) > 0) {
                throw new RuntimeException(String.format(
                        "商品 [%s] 退货数量 (%s) 超过退货申请数量 (%s)，已退货 %s",
                        docItem.getProductName(), totalReturned, appliedQty, alreadyReturnedQty));
            }
        }
    }

    /**
     * 审批通过后回写退货申请单状态
     */
    private void writebackReturnApplyStatus(Long returnApplyId) {
        if (returnApplyId == null) {
            return;
        }

        SaleReturn returnApply = saleReturnService.getById(returnApplyId);
        if (returnApply == null) {
            log.warn("退货申请单不存在，无法回写状态，ID: {}", returnApplyId);
            return;
        }

        // 查询该退货申请关联的所有退货单
        LambdaQueryWrapper<SaleReturnDoc> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(SaleReturnDoc::getReturnApplyId, returnApplyId);
        List<SaleReturnDoc> relatedDocs = this.list(docWrapper);

        // 检查是否所有退货单都已审批通过
        boolean allApproved = !relatedDocs.isEmpty() && relatedDocs.stream()
                .allMatch(doc -> doc.getStatus() != null && doc.getStatus() >= 2);

        if (allApproved && returnApply.getStatus() != null && returnApply.getStatus() < 3) {
            // 更新退货申请状态为已完成
            returnApply.setStatus(3);
            returnApply.setUpdateTime(LocalDateTime.now());
            saleReturnService.updateById(returnApply);
            log.info("退货申请单 {} 已关联的所有退货单均审批通过，状态更新为已完成", returnApply.getReturnNo());
        }
    }
}
