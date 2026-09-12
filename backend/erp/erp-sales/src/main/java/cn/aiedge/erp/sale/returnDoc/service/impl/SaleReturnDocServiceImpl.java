package cn.aiedge.erp.sale.returnDoc.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDocItem;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocItemMapper;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocMapper;
import cn.aiedge.erp.sale.returnDoc.service.SaleReturnDocService;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
import cn.aiedge.erp.sale.service.integration.SalesAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    /** 库存唯一写入口：发布库存变动事件，由 WMS InventoryService 统一过账（ERP 侧不直写 erp_stock） */
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /** 业财集成：退货单审核/取消生成冲销与回转凭证 */
    @Autowired
    private SalesAccountingService salesAccountingService;

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
                                         String creatorName, String bookkeeperName, String auditorName, String submitBy,
                                         String productLineAttr, String remark, String summary,
                                         String deliveryMethod, Integer extNum1, Integer extNum2,
                                         String extText1, String extText2, String extText3,
                                         String contactName, String contactPhone, String contactAddress,
                                         String auditTime, String salesType, String receiverName,
                                         String logisticsCompany, String waybillNo, String region,
                                         Boolean showRedFlush) {
        LambdaQueryWrapper<SaleReturnDoc> wrapper = new LambdaQueryWrapper<>();
        // 红冲过滤：退货单的「红冲」即已取消(4)单据，默认不显示，勾选「显示红冲」才纳入
        wrapper.ne(!Boolean.TRUE.equals(showRedFlush), SaleReturnDoc::getStatus, 4);
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
                .like(bookkeeperName != null, SaleReturnDoc::getBookkeeperName, bookkeeperName)
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
        // 明细维度条件（商品/明细备注/商品分类）：按明细表 EXISTS 下推，避免参数空转
        if (productName != null && !productName.isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_doc_item i "
                    + "WHERE i.return_doc_id = erp_sale_return_doc.id AND i.deleted = 0 "
                    + "AND i.product_name LIKE CONCAT('%', {0}, '%'))", productName);
        }
        if (itemRemark != null && !itemRemark.isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_doc_item i "
                    + "WHERE i.return_doc_id = erp_sale_return_doc.id AND i.deleted = 0 "
                    + "AND i.item_remark LIKE CONCAT('%', {0}, '%'))", itemRemark);
        }
        if (categoryId != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_return_doc_item i "
                    + "JOIN erp_product p ON p.id = i.product_id "
                    + "WHERE i.return_doc_id = erp_sale_return_doc.id AND i.deleted = 0 AND p.category_id = {0})", categoryId);
        }
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
        // 后端号段：XSTHD-yyyyMMdd-0001，按当天已存在最大单号自增（去随机，避免撞唯一索引与不可读）
        String prefix = "XSTHD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String lastNo = this.baseMapper.selectLastReturnDocNo(prefix);
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
    public SaleReturnDoc createReturnDoc(SaleReturnDoc returnDoc) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 校验退货数量不超过退货申请数量
        if (returnDoc.getReturnApplyId() != null) {
            validateReturnApplyQuantities(returnDoc.getReturnApplyId(), returnDoc.getItems(), null);
        }

        // 租户从 Sa-Token Session 解析（StpUtil.getExtra 需 sa-token-jwt 插件）；
        // 上下文不可用时置空，由多租户拦截器在 insert 时统一注入
        returnDoc.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
        // 单号来自后端号段 /next-no，前端原样传入则保留；未传时兜底自动生成
        if (returnDoc.getReturnDocNo() == null || returnDoc.getReturnDocNo().isBlank()) {
            returnDoc.setReturnDocNo(generateReturnDocNo());
        }
        // 保存草稿(0) / 直接提交审批(1)；进入审核中时记录提交人与提交时间
        Integer status = returnDoc.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            status = 0;
        }
        returnDoc.setStatus(status);
        if (status == 1) {
            returnDoc.setSubmitBy(userId);
            returnDoc.setSubmitTime(LocalDateTime.now());
        }
        returnDoc.setCreateTime(LocalDateTime.now());
        returnDoc.setCreateBy(userId);
        if (!StringUtils.hasText(returnDoc.getCreatorName())) {
            returnDoc.setCreatorName(currentUserName());
        }
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

        // 返回落库后的完整单据（含明细与汇总金额），避免调用方拿到未回填的对象
        return this.getByIdWithItems(returnDoc.getId());
    }

    /** 当前操作员显示名（昵称 → 用户名 → 系统），避免会话缺字段时抛异常 */
    private String currentUserName() {
        try {
            if (StpUtil.isLogin()) {
                Object nickname = StpUtil.getSession().get("nickname");
                if (nickname != null && !nickname.toString().isBlank()) {
                    return nickname.toString();
                }
            }
        } catch (Exception ignored) {
            // 会话不可用时降级
        }
        String username = SecurityUtils.getCurrentUsername();
        return username != null && !username.isBlank() ? username : "系统";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc updateReturnDoc(SaleReturnDoc returnDoc) {
        SaleReturnDoc existing = this.getById(returnDoc.getId());
        if (existing == null) {
            throw BusinessException.badRequest("退货单不存在");
        }
        if (existing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的退货单可以编辑");
        }

        // 校验退货数量不超过退货申请数量
        Long returnApplyId = returnDoc.getReturnApplyId() != null ? returnDoc.getReturnApplyId() : existing.getReturnApplyId();
        if (returnApplyId != null) {
            validateReturnApplyQuantities(returnApplyId, returnDoc.getItems(), returnDoc.getId());
        }

        returnDoc.setUpdateTime(LocalDateTime.now());
        returnDoc.setUpdateBy(StpUtil.getLoginIdAsLong());
        // 草稿编辑后直接提交：记录提交人与提交时间
        if (returnDoc.getStatus() != null && returnDoc.getStatus() == 1) {
            returnDoc.setSubmitBy(StpUtil.getLoginIdAsLong());
            returnDoc.setSubmitTime(LocalDateTime.now());
        }
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

        return this.getByIdWithItems(returnDoc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc submitForApproval(Long returnDocId) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的退货单可以提交审批");
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
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的退货单可以审批");
        }

        String operator = currentUserName();
        returnDoc.setStatus(2);
        returnDoc.setApprovedBy(StpUtil.getLoginIdAsLong());
        returnDoc.setApprovedTime(LocalDateTime.now());
        returnDoc.setApprovedNote(note);
        returnDoc.setAuditor(operator);
        returnDoc.setAuditorId(StpUtil.getLoginIdAsLong());
        returnDoc.setAuditorName(operator);
        returnDoc.setAuditTime(LocalDateTime.now());
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);

        // 审核通过即真实业务闭环：退货入库回写库存 + 生成冲销凭证
        // 幂等：bookkeepingTime 已存在说明已过账，跳过（重复审核已被状态机拦截）
        if (returnDoc.getBookkeepingTime() == null) {
            applyStockIncrease(returnDoc);
            String voucherNo = salesAccountingService.createSaleReturnDocVoucher(
                    returnDoc.getId(), returnDoc.getReturnDocNo(), returnDoc.getCustomerId(),
                    returnDoc.getCustomerName(), calcCostedAmount(returnDoc), calcCostAmount(returnDoc.getId()));
            if (voucherNo != null && !voucherNo.isEmpty()) {
                returnDoc.setBookkeepingTime(LocalDateTime.now());
                returnDoc.setBookkeeperName(operator);
                this.updateById(returnDoc);
            }
        }

        // 审批通过后回写退货申请单状态
        writebackReturnApplyStatus(returnDoc.getReturnApplyId());

        return returnDoc;
    }

    /**
     * 审核通过后回写库存（退货入库增加）。
     * <p>发布 {@link InventoryChangeEvent}，由 WMS {@code InventoryService} 统一过账——
     * 库存唯一写入口，ERP 侧不直写 erp_stock。</p>
     */
    private void applyStockIncrease(SaleReturnDoc returnDoc) {
        if (returnDoc.getWarehouseId() == null) {
            throw BusinessException.badRequest("退货单未指定入库仓库，无法回写库存");
        }
        List<SaleReturnDocItem> items = getItems(returnDoc.getId());
        boolean anyApplied = false;
        for (SaleReturnDocItem item : items) {
            BigDecimal qty = item.getReturnQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.INCREASE, item.getProductId(), returnDoc.getWarehouseId(), null,
                    null, qty, "SALE_RETURN_DOC", returnDoc.getId(), returnDoc.getReturnDocNo(),
                    returnDoc.getHandlerId(), returnDoc.getHandlerName()));
            log.info("销售退货单审核触发库存入账: returnDocNo=" + returnDoc.getReturnDocNo()
                    + ", productId=" + item.getProductId() + ", qty=" + qty);
            anyApplied = true;
        }
        if (!anyApplied) {
            throw BusinessException.badRequest("退货单无有效退货明细，无法回写库存");
        }
    }

    /**
     * 取消已记账单据时冲回库存（与 {@link #applyStockIncrease} 对称）
     */
    private void applyStockDecrease(SaleReturnDoc returnDoc) {
        if (returnDoc.getWarehouseId() == null) {
            return;
        }
        for (SaleReturnDocItem item : getItems(returnDoc.getId())) {
            BigDecimal qty = item.getReturnQuantity();
            if (item.getProductId() == null || qty == null || qty.signum() <= 0) {
                continue;
            }
            applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                    InventoryChangeEvent.ChangeType.DECREASE, item.getProductId(), returnDoc.getWarehouseId(), null,
                    null, qty, "SALE_RETURN_DOC_CANCEL", returnDoc.getId(), returnDoc.getReturnDocNo(),
                    returnDoc.getHandlerId(), returnDoc.getHandlerName()));
            log.info("销售退货单取消触发库存回冲: returnDocNo=" + returnDoc.getReturnDocNo()
                    + ", productId=" + item.getProductId() + ", qty=" + qty);
        }
    }

    /**
     * 退货记账金额（含税冲减额）：优先取本单金额，回退到折后金额/商品金额。
     * <p>与列表「本单金额」列口径一致，避免用明细裸乘口径导致与展示金额不符。</p>
     */
    private BigDecimal calcCostedAmount(SaleReturnDoc returnDoc) {
        BigDecimal amount = returnDoc.getBillAmount();
        if (amount == null || amount.signum() <= 0) {
            amount = returnDoc.getDiscountBillAmount();
        }
        if (amount == null || amount.signum() <= 0) {
            amount = returnDoc.getTotalAmount();
        }
        return amount;
    }

    /** 退货成本合计（明细参考成本金额之和），无成本数据时返回 null */
    private BigDecimal calcCostAmount(Long returnDocId) {
        BigDecimal cost = getItems(returnDocId).stream()
                .map(SaleReturnDocItem::getRefCostAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cost.signum() > 0 ? cost : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc reject(Long returnDocId, String reason) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的退货单可以拒绝");
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
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() != 2) {
            throw BusinessException.badRequest("只有已审批状态的退货单可以完成");
        }

        returnDoc.setStatus(3);
        returnDoc.setUpdateTime(LocalDateTime.now());
        this.updateById(returnDoc);
        return returnDoc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnDoc cancel(Long returnDocId, String reason) {
        SaleReturnDoc returnDoc = this.getById(returnDocId);
        if (returnDoc == null) {
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() == 3) {
            throw BusinessException.badRequest("已完成的退货单不能取消");
        }
        if (returnDoc.getStatus() == 4) {
            throw BusinessException.badRequest("已取消的退货单不能重复取消");
        }

        // 已记账单据取消：冲回库存 + 生成反向凭证，保持账实一致
        if (returnDoc.getBookkeepingTime() != null) {
            applyStockDecrease(returnDoc);
            salesAccountingService.createSaleReturnDocReverseVoucher(
                    returnDoc.getId(), returnDoc.getReturnDocNo(), returnDoc.getCustomerId(),
                    returnDoc.getCustomerName(), calcCostedAmount(returnDoc), calcCostAmount(returnDoc.getId()));
        }

        returnDoc.setStatus(4);
        // 取消原因写入内部备注：不覆盖用户录入的「单据备注」，避免取消动作造成备注数据丢失
        if (StringUtils.hasText(reason)) {
            String existing = returnDoc.getInternalNote();
            returnDoc.setInternalNote("取消原因：" + reason + (StringUtils.hasText(existing) ? "；" + existing : ""));
        }
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
            throw BusinessException.badRequest("退货单不存在");
        }
        if (returnDoc.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的退货单可以添加明细");
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
            throw BusinessException.badRequest("退货明细不存在");
        }

        SaleReturnDoc returnDoc = this.getById(existing.getReturnDocId());
        if (returnDoc.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的退货单可以修改明细");
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
            throw BusinessException.badRequest("退货明细不存在");
        }

        SaleReturnDoc returnDoc = this.getById(item.getReturnDocId());
        if (returnDoc.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的退货单可以删除明细");
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
            throw BusinessException.badRequest("关联的退货申请单不存在，ID: " + returnApplyId);
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
                throw BusinessException.badRequest(String.format(
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
