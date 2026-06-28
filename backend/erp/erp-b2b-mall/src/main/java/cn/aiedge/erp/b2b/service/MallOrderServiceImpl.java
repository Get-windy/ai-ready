package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMallMapper;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMallMapper;
import cn.aiedge.erp.b2b.dto.*;
import cn.aiedge.erp.b2b.mapper.MallAddressMapper;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.MallAddress;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopUser;
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
 * 数据存储于 erp_sale_order/erp_sale_order_item（order_source=2）
 * 替代原有的 mall_order/mall_order_item 表，消除数据冗余
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl implements MallOrderService {

    private final ErpSaleOrderMallMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMallMapper erpSaleOrderItemMapper;
    private final MallProductMapper mallProductMapper;
    private final ShopUserMapper shopUserMapper;
    private final MallAddressMapper mallAddressMapper;

    private static final AtomicLong ORDER_NO_COUNTER = new AtomicLong(0);

    /** B2B 商城订单来源值（对应 erp_sale_order.order_source） */
    private static final int ORDER_SOURCE_B2B_MALL = 2;

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
            case "CANCELLED":       return 5;  // 取消
            case "REJECTED":        return 5;  // 取消
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
            case 5:  return "CANCELLED";
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
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest("订单商品不能为空");
        }

        // 获取用户信息用于订单客户名称
        ShopUser user = shopUserMapper.selectById(customerId);
        String customerName = (user != null && user.getCompanyName() != null && !user.getCompanyName().isEmpty())
                ? user.getCompanyName() : (user != null ? user.getUsername() : "");

        // 计算金额和创建订单明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ErpSaleOrderItemMall> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            MallProduct product = mallProductMapper.selectOne(
                    new LambdaQueryWrapper<MallProduct>()
                            .eq(MallProduct::getProductId, itemRequest.getProductId())
                            .eq(MallProduct::getDeleted, 0)
            );
            if (product == null) {
                throw BusinessException.notFound("商品不存在: " + itemRequest.getProductId());
            }
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
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

            // 扣除 mall_product 库存（由 DB 触发器同步到 erp_product）
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            mallProductMapper.updateById(product);
        }

        // 创建 erp_sale_order 订单
        ErpSaleOrderMall order = new ErpSaleOrderMall();
        order.setTenantId(tenantId);
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setCustomerName(customerName);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(toErpStatus("PENDING_PAYMENT"));
        order.setOrderSource(ORDER_SOURCE_B2B_MALL);
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
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        LambdaQueryWrapper<ErpSaleOrderMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpSaleOrderMall::getCustomerId, customerId);
        wrapper.eq(ErpSaleOrderMall::getTenantId, tenantId);
        wrapper.eq(ErpSaleOrderMall::getOrderSource, ORDER_SOURCE_B2B_MALL);
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

    @Override
    public OrderDetailDTO getOrderDetail(Long id) {
        log.info("获取订单详情: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

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
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        String currentMallStatus = toMallStatus(order);
        if (!"PENDING_PAYMENT".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许取消");
        }

        order.setStatus(toErpStatus("CANCELLED"));
        order.setExtInfo(buildExtInfo("CANCELLED"));
        erpSaleOrderMapper.updateById(order);

        // 归还库存
        restoreStock(order.getId());
        log.info("订单已取消: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void confirmOrder(Long id) {
        log.info("确认收货: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

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

    @Override
    @Transactional
    public void payOrder(Long id) {
        log.info("支付订单: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        String currentMallStatus = toMallStatus(order);
        if (!"PENDING_PAYMENT".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }

        order.setStatus(toErpStatus("PAID"));
        order.setPaymentStatus(toErpPaymentStatus("PAID"));
        order.setReceivedAmount(order.getTotalAmount());
        order.setExtInfo(buildExtInfo("PAID"));
        erpSaleOrderMapper.updateById(order);

        log.info("订单支付成功: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void approveOrder(Long id) {
        log.info("审核通过订单: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        String currentMallStatus = toMallStatus(order);
        if (!"PAID".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许审核");
        }

        order.setStatus(toErpStatus("APPROVED"));
        order.setExtInfo(buildExtInfo("APPROVED"));
        erpSaleOrderMapper.updateById(order);

        log.info("订单已审核通过: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void rejectOrder(Long id, String reason) {
        log.info("审核驳回订单: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        String currentMallStatus = toMallStatus(order);
        if (!"PAID".equals(currentMallStatus) && !"PENDING_PAYMENT".equals(currentMallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许驳回");
        }

        order.setStatus(toErpStatus("REJECTED"));
        order.setRemark(reason);
        order.setExtInfo(buildExtInfo("REJECTED"));
        erpSaleOrderMapper.updateById(order);

        // 驳回时归还库存
        restoreStock(order.getId());
        log.info("订单已驳回: {}", order.getOrderNo());
    }

    /** 归还订单占用的库存 */
    private void restoreStock(Long orderId) {
        List<ErpSaleOrderItemMall> items = erpSaleOrderItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrderItemMall>()
                        .eq(ErpSaleOrderItemMall::getOrderId, orderId)
        );
        for (ErpSaleOrderItemMall item : items) {
            MallProduct product = mallProductMapper.selectOne(
                    new LambdaQueryWrapper<MallProduct>()
                            .eq(MallProduct::getProductId, item.getProductCode())
            );
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity().intValue());
                mallProductMapper.updateById(product);
            }
        }
    }

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
