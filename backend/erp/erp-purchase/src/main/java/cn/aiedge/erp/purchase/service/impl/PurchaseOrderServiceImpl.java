package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.base.workflow.facade.ApprovalFacade;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.mapper.*;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final PurchaseOrderItemMapper orderItemMapper;
    private final PurchaseOrderPartnerSnapshotMapper partnerSnapshotMapper;
    private final PurchaseOrderSettlementMapper settlementMapper;
    private final PurchaseOrderLogisticsMapper logisticsMapper;
    private final PurchaseOrderDepositMapper depositMapper;
    private final PurchaseOrderAuditTrailMapper auditTrailMapper;
    private final PurchaseOrderExtInfoMapper extInfoMapper;

    /** 审批门面（影子模式）：core-api 有引擎实现时可选注入，无实现时保持原行为 */
    private final ObjectProvider<ApprovalFacade> approvalFacadeProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrderDTO dto) {
        PurchaseOrder order = dto.getOrder();
        if (order == null) {
            throw BusinessException.badRequest("订单主表数据不能为空");
        }

        // 生成单据编号
        order.setOrderNo(generateNextOrderNo(LocalDate.now()));
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy(StpUtil.getLoginIdAsLong());

        // 计算金额
        calculateOrderAmount(dto);

        // 保存主表
        save(order);
        Long orderId = order.getId();

        // 保存子表
        saveSubTables(orderId, dto);

        logger.info("创建采购订单成功: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, PurchaseOrderDTO dto) {
        PurchaseOrder existing = getById(id);
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法修改");
        }

        PurchaseOrder order = dto.getOrder();
        order.setId(id);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(StpUtil.getLoginIdAsLong());

        calculateOrderAmount(dto);
        updateById(order);

        // 更新子表
        updateSubTables(id, dto);

        logger.info("更新采购订单成功: orderId={}", id);
    }

    @Override
    public PurchaseOrderDTO getOrderDetail(Long id) {
        PurchaseOrder order = getById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setOrder(order);

        // 查询子表
        LambdaQueryWrapper<PurchaseOrderPartnerSnapshot> psWrapper = new LambdaQueryWrapper<>();
        psWrapper.eq(PurchaseOrderPartnerSnapshot::getOrderId, id);
        dto.setPartnerSnapshot(partnerSnapshotMapper.selectOne(psWrapper));

        LambdaQueryWrapper<PurchaseOrderSettlement> stWrapper = new LambdaQueryWrapper<>();
        stWrapper.eq(PurchaseOrderSettlement::getOrderId, id);
        dto.setSettlement(settlementMapper.selectOne(stWrapper));

        LambdaQueryWrapper<PurchaseOrderLogistics> lgWrapper = new LambdaQueryWrapper<>();
        lgWrapper.eq(PurchaseOrderLogistics::getOrderId, id);
        dto.setLogisticsList(logisticsMapper.selectList(lgWrapper));

        LambdaQueryWrapper<PurchaseOrderDeposit> dpWrapper = new LambdaQueryWrapper<>();
        dpWrapper.eq(PurchaseOrderDeposit::getOrderId, id)
                 .orderByAsc(PurchaseOrderDeposit::getSequenceNo);
        dto.setDeposits(depositMapper.selectList(dpWrapper));

        LambdaQueryWrapper<PurchaseOrderExtInfo> eiWrapper = new LambdaQueryWrapper<>();
        eiWrapper.eq(PurchaseOrderExtInfo::getOrderId, id);
        dto.setExtInfo(extInfoMapper.selectOne(eiWrapper));

        dto.setItems(orderItemMapper.selectByOrderId(id));

        return dto;
    }

    @Override
    public Page<PurchaseOrderListDTO> pageOrders(Integer current, Integer size, Integer status,
                                                 String orderNo, String supplierName, String keyword,
                                                 String startDate, String endDate) {
        Page<PurchaseOrderListDTO> page = new Page<>(
                current == null || current < 1 ? 1L : current.longValue(),
                size == null || size < 1 ? 10L : size.longValue());

        // 复用「按单据」39 列查询（selectDocListWithNames），口径与采购单据查询页完全一致。
        // 注意：wrapper 内列名带表别名，与该 SQL 的 JOIN 别名（o / ps）对应。
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        // 自定义 SQL + ${ew.customSqlSegment} 不会自动补逻辑删除条件（@TableLogic 只作用于 BaseMapper）
        wrapper.eq("o.deleted", 0);
        wrapper.eq(status != null, "o.status", status);
        wrapper.like(isNotBlank(orderNo), "o.order_no", orderNo);
        wrapper.like(isNotBlank(supplierName), "ps.supplier_name", supplierName);
        if (isNotBlank(keyword)) {
            // 通用关键字：单据编号 或 供应商名称 命中即可
            wrapper.and(w -> w.like("o.order_no", keyword).or().like("ps.supplier_name", keyword));
        }
        LocalDate start = parseIsoDate(startDate);
        LocalDate end = parseIsoDate(endDate);
        wrapper.ge(start != null, "o.order_date", start == null ? null : start.atStartOfDay());
        wrapper.le(end != null, "o.order_date", end == null ? null : end.atTime(23, 59, 59));
        wrapper.orderByDesc("o.create_time");

        return baseMapper.selectDocListWithNames(page, wrapper);
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 容错解析 yyyy-MM-dd（前端可能传 yyyyMMdd）；解析不了按「不传」处理，不抛异常。 */
    private static LocalDate parseIsoDate(String s) {
        if (!isNotBlank(s)) {
            return null;
        }
        try {
            return s.matches("\\d{8}") ? LocalDate.parse(s, DateTimeFormatter.BASIC_ISO_DATE)
                    : LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        PurchaseOrder existing = getById(orderId);
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法删除");
        }

        // 删除子表
        deleteSubTables(orderId);
        removeById(orderId);
        logger.info("删除采购订单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交审批");
        }

        order.setStatus(1);
        order.setSubmitterId(StpUtil.getLoginIdAsLong());
        order.setSubmitTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 记录审核流水
        saveAuditTrail(orderId, "submit", "提交审批");

        // 影子模式：并行发起工作流引擎实例（不回写单据状态；无引擎/失败仅记日志不阻断）
        startShadowApproval(order);

        logger.info("提交采购订单审批: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId) {
        approve(orderId, currentUserIdOrNull(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId, Long auditorId, String auditorName) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw BusinessException.badRequest("订单不在待审批状态");
        }

        // 第三期（影子转正式）：存在在途引擎实例时由引擎驱动审批，
        // 终态经 ApprovalCallback 回调本方法完成回写，此处不再直接翻转
        if (tryDriveEngineApproval(orderId, "approve", null)) {
            return;
        }

        order.setStatus(2);
        order.setApprovalStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 工作流回调线程无会话：终审人由回调显式传入，写入审核流水
        saveAuditTrail(orderId, "approve", "审批通过", auditorId, auditorName);
        logger.info("审批通过采购订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        // 第三期（影子转正式）：存在在途引擎实例时由引擎驱动驳回（终态由回调回写）
        if (tryDriveEngineApproval(orderId, "reject", reason)) {
            return;
        }

        order.setStatus(0);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        saveAuditTrail(orderId, "reject", "审批拒绝: " + reason);
        logger.info("审批拒绝采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() >= 3) {
            throw BusinessException.badRequest("订单已开始履行，无法取消");
        }

        order.setStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        saveAuditTrail(orderId, "cancel", "取消原因: " + reason);
        logger.info("取消采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(List<Long> orderIds) {
        for (Long orderId : orderIds) {
            PurchaseOrder order = getById(orderId);
            if (order == null) {
                logger.warn("批量审批跳过不存在的订单: orderId={}", orderId);
                continue;
            }
            if (order.getStatus() != 1) {
                logger.warn("批量审批跳过非待审批订单: orderId={}, status={}", orderId, order.getStatus());
                continue;
            }
            order.setStatus(2);
            order.setApprovalStatus(1);
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);
            saveAuditTrail(orderId, "approve", "批量审批通过");
        }
        logger.info("批量审批通过采购订单: count={}", orderIds.size());
    }

    @Override
    public String generateNextOrderNo(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CG" + dateStr;
        String pattern = prefix + "%";

        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseOrder::getOrderNo, prefix)
               .orderByDesc(PurchaseOrder::getOrderNo)
               .last("LIMIT 1");
        PurchaseOrder lastOrder = getOne(wrapper);

        int seq = 1;
        if (lastOrder != null && lastOrder.getOrderNo() != null) {
            String lastNo = lastOrder.getOrderNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    @Override
    public List<PurchaseOrder> exportOrders(Long tenantId, String orderNo, Long supplierId, Integer status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, PurchaseOrder::getTenantId, tenantId)
               .like(orderNo != null, PurchaseOrder::getOrderNo, orderNo)
               .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
               .eq(status != null, PurchaseOrder::getStatus, status)
               .orderByDesc(PurchaseOrder::getCreateTime);
        return list(wrapper);
    }

    // ═══ 私有方法 ═══

    private void saveSubTables(Long orderId, PurchaseOrderDTO dto) {
        if (dto.getPartnerSnapshot() != null) {
            dto.getPartnerSnapshot().setId(null).setOrderId(orderId);
            partnerSnapshotMapper.insert(dto.getPartnerSnapshot());
        }
        if (dto.getSettlement() != null) {
            dto.getSettlement().setId(null).setOrderId(orderId);
            settlementMapper.insert(dto.getSettlement());
        }
        if (dto.getExtInfo() != null) {
            dto.getExtInfo().setId(null).setOrderId(orderId);
            extInfoMapper.insert(dto.getExtInfo());
        }
        if (dto.getLogisticsList() != null) {
            for (PurchaseOrderLogistics lg : dto.getLogisticsList()) {
                lg.setId(null).setOrderId(orderId);
                logisticsMapper.insert(lg);
            }
        }
        if (dto.getDeposits() != null) {
            for (PurchaseOrderDeposit dp : dto.getDeposits()) {
                dp.setId(null).setOrderId(orderId);
                depositMapper.insert(dp);
            }
        }
        if (dto.getItems() != null) {
            int lineNo = 1;
            for (PurchaseOrderItem item : dto.getItems()) {
                item.setId(null).setOrderId(orderId).setLineNo(lineNo++);
                orderItemMapper.insert(item);
            }
        }
    }

    private void updateSubTables(Long orderId, PurchaseOrderDTO dto) {
        // 1:1 子表：查到则更新，否则新增
        upsertPartnerSnapshot(orderId, dto.getPartnerSnapshot());
        upsertSettlement(orderId, dto.getSettlement());
        upsertExtInfo(orderId, dto.getExtInfo());

        // 1:N 子表：先删后插
        if (dto.getLogisticsList() != null) {
            LambdaQueryWrapper<PurchaseOrderLogistics> lgDel = new LambdaQueryWrapper<>();
            lgDel.eq(PurchaseOrderLogistics::getOrderId, orderId);
            logisticsMapper.delete(lgDel);
            for (PurchaseOrderLogistics lg : dto.getLogisticsList()) {
                lg.setId(null).setOrderId(orderId);
                logisticsMapper.insert(lg);
            }
        }
        if (dto.getDeposits() != null) {
            LambdaQueryWrapper<PurchaseOrderDeposit> dpDel = new LambdaQueryWrapper<>();
            dpDel.eq(PurchaseOrderDeposit::getOrderId, orderId);
            depositMapper.delete(dpDel);
            for (PurchaseOrderDeposit dp : dto.getDeposits()) {
                dp.setId(null).setOrderId(orderId);
                depositMapper.insert(dp);
            }
        }
        // 明细：先删后插
        if (dto.getItems() != null) {
            LambdaQueryWrapper<PurchaseOrderItem> itemDel = new LambdaQueryWrapper<>();
            itemDel.eq(PurchaseOrderItem::getOrderId, orderId);
            orderItemMapper.delete(itemDel);
            int lineNo = 1;
            for (PurchaseOrderItem item : dto.getItems()) {
                item.setId(null).setOrderId(orderId).setLineNo(lineNo++);
                orderItemMapper.insert(item);
            }
        }
    }

    private void deleteSubTables(Long orderId) {
        LambdaQueryWrapper<PurchaseOrderPartnerSnapshot> psDel = new LambdaQueryWrapper<>();
        psDel.eq(PurchaseOrderPartnerSnapshot::getOrderId, orderId);
        partnerSnapshotMapper.delete(psDel);

        LambdaQueryWrapper<PurchaseOrderSettlement> stDel = new LambdaQueryWrapper<>();
        stDel.eq(PurchaseOrderSettlement::getOrderId, orderId);
        settlementMapper.delete(stDel);

        LambdaQueryWrapper<PurchaseOrderLogistics> lgDel = new LambdaQueryWrapper<>();
        lgDel.eq(PurchaseOrderLogistics::getOrderId, orderId);
        logisticsMapper.delete(lgDel);

        LambdaQueryWrapper<PurchaseOrderDeposit> dpDel = new LambdaQueryWrapper<>();
        dpDel.eq(PurchaseOrderDeposit::getOrderId, orderId);
        depositMapper.delete(dpDel);

        LambdaQueryWrapper<PurchaseOrderAuditTrail> atDel = new LambdaQueryWrapper<>();
        atDel.eq(PurchaseOrderAuditTrail::getOrderId, orderId);
        auditTrailMapper.delete(atDel);

        LambdaQueryWrapper<PurchaseOrderExtInfo> eiDel = new LambdaQueryWrapper<>();
        eiDel.eq(PurchaseOrderExtInfo::getOrderId, orderId);
        extInfoMapper.delete(eiDel);

        LambdaQueryWrapper<PurchaseOrderItem> itemDel = new LambdaQueryWrapper<>();
        itemDel.eq(PurchaseOrderItem::getOrderId, orderId);
        orderItemMapper.delete(itemDel);
    }

    private void upsertPartnerSnapshot(Long orderId, PurchaseOrderPartnerSnapshot snapshot) {
        if (snapshot == null) return;
        LambdaQueryWrapper<PurchaseOrderPartnerSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderPartnerSnapshot::getOrderId, orderId);
        PurchaseOrderPartnerSnapshot existing = partnerSnapshotMapper.selectOne(wrapper);
        if (existing != null) {
            snapshot.setId(existing.getId()).setOrderId(orderId);
            partnerSnapshotMapper.updateById(snapshot);
        } else {
            snapshot.setId(null).setOrderId(orderId);
            partnerSnapshotMapper.insert(snapshot);
        }
    }

    private void upsertSettlement(Long orderId, PurchaseOrderSettlement settlement) {
        if (settlement == null) return;
        LambdaQueryWrapper<PurchaseOrderSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderSettlement::getOrderId, orderId);
        PurchaseOrderSettlement existing = settlementMapper.selectOne(wrapper);
        if (existing != null) {
            settlement.setId(existing.getId()).setOrderId(orderId);
            settlementMapper.updateById(settlement);
        } else {
            settlement.setId(null).setOrderId(orderId);
            settlementMapper.insert(settlement);
        }
    }

    private void upsertExtInfo(Long orderId, PurchaseOrderExtInfo extInfo) {
        if (extInfo == null) return;
        LambdaQueryWrapper<PurchaseOrderExtInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderExtInfo::getOrderId, orderId);
        PurchaseOrderExtInfo existing = extInfoMapper.selectOne(wrapper);
        if (existing != null) {
            extInfo.setId(existing.getId()).setOrderId(orderId);
            extInfoMapper.updateById(extInfo);
        } else {
            extInfo.setId(null).setOrderId(orderId);
            extInfoMapper.insert(extInfo);
        }
    }

    private void saveAuditTrail(Long orderId, String action, String comment) {
        saveAuditTrail(orderId, action, comment, currentUserIdOrNull(), null);
    }

    /**
     * 记录审核流水（显式指定操作人）：工作流回调等无会话线程由调用方传入终审人，
     * 避免 operator 落空；Web 请求线程走 3 参版本按当前登录人入账
     */
    private void saveAuditTrail(Long orderId, String action, String comment, Long operatorId, String operatorName) {
        PurchaseOrderAuditTrail trail = new PurchaseOrderAuditTrail();
        trail.setOrderId(orderId);
        trail.setAction(action);
        trail.setOperatorId(operatorId);
        trail.setOperatorName(operatorName);
        trail.setComment(comment);
        trail.setOperateTime(LocalDateTime.now());
        trail.setCreateTime(LocalDateTime.now());
        auditTrailMapper.insert(trail);
    }

    /**
     * 安全解析当前登录人：Web 请求线程返回登录用户ID；工作流回调等异步线程
     * 无 Sa-Token 会话时返回 null（getLoginIdAsLong 在无会话时会抛异常）。
     */
    private Long currentUserIdOrNull() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 第三期（影子转正式）：存在在途引擎实例时由引擎驱动审批（approve/reject），
     * 引擎到达终态后经 ApprovalCallbackDispatcher 回调本服务的 approve/reject 回写单据，
     * 调用方不再直接翻转状态。
     *
     * 防循环：引擎回调到达时实例已是终态（非在途），findInFlightInstance 返回 null，
     * 自然回退为直接翻转——同一方法被回调复用也不会再次驱动引擎。
     *
     * @return true=已由引擎受理（状态回写交给回调）；false=无在途实例/引擎不可用/驱动失败，
     *         调用方回退现有的直接翻转逻辑
     */
    private boolean tryDriveEngineApproval(Long orderId, String action, String comment) {
        try {
            // Spring 环境必然注入 ObjectProvider；判空仅为兼容手工构造的单元测试
            ApprovalFacade facade = approvalFacadeProvider == null ? null : approvalFacadeProvider.getIfAvailable();
            if (facade == null) {
                return false;
            }
            ApprovalFacade.InFlightInstance inFlight = facade.findInFlightInstance("purchase_order", orderId);
            if (inFlight == null) {
                return false;
            }
            boolean driven = facade.driveApproval(inFlight, action, StpUtil.getLoginIdAsLong(), comment);
            if (driven) {
                logger.info("采购订单审批经引擎驱动(终态由回调回写): orderId={}, action={}, instanceId={}",
                        orderId, action, inFlight.getInstanceId());
            } else {
                logger.warn("引擎驱动审批失败，回退直接翻转: orderId={}, action={}, instanceId={}",
                        orderId, action, inFlight.getInstanceId());
            }
            return driven;
        } catch (Exception e) {
            logger.warn("引擎驱动审批异常，回退直接翻转: orderId={}, action={}, error={}",
                    orderId, action, e.getMessage());
            return false;
        }
    }

    /**
     * 影子模式发起工作流实例：引擎实例并行可见于工作流待办页，不回写单据 status。
     * instanceId 追加在审核流水 comment 中（不新增列）。无引擎实现或发起失败时不阻断业务。
     */
    private void startShadowApproval(PurchaseOrder order) {
        try {
            // Spring 环境必然注入 ObjectProvider；判空仅为兼容手工构造的单元测试
            ApprovalFacade facade = approvalFacadeProvider == null ? null : approvalFacadeProvider.getIfAvailable();
            if (facade == null) {
                return;
            }
            Map<String, Object> businessData = new HashMap<>();
            businessData.put("orderNo", order.getOrderNo());
            businessData.put("amount", order.getBillAmount());
            businessData.put("supplierId", order.getSupplierId());
            LambdaQueryWrapper<PurchaseOrderPartnerSnapshot> psWrapper = new LambdaQueryWrapper<>();
            psWrapper.eq(PurchaseOrderPartnerSnapshot::getOrderId, order.getId());
            PurchaseOrderPartnerSnapshot snapshot = partnerSnapshotMapper.selectOne(psWrapper);
            if (snapshot != null && snapshot.getSupplierName() != null) {
                businessData.put("supplierName", snapshot.getSupplierName());
            }
            String instanceId = facade.startApproval("order_approval", "purchase_order", order.getId(),
                    order.getOrderNo(), businessData, StpUtil.getLoginIdAsLong(), null);
            if (instanceId != null) {
                saveAuditTrail(order.getId(), "workflow", "影子工作流实例已发起: instanceId=" + instanceId);
            }
        } catch (Exception e) {
            logger.warn("发起采购订单影子工作流失败(不阻断): orderId={}, error={}", order.getId(), e.getMessage());
        }
    }

    private void calculateOrderAmount(PurchaseOrderDTO dto) {
        PurchaseOrder order = dto.getOrder();
        List<PurchaseOrderItem> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            if (order.getProductAmount() == null) {
                order.setProductAmount(BigDecimal.ZERO);
            }
            if (order.getDiscountAmount() == null) {
                order.setDiscountAmount(BigDecimal.ZERO);
            }
            if (order.getBillAmount() == null) {
                order.setBillAmount(BigDecimal.ZERO);
            }
            return;
        }

        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal amount = qty.multiply(price);
            item.setAmount(amount);
            productAmount = productAmount.add(amount);
            totalQuantity = totalQuantity.add(qty);
        }

        order.setProductAmount(productAmount);
        order.setTotalQuantity(totalQuantity);

        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal otherExpense = BigDecimal.ZERO;
        if (dto.getSettlement() != null && dto.getSettlement().getOtherExpense() != null) {
            otherExpense = dto.getSettlement().getOtherExpense();
        }
        order.setBillAmount(productAmount.subtract(discount).add(otherExpense));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importOrders(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }
        // XLSX 解析：通过 Apache POI 读取并批量创建订单
        int count = 0;
        try (var inputStream = file.getInputStream()) {
            var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                PurchaseOrderDTO dto = new PurchaseOrderDTO();
                PurchaseOrder order = new PurchaseOrder();
                order.setOrderDate(LocalDateTime.now());
                order.setStatus(0);
                order.setPurchaseType(0);
                // 读取 Excel 列: 0=供应商名, 1=仓库名, 2=商品名, 3=数量, 4=单价
                var supplierCell = row.getCell(0);
                var warehouseCell = row.getCell(1);
                var productCell = row.getCell(2);
                var qtyCell = row.getCell(3);
                var priceCell = row.getCell(4);
                if (productCell == null) continue;

                if (dto.getPartnerSnapshot() == null) {
                    dto.setPartnerSnapshot(new cn.aiedge.erp.purchase.entity.PurchaseOrderPartnerSnapshot());
                }
                if (supplierCell != null) dto.getPartnerSnapshot().setSupplierName(supplierCell.getStringCellValue());
                if (warehouseCell != null) order.setWarehouseId(0L);

                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setProductName(productCell.getStringCellValue());
                item.setQuantity(qtyCell != null ? BigDecimal.valueOf(qtyCell.getNumericCellValue()) : BigDecimal.ZERO);
                item.setUnitPrice(priceCell != null ? BigDecimal.valueOf(priceCell.getNumericCellValue()) : BigDecimal.ZERO);
                item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                item.setLineNo(1);

                dto.setOrder(order);
                dto.setItems(List.of(item));
                try {
                    createOrder(dto);
                    count++;
                } catch (Exception e) {
                    logger.warn("导入第{}行失败: {}", i, e.getMessage());
                }
            }
            workbook.close();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导入文件解析失败: " + e.getMessage());
        }
        return count;
    }

    @Override
    public void batchPrint(List<Long> orderIds, String template) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException("请选择要打印的订单");
        }
        // 更新打印次数
        for (Long orderId : orderIds) {
            PurchaseOrder order = baseMapper.selectById(orderId);
            if (order == null) continue;
            var extInfo = extInfoMapper.selectOne(
                new LambdaQueryWrapper<PurchaseOrderExtInfo>()
                    .eq(PurchaseOrderExtInfo::getOrderId, orderId));
            if (extInfo != null) {
                extInfo.setPrintCount((extInfo.getPrintCount() != null ? extInfo.getPrintCount() : 0) + 1);
                extInfoMapper.updateById(extInfo);
            }
        }
        logger.info("批量打印 {} 条订单，模板: {}", orderIds.size(), template);
    }

    /**
     * 采购订单统计。
     *
     * <p>实现照搬旧模块 {@code cn.aiedge.erp.order.service.impl.PurchaseOrderServiceImpl#getPurchaseStatistics}，
     * 口径刻意保持一致（同样按 tenantId + deleted=0 + createTime 区间过滤，总额同样取 total_amount 列），
     * 目的是让首页 KPI 与采购分析页的数值在切换实现前后**不发生任何变化**。</p>
     */
    @Override
    public PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("获取采购订单统计: {} - {}", startDate, endDate);

        PurchaseOrderStatisticsDTO statistics = new PurchaseOrderStatisticsDTO();
        statistics.setStartDate(startDate);
        statistics.setEndDate(endDate);

        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .eq(PurchaseOrder::getDeleted, 0)
                .between(PurchaseOrder::getCreateTime, startDate, endDate);
        List<PurchaseOrder> orders = list(queryWrapper);

        statistics.setTotalOrders(orders.size());
        statistics.setTotalAmount(orders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        statistics.setOrdersByStatus(orders.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getStatus()), Collectors.summingInt(o -> 1))));
        statistics.setOrdersByPurchaseType(orders.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getPurchaseType()), Collectors.summingInt(o -> 1))));

        logger.info("采购订单统计完成, 总计: {} 个订单", orders.size());
        return statistics;
    }
}
