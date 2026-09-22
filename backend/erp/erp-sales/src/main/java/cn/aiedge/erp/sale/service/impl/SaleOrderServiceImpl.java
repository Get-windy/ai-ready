package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.base.workflow.facade.ApprovalFacade;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.erp.pricing.service.PriceEngineService;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationResult;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.service.CustomerGradeService;
import cn.aiedge.erp.party.service.PartyGradeRelationService;
import cn.aiedge.erp.sale.dto.SaleLogisticsRemarkDTO;
import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.dto.SaleOrderDetailDTO;
import cn.aiedge.erp.sale.dto.SaleOrderItemDTO;
import cn.aiedge.erp.sale.dto.SaleOrderListDTO;
import cn.aiedge.erp.sale.entity.*;
import cn.aiedge.erp.sale.mapper.*;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.service.SaleLogisticsService;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.payment.mapper.PreReceiptMapper;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.StockService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl extends ServiceImpl<SaleOrderMapper, SaleOrder>
        implements ISaleOrderService {

    private final SaleOrderMapper orderMapper;
    private final SaleOrderItemMapper itemMapper;
    private final StockService stockService;
    private final PriceEngineService priceEngineService;
    private final CustomerGradeService customerGradeService;
    private final ProductService productService;
    private final ProductUnitService productUnitService;
    private final PartyGradeRelationService partyGradeRelationService;
    private final BizNumberGeneratorService bizNumberGeneratorService;

    // 子表 Mapper
    private final SaleOrderPartnerSnapshotMapper partnerSnapshotMapper;
    private final SaleOrderDeliveryAddressMapper deliveryAddressMapper;
    private final SaleOrderSettlementMapper settlementMapper;
    private final SaleOrderLogisticsMapper logisticsMapper;
    private final SaleOrderDepositMapper depositMapper;
    private final SaleOrderPointsJournalMapper pointsJournalMapper;
    private final SaleOrderAuditTrailMapper auditTrailMapper;
    private final SaleOrderExtInfoMapper extInfoMapper;

    /** 往来单位Mapper */
    private final PartyMapper partyMapper;

    /**
     * 仓库 / 业务员 / 部门 Mapper —— **仅供批量导入**按名称解析成 ID（SAL-BREAK-03）。
     *
     * <p>为什么是这三个而非常规依赖：批量导入的 Excel 里这几列是「名称」，
     * 而订单主表要的是 ID。此前 {@code findOrCreateOrder} 只写单号/日期/类型/状态，
     * 客户、仓库、业务员、部门**一个都没写** ⇒ 导入单在服务端无主体，
     * 出库/对账带不出往来单位，订单中心的仓库筛选恒无结果。</p>
     */
    private final cn.aiedge.erp.stock.mapper.WarehouseMapper warehouseMapper;
    private final cn.aiedge.base.mapper.SysUserMapper sysUserMapper;
    private final cn.aiedge.base.mapper.SysDeptMapper sysDeptMapper;

    /** 销售物流域服务（包裹/运费/取号/发货通知） */
    private final SaleLogisticsService saleLogisticsService;

    /** 预收款/订金Mapper */
    private final PreReceiptMapper preReceiptMapper;

    /** 审批门面（影子模式）：core-api 有引擎实现时可选注入，无实现时保持原行为 */
    private final ObjectProvider<ApprovalFacade> approvalFacadeProvider;

    /** 出库 Service（延迟注入避免循环依赖）：确认出库时生成销售出库单 */
    private final ObjectProvider<cn.aiedge.erp.sale.outbound.service.SaleOutboundService> outboundServiceProvider;

    /** 促销引擎（营销域）：服务端优惠计算的**单一真源** */
    private final cn.aiedge.erp.marketing.promotion.PromotionEngine promotionEngine;

    /** 优惠券核销（营销域）：与订单同事务 */
    private final cn.aiedge.erp.marketing.promotion.CouponRedemptionService couponRedemptionService;

    /** 优惠分摊明细 Mapper（订单优惠可回溯 + 分析域数据源） */
    private final SaleOrderPromoDetailMapper promoDetailMapper;

    /**
     * 最近一次 calculateAmount 的促销引擎结果（ThreadLocal）。
     * calculateAmount 是接口方法（无返回值），而分摊明细与券核销要在订单落库后执行，
     * 故用 ThreadLocal 在两者之间传递结果；用完即 remove，避免线程池串单。
     */
    private final ThreadLocal<cn.aiedge.erp.marketing.promotion.PromotionResult> lastPromotionResult =
            new ThreadLocal<>();

    // ═══════════════════════════════════════════
    // 基础 CRUD
    // ═══════════════════════════════════════════

    @Override
    public Page<SaleOrderListDTO> pageOrders(Page<SaleOrder> page, Long tenantId, String orderNo,
                                              Long customerId, Integer status, String startDate, String endDate) {
        // ⚠️ 这里**不能**用 `Map.of(...)`：`Map.of` 对 null 值直接抛 NPE，
        // 而 customerId / status 是可选筛选条件（不传就是 null）⇒
        // **不带这两个参数查订单列表必然 500**（2026-09-22 实机踩到：
        // 无参 `GET /api/erp/sale/order/page` → NPE at 本方法）。
        // 改用允许 null 的 HashMap；下游 buildQueryWrapper 本就是 `get(...) != null` 判空。
        Map<String, Object> filters = new HashMap<>();
        filters.put("orderNo", orderNo != null ? orderNo : "");
        filters.put("customerId", customerId);
        filters.put("status", status);
        filters.put("startDate", startDate != null ? startDate : "");
        filters.put("endDate", endDate != null ? endDate : "");
        LambdaQueryWrapper<SaleOrder> wrapper = buildQueryWrapper(tenantId, filters);
        wrapper.orderByDesc(SaleOrder::getCreateTime);

        Page<SaleOrder> result = page(page, wrapper);

        // 批量查询往来单位名称
        Set<Long> customerIds = result.getRecords().stream()
                .map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNameMap = batchGetCustomerNames(customerIds);

        Page<SaleOrderListDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(customerNameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        fillLineCount(dtoPage.getRecords());
        return dtoPage;
    }

    @Override
    public SaleOrderDetailDTO getOrderDetail(Long id) {
        SaleOrder order = getById(id);
        if (order == null) return null;

        SaleOrderDetailDTO dto = new SaleOrderDetailDTO();
        BeanUtils.copyProperties(order, dto);
        dto.setId(order.getId().toString());
        dto.setStatusName(getStatusName(order.getStatus()));

        // 加载子表数据
        loadSubTableData(id, dto);

        // 加载明细
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        enrichOrderItems(items);
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(SaleOrderDTO dto) {
        if (dto.getOrderNo() == null || dto.getOrderNo().isEmpty()) {
            dto.setOrderNo(generateOrderNo());
        }

        calculateAmount(dto);

        // 信用额度校验 & 填充信用快照
        BigDecimal creditLimit = BigDecimal.ZERO;
        BigDecimal availableCredit = BigDecimal.ZERO;
        BigDecimal prevDebt = BigDecimal.ZERO;
        if (dto.getCustomerId() != null && dto.getBillAmount() != null && dto.getBillAmount().compareTo(BigDecimal.ZERO) > 0) {
            Party party = partyMapper.selectById(dto.getCustomerId());
            if (party != null && party.getCreditLimit() != null && party.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                prevDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                creditLimit = party.getCreditLimit();
                availableCredit = creditLimit.subtract(prevDebt).max(BigDecimal.ZERO);
                if (dto.getBillAmount().compareTo(availableCredit) > 0) {
                    throw BusinessException.badRequest(String.format(
                            "客户「%s」信用额度不足：额度 %.2f，已欠款 %.2f，可用 %.2f，本单金额 %.2f",
                            party.getPartyName(), creditLimit, prevDebt, availableCredit, dto.getBillAmount()));
                }
            }
        }

        // 保存主表 (只保存核心字段)
        SaleOrder order = new SaleOrder();
        order.setTenantId(dto.getTenantId());
        order.setOrderNo(dto.getOrderNo());
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setSaleType(dto.getSaleType() != null ? dto.getSaleType() : 1);
        order.setStatus(0);
        order.setCustomerId(dto.getCustomerId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setSalesmanId(dto.getSalesmanId());
        order.setDeptId(dto.getDeptId());
        order.setProductAmount(dto.getProductAmount());
        order.setDiscountAmount(dto.getDiscountAmount());
        // 促销引擎算出的两项优惠必须落库（否则 billAmount 里减了、列里却是 0，对账与毛利分析失真）
        order.setPromoDiscount(dto.getPromoDiscount() != null ? dto.getPromoDiscount() : BigDecimal.ZERO);
        order.setCouponAmount(dto.getCouponAmount() != null ? dto.getCouponAmount() : BigDecimal.ZERO);
        order.setBillAmount(dto.getBillAmount());
        order.setSettledAmount(BigDecimal.ZERO);
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setTotalQuantity(dto.getTotalQuantity());
        order.setExpectedShipTime(dto.getExpectedShipTime());
        order.setSupplementType(dto.getSupplementType());
        order.setGenerationMethod(dto.getGenerationMethod());
        order.setSourceOrder(dto.getSourceOrder());
        order.setOrderSource(dto.getOrderSource());
        order.setRemark(dto.getRemark());
        order.setOriginalOrderId(dto.getOriginalOrderId());
        order.setOriginalOrderNo(dto.getOriginalOrderNo());
        // 冗余字段同步
        order.setBookkeepingTime(LocalDateTime.now());
        order.setCreatorName(getCurrentUserName());
        order.setPrintCount(0);
        // 客户快照冗余
        if (dto.getCustomerId() != null) {
            try {
                Party party = partyMapper.selectById(dto.getCustomerId());
                if (party != null) {
                    order.setCustomerName(party.getPartyName());
                    order.setCustomerCode(party.getPartyCode());
                }
            } catch (Exception e) {
                log.warn("加载客户信息失败: customerId={}", dto.getCustomerId());
            }
        }
        // 收货/备注等扩展字段
        if (dto.getBuyerRemark() != null) order.setBuyerRemark(dto.getBuyerRemark());
        if (dto.getOrderRemark() != null) order.setOrderRemark(dto.getOrderRemark());
        if (dto.getReceiverName() != null) order.setReceiverName(dto.getReceiverName());
        if (dto.getReceiverPhone() != null) order.setReceiverPhone(dto.getReceiverPhone());
        if (dto.getShippingAddress() != null) order.setShippingAddress(dto.getShippingAddress());
        if (dto.getLogisticsCompany() != null) order.setLogisticsCompany(dto.getLogisticsCompany());
        if (dto.getFreightPayer() != null) order.setFreightPayer(dto.getFreightPayer());
        if (dto.getWaybillNo() != null) order.setWaybillNo(dto.getWaybillNo());
        if (dto.getDeliveryMethod() != null) order.setDeliveryMethod(dto.getDeliveryMethod());
        if (dto.getDeliveryRoute() != null) order.setDeliveryRoute(dto.getDeliveryRoute());
        if (dto.getDeliveryRouteId() != null) order.setDeliveryRouteId(dto.getDeliveryRouteId());
        if (dto.getSettlementMethod() != null) order.setSettlementMethod(dto.getSettlementMethod());
        if (dto.getRegion() != null) order.setRegion(dto.getRegion());
        if (dto.getSummary() != null) order.setSummary(dto.getSummary());
        if (dto.getAttachment() != null) order.setAttachment(dto.getAttachment());
        if (dto.getShippingFee() != null) order.setShippingFee(dto.getShippingFee());
        // 信用额度快照
        order.setCreditLimit(creditLimit);
        order.setAvailableCredit(availableCredit);
        order.setPrevDebt(prevDebt);
        // 自定义字段
        if (dto.getExtNum1() != null) order.setExtNum1(dto.getExtNum1());
        if (dto.getExtNum2() != null) order.setExtNum2(dto.getExtNum2());
        if (dto.getExtText1() != null) order.setExtText1(dto.getExtText1());
        if (dto.getExtText2() != null) order.setExtText2(dto.getExtText2());
        if (dto.getExtText3() != null) order.setExtText3(dto.getExtText3());
        if (dto.getFooterExtText1() != null) order.setFooterExtText1(dto.getFooterExtText1());
        if (dto.getFooterExtText2() != null) order.setFooterExtText2(dto.getFooterExtText2());
        save(order);

        // 保存子表
        saveSubTables(order.getId(), dto);

        // 保存明细
        if (dto.getItems() != null) {
            int lineNo = 1;
            for (SaleOrderItemDTO itemDTO : dto.getItems()) {
                SaleOrderItem item = createOrderItem(itemDTO, order.getId(), dto.getCustomerId(), dto.getWarehouseId());
                item.setLineNo(lineNo++);
                itemMapper.insert(item);
            }
        }

        // 优惠分摊明细落库 + 活动使用次数累加
        savePromotionDetails(order, dto);

        // 优惠券核销（与订单同一事务：核销失败则整单回滚，避免"券没用上但单据已存"）
        if (dto.getCouponIds() != null) {
            for (Long couponId : dto.getCouponIds()) {
                couponRedemptionService.redeem(couponId, order.getId(), order.getOrderNo());
            }
        }

        log.info("创建销售订单: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(SaleOrderDTO dto) {
        SaleOrder order = getById(dto.getId());
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() > 1) throw BusinessException.badRequest("只有草稿和待审批状态的订单可以修改");

        calculateAmount(dto);

        // 信用额度校验（更新时重新校验）
        if (dto.getCustomerId() != null && dto.getBillAmount() != null && dto.getBillAmount().compareTo(BigDecimal.ZERO) > 0) {
            Party party = partyMapper.selectById(dto.getCustomerId());
            if (party != null && party.getCreditLimit() != null && party.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                BigDecimal availableCredit = party.getCreditLimit().subtract(currentDebt).max(BigDecimal.ZERO);
                if (dto.getBillAmount().compareTo(availableCredit) > 0) {
                    throw BusinessException.badRequest(String.format(
                            "客户「%s」信用额度不足：额度 %.2f，已欠款 %.2f，可用 %.2f，本单金额 %.2f",
                            party.getPartyName(), party.getCreditLimit(), currentDebt, availableCredit, dto.getBillAmount()));
                }
                order.setCreditLimit(party.getCreditLimit());
                order.setAvailableCredit(availableCredit);
                order.setPrevDebt(currentDebt);
            }
        }

        // 更新主表
        order.setOrderDate(dto.getOrderDate());
        order.setCustomerId(dto.getCustomerId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setSalesmanId(dto.getSalesmanId());
        order.setDeptId(dto.getDeptId());
        order.setProductAmount(dto.getProductAmount());
        order.setDiscountAmount(dto.getDiscountAmount());
        // 促销引擎算出的两项优惠必须落库（否则 billAmount 里减了、列里却是 0，对账与毛利分析失真）
        order.setPromoDiscount(dto.getPromoDiscount() != null ? dto.getPromoDiscount() : BigDecimal.ZERO);
        order.setCouponAmount(dto.getCouponAmount() != null ? dto.getCouponAmount() : BigDecimal.ZERO);
        order.setBillAmount(dto.getBillAmount());
        order.setTotalQuantity(dto.getTotalQuantity());
        order.setRemark(dto.getRemark());
        if (dto.getBuyerRemark() != null) order.setBuyerRemark(dto.getBuyerRemark());
        if (dto.getOrderRemark() != null) order.setOrderRemark(dto.getOrderRemark());
        if (dto.getReceiverName() != null) order.setReceiverName(dto.getReceiverName());
        if (dto.getReceiverPhone() != null) order.setReceiverPhone(dto.getReceiverPhone());
        if (dto.getShippingAddress() != null) order.setShippingAddress(dto.getShippingAddress());
        if (dto.getLogisticsCompany() != null) order.setLogisticsCompany(dto.getLogisticsCompany());
        if (dto.getFreightPayer() != null) order.setFreightPayer(dto.getFreightPayer());
        if (dto.getDeliveryMethod() != null) order.setDeliveryMethod(dto.getDeliveryMethod());
        if (dto.getDeliveryRoute() != null) order.setDeliveryRoute(dto.getDeliveryRoute());
        if (dto.getSettlementMethod() != null) order.setSettlementMethod(dto.getSettlementMethod());
        if (dto.getRegion() != null) order.setRegion(dto.getRegion());
        if (dto.getSummary() != null) order.setSummary(dto.getSummary());
        if (dto.getShippingFee() != null) order.setShippingFee(dto.getShippingFee());
        updateById(order);

        // 更新子表 (先删后增)
        deleteSubTables(order.getId());
        saveSubTables(order.getId(), dto);

        // 更新明细。
        //
        // ⚠️ 2026-09-21 修复「明细被静默删光」（SAL-BREAK-02 顺带查出）：
        //    原实现是「**无条件**删光旧明细 + **有条件**重建（dto.getItems() != null）」——
        //    删与建的条件不一致，于是任何一次**不带明细的更新**（只改表头字段的保存、
        //    局部字段补写、自动化脚本的部分字段 PUT）都会把该单**全部明细删掉且不补回**，
        //    而主表照常更新 ⇒ 表现为「订单在、明细没了、金额还在」（主表金额来自 dto，不会被清）。
        //    现在把删除放进同一个条件里，并明确两种语义：
        //      · items == null   → 「本次不改明细」，保留原样（局部更新）
        //      · items == []     → 「显式清空明细」（调用方确实要清）
        //      · items 非空      → 先删后插，行号重排
        if (dto.getItems() != null) {
            List<SaleOrderItem> oldItems = itemMapper.selectByOrderId(dto.getId());
            oldItems.forEach(item -> itemMapper.deleteById(item.getId()));
            int lineNo = 1;
            for (SaleOrderItemDTO itemDTO : dto.getItems()) {
                SaleOrderItem item = createOrderItem(itemDTO, order.getId(), dto.getCustomerId(), dto.getWarehouseId());
                item.setLineNo(lineNo++);
                itemMapper.insert(item);
            }
        }

        // 优惠分摊明细重算落库（先清后插，改单幂等）
        savePromotionDetails(order, dto);
        log.info("更新销售订单: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() > 1) throw BusinessException.badRequest("只有草稿和待审批状态的订单可以删除");

        deleteSubTables(id);
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        items.forEach(item -> itemMapper.deleteById(item.getId()));
        removeById(id);
        // 券回滚：订单删除后券退回「已领取未使用」，否则券会被永久占用
        couponRedemptionService.rollbackByOrder(id);
        log.info("删除销售订单: orderId={}", id);
    }

    // ═══════════════════════════════════════════
    // 审批/状态流转
    // ═══════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的订单可以提交审批");

        // 更新状态 + 提交人信息
        order.setStatus(1);
        order.setSubmitTime(LocalDateTime.now());
        order.setSubmitterId(StpUtil.getLoginIdAsLong());
        order.setSubmitterName(getCurrentUserName());
        updateById(order);

        // 记录审核流水
        saveAuditTrail(id, "SUBMIT", StpUtil.getLoginIdAsLong(), getCurrentUserName(), null);

        // 影子模式：并行发起工作流引擎实例（不回写单据状态；无引擎/失败仅记日志不阻断）
        startShadowApproval(order);

        log.info("提交销售订单审批: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long auditorId) {
        approve(id, auditorId, getCurrentUserName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long auditorId, String auditorName) {
        // 工作流回调线程无 Sa-Token 会话，getCurrentUserName() 会落"系统"：
        // 调用方显式传入的终审人姓名优先，缺省回退当前会话用户名
        String resolvedAuditorName = (auditorName != null && !auditorName.isBlank())
                ? auditorName : getCurrentUserName();
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 1) throw BusinessException.badRequest("订单不是待审批状态");

        // 第三期（影子转正式）：存在在途引擎实例时由引擎驱动审批，终态经回调回写单据。
        // 走引擎前先做与直接审批一致的前置校验（库存可用性，仅校验不冻结——
        // 冻结由回调里的 approve 完成），校验失败则不发起引擎审批。
        ApprovalFacade.InFlightInstance inFlight = findInFlightApproval(id);
        if (inFlight != null) {
            List<String> shortages = checkStockAvailability(order);
            if (!shortages.isEmpty()) {
                throw BusinessException.badRequest("库存不足，无法审批通过：\n" + String.join("\n", shortages));
            }
            if (driveEngineApproval(inFlight, "approve", auditorId, null)) {
                log.info("销售订单审批经引擎驱动(终态由回调回写): orderId={}, instanceId={}", id, inFlight.getInstanceId());
                return;
            }
            log.warn("引擎驱动审批失败，回退直接审批: orderId={}, instanceId={}", id, inFlight.getInstanceId());
        }

        // 审批通过前校验库存可用性并冻结
        List<String> stockShortages = checkAndFreezeStock(order);
        if (!stockShortages.isEmpty()) {
            throw BusinessException.badRequest("库存不足，无法审批通过：\n" + String.join("\n", stockShortages));
        }

        // 更新状态 + 审核人信息
        order.setStatus(2);
        order.setAuditorId(auditorId);
        order.setAuditorName(resolvedAuditorName);
        order.setAuditTime(LocalDateTime.now());
        updateById(order);

        // 审核通过后增加客户欠款
        if (order.getCustomerId() != null && order.getBillAmount() != null && order.getBillAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                Party party = partyMapper.selectById(order.getCustomerId());
                if (party != null) {
                    BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                    party.setCurrentDebt(currentDebt.add(order.getBillAmount()));
                    partyMapper.updateById(party);
                    log.info("审核通过增加客户欠款: customerId={}, {} + {} = {}",
                            order.getCustomerId(), currentDebt, order.getBillAmount(), currentDebt.add(order.getBillAmount()));
                }
            } catch (Exception e) {
                log.warn("更新客户欠款失败: customerId={}", order.getCustomerId(), e);
            }
        }

        // 记录审核流水
        saveAuditTrail(id, "APPROVE", auditorId, resolvedAuditorName, null);
        log.info("销售订单审批通过: orderId={}, auditorId={}", id, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long auditorId, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 1) throw BusinessException.badRequest("订单不是待审批状态");

        // 第三期（影子转正式）：存在在途引擎实例时由引擎驱动驳回（终态由回调回写）
        ApprovalFacade.InFlightInstance inFlight = findInFlightApproval(id);
        if (inFlight != null && driveEngineApproval(inFlight, "reject", auditorId, reason)) {
            log.info("销售订单驳回经引擎驱动(终态由回调回写): orderId={}, instanceId={}", id, inFlight.getInstanceId());
            return;
        }

        // 拒绝回退到草稿
        order.setStatus(0);
        order.setAuditorId(auditorId);
        order.setAuditorName(getCurrentUserName());
        order.setAuditTime(LocalDateTime.now());
        updateById(order);

        saveAuditTrail(id, "REJECT", auditorId, getCurrentUserName(), reason);
        log.info("销售订单审批拒绝: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() >= 4) throw BusinessException.badRequest("已完成的订单不能取消");

        // 如果已审核通过（status >= 2），取消时需要解冻库存
        if (order.getStatus() != null && order.getStatus() >= 2) {
            unfreezeOrderStock(order);
        }

        // 如果已审核通过（status >= 2），取消时需要减少客户欠款
        if (order.getStatus() != null && order.getStatus() >= 2
                && order.getCustomerId() != null && order.getBillAmount() != null) {
            try {
                Party party = partyMapper.selectById(order.getCustomerId());
                if (party != null) {
                    BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                    BigDecimal newDebt = currentDebt.subtract(order.getBillAmount()).max(BigDecimal.ZERO);
                    party.setCurrentDebt(newDebt);
                    partyMapper.updateById(party);
                    log.info("取消订单减少客户欠款: customerId={}, {} - {} = {}",
                            order.getCustomerId(), currentDebt, order.getBillAmount(), newDebt);
                }
            } catch (Exception e) {
                log.warn("取消订单更新欠款失败: customerId={}", order.getCustomerId(), e);
            }
        }

        orderMapper.updateStatus(id, 6);

        saveAuditTrail(id, "CANCEL", StpUtil.getLoginIdAsLong(), getCurrentUserName(), reason);
        log.info("取消销售订单: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShipment(Long id, Long warehouseId) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 2 && order.getStatus() != 3)
            throw BusinessException.badRequest("订单状态不允许出库");

        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        BigDecimal totalShipped = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (SaleOrderItem item : items) {
            // 先解冻（审批时已冻结），再扣减：净效果为总库存减少、冻结归零
            try {
                stockService.unfreezeStock(item.getProductId(), warehouseId, item.getQuantity());
                stockService.decreaseStock(item.getProductId(), warehouseId, item.getQuantity());
            } catch (Exception e) {
                log.error("库存扣减异常: productId={}", item.getProductId(), e);
            }
            itemMapper.updateShippedQuantity(item.getId(), item.getQuantity());
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
            totalShipped = totalShipped.add(qty);
        }

        // 更新主表数量汇总
        order.setShippedQuantity(order.getShippedQuantity() != null ? order.getShippedQuantity().add(totalShipped) : totalShipped);
        order.setUnshippedQuantity(totalQty.subtract(order.getShippedQuantity() != null ? order.getShippedQuantity() : BigDecimal.ZERO));
        if (order.getUnshippedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            order.setUnshippedQuantity(BigDecimal.ZERO);
        }

        List<SaleOrderItem> updatedItems = itemMapper.selectByOrderId(id);
        // 已发数量以 shipped_quantity 为准（shipped_quantity_detail 为历史遗留列，本流程不写）
        boolean allShipped = updatedItems.stream().allMatch(item -> {
            BigDecimal shipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            return shipped.compareTo(qty) >= 0;
        });
        order.setStatus(allShipped ? 4 : 3);
        updateById(order);

        // 生成销售出库单（业务闭环：出库查询/价格跟踪数据来源）
        try {
            cn.aiedge.erp.sale.outbound.service.SaleOutboundService outboundService = outboundServiceProvider.getIfAvailable();
            if (outboundService != null) {
                outboundService.createFromOrder(id);
                log.info("销售出库单已生成: orderId={}", id);
            }
        } catch (Exception e) {
            log.error("销售出库单生成失败: orderId={}", id, e);
        }

        // 包裹层（P1）：发货 → 包裹置「已发货」
        try {
            saleLogisticsService.markPackagesShipped(id);
        } catch (Exception e) {
            log.warn("包裹状态回写跳过: orderId={}, err={}", id, e.getMessage());
        }
        // 发货通知 ASN（P2-6）：按 订单+运单号 幂等生成台账
        try {
            int created = saleLogisticsService.createNotifyForOrder(id);
            if (created > 0) {
                log.info("发货通知(ASN)已生成: orderId={}, count={}", id, created);
            }
        } catch (Exception e) {
            log.warn("发货通知生成失败: orderId={}, err={}", id, e.getMessage());
        }

        log.info("确认销售订单出库: orderId={}, allShipped={}", id, allShipped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(Long id, BigDecimal amount) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        orderMapper.addReceivedAmount(id, amount);
        // 同步更新客户欠款（收款减少欠款）
        if (order.getCustomerId() != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                Party party = partyMapper.selectById(order.getCustomerId());
                if (party != null) {
                    BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                    BigDecimal newDebt = currentDebt.subtract(amount).max(BigDecimal.ZERO);
                    party.setCurrentDebt(newDebt);
                    partyMapper.updateById(party);
                    log.info("更新客户欠款: customerId={}, {} - {} = {}", order.getCustomerId(), currentDebt, amount, newDebt);
                }
            } catch (Exception e) {
                log.warn("更新客户欠款失败: customerId={}", order.getCustomerId(), e);
            }
        }
        log.info("记录销售订单收款: orderId={}, amount={}", id, amount);
    }

    @Override
    public List<SaleOrderListDTO> getPendingOrders(Long tenantId) {
        List<SaleOrder> orders = orderMapper.selectPendingOrders(tenantId);
        Set<Long> customerIds = orders.stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);
        return orders.stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public String generateOrderNo() {
        return bizNumberGeneratorService.nextSaleOrderNo();
    }

    @Override
    public void calculateAmount(SaleOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) return;

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (SaleOrderItemDTO item : dto.getItems()) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getCalculatedPrice() != null ? item.getCalculatedPrice() :
                    (item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
            BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;

            BigDecimal amount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = amount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            item.setAmount(amount);
            item.setTaxAmount(tax);
            item.setAmountWithTax(amount.add(tax));

            totalAmount = totalAmount.add(amount);
            totalQty = totalQty.add(qty);
        }

        dto.setProductAmount(totalAmount);
        dto.setTotalQuantity(totalQty);

        BigDecimal otherFee = dto.getOtherFee() != null ? dto.getOtherFee() : BigDecimal.ZERO;
        BigDecimal discountAmt = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingFee = dto.getShippingFee() != null ? dto.getShippingFee() : BigDecimal.ZERO;
        BigDecimal directDiscount = dto.getDirectDiscount() != null ? dto.getDirectDiscount() : BigDecimal.ZERO;

        // ── 促销与优惠券：由**服务端促销引擎**计算，不再信任前端传参 ──
        //    （此前 promoDiscount/couponAmount 直接取前端值 ⇒ 促销活动/券配置完不生效）
        cn.aiedge.erp.marketing.promotion.PromotionResult promo = evaluatePromotion(dto, totalAmount);
        BigDecimal promoDiscount = promo.getPromoDiscount();
        BigDecimal couponAmount = promo.getCouponDiscount();
        this.lastPromotionResult.set(promo);

        dto.setProductAmount(totalAmount);
        dto.setPromoDiscount(promoDiscount);
        dto.setCouponAmount(couponAmount);

        BigDecimal billAmount = totalAmount.add(otherFee).subtract(discountAmt)
                .subtract(promoDiscount).subtract(couponAmount).subtract(directDiscount).add(shippingFee);
        if (billAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.badRequest(
                    "优惠合计（促销 " + promoDiscount + " + 券 " + couponAmount + " + 让价 " + directDiscount
                            + "）超过商品金额，请调整优惠后重试");
        }
        dto.setBillAmount(billAmount);
    }

    /** 组装促销引擎上下文并求解（纯计算；命中/跳过原因写入日志便于排障） */
    private cn.aiedge.erp.marketing.promotion.PromotionResult evaluatePromotion(SaleOrderDTO dto, BigDecimal totalAmount) {
        try {
            cn.aiedge.erp.marketing.promotion.PromotionRequest req =
                    new cn.aiedge.erp.marketing.promotion.PromotionRequest();
            req.setTenantId(dto.getTenantId());
            req.setCustomerId(dto.getCustomerId());
            req.setOrderDate(dto.getOrderDate());
            // 商城来源（orderSource>0 视为商城下单，线下开单为 0 / null）
            req.setChannel(dto.getOrderSource() != null && dto.getOrderSource() > 0 ? "MALL" : "OFFLINE");
            req.setCouponIds(dto.getCouponIds());
            List<cn.aiedge.erp.marketing.promotion.CartLine> lines = new ArrayList<>();
            for (SaleOrderItemDTO it : dto.getItems()) {
                cn.aiedge.erp.marketing.promotion.CartLine line =
                        new cn.aiedge.erp.marketing.promotion.CartLine();
                line.setLineNo(it.getLineNo());
                line.setProductId(it.getProductId());
                line.setProductName(it.getProductName());
                line.setQuantity(it.getQuantity());
                line.setUnitPrice(it.getCalculatedPrice() != null ? it.getCalculatedPrice() : it.getUnitPrice());
                lines.add(line);
            }
            req.setLines(lines);
            cn.aiedge.erp.marketing.promotion.PromotionResult r = promotionEngine.evaluate(req);
            if (!r.getNotes().isEmpty() || !r.getSkipped().isEmpty()) {
                log.info("促销引擎：订单 {} 命中活动 {}（促销 {} / 券 {}）；说明={}；跳过={}",
                        dto.getOrderNo(), r.getAppliedPromotionIds(), r.getPromoDiscount(), r.getCouponDiscount(),
                        r.getNotes(), r.getSkipped());
            }
            return r;
        } catch (Exception e) {
            // 促销计算失败不得阻断开单：降级为"无促销优惠"并告警（金额以无优惠为准，不会多算）
            log.error("促销引擎计算失败，本单按无促销优惠处理：orderNo={}", dto.getOrderNo(), e);
            return new cn.aiedge.erp.marketing.promotion.PromotionResult();
        }
    }

    /** 落库优惠分摊明细（先清后插，保证改单幂等） */
    private void savePromotionDetails(SaleOrder order, SaleOrderDTO dto) {
        try {
            promoDetailMapper.delete(new LambdaQueryWrapper<SaleOrderPromoDetail>()
                    .eq(SaleOrderPromoDetail::getOrderId, order.getId()));
            cn.aiedge.erp.marketing.promotion.PromotionResult r = this.lastPromotionResult.get();
            if (r == null || r.getAllocations().isEmpty()) return;
            LocalDateTime now = LocalDateTime.now();
            for (cn.aiedge.erp.marketing.promotion.PromotionAllocation a : r.getAllocations()) {
                promoDetailMapper.insert(new SaleOrderPromoDetail()
                        .setTenantId(order.getTenantId())
                        .setOrderId(order.getId())
                        .setOrderNo(order.getOrderNo())
                        .setPromotionId(a.getPromotionId())
                        .setPromotionName(a.getPromotionName())
                        .setPromoMode(a.getPromoMode())
                        .setScopeType(a.getScopeType())
                        .setLineNo(a.getLineNo())
                        .setProductId(a.getProductId())
                        .setDiscountAmount(a.getDiscountAmount())
                        .setCouponId(a.getCouponId())
                        .setCouponCode(a.getCouponCode())
                        .setGiftProductId(a.getGiftProductId())
                        .setGiftQuantity(a.getGiftQuantity())
                        .setRemark(a.getRemark())
                        .setDeleted(0)
                        .setCreateTime(now));
            }
            // 活动已用次数累加（次数上限控制的数据源；试算不累加，只有落单才累加）
            promotionEngine.recordUsage(r.getAppliedPromotionIds());
        } finally {
            this.lastPromotionResult.remove();
        }
    }

    @Override
    public Map<String, Object> getOrderStats(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingProcessCount", orderMapper.countPendingProcess(tenantId));
        stats.put("pendingApprovalCount", orderMapper.countPendingApproval(tenantId));
        stats.put("todayOrderCount", orderMapper.countTodayOrders(tenantId));
        stats.put("monthOrderCount", orderMapper.countMonthOrders(tenantId));
        return stats;
    }

    // ═══════════════════════════════════════════
    // 订单处理中心 - 多tab查询
    // ═══════════════════════════════════════════

    @Override
    public Page<SaleOrderListDTO> orderCenterPageByDoc(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = buildOrderCenterWrapper(tenantId, filters);
        wrapper.orderByDesc(SaleOrder::getOrderDate).orderByDesc(SaleOrder::getCreateTime);

        Page<SaleOrder> result = page(page, wrapper);
        Set<Long> customerIds = result.getRecords().stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);

        Page<SaleOrderListDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        fillLineCount(dtoPage.getRecords());
        return dtoPage;
    }

    @Override
    public List<Map<String, Object>> orderCenterGroupByDate(Long tenantId, Map<String, Object> filters) {
        List<SaleOrder> orders = list(buildOrderCenterWrapper(tenantId, filters));
        Map<LocalDate, List<SaleOrder>> grouped = orders.stream().collect(Collectors.groupingBy(SaleOrder::getOrderDate));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((date, dayOrders) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderDay", date.toString());
            row.put("totalOrders", dayOrders.size());
            row.put("totalAmount", dayOrders.stream().map(o -> o.getBillAmount() != null ? o.getBillAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("pendingReviewCount", dayOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
            row.put("pendingOutboundCount", dayOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
            row.put("pendingShipCount", dayOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 2 || o.getStatus() == 3)).count());
            row.put("outboundCount", dayOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 3 || o.getStatus() == 4 || o.getStatus() == 5)).count());
            row.put("shippedCount", dayOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
            row.put("completedCount", dayOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 5).count());
            result.add(row);
        });
        result.sort((a, b) -> b.get("orderDay").toString().compareTo(a.get("orderDay").toString()));
        return result;
    }

    @Override
    public List<Map<String, Object>> orderCenterGroupByRoute(Long tenantId, Map<String, Object> filters) {
        List<SaleOrder> orders = list(buildOrderCenterWrapper(tenantId, filters));
        Set<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toSet());

        // 批量查询物流信息获取配送线路
        final Map<Long, String> routeMap;
        if (!orderIds.isEmpty()) {
            List<SaleOrderLogistics> logisticsList = logisticsMapper.selectList(
                    new LambdaQueryWrapper<SaleOrderLogistics>().in(SaleOrderLogistics::getOrderId, orderIds));
            routeMap = logisticsList.stream().collect(Collectors.toMap(
                    SaleOrderLogistics::getOrderId,
                    l -> l.getDeliveryRoute() != null ? l.getDeliveryRoute() : "无线路",
                    (a, b) -> a));
        } else {
            routeMap = java.util.Collections.emptyMap();
        }

        Map<String, List<SaleOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o -> routeMap.getOrDefault(o.getId(), "无线路")));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((route, routeOrders) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deliveryRoute", route);
            row.put("totalOrders", routeOrders.size());
            row.put("totalAmount", routeOrders.stream().map(o -> o.getBillAmount() != null ? o.getBillAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("pendingReviewCount", routeOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
            row.put("pendingOutboundCount", routeOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
            row.put("pendingShipCount", routeOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 2 || o.getStatus() == 3)).count());
            row.put("outboundCount", routeOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 3 || o.getStatus() == 4 || o.getStatus() == 5)).count());
            row.put("shippedCount", routeOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
            row.put("completedCount", routeOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 5).count());
            result.add(row);
        });
        return result;
    }

    @Override
    public List<Map<String, Object>> orderCenterGroupByCustomer(Page<?> page, Long tenantId, Map<String, Object> filters) {
        List<SaleOrder> orders = list(buildOrderCenterWrapper(tenantId, filters));
        Map<Long, List<SaleOrder>> grouped = orders.stream().collect(Collectors.groupingBy(SaleOrder::getCustomerId));

        Set<Long> customerIds = grouped.keySet();
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((custId, custOrders) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerId", custId);
            row.put("customerName", nameMap.getOrDefault(custId, ""));
            row.put("totalOrders", custOrders.size());
            row.put("totalAmount", custOrders.stream().map(o -> o.getBillAmount() != null ? o.getBillAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("pendingReviewCount", custOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
            row.put("pendingOutboundCount", custOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
            row.put("pendingShipCount", custOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 2 || o.getStatus() == 3)).count());
            row.put("outboundCount", custOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 3 || o.getStatus() == 4 || o.getStatus() == 5)).count());
            row.put("shippedCount", custOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
            row.put("completedCount", custOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 5).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((long) b.get("totalOrders"), (long) a.get("totalOrders")));
        return result;
    }

    @Override
    public Page<SaleOrderListDTO> orderCenterFulfillmentPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = buildOrderCenterWrapper(tenantId, filters);
        wrapper.isNotNull(SaleOrder::getOriginalOrderId);
        wrapper.orderByDesc(SaleOrder::getOrderDate);

        Page<SaleOrder> result = page(page, wrapper);
        Set<Long> customerIds = result.getRecords().stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);

        Page<SaleOrderListDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        fillLineCount(dtoPage.getRecords());
        return dtoPage;
    }

    @Override
    public Map<String, Object> orderCenterFulfillmentOverview(Long tenantId, Map<String, Object> filters) {
        List<SaleOrder> orders = list(buildOrderCenterWrapper(tenantId, filters));
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalOrders", (long) orders.size());
        overview.put("pendingOutboundCount", orders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 2 || o.getStatus() == 3)).count());
        overview.put("outboundCount", orders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 4 || o.getStatus() == 5)).count());
        // 已完成订单的营收汇总
        BigDecimal fulfilledRevenue = orders.stream()
                .filter(o -> o.getStatus() != null && (o.getStatus() == 4 || o.getStatus() == 5))
                .map(o -> o.getBillAmount() != null ? o.getBillAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("fulfilledRevenue", fulfilledRevenue);
        return overview;
    }

    @Override
    public Map<String, Object> orderCenterStats(Long tenantId, Map<String, Object> filters) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总数
        LambdaQueryWrapper<SaleOrder> base = buildOrderCenterWrapper(tenantId, filters);
        stats.put("totalOrders", count(base));

        // 待审核 (status = 1)
        LambdaQueryWrapper<SaleOrder> w1 = buildOrderCenterWrapper(tenantId, filters);
        w1.eq(SaleOrder::getStatus, 1);
        stats.put("pendingReview", count(w1));

        // 待出库 (status = 2 待发货)
        LambdaQueryWrapper<SaleOrder> w2 = buildOrderCenterWrapper(tenantId, filters);
        w2.eq(SaleOrder::getStatus, 2);
        stats.put("pendingOutbound", count(w2));

        // 待发货 (status = 2 或 3)
        LambdaQueryWrapper<SaleOrder> wPending = buildOrderCenterWrapper(tenantId, filters);
        wPending.in(SaleOrder::getStatus, 2, 3);
        stats.put("pendingShip", count(wPending));

        // 已出库 (status >= 3 部分发货/发货完成/交易完成)
        LambdaQueryWrapper<SaleOrder> w3 = buildOrderCenterWrapper(tenantId, filters);
        w3.in(SaleOrder::getStatus, 3, 4, 5);
        stats.put("outboundCount", count(w3));

        // 发货完成 (status = 4)
        LambdaQueryWrapper<SaleOrder> w4 = buildOrderCenterWrapper(tenantId, filters);
        w4.eq(SaleOrder::getStatus, 4);
        stats.put("shippedCount", count(w4));

        // 交易完成 (status = 5)
        LambdaQueryWrapper<SaleOrder> w5 = buildOrderCenterWrapper(tenantId, filters);
        w5.eq(SaleOrder::getStatus, 5);
        stats.put("completedCount", count(w5));

        return stats;
    }

    @Override
    public Page<SaleOrderListDTO> pendingReviewPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = buildOrderCenterWrapper(tenantId, filters);
        wrapper.eq(SaleOrder::getStatus, 1);
        wrapper.orderByDesc(SaleOrder::getCreateTime);

        Page<SaleOrder> result = page(page, wrapper);
        Set<Long> customerIds = result.getRecords().stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);

        Page<SaleOrderListDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        fillLineCount(dtoPage.getRecords());
        return dtoPage;
    }

    @Override
    public Page<SaleOrderListDTO> pickingShippingPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = buildOrderCenterWrapper(tenantId, filters);
        wrapper.in(SaleOrder::getStatus, 2, 3);
        wrapper.orderByDesc(SaleOrder::getOrderDate);

        Page<SaleOrder> result = page(page, wrapper);
        Set<Long> customerIds = result.getRecords().stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);

        Page<SaleOrderListDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        fillLineCount(dtoPage.getRecords());
        return dtoPage;
    }

    @Override
    public Map<String, Object> pickingShippingSummary(Long tenantId, Map<String, Object> filters) {
        // 与列表同源查询条件（口径唯一），只取两个数值列做全量汇总，避免口径漂移
        LambdaQueryWrapper<SaleOrder> wrapper = buildOrderCenterWrapper(tenantId, filters);
        wrapper.in(SaleOrder::getStatus, 2, 3);
        wrapper.select(SaleOrder::getProductAmount, SaleOrder::getTotalQuantity);
        List<SaleOrder> rows = list(wrapper);
        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (SaleOrder o : rows) {
            productAmount = productAmount.add(o.getProductAmount() != null ? o.getProductAmount() : BigDecimal.ZERO);
            totalQuantity = totalQuantity.add(o.getTotalQuantity() != null ? o.getTotalQuantity() : BigDecimal.ZERO);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("productAmount", productAmount);
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalOrders", (long) rows.size());
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePicking(Long id) {
        doCompletePicking(id);
    }

    @Override
    public int batchCompletePicking(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int done = 0;
        for (Long id : ids) {
            if (id == null) continue;
            try {
                doCompletePicking(id);
                done++;
            } catch (Exception e) {
                // 逐单尽力而为：单张状态不允许时跳过，不阻断整批
                log.warn("批量拣货完成跳过 orderId={}: {}", id, e.getMessage());
            }
        }
        return done;
    }

    /** 拣货完成内核：明细 picked_quantity 回写 + 主表汇总 */
    private void doCompletePicking(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() == null || (order.getStatus() != 2 && order.getStatus() != 3)) {
            throw BusinessException.badRequest("订单状态不允许拣货");
        }
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        BigDecimal totalPicked = BigDecimal.ZERO;
        for (SaleOrderItem item : items) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            // 已发货部分不再需要拣货：拣货目标 = 订货数量 − 已发货数量（不小于 0）
            BigDecimal shipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
            BigDecimal target = qty.subtract(shipped).max(BigDecimal.ZERO);
            itemMapper.updatePickedQuantity(item.getId(), target);
            totalPicked = totalPicked.add(target);
        }
        order.setPickedQuantity(totalPicked);
        // 拣货仓库未指定时以订单仓库兜底，保证拣货作业有据可依
        if (order.getPickingWarehouse() == null || order.getPickingWarehouse().isEmpty()) {
            order.setPickingWarehouse(order.getWarehouseName());
        }
        updateById(order);
        log.info("拣货完成: orderId={}, pickedQuantity={}", id, totalPicked);
    }

    @Override
    public List<SaleOrderListDTO> exportList(String keyword, Long customerId, Integer status) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null && !keyword.isEmpty(), SaleOrder::getOrderNo, keyword)
               .eq(customerId != null, SaleOrder::getCustomerId, customerId)
               .eq(status != null, SaleOrder::getStatus, status)
               .orderByDesc(SaleOrder::getCreateTime);
        List<SaleOrder> orders = list(wrapper);
        Set<Long> customerIds = orders.stream().map(SaleOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchGetCustomerNames(customerIds);
        return orders.stream().map(o -> {
            SaleOrderListDTO dto = convertToListDTO(o);
            dto.setCustomerName(nameMap.getOrDefault(o.getCustomerId(), ""));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchImport(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new IllegalArgumentException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }

        Long tenantId = getCurrentTenantId();
        if (tenantId == null) throw BusinessException.badRequest("无法获取租户信息");

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Excel 文件中没有工作表");

            int rowCount = sheet.getLastRowNum();
            if (rowCount < 1) throw new IllegalArgumentException("Excel 文件没有数据行（至少需要表头+1行数据）");

            // 解析表头
            Row headerRow = sheet.getRow(0);
            Map<Integer, String> headers = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = getCellStringValue(cell);
                    if (!header.isEmpty()) {
                        headers.put(i, header);
                    }
                }
            }

            if (headers.isEmpty()) throw new IllegalArgumentException("Excel 文件缺少表头行");

            // 按单据编号分组行数据（第一列默认为单据编号）
            Map<String, List<Map<String, Object>>> orderGroups = new LinkedHashMap<>();
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMsg = new StringBuilder();

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                    rowData.put(entry.getValue(), getCellStringValue(row.getCell(entry.getKey())));
                }

                String orderNo = rowData.getOrDefault("单据编号", rowData.getOrDefault("orderNo", "")).toString();
                if (orderNo.isEmpty()) {
                    failCount++;
                    errorMsg.append("第").append(i + 1).append("行缺少单据编号\n");
                    continue;
                }

                orderGroups.computeIfAbsent(orderNo, k -> new ArrayList<>()).add(rowData);
                successCount++;
            }

            if (orderGroups.isEmpty()) {
                throw new IllegalArgumentException("没有可导入的有效数据");
            }

            // 创建订单
            int orderCount = 0;
            for (Map.Entry<String, List<Map<String, Object>>> group : orderGroups.entrySet()) {
                String orderNo = group.getKey();
                List<Map<String, Object>> rows = group.getValue();

                // 查找或创建订单（主体字段取该单第一行——同一单据号的各行本应同客户/同仓库）
                SaleOrder order = findOrCreateOrder(orderNo, tenantId, rows.get(0));
                orderCount++;

                // 创建订单明细
                for (Map<String, Object> rowData : rows) {
                    SaleOrderItem item = new SaleOrderItem();
                    item.setOrderId(order.getId());
                    item.setProductName(getString(rowData, "productName", "商品名称"));
                    item.setProductCode(getString(rowData, "productCode", "商品编码"));
                    item.setBarcode(getString(rowData, "barcode", "条码"));
                    item.setUnit(getString(rowData, "unit", "单位"));
                    item.setSpecification(getString(rowData, "specification", "规格"));

                    BigDecimal quantity = getBigDecimal(rowData, "quantity", "数量");
                    item.setQuantity(quantity);
                    BigDecimal price = getBigDecimal(rowData, "price", "单价");
                    item.setUnitPrice(price);
                    if (quantity != null && price != null) {
                        item.setAmount(quantity.multiply(price).setScale(2, RoundingMode.HALF_UP));
                    }
                    item.setLineNo(rows.indexOf(rowData) + 1);
                    item.setCreateTime(LocalDateTime.now());
                    itemMapper.insert(item);
                }

                // 重新计算订单金额
                recalculateOrderAmount(order.getId());
            }

            log.info("批量导入完成: 创建{}个订单, {}条明细, 失败{}条", orderCount, successCount, failCount);
            if (errorMsg.length() > 0) {
                throw BusinessException.badRequest("导入部分完成：" + successCount + "条成功，" + failCount + "条失败。\n" + errorMsg);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量导入异常", e);
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找或创建导入订单。
     *
     * <p><b>2026-09-21 修复（SAL-BREAK-03）</b>：原实现只写 orderNo/orderDate/saleType/status/
     * generationMethod 与三个 0，**客户、仓库、业务员、部门一个都没写** —— 导入单建出来即无主体，
     * 出库/对账带不出往来单位、订单中心的仓库筛选恒无结果。现从该单第一行解析这四项。</p>
     *
     * <p>四个字段都是**可选列**：Excel 没有这些表头就照旧留空（不报错），
     * 名称解析不到对应主数据也留空（不报错）—— 导入的职责是把能认出来的写上，
     * 不该因为一个仓库名拼错就整单导入失败。</p>
     */
    private SaleOrder findOrCreateOrder(String orderNo, Long tenantId, Map<String, Object> firstRow) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrder::getOrderNo, orderNo);
        SaleOrder existing = getOne(wrapper, false);
        if (existing != null) return existing;

        SaleOrder order = new SaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setOrderDate(LocalDate.now());
        order.setSaleType(1);
        order.setStatus(0);
        order.setGenerationMethod("导入");
        order.setProductAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setBillAmount(BigDecimal.ZERO);
        order.setTotalQuantity(BigDecimal.ZERO);
        order.setPrintCount(0);
        order.setCreatorName("导入");
        applyImportedHeaderFields(order, firstRow);
        save(order);
        return order;
    }

    /**
     * 把导入行里的「客户 / 仓库 / 业务员 / 部门」解析成主数据 ID 写到订单上（SAL-BREAK-03）。
     *
     * <p>表头别名两套（英文驼峰 + 中文），因为导入模板由用户自己准备、没有随代码发布的模板文件。
     * 解析不到就跳过该字段（留空），不抛异常。</p>
     */
    private void applyImportedHeaderFields(SaleOrder order, Map<String, Object> rowData) {
        if (rowData == null || rowData.isEmpty()) {
            return;
        }

        // ── 客户：编码优先，其次名称（两者都没有则完全不查，避免无条件的 selectOne 取到任意一行） ──
        String customerCode = firstNonBlank(getString(rowData, "customerCode", "客户编码"),
                getString(rowData, "partyCode", "客户编码"));
        String customerName = firstNonBlank(getString(rowData, "customerName", "客户名称"),
                getString(rowData, "partyName", "客户"), getString(rowData, "customer", "客户"));
        if (customerCode != null || customerName != null) {
            Party party = partyMapper.selectOne(new LambdaQueryWrapper<Party>()
                    .eq(customerCode != null, Party::getPartyCode, customerCode)
                    .eq(customerName != null, Party::getPartyName, customerName)
                    .last("LIMIT 1"));
            if (party != null) {
                order.setCustomerId(party.getId());
                order.setCustomerName(party.getPartyName());
                order.setCustomerCode(party.getPartyCode());
            }
        }

        // ── 仓库：按名称（erp_warehouse.warehouse_name） ──
        String warehouseName = firstNonBlank(getString(rowData, "warehouseName", "仓库名称"),
                getString(rowData, "warehouse", "仓库"));
        if (warehouseName != null) {
            cn.aiedge.erp.stock.entity.Warehouse warehouse = warehouseMapper.selectOne(
                    new LambdaQueryWrapper<cn.aiedge.erp.stock.entity.Warehouse>()
                            .eq(cn.aiedge.erp.stock.entity.Warehouse::getWarehouseName, warehouseName)
                            .last("LIMIT 1"));
            if (warehouse != null) {
                order.setWarehouseId(warehouse.getId());
            }
        }

        // ── 业务员：登录名或姓名都可以（模板里人写的是姓名，系统里存的是 username） ──
        String salesmanName = firstNonBlank(getString(rowData, "salesmanName", "业务员"),
                getString(rowData, "salesman", "业务员"));
        if (salesmanName != null) {
            cn.aiedge.base.entity.SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<cn.aiedge.base.entity.SysUser>()
                            .and(w -> w.eq(cn.aiedge.base.entity.SysUser::getUsername, salesmanName)
                                    .or().eq(cn.aiedge.base.entity.SysUser::getRealName, salesmanName))
                            .last("LIMIT 1"));
            if (user != null) {
                order.setSalesmanId(user.getId());
            }
        }

        // ── 部门 ──
        String deptName = firstNonBlank(getString(rowData, "deptName", "部门"),
                getString(rowData, "departmentName", "部门"));
        if (deptName != null) {
            cn.aiedge.base.entity.SysDept dept = sysDeptMapper.selectOne(
                    new LambdaQueryWrapper<cn.aiedge.base.entity.SysDept>()
                            .eq(cn.aiedge.base.entity.SysDept::getDeptName, deptName)
                            .last("LIMIT 1"));
            if (dept != null) {
                order.setDeptId(dept.getId());
            }
        }
    }

    /** 取第一个非空白值（全为空白/ null 时返回 null） */
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    /** 重新计算订单金额 */
    private void recalculateOrderAmount(Long orderId) {
        List<SaleOrderItem> items = itemMapper.selectByOrderId(orderId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (SaleOrderItem item : items) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amt);
        }

        SaleOrder order = getById(orderId);
        if (order != null) {
            order.setProductAmount(totalAmount);
            order.setBillAmount(totalAmount);
            order.setTotalQuantity(totalQty);
            updateById(order);
        }
    }

    @Override
    public void incrementPrintCount(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            orderMapper.incrementPrintCount(id);
        }
    }

    @Override
    public Map<String, Object> getCustomerCreditInfo(Long customerId) {
        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        if (customerId == null) {
            result.put("creditLimit", BigDecimal.ZERO);
            result.put("currentDebt", BigDecimal.ZERO);
            result.put("availableCredit", BigDecimal.ZERO);
            return result;
        }
        Party party = partyMapper.selectById(customerId);
        if (party == null) {
            result.put("creditLimit", BigDecimal.ZERO);
            result.put("currentDebt", BigDecimal.ZERO);
            result.put("availableCredit", BigDecimal.ZERO);
            return result;
        }
        BigDecimal creditLimit = party.getCreditLimit() != null ? party.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal availableCredit = creditLimit.subtract(currentDebt).max(BigDecimal.ZERO);
        result.put("creditLimit", creditLimit);
        result.put("currentDebt", currentDebt);
        result.put("availableCredit", availableCredit);
        result.put("customerName", party.getPartyName());
        return result;
    }

    @Override
    public List<Map<String, Object>> getCustomerDepositBalance(Long customerId) {
        if (customerId == null) return Collections.emptyList();
        try {
            List<cn.aiedge.erp.payment.entity.PreReceipt> receipts = preReceiptMapper.selectByCustomerId(customerId);
            if (receipts == null || receipts.isEmpty()) return Collections.emptyList();
            return receipts.stream().filter(r -> r.getRemainingAmount() != null
                    && r.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0).map(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("accountName", r.getPreReceiptNo() + " - " + (r.getDepositType() != null ? r.getDepositType() : "订金"));
                item.put("amount", r.getRemainingAmount());
                item.put("receiptDate", r.getReceiptDate() != null ? r.getReceiptDate().toString() : "");
                return item;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("查询客户订金余额失败: customerId={}", customerId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public LambdaQueryWrapper<SaleOrder> buildQueryWrapper(Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrder::getTenantId, tenantId);

        String orderNo = filters.get("orderNo") != null ? filters.get("orderNo").toString() : null;
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like(SaleOrder::getOrderNo, orderNo);

        if (filters.get("customerId") != null) wrapper.eq(SaleOrder::getCustomerId, filters.get("customerId"));
        if (filters.get("status") != null) wrapper.eq(SaleOrder::getStatus, filters.get("status"));

        String startDate = filters.get("startDate") != null ? filters.get("startDate").toString() : null;
        String endDate = filters.get("endDate") != null ? filters.get("endDate").toString() : null;
        if (startDate != null && !startDate.isEmpty()) {
            try { wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(startDate)); } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isEmpty()) {
            try { wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(endDate)); } catch (Exception ignored) {}
        }

        return wrapper;
    }

    // ═══════════════════════════════════════════
    // 子表操作
    // ═══════════════════════════════════════════

    private void saveSubTables(Long orderId, SaleOrderDTO dto) {
        // 1. 往来单位快照
        if (dto.getPartnerInfo() != null) {
            SaleOrderPartnerSnapshot snapshot = new SaleOrderPartnerSnapshot();
            snapshot.setOrderId(orderId);
            BeanUtils.copyProperties(dto.getPartnerInfo(), snapshot);
            partnerSnapshotMapper.insert(snapshot);
        }

        // 2. 收货地址
        if (dto.getDeliveryAddresses() != null) {
            int seq = 0;
            for (SaleOrderDTO.AddressInfo addr : dto.getDeliveryAddresses()) {
                SaleOrderDeliveryAddress entity = new SaleOrderDeliveryAddress();
                entity.setOrderId(orderId);
                BeanUtils.copyProperties(addr, entity);
                entity.setSequence(seq++);
                deliveryAddressMapper.insert(entity);
            }
        }

        // 3. 结算信息
        if (dto.getSettlementInfo() != null) {
            SaleOrderSettlement settlement = new SaleOrderSettlement();
            settlement.setOrderId(orderId);
            BeanUtils.copyProperties(dto.getSettlementInfo(), settlement);
            settlementMapper.insert(settlement);
        }

        // 4. 物流信息
        if (dto.getLogisticsInfoList() != null) {
            for (SaleOrderDTO.LogisticsInfo logistics : dto.getLogisticsInfoList()) {
                SaleOrderLogistics entity = new SaleOrderLogistics();
                entity.setOrderId(orderId);
                BeanUtils.copyProperties(logistics, entity);
                logisticsMapper.insert(entity);
            }
        }

        // 5. 订金账户
        if (dto.getDeposits() != null) {
            int seq = 0;
            for (SaleOrderDTO.DepositInfo deposit : dto.getDeposits()) {
                SaleOrderDeposit entity = new SaleOrderDeposit();
                entity.setOrderId(orderId);
                BeanUtils.copyProperties(deposit, entity);
                entity.setSequence(seq++);
                depositMapper.insert(entity);
            }
        }

        // 6. 会员积分
        if (dto.getMemberInfo() != null) {
            SaleOrderPointsJournal journal = new SaleOrderPointsJournal();
            journal.setOrderId(orderId);
            BeanUtils.copyProperties(dto.getMemberInfo(), journal);
            pointsJournalMapper.insert(journal);
        }

        // 7. 扩展信息
        if (dto.getExtInfoData() != null) {
            SaleOrderExtInfo extInfo = new SaleOrderExtInfo();
            extInfo.setOrderId(orderId);
            BeanUtils.copyProperties(dto.getExtInfoData(), extInfo);
            extInfoMapper.insert(extInfo);
        }
    }

    private void deleteSubTables(Long orderId) {
        partnerSnapshotMapper.delete(new LambdaQueryWrapper<SaleOrderPartnerSnapshot>()
                .eq(SaleOrderPartnerSnapshot::getOrderId, orderId));
        deliveryAddressMapper.delete(new LambdaQueryWrapper<SaleOrderDeliveryAddress>()
                .eq(SaleOrderDeliveryAddress::getOrderId, orderId));
        settlementMapper.delete(new LambdaQueryWrapper<SaleOrderSettlement>()
                .eq(SaleOrderSettlement::getOrderId, orderId));
        logisticsMapper.delete(new LambdaQueryWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getOrderId, orderId));
        depositMapper.delete(new LambdaQueryWrapper<SaleOrderDeposit>()
                .eq(SaleOrderDeposit::getOrderId, orderId));
        pointsJournalMapper.delete(new LambdaQueryWrapper<SaleOrderPointsJournal>()
                .eq(SaleOrderPointsJournal::getOrderId, orderId));
        extInfoMapper.delete(new LambdaQueryWrapper<SaleOrderExtInfo>()
                .eq(SaleOrderExtInfo::getOrderId, orderId));
    }

    private void loadSubTableData(Long orderId, SaleOrderDetailDTO dto) {
        // 1. 往来单位快照
        SaleOrderPartnerSnapshot snapshot = partnerSnapshotMapper.selectByOrderId(orderId);
        if (snapshot != null) {
            SaleOrderDetailDTO.PartnerSnapshotDTO ps = new SaleOrderDetailDTO.PartnerSnapshotDTO();
            BeanUtils.copyProperties(snapshot, ps);
            dto.setPartnerSnapshot(ps);
        }

        // 2. 收货地址
        List<SaleOrderDeliveryAddress> addresses = deliveryAddressMapper.selectByOrderId(orderId);
        dto.setDeliveryAddresses(addresses.stream().map(a -> {
            SaleOrderDetailDTO.DeliveryAddressDTO d = new SaleOrderDetailDTO.DeliveryAddressDTO();
            BeanUtils.copyProperties(a, d);
            return d;
        }).collect(Collectors.toList()));

        // 3. 结算信息
        SaleOrderSettlement settlement = settlementMapper.selectByOrderId(orderId);
        if (settlement != null) {
            SaleOrderDetailDTO.SettlementDTO s = new SaleOrderDetailDTO.SettlementDTO();
            BeanUtils.copyProperties(settlement, s);
            dto.setSettlement(s);
        }

        // 4. 物流信息
        List<SaleOrderLogistics> logistics = logisticsMapper.selectByOrderId(orderId);
        dto.setLogisticsList(logistics.stream().map(l -> {
            SaleOrderDetailDTO.LogisticsDTO ld = new SaleOrderDetailDTO.LogisticsDTO();
            BeanUtils.copyProperties(l, ld);
            return ld;
        }).collect(Collectors.toList()));

        // 5. 订金
        List<SaleOrderDeposit> deposits = depositMapper.selectByOrderId(orderId);
        dto.setDeposits(deposits.stream().map(d -> {
            SaleOrderDetailDTO.DepositDTO dd = new SaleOrderDetailDTO.DepositDTO();
            BeanUtils.copyProperties(d, dd);
            return dd;
        }).collect(Collectors.toList()));

        // 6. 积分
        SaleOrderPointsJournal points = pointsJournalMapper.selectByOrderId(orderId);
        if (points != null) {
            SaleOrderDetailDTO.PointsJournalDTO pj = new SaleOrderDetailDTO.PointsJournalDTO();
            BeanUtils.copyProperties(points, pj);
            dto.setPointsJournal(pj);
        }

        // 7. 审核流水
        List<SaleOrderAuditTrail> audits = auditTrailMapper.selectByOrderId(orderId);
        dto.setAuditTrails(audits.stream().map(a -> {
            SaleOrderDetailDTO.AuditTrailDTO at = new SaleOrderDetailDTO.AuditTrailDTO();
            BeanUtils.copyProperties(a, at);
            return at;
        }).collect(Collectors.toList()));

        // 8. 扩展信息
        SaleOrderExtInfo extInfo = extInfoMapper.selectByOrderId(orderId);
        if (extInfo != null) {
            SaleOrderDetailDTO.ExtInfoDTO ei = new SaleOrderDetailDTO.ExtInfoDTO();
            BeanUtils.copyProperties(extInfo, ei);
            dto.setExtInfo(ei);
        }
    }

    private void saveAuditTrail(Long orderId, String action, Long operatorId, String operatorName, String remark) {
        SaleOrderAuditTrail trail = new SaleOrderAuditTrail();
        trail.setOrderId(orderId);
        trail.setAction(action);
        trail.setOperatorId(operatorId);
        trail.setOperatorName(operatorName);
        trail.setActionTime(LocalDateTime.now());
        trail.setRemark(remark);
        auditTrailMapper.insert(trail);
    }

    /**
     * 第三期（影子转正式）：查询单据的在途引擎实例。
     * 无引擎实现/无在途实例/查询异常均返回 null（降级为直接翻转），不阻断业务。
     *
     * 防循环：引擎回调到达时实例已是终态（非在途），本方法返回 null，
     * 被回调复用的 approve/reject 自然回退为直接翻转，不会再次驱动引擎。
     */
    private ApprovalFacade.InFlightInstance findInFlightApproval(Long orderId) {
        try {
            // Spring 环境必然注入 ObjectProvider；判空仅为兼容手工构造的单元测试
            ApprovalFacade facade = approvalFacadeProvider == null ? null : approvalFacadeProvider.getIfAvailable();
            return facade == null ? null : facade.findInFlightInstance("sale_order", orderId);
        } catch (Exception e) {
            log.warn("查询在途审批实例失败(降级为无引擎): orderId={}, error={}", orderId, e.getMessage());
            return null;
        }
    }

    /**
     * 驱动在途实例审批：成功返回 true（终态由回调回写单据），失败返回 false 由调用方回退直接翻转
     */
    private boolean driveEngineApproval(ApprovalFacade.InFlightInstance inFlight, String action,
                                        Long operatorId, String comment) {
        try {
            ApprovalFacade facade = approvalFacadeProvider == null ? null : approvalFacadeProvider.getIfAvailable();
            return facade != null && facade.driveApproval(inFlight, action, operatorId, comment);
        } catch (Exception e) {
            log.warn("引擎驱动审批异常: instanceId={}, action={}, error={}",
                    inFlight.getInstanceId(), action, e.getMessage());
            return false;
        }
    }

    /**
     * 影子模式发起工作流实例：引擎实例并行可见于工作流待办页，不回写单据 status。
     * instanceId 追加在审核流水 remark 中（不新增列）。无引擎实现或发起失败时不阻断业务。
     */
    private void startShadowApproval(SaleOrder order) {
        try {
            // Spring 环境必然注入 ObjectProvider；判空仅为兼容手工构造的单元测试
            ApprovalFacade facade = approvalFacadeProvider == null ? null : approvalFacadeProvider.getIfAvailable();
            if (facade == null) {
                return;
            }
            Map<String, Object> businessData = new HashMap<>();
            businessData.put("orderNo", order.getOrderNo());
            businessData.put("amount", order.getBillAmount());
            businessData.put("customerId", order.getCustomerId());
            businessData.put("customerName", order.getCustomerName());
            String instanceId = facade.startApproval("order_approval", "sale_order", order.getId(),
                    order.getOrderNo(), businessData, order.getSubmitterId(), order.getSubmitterName());
            if (instanceId != null) {
                saveAuditTrail(order.getId(), "WORKFLOW", order.getSubmitterId(), order.getSubmitterName(),
                        "影子工作流实例已发起: instanceId=" + instanceId);
            }
        } catch (Exception e) {
            log.warn("发起销售订单影子工作流失败(不阻断): orderId={}, error={}", order.getId(), e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════

    private LambdaQueryWrapper<SaleOrder> buildOrderCenterWrapper(Long tenantId, Map<String, Object> filters) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrder::getTenantId, tenantId);

        // 日期范围
        String startDate = filters.get("startDate") != null ? filters.get("startDate").toString() : null;
        String endDate = filters.get("endDate") != null ? filters.get("endDate").toString() : null;
        if (startDate != null && !startDate.isEmpty()) {
            try { wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(startDate)); } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isEmpty()) {
            try { wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(endDate)); } catch (Exception ignored) {}
        }

        // 单据日期（精确到某一天）
        String orderDate = filters.get("orderDate") != null ? filters.get("orderDate").toString() : null;
        if (orderDate != null && !orderDate.isEmpty()) {
            try { wrapper.eq(SaleOrder::getOrderDate, LocalDate.parse(orderDate)); } catch (Exception ignored) {}
        }

        // 单据编号
        String orderNo = filters.get("orderNo") != null ? filters.get("orderNo").toString() : null;
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(SaleOrder::getOrderNo, orderNo);
        }

        // 客户ID/名称
        if (filters.get("customerId") != null) wrapper.eq(SaleOrder::getCustomerId, filters.get("customerId"));
        String customerName = filters.get("customerName") != null ? filters.get("customerName").toString() : null;
        if (customerName != null && !customerName.isEmpty()) {
            wrapper.like(SaleOrder::getCustomerName, customerName);
        }

        // 经手人ID/名称
        if (filters.get("salesmanId") != null) wrapper.eq(SaleOrder::getSalesmanId, filters.get("salesmanId"));
        String salesmanName = filters.get("salesmanName") != null ? filters.get("salesmanName").toString() : null;
        if (salesmanName != null && !salesmanName.isEmpty()) {
            wrapper.like(SaleOrder::getSalesmanName, salesmanName);
        }

        // 仓库ID/名称
        if (filters.get("warehouseId") != null) wrapper.eq(SaleOrder::getWarehouseId, filters.get("warehouseId"));
        String warehouseName = filters.get("warehouseName") != null ? filters.get("warehouseName").toString() : null;
        if (warehouseName != null && !warehouseName.isEmpty()) {
            wrapper.like(SaleOrder::getWarehouseName, warehouseName);
        }

        // 部门ID/名称
        if (filters.get("deptId") != null) wrapper.eq(SaleOrder::getDeptId, filters.get("deptId"));
        String deptName = filters.get("deptName") != null ? filters.get("deptName").toString() : null;
        if (deptName != null && !deptName.isEmpty()) {
            wrapper.like(SaleOrder::getDeptName, deptName);
        }

        // 单据状态
        if (filters.get("status") != null && !filters.get("status").toString().isEmpty()) {
            wrapper.eq(SaleOrder::getStatus, filters.get("status"));
        }

        // 结款方式
        String settlementMethod = filters.get("settlementMethod") != null ? filters.get("settlementMethod").toString() : null;
        if (settlementMethod != null && !settlementMethod.isEmpty()) {
            wrapper.eq(SaleOrder::getSettlementMethod, settlementMethod);
        }

        // 补单类型
        String supplementType = filters.get("supplementType") != null ? filters.get("supplementType").toString() : null;
        if (supplementType != null && !supplementType.isEmpty()) {
            wrapper.eq(SaleOrder::getSupplementType, supplementType);
        }

        // 配送线路
        String deliveryRoute = filters.get("deliveryRoute") != null ? filters.get("deliveryRoute").toString() : null;
        if (deliveryRoute != null && !deliveryRoute.isEmpty()) {
            wrapper.like(SaleOrder::getDeliveryRoute, deliveryRoute);
        }

        // 推广人名称
        String promoterName = filters.get("promoterName") != null ? filters.get("promoterName").toString() : null;
        if (promoterName != null && !promoterName.isEmpty()) {
            wrapper.like(SaleOrder::getPromoterName, promoterName);
        }

        // 区域
        String region = filters.get("region") != null ? filters.get("region").toString() : null;
        if (region != null && !region.isEmpty()) {
            wrapper.like(SaleOrder::getRegion, region);
        }

        // 商品名称(通过明细子表关联查询)
        String productName = filters.get("productName") != null ? filters.get("productName").toString() : null;
        if (productName != null && !productName.isEmpty()) {
            wrapper.inSql(SaleOrder::getId,
                    "SELECT DISTINCT order_id FROM erp_sale_order_item WHERE product_name LIKE '%" + productName.replace("'", "''") + "%'");
        }

        // 单据备注（表头 remark 列；orderRemark 为卖家备注，两者独立）
        String remark = filters.get("remark") != null ? filters.get("remark").toString() : null;
        if (remark != null && !remark.isEmpty()) {
            wrapper.like(SaleOrder::getRemark, remark);
        }

        // 单据备注（卖家备注 orderRemark）
        String orderRemark = filters.get("orderRemark") != null ? filters.get("orderRemark").toString() : null;
        if (orderRemark != null && !orderRemark.isEmpty()) {
            wrapper.like(SaleOrder::getOrderRemark, orderRemark);
        }

        // 产生方式
        String generationMethod = filters.get("generationMethod") != null ? filters.get("generationMethod").toString() : null;
        if (generationMethod != null && !generationMethod.isEmpty()) {
            wrapper.eq(SaleOrder::getGenerationMethod, generationMethod);
        }

        // 商品品牌
        String productBrand = filters.get("productBrand") != null ? filters.get("productBrand").toString() : null;
        if (productBrand != null && !productBrand.isEmpty()) {
            wrapper.like(SaleOrder::getProductBrand, productBrand);
        }

        // 所属行业类别
        String industryCategory = filters.get("industryCategory") != null ? filters.get("industryCategory").toString() : null;
        if (industryCategory != null && !industryCategory.isEmpty()) {
            wrapper.like(SaleOrder::getIndustryCategory, industryCategory);
        }

        // 制单人
        String creatorName = filters.get("creatorName") != null ? filters.get("creatorName").toString() : null;
        if (creatorName != null && !creatorName.isEmpty()) {
            wrapper.like(SaleOrder::getCreatorName, creatorName);
        }

        // 审核人
        String auditorName = filters.get("auditorName") != null ? filters.get("auditorName").toString() : null;
        if (auditorName != null && !auditorName.isEmpty()) {
            wrapper.like(SaleOrder::getAuditorName, auditorName);
        }

        // 提交人
        String submitterName = filters.get("submitterName") != null ? filters.get("submitterName").toString() : null;
        if (submitterName != null && !submitterName.isEmpty()) {
            wrapper.like(SaleOrder::getSubmitterName, submitterName);
        }

        // 配送方式
        String deliveryMethod = filters.get("deliveryMethod") != null ? filters.get("deliveryMethod").toString() : null;
        if (deliveryMethod != null && !deliveryMethod.isEmpty()) {
            wrapper.eq(SaleOrder::getDeliveryMethod, deliveryMethod);
        }

        // 配送司机
        String driverName = filters.get("driverName") != null ? filters.get("driverName").toString() : null;
        if (driverName != null && !driverName.isEmpty()) {
            wrapper.like(SaleOrder::getDriverName, driverName);
        }

        // 配送车辆
        String deliveryVehicle = filters.get("deliveryVehicle") != null ? filters.get("deliveryVehicle").toString() : null;
        if (deliveryVehicle != null && !deliveryVehicle.isEmpty()) {
            wrapper.like(SaleOrder::getDeliveryVehicle, deliveryVehicle);
        }

        // 收货人
        String receiverName = filters.get("receiverName") != null ? filters.get("receiverName").toString() : null;
        if (receiverName != null && !receiverName.isEmpty()) {
            wrapper.like(SaleOrder::getReceiverName, receiverName);
        }

        // 联系电话
        String receiverPhone = filters.get("receiverPhone") != null ? filters.get("receiverPhone").toString() : null;
        if (receiverPhone != null && !receiverPhone.isEmpty()) {
            wrapper.like(SaleOrder::getReceiverPhone, receiverPhone);
        }

        // 物流公司
        String logisticsCompany = filters.get("logisticsCompany") != null ? filters.get("logisticsCompany").toString() : null;
        if (logisticsCompany != null && !logisticsCompany.isEmpty()) {
            wrapper.like(SaleOrder::getLogisticsCompany, logisticsCompany);
        }

        // 运单号
        String waybillNo = filters.get("waybillNo") != null ? filters.get("waybillNo").toString() : null;
        if (waybillNo != null && !waybillNo.isEmpty()) {
            wrapper.like(SaleOrder::getWaybillNo, waybillNo);
        }

        // 销售类型
        if (filters.get("saleType") != null) {
            wrapper.eq(SaleOrder::getSaleType, filters.get("saleType"));
        }

        // 表头自定义字段
        if (filters.get("extNum1") != null) wrapper.eq(SaleOrder::getExtNum1, filters.get("extNum1"));
        if (filters.get("extNum2") != null) wrapper.eq(SaleOrder::getExtNum2, filters.get("extNum2"));
        if (filters.get("extText1") != null && !filters.get("extText1").toString().isEmpty()) wrapper.like(SaleOrder::getExtText1, filters.get("extText1").toString());
        if (filters.get("extText2") != null && !filters.get("extText2").toString().isEmpty()) wrapper.like(SaleOrder::getExtText2, filters.get("extText2").toString());
        if (filters.get("extText3") != null && !filters.get("extText3").toString().isEmpty()) wrapper.like(SaleOrder::getExtText3, filters.get("extText3").toString());
        if (filters.get("footerExtText1") != null && !filters.get("footerExtText1").toString().isEmpty()) wrapper.like(SaleOrder::getFooterExtText1, filters.get("footerExtText1").toString());
        if (filters.get("footerExtText2") != null && !filters.get("footerExtText2").toString().isEmpty()) wrapper.like(SaleOrder::getFooterExtText2, filters.get("footerExtText2").toString());

        // 发货日期范围
        String shipDateStart = filters.get("shipDateStart") != null ? filters.get("shipDateStart").toString() : null;
        String shipDateEnd = filters.get("shipDateEnd") != null ? filters.get("shipDateEnd").toString() : null;
        if (shipDateStart != null && !shipDateStart.isEmpty()) {
            try { wrapper.ge(SaleOrder::getExpectedShipTime, LocalDate.parse(shipDateStart).atStartOfDay()); } catch (Exception ignored) {}
        }
        if (shipDateEnd != null && !shipDateEnd.isEmpty()) {
            try { wrapper.le(SaleOrder::getExpectedShipTime, LocalDate.parse(shipDateEnd).plusDays(1).atStartOfDay()); } catch (Exception ignored) {}
        }

        // 本单金额范围（兼容 billAmountMin/Max 和 minAmount/maxAmount 两种参数名）
        String billAmountMin = filters.get("billAmountMin") != null ? filters.get("billAmountMin").toString()
                : (filters.get("minAmount") != null ? filters.get("minAmount").toString() : null);
        String billAmountMax = filters.get("billAmountMax") != null ? filters.get("billAmountMax").toString()
                : (filters.get("maxAmount") != null ? filters.get("maxAmount").toString() : null);
        if (billAmountMin != null && !billAmountMin.isEmpty()) {
            try { wrapper.ge(SaleOrder::getBillAmount, new BigDecimal(billAmountMin)); } catch (Exception ignored) {}
        }
        if (billAmountMax != null && !billAmountMax.isEmpty()) {
            try { wrapper.le(SaleOrder::getBillAmount, new BigDecimal(billAmountMax)); } catch (Exception ignored) {}
        }

        // 订单来源
        if (filters.get("orderSource") != null && !filters.get("orderSource").toString().isEmpty()) {
            try { wrapper.eq(SaleOrder::getOrderSource, Integer.parseInt(filters.get("orderSource").toString())); } catch (Exception ignored) {}
        }

        // 商品行属性（通过明细子表 line_attribute 字段查询）
        String itemProperty = filters.get("itemProperty") != null ? filters.get("itemProperty").toString() : null;
        if (itemProperty != null && !itemProperty.isEmpty()) {
            wrapper.inSql(SaleOrder::getId,
                    "SELECT DISTINCT order_id FROM erp_sale_order_item WHERE line_attribute LIKE '%" + itemProperty.replace("'", "''") + "%'");
        }

        // 收货地址
        String shippingAddress = filters.get("shippingAddress") != null ? filters.get("shippingAddress").toString() : null;
        if (shippingAddress != null && !shippingAddress.isEmpty()) {
            wrapper.like(SaleOrder::getShippingAddress, shippingAddress);
        }

        // 买家备注
        String buyerRemark = filters.get("buyerRemark") != null ? filters.get("buyerRemark").toString() : null;
        if (buyerRemark != null && !buyerRemark.isEmpty()) {
            wrapper.like(SaleOrder::getBuyerRemark, buyerRemark);
        }

        // 摘要
        String summary = filters.get("summary") != null ? filters.get("summary").toString() : null;
        if (summary != null && !summary.isEmpty()) {
            wrapper.like(SaleOrder::getSummary, summary);
        }

        // 打印次数
        if (filters.get("printCount") != null && !filters.get("printCount").toString().isEmpty()) {
            try { wrapper.eq(SaleOrder::getPrintCount, Integer.parseInt(filters.get("printCount").toString())); } catch (Exception ignored) {}
        }

        // 提交时间范围
        String submitTimeStart = filters.get("submitTimeStart") != null ? filters.get("submitTimeStart").toString() : null;
        String submitTimeEnd = filters.get("submitTimeEnd") != null ? filters.get("submitTimeEnd").toString() : null;
        // 兼容 submitTime 单值（作为起始日期）
        if (submitTimeStart == null && filters.get("submitTime") != null && !filters.get("submitTime").toString().isEmpty()) {
            submitTimeStart = filters.get("submitTime").toString();
        }
        if (submitTimeStart != null && !submitTimeStart.isEmpty()) {
            try { wrapper.ge(SaleOrder::getSubmitTime, LocalDateTime.parse(submitTimeStart + "T00:00:00")); } catch (Exception ignored) {}
        }
        if (submitTimeEnd != null && !submitTimeEnd.isEmpty()) {
            try { wrapper.le(SaleOrder::getSubmitTime, LocalDateTime.parse(submitTimeEnd + "T23:59:59")); } catch (Exception ignored) {}
        }

        // 审核时间范围
        String auditTimeStart = filters.get("auditTimeStart") != null ? filters.get("auditTimeStart").toString() : null;
        String auditTimeEnd = filters.get("auditTimeEnd") != null ? filters.get("auditTimeEnd").toString() : null;
        if (auditTimeStart == null && filters.get("auditTime") != null && !filters.get("auditTime").toString().isEmpty()) {
            auditTimeStart = filters.get("auditTime").toString();
        }
        if (auditTimeStart != null && !auditTimeStart.isEmpty()) {
            try { wrapper.ge(SaleOrder::getAuditTime, LocalDateTime.parse(auditTimeStart + "T00:00:00")); } catch (Exception ignored) {}
        }
        if (auditTimeEnd != null && !auditTimeEnd.isEmpty()) {
            try { wrapper.le(SaleOrder::getAuditTime, LocalDateTime.parse(auditTimeEnd + "T23:59:59")); } catch (Exception ignored) {}
        }

        // 第三方单号
        String thirdPartyOrderNo = filters.get("thirdPartyOrderNo") != null ? filters.get("thirdPartyOrderNo").toString() : null;
        if (thirdPartyOrderNo != null && !thirdPartyOrderNo.isEmpty()) {
            wrapper.like(SaleOrder::getThirdPartyOrderNo, thirdPartyOrderNo);
        }

        // 明细备注
        String detailRemark = filters.get("detailRemark") != null ? filters.get("detailRemark").toString() : null;
        if (detailRemark != null && !detailRemark.isEmpty()) {
            wrapper.inSql(SaleOrder::getId,
                    "SELECT DISTINCT order_id FROM erp_sale_order_item WHERE remark LIKE '%" + detailRemark.replace("'", "''") + "%'");
        }

        // 自定义字段 extText4 / extText5
        String extText4 = filters.get("extText4") != null ? filters.get("extText4").toString() : null;
        if (extText4 != null && !extText4.isEmpty()) {
            wrapper.like(SaleOrder::getExtText4, extText4);
        }
        String extText5 = filters.get("extText5") != null ? filters.get("extText5").toString() : null;
        if (extText5 != null && !extText5.isEmpty()) {
            wrapper.like(SaleOrder::getExtText5, extText5);
        }

        // 客户一票通
        String customerTicket = filters.get("customerTicket") != null ? filters.get("customerTicket").toString() : null;
        if (customerTicket != null && !customerTicket.isEmpty()) {
            wrapper.eq(SaleOrder::getCustomerTicket, customerTicket);
        }

        // 客户备注
        String customerRemark = filters.get("customerRemark") != null ? filters.get("customerRemark").toString() : null;
        if (customerRemark != null && !customerRemark.isEmpty()) {
            wrapper.like(SaleOrder::getCustomerRemark, customerRemark);
        }

        // 商品金额范围
        String productAmountMin = filters.get("productAmountMin") != null ? filters.get("productAmountMin").toString()
                : (filters.get("productAmount") != null ? filters.get("productAmount").toString() : null);
        if (productAmountMin != null && !productAmountMin.isEmpty()) {
            try { wrapper.ge(SaleOrder::getProductAmount, new BigDecimal(productAmountMin)); } catch (Exception ignored) {}
        }

        // 优惠券
        if (filters.get("couponAmount") != null && !filters.get("couponAmount").toString().isEmpty()) {
            try { wrapper.eq(SaleOrder::getCouponAmount, new BigDecimal(filters.get("couponAmount").toString())); } catch (Exception ignored) {}
        }

        // 直接优惠
        if (filters.get("directDiscount") != null && !filters.get("directDiscount").toString().isEmpty()) {
            try { wrapper.eq(SaleOrder::getDirectDiscount, new BigDecimal(filters.get("directDiscount").toString())); } catch (Exception ignored) {}
        }

        // 是否赠品（通过明细子表 gift 字段查询）
        String isGift = filters.get("isGift") != null ? filters.get("isGift").toString() : null;
        if (isGift != null && !isGift.isEmpty()) {
            boolean giftVal = "1".equals(isGift) || "true".equalsIgnoreCase(isGift);
            wrapper.inSql(SaleOrder::getId,
                    "SELECT DISTINCT order_id FROM erp_sale_order_item WHERE gift = " + (giftVal ? "true" : "false"));
        }

        // 补单状态（fulfillment tab 专用）
        String supplementStatus = filters.get("supplementStatus") != null ? filters.get("supplementStatus").toString() : null;
        if (supplementStatus != null && !supplementStatus.isEmpty()) {
            wrapper.like(SaleOrder::getSupplementStatus, supplementStatus);
        }

        // 订单未补金额（fulfillment tab 专用）
        String remainingUnshippedAmount = filters.get("remainingUnshippedAmount") != null ? filters.get("remainingUnshippedAmount").toString() : null;
        if (remainingUnshippedAmount != null && !remainingUnshippedAmount.isEmpty()) {
            try { wrapper.eq(SaleOrder::getRemainingUnshippedAmount, new BigDecimal(remainingUnshippedAmount)); } catch (Exception ignored) {}
        }

        // 仅显示已选中（前端传入选中的订单ID列表）
        String showSelected = filters.get("showSelected") != null ? filters.get("showSelected").toString() : null;
        if ("true".equals(showSelected) && filters.get("selectedIds") != null) {
            String selectedIds = filters.get("selectedIds").toString();
            if (!selectedIds.isEmpty()) {
                List<Long> idList = java.util.Arrays.stream(selectedIds.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
                if (!idList.isEmpty()) {
                    wrapper.in(SaleOrder::getId, idList);
                }
            }
        }

        // 不显示有关联审核中退货申请单的单据
        String hideReturnRelated = filters.get("hideReturnRelated") != null ? filters.get("hideReturnRelated").toString() : null;
        if ("true".equals(hideReturnRelated)) {
            wrapper.notInSql(SaleOrder::getId,
                    "SELECT source_order_id FROM erp_sale_return WHERE status = 1 AND source_order_id IS NOT NULL AND deleted = 0");
        }

        return wrapper;
    }

    private SaleOrderListDTO convertToListDTO(SaleOrder order) {
        SaleOrderListDTO dto = new SaleOrderListDTO();
        dto.setId(order.getId().toString());
        dto.setOrderNo(order.getOrderNo());
        dto.setOrderDate(order.getOrderDate());
        dto.setSaleType(order.getSaleType());
        dto.setStatus(order.getStatus());
        dto.setStatusName(getStatusName(order.getStatus()));
        // 客户信息
        dto.setCustomerId(order.getCustomerId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerCode(order.getCustomerCode());
        dto.setCustomerLevel(order.getCustomerLevel());
        dto.setCustomerRemark(order.getCustomerRemark());
        dto.setCustomerTicket(order.getCustomerTicket());
        // 经手人/部门
        dto.setSalesmanId(order.getSalesmanId());
        dto.setSalesmanName(order.getSalesmanName());
        dto.setDeptName(order.getDeptName());
        // 仓库
        dto.setWarehouseId(order.getWarehouseId());
        dto.setWarehouseName(order.getWarehouseName());
        // 收货信息
        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setShippingAddress(order.getShippingAddress());
        // 推广人
        dto.setPromoterId(order.getPromoterId());
        dto.setPromoterName(order.getPromoterName());
        // 金额
        dto.setProductAmount(order.getProductAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        // 优惠后金额 = 商品金额 − 促销优惠 − 优惠金额（与单据摘要口径一致：本单金额 − 其他费用）
        dto.setFavorableAmount(
                (order.getProductAmount() != null ? order.getProductAmount() : BigDecimal.ZERO)
                        .subtract(order.getPromoDiscount() != null ? order.getPromoDiscount() : BigDecimal.ZERO)
                        .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO));
        dto.setBillAmount(order.getBillAmount());
        dto.setSettledAmount(order.getSettledAmount());
        dto.setReceivedAmount(order.getReceivedAmount());
        dto.setPromoDiscount(order.getPromoDiscount());
        dto.setCouponAmount(order.getCouponAmount());
        dto.setDirectDiscount(order.getDirectDiscount());
        dto.setOtherFee(order.getOtherFee());
        // 结算状态（由 receivedAmount 与 billAmount 计算得出）
        BigDecimal received = order.getReceivedAmount();
        BigDecimal settled = order.getSettledAmount();
        BigDecimal bill = order.getBillAmount();
        // 取已收款和已结金额的较大值作为实际结算金额
        BigDecimal effectiveAmount = BigDecimal.ZERO;
        if (received != null && received.compareTo(effectiveAmount) > 0) effectiveAmount = received;
        if (settled != null && settled.compareTo(effectiveAmount) > 0) effectiveAmount = settled;
        if (bill != null && bill.compareTo(BigDecimal.ZERO) > 0) {
            if (effectiveAmount.compareTo(BigDecimal.ZERO) == 0) {
                dto.setSettlementStatus("未结算");
            } else if (effectiveAmount.compareTo(bill) >= 0) {
                dto.setSettlementStatus("已结算");
            } else {
                dto.setSettlementStatus("部分结算");
            }
        } else {
            dto.setSettlementStatus("未结算");
        }
        // 运费
        dto.setFreightPayer(order.getFreightPayer());
        dto.setShippingFee(order.getShippingFee());
        // 数量
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setShippedQuantity(order.getShippedQuantity());
        dto.setUnshippedQuantity(order.getUnshippedQuantity());
        dto.setReturnQuantity(order.getReturnQuantity());
        dto.setReturnAmount(order.getReturnAmount());
        // 物理汇总
        dto.setTotalWeight(order.getTotalWeight());
        dto.setTotalVolume(order.getTotalVolume());
        // 物流
        dto.setLogisticsCompany(order.getLogisticsCompany());
        dto.setWaybillNo(order.getWaybillNo());
        // 订金账户
        dto.setDepositAccount1(order.getDepositAccount1());
        dto.setDepositAccount2(order.getDepositAccount2());
        dto.setDepositAccount3(order.getDepositAccount3());
        dto.setDepositAccount4(order.getDepositAccount4());
        // 区域/销售类型/配送方式
        dto.setRegion(order.getRegion());
        dto.setDeliveryMethod(order.getDeliveryMethod());
        // 备注
        dto.setBuyerRemark(order.getBuyerRemark());
        dto.setOrderRemark(order.getOrderRemark());
        dto.setSummary(order.getSummary());
        // 附件
        dto.setAttachment(order.getAttachment());
        // 自定义字段
        dto.setExtNum1(order.getExtNum1());
        dto.setExtNum2(order.getExtNum2());
        dto.setExtText1(order.getExtText1());
        dto.setExtText2(order.getExtText2());
        dto.setExtText3(order.getExtText3());
        dto.setFooterExtText1(order.getFooterExtText1());
        dto.setFooterExtText2(order.getFooterExtText2());
        // 提交/审核/制单
        dto.setSubmitTime(order.getSubmitTime());
        dto.setGenerationMethod(order.getGenerationMethod());
        dto.setBookkeepingTime(order.getBookkeepingTime());
        dto.setCreatorName(order.getCreatorName());
        dto.setSubmitterName(order.getSubmitterName());
        dto.setAuditorName(order.getAuditorName());
        dto.setAuditTime(order.getAuditTime());
        dto.setPrintCount(order.getPrintCount());
        // 第三方/来源
        dto.setThirdPartyOrderNo(order.getThirdPartyOrderNo());
        dto.setSourceOrder(order.getSourceOrder());
        // 结款方式
        dto.setSettlementMethod(order.getSettlementMethod());
        // 预计发货时间
        dto.setExpectedShipTime(order.getExpectedShipTime());
        // 履约相关
        dto.setOriginalOrderNo(order.getOriginalOrderNo());
        dto.setShippedOrderNo(order.getShippedOrderNo());
        dto.setSupplementStatus(order.getSupplementStatus());
        dto.setOriginalAmount(order.getOriginalAmount());
        dto.setRemainingUnshippedAmount(order.getRemainingUnshippedAmount());
        dto.setOriginalDiscount(order.getOriginalDiscount());
        dto.setOriginalItemCount(order.getOriginalItemCount());
        dto.setUnshippedItemCount(order.getUnshippedItemCount());
        dto.setOriginalQuantity(order.getOriginalQuantity());
        dto.setUnshippedQuantityItems(order.getUnshippedQuantityItems());
        // 拣货/发货
        dto.setPickingWarehouse(order.getPickingWarehouse());
        dto.setCollectionLocation(order.getCollectionLocation());
        dto.setPickupAddress(order.getPickupAddress());
        dto.setDeliveryRoute(order.getDeliveryRoute());
        dto.setDriverName(order.getDriverName());
        dto.setDeliveryVehicle(order.getDeliveryVehicle());
        dto.setSortOrder(order.getSortOrder());
        dto.setSortValue(order.getSortValue());
        dto.setOrderSource(order.getOrderSource());
        // 拣货进度：已拣货取主表汇总，未拣货 = 订货数量 − 已拣货数量（派生，不落库）
        BigDecimal picked = order.getPickedQuantity() != null ? order.getPickedQuantity() : BigDecimal.ZERO;
        dto.setPickedQuantity(picked);
        BigDecimal orderedQty = order.getTotalQuantity() != null ? order.getTotalQuantity() : BigDecimal.ZERO;
        dto.setUnpickedQuantity(orderedQty.subtract(picked).max(BigDecimal.ZERO));
        // 时间
        dto.setCreateTime(order.getCreateTime());
        dto.setUpdateTime(order.getUpdateTime());

        // 业务扩展字段
        dto.setProductBrand(order.getProductBrand());
        dto.setIndustryCategory(order.getIndustryCategory());
        dto.setExtText4(order.getExtText4());
        dto.setExtText5(order.getExtText5());

        return dto;
    }

    /**
     * 批量填充「商品行数」—— 取订单明细真实行数，一次分组查询避免 N+1。
     * 拣货/发货 Tab「商品行数」列与按单据 Tab 均以此为准。
     */
    private void fillLineCount(List<SaleOrderListDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        List<Long> orderIds = dtos.stream()
                .map(SaleOrderListDTO::getId)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) return;
        List<SaleOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>()
                        .select(SaleOrderItem::getOrderId)
                        .in(SaleOrderItem::getOrderId, orderIds));
        Map<Long, Integer> countMap = items.stream()
                .filter(i -> i.getOrderId() != null)
                .collect(Collectors.groupingBy(SaleOrderItem::getOrderId, Collectors.summingInt(i -> 1)));
        dtos.forEach(d -> d.setLineCount(countMap.getOrDefault(Long.valueOf(d.getId()), 0)));
    }

    private SaleOrderItemDTO convertItemToDTO(SaleOrderItem item) {
        SaleOrderItemDTO dto = new SaleOrderItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }

    private Map<Long, String> batchGetCustomerNames(Set<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) return Collections.emptyMap();
        try {
            List<Party> parties = partyMapper.selectBatchIds(customerIds);
            return parties.stream()
                    .filter(p -> p != null && p.getId() != null)
                    .collect(Collectors.toMap(
                            Party::getId,
                            p -> p.getPartyName() != null ? p.getPartyName() : "",
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量查询客户名称失败", e);
            return Collections.emptyMap();
        }
    }

    private SaleOrderItem createOrderItem(SaleOrderItemDTO itemDTO, Long orderId, Long customerId, Long warehouseId) {
        SaleOrderItem item = new SaleOrderItem();
        BeanUtils.copyProperties(itemDTO, item);
        item.setOrderId(orderId);
        item.setShippedQuantity(BigDecimal.ZERO);
        item.setCreateTime(LocalDateTime.now());

        // 查询当前可用库存，填充到明细行（供前端查看）
        if (item.getProductId() != null && warehouseId != null) {
            try {
                Stock stock = stockService.getStockDetail(item.getProductId(), warehouseId);
                if (stock != null) {
                    item.setAvailableStock(stock.getAvailableQuantity());
                    item.setBookStock(stock.getQuantity());
                }
            } catch (Exception e) {
                log.warn("查询库存可用量失败: productId={}, warehouseId={}", item.getProductId(), warehouseId, e);
            }
        }

        // 价格引擎
        PriceCalculationRequest priceRequest = new PriceCalculationRequest();
        priceRequest.setCustomerId(customerId != null ? customerId.toString() : "");
        priceRequest.setProductId(itemDTO.getProductId() != null ? itemDTO.getProductId().toString() : "");
        priceRequest.setQuantity(itemDTO.getQuantity() != null ? itemDTO.getQuantity().intValue() : 1);
        priceRequest.setCustomerLevel(getCustomerGradeCode(customerId));
        priceRequest.setCalculationTime(LocalDateTime.now());

        Product product = itemDTO.getProductId() != null ? productService.getById(itemDTO.getProductId()) : null;
        if (product != null) {
            priceRequest.setProductName(product.getProductName());
            priceRequest.setBasePrice(product.getStandardPrice());
        }

        PriceCalculationResult priceResult = priceEngineService.calculatePrice(priceRequest);
        item.setCalculatedPrice(priceResult.getFinalPrice() != null ? priceResult.getFinalPrice() : item.getUnitPrice());
        item.setPriceSource(priceResult.getCalculationExplanation() != null ? priceResult.getCalculationExplanation() : "Default");
        item.setCustomerGradeCode(getCustomerGradeCode(customerId));
        item.setCustomerGradeName(getCustomerGradeName(customerId));

        if (item.getPriceGradeCode() == null || item.getPriceGradeCode().isEmpty()) {
            item.setPriceGradeCode("PG_" + (getCustomerGradeCode(customerId) != null ? getCustomerGradeCode(customerId) : "DEFAULT"));
        }

        item.setUnitPrice(priceResult.getFinalPrice() != null ? priceResult.getFinalPrice() : item.getUnitPrice());
        if (product != null) {
            item.setCostPrice(product.getCostPrice());
        }
        if (item.getQuantity() != null && item.getUnitPrice() != null) {
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
        }

        return item;
    }

    private void enrichOrderItems(List<SaleOrderItem> items) {
        if (items == null || items.isEmpty()) return;
        Set<Long> productIds = items.stream().map(SaleOrderItem::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (productIds.isEmpty()) return;

        List<Product> productList = productService.listByIds(productIds);
        Map<Long, Product> productMap = productList.stream()
                .filter(p -> p != null && p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        for (SaleOrderItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                item.setImage(product.getImageUrl());
                item.setBarcode(product.getBarcode());
                item.setUnit(product.getUnit());
            }
        }
    }

    private String getCustomerGradeCode(Long customerId) {
        if (customerId == null) return null;
        try {
            Long gradeId = partyGradeRelationService.getCurrentGradeId(customerId);
            if (gradeId != null) {
                CustomerGrade grade = customerGradeService.getById(gradeId);
                if (grade != null) return grade.getGradeCode();
            }
            CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
            return defaultGrade != null ? defaultGrade.getGradeCode() : "DEFAULT";
        } catch (Exception e) {
            log.warn("获取客户等级信息失败: customerId={}", customerId);
            return "DEFAULT";
        }
    }

    private String getCustomerGradeName(Long customerId) {
        if (customerId == null) return null;
        try {
            Long gradeId = partyGradeRelationService.getCurrentGradeId(customerId);
            if (gradeId != null) {
                CustomerGrade grade = customerGradeService.getById(gradeId);
                if (grade != null) return grade.getGradeName();
            }
            CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
            return defaultGrade != null ? defaultGrade.getGradeName() : "默认等级";
        } catch (Exception e) {
            log.warn("获取客户等级名称失败: customerId={}", customerId);
            return "默认等级";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待审核";
            case 2 -> "待发货";
            case 3 -> "部分发货";
            case 4 -> "发货完成";
            case 5 -> "交易完成";
            case 6 -> "已取消";
            default -> "未知";
        };
    }

    private String getCurrentUserName() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getSession().get("username", "系统").toString();
            }
        } catch (Exception ignored) {}
        return "系统";
    }

    // ═══════════════════════════════════════════
    // 库存校验 & 冻结/解冻
    // ═══════════════════════════════════════════

    /**
     * 校验订单的库存可用性（仅校验不冻结）
     * @return 库存不足的商品说明列表（空列表表示全部充足）
     */
    private List<String> checkStockAvailability(SaleOrder order) {
        List<String> insufficientItems = new ArrayList<>();
        Long warehouseId = order.getWarehouseId();
        if (warehouseId == null) {
            log.warn("订单未指定仓库，跳过库存校验: orderId={}", order.getId());
            return insufficientItems;
        }

        List<SaleOrderItem> items = itemMapper.selectByOrderId(order.getId());
        if (items == null || items.isEmpty()) return insufficientItems;

        for (SaleOrderItem item : items) {
            if (item.getProductId() == null || item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;

            Stock stock = stockService.getStockDetail(item.getProductId(), warehouseId);
            BigDecimal available = stock != null ? stock.getAvailableQuantity() : BigDecimal.ZERO;

            if (available.compareTo(item.getQuantity()) < 0) {
                insufficientItems.add(String.format(
                        "商品「%s」(编码:%s) 可用库存 %.2f %s，需要 %.2f %s",
                        item.getProductName() != null ? item.getProductName() : "",
                        item.getProductCode() != null ? item.getProductCode() : "",
                        available,
                        item.getUnit() != null ? item.getUnit() : "个",
                        item.getQuantity(),
                        item.getUnit() != null ? item.getUnit() : "个"));
            }
        }
        return insufficientItems;
    }

    /**
     * 校验订单的库存可用性并冻结库存（审批通过时调用）
     * @return 库存不足的商品说明列表（空列表表示全部充足且已冻结）
     */
    private List<String> checkAndFreezeStock(SaleOrder order) {
        List<String> insufficientItems = checkStockAvailability(order);

        // 全部充足才执行冻结
        if (insufficientItems.isEmpty() && order.getWarehouseId() != null) {
            Long warehouseId = order.getWarehouseId();
            List<SaleOrderItem> items = itemMapper.selectByOrderId(order.getId());
            if (items != null) {
                for (SaleOrderItem item : items) {
                    if (item.getProductId() == null || item.getQuantity() == null
                            || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;

                    boolean frozen = stockService.freezeStock(item.getProductId(), warehouseId, item.getQuantity());
                    if (!frozen) {
                        log.warn("库存冻结失败: productId={}, warehouseId={}, quantity={}",
                                item.getProductId(), warehouseId, item.getQuantity());
                    }
                }
                log.info("订单库存冻结完成: orderId={}, warehouseId={}", order.getId(), warehouseId);
            }
        }

        return insufficientItems;
    }

    /**
     * 解冻订单占用的库存（取消已审批订单时调用）
     */
    private void unfreezeOrderStock(SaleOrder order) {
        Long warehouseId = order.getWarehouseId();
        if (warehouseId == null) return;

        List<SaleOrderItem> items = itemMapper.selectByOrderId(order.getId());
        if (items == null || items.isEmpty()) return;

        for (SaleOrderItem item : items) {
            if (item.getProductId() == null || item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            try {
                stockService.unfreezeStock(item.getProductId(), warehouseId, item.getQuantity());
            } catch (Exception e) {
                log.warn("库存解冻失败: productId={}, warehouseId={}, quantity={}",
                        item.getProductId(), warehouseId, item.getQuantity(), e);
            }
        }
        log.info("订单库存解冻完成: orderId={}, warehouseId={}", order.getId(), warehouseId);
    }

    // ═══════════════════════════════════════════
    // 批量导入辅助方法
    // ═══════════════════════════════════════════

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) yield String.valueOf((long) num);
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String getString(Map<String, Object> rowData, String key, String fallbackKey) {
        Object val = rowData.get(key);
        if (val == null || val.toString().isEmpty()) val = rowData.get(fallbackKey);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> rowData, String key, String fallbackKey) {
        Object val = rowData.get(key);
        if (val == null) val = rowData.get(fallbackKey);
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getCurrentTenantId() {
        try {
            if (StpUtil.isLogin()) {
                Object tenantId = StpUtil.getSession().get("tenantId");
                if (tenantId != null) return Long.valueOf(tenantId.toString());
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ═══════════════════════════════════════════
    // 商品汇总 (productSummary)
    // ═══════════════════════════════════════════

    @Override
    public List<Map<String, Object>> productSummary(Long tenantId, Map<String, Object> filters) {
        // 获取符合条件的订单ID列表
        LambdaQueryWrapper<SaleOrder> orderWrapper = buildOrderCenterWrapper(tenantId, filters);
        List<SaleOrder> orders = list(orderWrapper);
        if (orders.isEmpty()) return Collections.emptyList();

        Set<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toSet());

        // 按商品维度聚合明细
        List<SaleOrderItem> allItems = itemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>().in(SaleOrderItem::getOrderId, orderIds));

        // 按商品分组汇总
        Map<Long, List<SaleOrderItem>> groupedByProduct = allItems.stream()
                .filter(i -> i.getProductId() != null)
                .collect(Collectors.groupingBy(SaleOrderItem::getProductId));

        List<Map<String, Object>> result = new ArrayList<>();
        groupedByProduct.forEach((productId, items) -> {
            SaleOrderItem first = items.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", productId);
            row.put("productName", first.getProductName());
            row.put("productCode", first.getProductCode());
            row.put("specification", first.getSpecification());
            row.put("unit", first.getUnit());
            BigDecimal totalQty = items.stream()
                    .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("totalQuantity", totalQty.setScale(2, RoundingMode.HALF_UP));
            BigDecimal totalAmount = items.stream()
                    .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("orderCount", items.stream().map(SaleOrderItem::getOrderId).distinct().count());
            result.add(row);
        });

        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
        return result;
    }

    // ═══════════════════════════════════════════
    // 批量更新物流备注
    // ═══════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateLogisticsRemark(List<Long> ids, String remark) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            SaleOrder order = getById(id);
            if (order != null) {
                order.setOrderRemark(remark);
                updateById(order);
            }
        }
        log.info("批量更新物流备注: ids={}, remark={}", ids.size(), remark);
    }

    /**
     * 「物流/备注」批量更新（对齐 ql361 `OrderRemarks` 弹窗实测字段）
     *
     * <p>前置校验（ql361 `onLogisticsRemarkClick` 实测口径）：
     * ① 所选单据必须「单据状态相同」且「配送方式相同」，否则整批拒绝；
     * ② 已进入配送（status ≥ 3 部分发货）的单据不允许修改配送方式（保留其余字段可改）。</p>
     *
     * <p>字段语义：<b>null / 空串 = 不改动该字段</b>——弹窗允许只填其中几项。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateLogistics(SaleLogisticsRemarkDTO dto) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            throw BusinessException.badRequest("请先勾选要操作的单据");
        }
        List<SaleOrder> orders = new ArrayList<>();
        for (Long id : dto.getIds().stream().filter(Objects::nonNull).distinct().toList()) {
            SaleOrder order = getById(id);
            if (order != null) {
                orders.add(order);
            }
        }
        if (orders.isEmpty()) {
            throw BusinessException.notFound("单据不存在");
        }

        Integer status = orders.get(0).getStatus();
        String deliveryMethod = orders.get(0).getDeliveryMethod();
        for (SaleOrder order : orders) {
            if (!Objects.equals(order.getStatus(), status)) {
                throw BusinessException.badRequest("请选择单据状态相同的单据进行操作");
            }
            if (!Objects.equals(blankToEmpty(order.getDeliveryMethod()), blankToEmpty(deliveryMethod))) {
                throw BusinessException.badRequest("请选择配送方式相同的单据进行操作");
            }
        }

        String newDeliveryMethod = trimToNull(dto.getDeliveryMethod());
        String newDriverName = trimToNull(dto.getDriverName());
        String newWaybillNo = trimToNull(dto.getWaybillNo());
        String newLogisticsCompany = trimToNull(dto.getLogisticsCompany());
        String newReceiverName = trimToNull(dto.getReceiverName());
        String newReceiverPhone = trimToNull(dto.getReceiverPhone());
        String newSalesmanName = trimToNull(dto.getSalesmanName());
        String newShippingAddress = trimToNull(dto.getShippingAddress());
        String newExtText1 = trimToNull(dto.getExtText1());
        String newExtText2 = trimToNull(dto.getExtText2());
        String newExtText3 = trimToNull(dto.getExtText3());
        // 单据备注：优先新字段名 orderRemark，兼容旧入参 remark
        String newOrderRemark = trimToNull(dto.getOrderRemark());
        if (newOrderRemark == null) {
            newOrderRemark = trimToNull(dto.getRemark());
        }

        if (newDeliveryMethod != null && status != null && status >= 3) {
            throw BusinessException.badRequest("配送中的销售订单不能修改配送方式");
        }

        boolean anyField = newDeliveryMethod != null || dto.getDriverId() != null || newDriverName != null
                || newWaybillNo != null || newLogisticsCompany != null || dto.getLogisticsCompanyId() != null
                || newReceiverName != null
                || newReceiverPhone != null || newSalesmanName != null || newShippingAddress != null
                || dto.getSaleType() != null || dto.getExtNum1() != null || dto.getExtNum2() != null
                || newExtText1 != null || newExtText2 != null || newExtText3 != null || newOrderRemark != null;
        if (!anyField) {
            throw BusinessException.badRequest("请至少填写一项要修改的内容");
        }

        // 物流公司：只给 id 时按档案取名称快照
        String carrierName = newLogisticsCompany;
        if (carrierName == null && dto.getLogisticsCompanyId() != null) {
            carrierName = resolveCarrierName(dto.getLogisticsCompanyId());
        }

        for (SaleOrder order : orders) {
            // ① 物流子表（erp_sale_order_logistics）：一条 = 一个包裹；此处写「默认包裹」（P1，无则建）
            SaleOrderLogistics pkg = defaultPackage(order.getId());
            if (pkg == null) {
                pkg = new SaleOrderLogistics().setOrderId(order.getId())
                        .setPackageNo("P1").setPackageStatus(0);
            }
            if (newDeliveryMethod != null) pkg.setDeliveryMethod(newDeliveryMethod);
            if (dto.getDriverId() != null) pkg.setDriverId(dto.getDriverId());
            if (newDriverName != null) pkg.setDriverName(newDriverName);
            if (newWaybillNo != null) pkg.setWaybillNo(newWaybillNo);
            if (dto.getLogisticsCompanyId() != null) pkg.setLogisticsCompanyId(dto.getLogisticsCompanyId());
            if (carrierName != null) pkg.setLogisticsCompany(carrierName);
            if (pkg.getId() == null) {
                logisticsMapper.insert(pkg);
            } else {
                logisticsMapper.updateById(pkg);
            }

            // ② 主表扁平列：降级为「列表展示快照」（拣货/发货 37 列读的就是这几列）
            if (newDeliveryMethod != null) order.setDeliveryMethod(newDeliveryMethod);
            if (dto.getDriverId() != null) order.setDriverId(dto.getDriverId());
            if (newDriverName != null) order.setDriverName(newDriverName);
            if (newWaybillNo != null) order.setWaybillNo(newWaybillNo);
            if (carrierName != null) order.setLogisticsCompany(carrierName);
            if (newReceiverName != null) order.setReceiverName(newReceiverName);
            if (newReceiverPhone != null) order.setReceiverPhone(newReceiverPhone);
            if (newSalesmanName != null) order.setSalesmanName(newSalesmanName);
            if (newShippingAddress != null) order.setShippingAddress(newShippingAddress);
            if (dto.getSaleType() != null) order.setSaleType(dto.getSaleType());
            if (dto.getExtNum1() != null) order.setExtNum1(dto.getExtNum1());
            if (dto.getExtNum2() != null) order.setExtNum2(dto.getExtNum2());
            if (newExtText1 != null) order.setExtText1(newExtText1);
            if (newExtText2 != null) order.setExtText2(newExtText2);
            if (newExtText3 != null) order.setExtText3(newExtText3);
            if (newOrderRemark != null) order.setOrderRemark(newOrderRemark);
            updateById(order);
        }
        log.info("物流/备注批量更新: 单数={}, 运单号={}, 承运商={}(id={}), 备注={}",
                orders.size(), newWaybillNo, carrierName, dto.getLogisticsCompanyId(), newOrderRemark);
        return orders.size();
    }

    /** 订单的「默认包裹」：子表按 id 升序第一条（P1 约定，无则为 null） */
    private SaleOrderLogistics defaultPackage(Long orderId) {
        return logisticsMapper.selectOne(new LambdaQueryWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getOrderId, orderId)
                .orderByAsc(SaleOrderLogistics::getId)
                .last("LIMIT 1"));
    }

    /** 承运商名称快照（取不到则回落 null，不阻断主流程） */
    private String resolveCarrierName(Long carrierId) {
        try {
            Party party = partyMapper.selectById(carrierId);
            return party == null ? null : party.getPartyName();
        } catch (Exception e) {
            log.warn("解析承运商名称失败: carrierId={}", carrierId, e);
            return null;
        }
    }

    /** 空串归一为 null：null / 空白 = 不修改 */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 比较用：null 与空串视为同一值 */
    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

}
