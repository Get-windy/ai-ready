package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpProductMallMapper;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMallMapper;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMallMapper;
import cn.aiedge.erp.b2b.dto.*;
import cn.aiedge.erp.b2b.mapper.MallAddressMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.MallAddress;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.PartyService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 商城订单服务实现
 * 数据存储于 erp_sale_order/erp_sale_order_item
 * - B2B 企业客户订单：order_source=2
 * - B2C 个人会员订单：order_source=3
 * 根据 shop_user.user_type 自动路由到对应通道
 *
 * <p><b>商城商品数据源（2026-09-14 修复）</b>：本类此前从已废弃的 {@code mall_product} 表读商品并回写其库存
 * （{@code V9.0.0__Trade_Center_Consolidation.sql} Part 9 已标注「[已废弃] 由 v_mall_product 视图替代，
 * 数据源为 erp_product」）。现读侧统一为 {@code v_mall_product} 视图（{@link ErpProductMallMapper}，
 * 与商城列表/购物车同源）；写侧不再回写库存（{@code erp_product} 无库存列，视图 {@code stock_quantity}
 * 由 {@code erp_stock} 实时聚合，商城侧无库存写入通道），详见 {@code createOrder} 内注释。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl implements MallOrderService {

    private final ErpSaleOrderMallMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMallMapper erpSaleOrderItemMapper;
    /**
     * 商城商品**只读**数据源：{@code v_mall_product} 视图（源表 {@code erp_product} + {@code erp_stock} 实时聚合）。
     * 与商城商品列表（{@code MallAdminServiceImpl.pageProducts}）、购物车（{@code MallCartServiceImpl}）同源。
     * 不再使用已废弃的 {@code mall_product} 表（见 {@link cn.aiedge.erp.b2b.model.MallProduct}）。
     */
    private final ErpProductMallMapper erpProductMallMapper;
    private final ShopUserMapper shopUserMapper;
    private final MallAddressMapper mallAddressMapper;
    private final PartyService partyService;

    private static final AtomicLong ORDER_NO_COUNTER = new AtomicLong(0);

    /** 企业客户商城订单来源值（对应 erp_sale_order.order_source） */
    private static final int ORDER_SOURCE_B2B_MALL = 2;

    /** 个人会员商城订单来源值（对应 erp_sale_order.order_source） */
    private static final int ORDER_SOURCE_MEMBER_MALL = 3;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    // ==================== 状态映射 ====================

    /** 商城状态 → erp_sale_order.status */
    private int toErpStatus(String mallStatus) {
        switch (mallStatus) {
            case "PENDING_PAYMENT": return 0;  // 草稿
            case "PAID":            return 1;  // 待审批
            case "APPROVED":        return 2;  // 已审批
            case "SHIPPED":         return 3;  // 部分出库
            case "COMPLETED":       return 4;  // 完成
            case "CANCELLED":       return 6;  // 已取消（erp_sale_order 规范：5=交易完成 6=已取消）
            case "REJECTED":        return 6;  // 已取消（同上）
            default:                return 0;
        }
    }

    /** erp_sale_order.status → 商城状态（从 extInfo 恢复原始状态） */
    private String toMallStatus(ErpSaleOrderMall order) {
        // 优先从 extInfo 恢复原始商城状态
        if (order.getExtInfo() != null && order.getExtInfo().contains("\"originalMallStatus\"")) {
            try {
                int idx = order.getExtInfo().indexOf("\"originalMallStatus\"");
                int valStart = order.getExtInfo().indexOf(':', idx) + 2;
                int valEnd = order.getExtInfo().indexOf('"', valStart);
                if (valStart > 1 && valEnd > valStart) {
                    return order.getExtInfo().substring(valStart, valEnd);
                }
            } catch (Exception e) {
                log.warn("解析 extInfo.originalMallStatus 失败", e);
            }
        }
        // 回退：根据 erp 状态推断
        switch (order.getStatus() != null ? order.getStatus() : 0) {
            case 0:  return "PENDING_PAYMENT";
            case 1:  return "PAID";
            case 2:  return "APPROVED";
            case 3:  return "SHIPPED";
            case 4:  return "COMPLETED";
            case 5:  return "COMPLETED";  // 交易完成（erp 规范口径）
            case 6:  return "CANCELLED";
            default: return "PENDING_PAYMENT";
        }
    }

    /** 商城支付状态 → erp_sale_order.payment_status */
    private int toErpPaymentStatus(String mallPaymentStatus) {
        switch (mallPaymentStatus) {
            case "UNPAID":   return 0;
            case "PAID":     return 2;
            case "REFUNDED": return 4;
            default:         return 0;
        }
    }

    /** erp_sale_order.payment_status → 商城支付状态 */
    private String toMallPaymentStatus(Integer erpPaymentStatus) {
        if (erpPaymentStatus == null) return "UNPAID";
        switch (erpPaymentStatus) {
            case 2:  return "PAID";
            case 4:  return "REFUNDED";
            default: return "UNPAID";
        }
    }

    /** 商城发货状态 → erp_sale_order.delivery_status */
    private int toErpDeliveryStatus(String mallDeliveryStatus) {
        switch (mallDeliveryStatus) {
            case "UNSHIPPED":  return 0;
            case "DELIVERING": return 2;
            case "RECEIVED":   return 3;
            case "UNDELIVERED":return 0;
            default:           return 0;
        }
    }

    /** erp_sale_order.delivery_status → 商城发货状态 */
    private String toMallDeliveryStatus(Integer erpDeliveryStatus) {
        if (erpDeliveryStatus == null) return "UNSHIPPED";
        switch (erpDeliveryStatus) {
            case 2:  return "DELIVERING";
            case 3:  return "RECEIVED";
            default: return "UNSHIPPED";
        }
    }

    /** 构建 extInfo JSON */
    private String buildExtInfo(String originalMallStatus) {
        return "{\"originalMallStatus\":\"" + originalMallStatus + "\",\"source\":\"b2b_mall\"}";
    }

    // ==================== 核心业务方法 ====================

    @Override
    @Transactional
    public OrderDetailDTO createOrder(OrderCreateRequest request) {
        log.info("创建商城订单");
        Long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest("订单商品不能为空");
        }

        // 获取用户信息
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // ── 身份路由：使用 session 中激活的身份（支持多身份切换） ──
        Object activeObj = StpUtil.getSession().get("activePartyId");
        Long activePartyId = activeObj instanceof Number ? ((Number) activeObj).longValue() : null;
        Long erpCustomerId = activePartyId != null ? activePartyId : user.getPartyId();

        String customerName;
        int orderSource;
        String activeUserType;

        // 查询激活身份对应的 biz_party 确定身份类型
        Party activeParty = null;
        if (erpCustomerId != null) {
            try {
                activeParty = partyService.getById(erpCustomerId);
            } catch (Exception e) {
                log.warn("获取激活身份失败: partyId={}", erpCustomerId, e);
            }
        }

        if (activeParty != null && !"MEMBER".equals(activeParty.getPartyLevel())) {
            // ENTERPRISE：走B2B销售订单通道
            activeUserType = "ENTERPRISE";
            customerName = activeParty.getPartyName() != null ? activeParty.getPartyName() : user.getUsername();
            orderSource = ORDER_SOURCE_B2B_MALL;
        } else if (activeParty != null) {
            // MEMBER：走零售/会员通道
            activeUserType = "MEMBER";
            customerName = activeParty.getPartyName() != null ? activeParty.getPartyName()
                    : (user.getNickname() != null ? user.getNickname() : user.getUsername());
            orderSource = ORDER_SOURCE_MEMBER_MALL;
        } else {
            // 没有 biz_party 关联，回退到 shop_user.userType
            activeUserType = user.getUserType() != null ? user.getUserType() : "MEMBER";
            if ("ENTERPRISE".equals(activeUserType)) {
                customerName = (user.getCompanyName() != null && !user.getCompanyName().isEmpty())
                        ? user.getCompanyName() : user.getUsername();
                orderSource = ORDER_SOURCE_B2B_MALL;
            } else {
                customerName = (user.getNickname() != null && !user.getNickname().isEmpty())
                        ? user.getNickname() : user.getUsername();
                orderSource = ORDER_SOURCE_MEMBER_MALL;
            }
        }

        // 如果没有 partyId，回退到旧的 erp_partner_id / erp_customer_id
        if (erpCustomerId == null) {
            erpCustomerId = user.getErpPartnerId() != null ? user.getErpPartnerId() : user.getErpCustomerId();
        }
        if (erpCustomerId == null) {
            erpCustomerId = user.getId();  // 兜底：用 shop_user.id
            log.warn("商城用户无关联 biz_party，使用 shop_user.id 作为 customerId: userId={}", user.getId());
        }

        // 计算金额和创建订单明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ErpSaleOrderItemMall> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            // ── 数据源修复（2026-09-14）：商品读取由已废弃的 mall_product 表改走 v_mall_product 视图 ──
            // mall_product 已在 V9.0.0 Part 9 标注「[已废弃] 由 v_mall_product 视图替代，数据源为 erp_product」，
            // 实为 erp_product → mall_product 的**单向**触发器缓存（trg_erp_product_sync_mall 只同步商品字段，
            // 且新行 stock_quantity 固定写 0）。真库复核：该表 85 行 stock_quantity 全为 0、sale_price 全为 0.00，
            // 故原读法使「库存不足」恒真、单价恒为 0（商城订单创建实际不可用）。
            // 视图与列表/购物车同源：product_id ← erp_product.product_code，
            // stock_quantity ← erp_stock 实时聚合，sale_price ← erp_product.retail_price。
            // （租户过滤由 TenantLineInnerInterceptor 自动注入 tenant_id，视图含该列。）
            ErpProductMall product = erpProductMallMapper.selectOne(
                    new LambdaQueryWrapper<ErpProductMall>()
                            .eq(ErpProductMall::getProductId, itemRequest.getProductId())
            );
            if (product == null) {
                throw BusinessException.notFound("商品不存在: " + itemRequest.getProductId());
            }
            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (availableStock < itemRequest.getQuantity()) {
                throw BusinessException.badRequest("商品库存不足: " + product.getProductName());
            }

            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            ErpSaleOrderItemMall item = new ErpSaleOrderItemMall();
            item.setProductId(null); // erp_product.id 未知，后续由产品编码映射
            item.setProductCode(itemRequest.getProductId());
            item.setProductName(product.getProductName());
            item.setQuantity(BigDecimal.valueOf(itemRequest.getQuantity()));
            item.setUnitPrice(price);
            item.setAmount(subtotal);
            orderItems.add(item);

            totalAmount = totalAmount.add(subtotal);

            // 库存**不**由商城侧回写（修复说明；如实保留为已知缺口，不做假实现）：
            // 1) 原写法扣减 mall_product.stock_quantity 后 updateById，但 mall_product 是
            //    erp_product → mall_product 的**单向**触发器缓存（旧注释「由 DB 触发器同步到 erp_product」方向写反了），
            //    且该表已无任何读路径（商品列表/购物车/本方法均读 v_mall_product 视图），属无效写入；
            // 2) 视图 stock_quantity 由 erp_stock 实时聚合，erp_product **无库存列**（真库核对 information_schema 确认），
            //    erp-mall 模块未依赖 erp-stock、也无 erp_stock 写入通道，硬写会造出假库存；
            // 3) 商城订单创建只做可用量校验（见上），实际扣减应由 ERP 侧出库单据驱动 erp_stock。
        }

        // 创建 erp_sale_order 订单
        ErpSaleOrderMall order = new ErpSaleOrderMall();
        order.setTenantId(tenantId);
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(erpCustomerId);
        order.setCustomerName(customerName);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(toErpStatus("PENDING_PAYMENT"));
        order.setOrderSource(orderSource);
        order.setTotalAmount(totalAmount);
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(toErpPaymentStatus("UNPAID"));
        order.setDeliveryStatus(toErpDeliveryStatus("UNSHIPPED"));
        order.setRemark(request.getRemark());
        order.setBuyerRemark(request.getRemark());
        order.setExtInfo(buildExtInfo("PENDING_PAYMENT"));

        // 地址信息
        if (request.getAddressId() != null) {
            MallAddress addr = mallAddressMapper.selectById(request.getAddressId());
            if (addr != null) {
                order.setConsignee(addr.getConsignee());
                order.setConsigneePhone(addr.getPhone());
                String fullAddr = addr.getProvince() + addr.getCity() + addr.getDistrict() + " " + addr.getDetailAddress();
                order.setConsigneeAddress(fullAddr);
                order.setShippingAddress(fullAddr);
            }
        } else {
            order.setConsignee(request.getConsignee());
            order.setConsigneePhone(request.getPhone());
            order.setConsigneeAddress(request.getAddress());
            order.setShippingAddress(request.getAddress());
        }

        erpSaleOrderMapper.insert(order);

        // 保存订单明细
        for (int i = 0; i < orderItems.size(); i++) {
            ErpSaleOrderItemMall item = orderItems.get(i);
            item.setOrderId(order.getId());
            item.setLineNo(i + 1);
            erpSaleOrderItemMapper.insert(item);
        }

        log.info("订单创建成功: {}", order.getOrderNo());
        return convertToDetailDTO(order, orderItems);
    }

    @Override
    public PageResult<OrderListDTO> listOrders(int page, int size, String orderStatus) {
        log.info("查询订单列表: page={}, size={}, orderStatus={}", page, size, orderStatus);
        Long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        // 使用 session 中激活的身份查询订单
        Object activeObj = StpUtil.getSession().get("activePartyId");
        Long activePartyId = activeObj instanceof Number ? ((Number) activeObj).longValue() : null;

        ShopUser currentUser = shopUserMapper.selectById(userId);
        Long queryPartyId = activePartyId;
        if (queryPartyId == null && currentUser != null) {
            queryPartyId = currentUser.getPartyId();
        }
        if (queryPartyId == null) {
            queryPartyId = userId;
        }

        // 根据激活身份类型确定订单来源过滤
        int expectedOrderSource = ORDER_SOURCE_MEMBER_MALL;
        if (activePartyId != null) {
            try {
                Party activeParty = partyService.getById(activePartyId);
                if (activeParty != null && !"MEMBER".equals(activeParty.getPartyLevel())) {
                    expectedOrderSource = ORDER_SOURCE_B2B_MALL;
                }
            } catch (Exception ignored) {}
        } else if (currentUser != null && "ENTERPRISE".equals(currentUser.getUserType())) {
            expectedOrderSource = ORDER_SOURCE_B2B_MALL;
        }

        LambdaQueryWrapper<ErpSaleOrderMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpSaleOrderMall::getCustomerId, queryPartyId);
        wrapper.eq(ErpSaleOrderMall::getTenantId, tenantId);
        wrapper.eq(ErpSaleOrderMall::getOrderSource, expectedOrderSource);
        wrapper.eq(ErpSaleOrderMall::getDeleted, 0);
        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(ErpSaleOrderMall::getStatus, toErpStatus(orderStatus));
        }
        wrapper.orderByDesc(ErpSaleOrderMall::getCreateTime);

        IPage<ErpSaleOrderMall> orderPage = erpSaleOrderMapper.selectPage(new Page<>(page, size), wrapper);

        List<OrderListDTO> records = orderPage.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

        PageResult<OrderListDTO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(orderPage.getTotal());
        result.setPage((int) orderPage.getCurrent());
        result.setSize((int) orderPage.getSize());

        return result;
    }

    /**
     * 当前调用者的「身份标识」——与 {@link #listOrders} 里 {@code queryPartyId} **同一口径**：
     * 会话激活身份 → shop_user.partyId → shop_user.id（下单时的兜底值）。
     *
     * <p>抽成一处是刻意的：订单写入时的 {@code customer_id} 用的就是这套解析
     * （见 {@code createOrder}），列表查询用的也是它。若这里再写第二套口径，
     * 迟早出现「列表看得到、详情说无权」这类自相矛盾。</p>
     */
    private Long currentCallerPartyId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Object activeObj = StpUtil.getSession().get("activePartyId");
        Long activePartyId = activeObj instanceof Number ? ((Number) activeObj).longValue() : null;
        if (activePartyId != null) {
            return activePartyId;
        }
        ShopUser user = shopUserMapper.selectById(userId);
        if (user != null && user.getPartyId() != null) {
            return user.getPartyId();
        }
        return userId;
    }

    /**
     * 取出**属于当前调用者**的订单；不属于则拒绝。
     *
     * <p>2026-09-23 补：本类此前所有按 id 的操作都是 {@code selectById} 之后直接用，
     * 不校验归属 —— 商城端点（{@code /api/v1/mall/orders/**}）只要求「已登录」，
     * 于是同租户内任意买家可按 id 读别人的订单（含收件人/电话/地址）、取消/确认别人的订单
     * （IDOR / BOLA，见 TRADE_MODULE_AUDIT_20260923.md P0-5）。
     * 租户维度由租户拦截器兜住（跨租户查到的是 null），**租户内跨用户**此前完全没有防线。</p>
     *
     * <p>返回 403 而非 404：调用方本来就是"合法登录但无权看这一单"，
     * 与"订单不存在"是两种不同的运维语义，分开更好排障。</p>
     */
    private ErpSaleOrderMall requireMyOrder(Long id) {
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }
        Long callerPartyId = currentCallerPartyId();
        if (!Objects.equals(order.getCustomerId(), callerPartyId)) {
            log.warn("越权访问商城订单被拒: orderId={}, orderCustomerId={}, caller={}",
                    id, order.getCustomerId(), callerPartyId);
            throw BusinessException.forbidden("无权访问该订单");
        }
        return order;
    }

    @Override
    public OrderDetailDTO getOrderDetail(Long id) {
        log.info("获取订单详情: {}", id);
        ErpSaleOrderMall order = requireMyOrder(id);

        List<ErpSaleOrderItemMall> items = erpSaleOrderItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrderItemMall>()
                        .eq(ErpSaleOrderItemMall::getOrderId, order.getId())
        );

        return convertToDetailDTO(order, items);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.info("取消订单: {}", id);
        ErpSaleOrderMall order = requireMyOrder(id);

        String currentMallStatus = toMallStatus(order);
        if (!"PENDING_PAYMENT".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许取消");
        }

        order.setStatus(toErpStatus("CANCELLED"));
        order.setExtInfo(buildExtInfo("CANCELLED"));
        erpSaleOrderMapper.updateById(order);

        // 无需归还库存：createOrder 已不再由商城侧扣减库存（见其修复说明），
        // 原 restoreStock() 回写 mall_product.stock_quantity 的写法已随之移除。
        log.info("订单已取消: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void confirmOrder(Long id) {
        log.info("确认收货: {}", id);
        ErpSaleOrderMall order = requireMyOrder(id);

        String currentMallStatus = toMallStatus(order);
        if (!"SHIPPED".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许确认收货");
        }

        order.setStatus(toErpStatus("COMPLETED"));
        order.setDeliveryStatus(toErpDeliveryStatus("RECEIVED"));
        order.setExtInfo(buildExtInfo("COMPLETED"));
        erpSaleOrderMapper.updateById(order);

        log.info("订单已确认收货: {}", order.getOrderNo());
    }

    // ⚠️ 2026-09-23 移除三个方法：payOrder / approveOrder / rejectOrder。
    //   · payOrder 只置状态为已付（receivedAmount=总额）而**不产生支付记录** ⇒ 买卖双方任一侧
    //     调一次即可"白拿单"；正确路径是 core-payment 的 createPayment + 渠道回调驱动状态。
    //   · approve/reject 是**审核动作**，在买家端暴露等于买家可自审通过；管理端已有带
    //     @SaCheckPermission 的等价实现（MallAdminServiceImpl#approveOrder/rejectOrder）。
    //   · 三者在前端 pc-admin / mobile-mall 中零调用方；对应控制器端点同步移除。
    //   详见 TRADE_MODULE_AUDIT_20260923.md P0-5、P2-5。

    @Override
    public List<Map<String, Object>> getPaymentMethods() {
        log.info("获取支付方式列表");
        List<Map<String, Object>> methods = new ArrayList<>();

        Map<String, Object> wechat = new LinkedHashMap<>();
        wechat.put("id", "wechat");
        wechat.put("name", "微信支付");
        wechat.put("icon", "wechat");
        methods.add(wechat);

        Map<String, Object> alipay = new LinkedHashMap<>();
        alipay.put("id", "alipay");
        alipay.put("name", "支付宝");
        alipay.put("icon", "alipay");
        methods.add(alipay);

        Map<String, Object> bank = new LinkedHashMap<>();
        bank.put("id", "bank_transfer");
        bank.put("name", "银行转账");
        bank.put("icon", "bank");
        methods.add(bank);

        return methods;
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_MS_PATTERN);
        long seq = ORDER_NO_COUNTER.incrementAndGet() % 10000;
        return "ORD" + dateStr + String.format("%04d", seq);
    }

    // ==================== DTO 转换 ====================

    private OrderListDTO convertToListDTO(ErpSaleOrderMall order) {
        OrderListDTO dto = new OrderListDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getTotalAmount());
        dto.setOrderStatus(toMallStatus(order));
        dto.setCreatedAt(order.getCreateTime());

        Long itemCount = erpSaleOrderItemMapper.selectCount(
                new LambdaQueryWrapper<ErpSaleOrderItemMall>()
                        .eq(ErpSaleOrderItemMall::getOrderId, order.getId())
        );
        dto.setItemCount(itemCount != null ? itemCount.intValue() : 0);

        return dto;
    }

    private OrderDetailDTO convertToDetailDTO(ErpSaleOrderMall order, List<ErpSaleOrderItemMall> items) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getTotalAmount());
        dto.setOrderStatus(toMallStatus(order));
        dto.setCreatedAt(order.getCreateTime());
        dto.setCustomerName(order.getCustomerName());
        dto.setConsignee(order.getConsignee());
        dto.setPhone(order.getConsigneePhone());
        dto.setAddress(order.getShippingAddress() != null ? order.getShippingAddress() : order.getConsigneeAddress());
        dto.setItemCount(items != null ? items.size() : 0);

        if (items != null) {
            List<OrderDetailDTO.OrderItemDTO> itemDTOs = items.stream().map(item -> {
                OrderDetailDTO.OrderItemDTO itemDTO = new OrderDetailDTO.OrderItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductCode());
                itemDTO.setProductName(item.getProductName());
                itemDTO.setProductImage(null); // erp_sale_order_item 不直接存图片，可由 productCode 查询
                itemDTO.setPrice(item.getUnitPrice());
                itemDTO.setQuantity(item.getQuantity().intValue());
                itemDTO.setSubtotal(item.getAmount());
                return itemDTO;
            }).collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }

        return dto;
    }
}
