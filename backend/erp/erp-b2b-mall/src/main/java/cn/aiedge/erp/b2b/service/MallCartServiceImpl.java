package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.CartAddRequest;
import cn.aiedge.erp.b2b.dto.CartDTO;
import cn.aiedge.erp.b2b.mapper.MallCartMapper;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.model.MallCart;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallCartServiceImpl implements MallCartService {

    private final MallCartMapper mallCartMapper;
    private final MallProductMapper mallProductMapper;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        // tenant_id 从 Sa-Token 会话中获取，在登录时存入
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    public List<CartDTO> getCart() {
        log.info("获取购物车");
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        List<MallCart> cartItems = mallCartMapper.selectList(
                new LambdaQueryWrapper<MallCart>()
                        .eq(MallCart::getCustomerId, customerId)
                        .eq(MallCart::getTenantId, tenantId)
                        .eq(MallCart::getDeleted, 0)
        );

        return cartItems.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartDTO addToCart(CartAddRequest request) {
        log.info("添加购物车: productId={}, quantity={}", request.getProductId(), request.getQuantity());
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        // Check if product exists
        MallProduct product = mallProductMapper.selectOne(
                new LambdaQueryWrapper<MallProduct>()
                        .eq(MallProduct::getProductId, request.getProductId())
                        .eq(MallProduct::getDeleted, 0)
        );
        if (product == null) {
            throw BusinessException.notFound("商品不存在: " + request.getProductId());
        }

        // Check if already in cart
        MallCart existing = mallCartMapper.selectOne(
                new LambdaQueryWrapper<MallCart>()
                        .eq(MallCart::getCustomerId, customerId)
                        .eq(MallCart::getProductId, request.getProductId())
                        .eq(MallCart::getTenantId, tenantId)
                        .eq(MallCart::getDeleted, 0)
        );

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setSubtotal(existing.getPrice().multiply(BigDecimal.valueOf(existing.getQuantity())));
            mallCartMapper.updateById(existing);
            return convertToDTO(existing);
        }

        MallCart cart = new MallCart();
        cart.setCustomerId(customerId);
        cart.setTenantId(tenantId);
        cart.setProductId(request.getProductId());
        cart.setProductName(product.getProductName());
        cart.setProductImage(product.getImageUrl());
        cart.setPrice(product.getSalePrice());
        cart.setQuantity(request.getQuantity());
        cart.setSubtotal(product.getSalePrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        cart.setChecked(true);

        mallCartMapper.insert(cart);
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateCartItem(Long id, CartAddRequest request) {
        log.info("更新购物车: id={}, quantity={}", id, request.getQuantity());
        MallCart cart = mallCartMapper.selectById(id);
        if (cart == null) {
            throw BusinessException.notFound("购物车项不存在: " + id);
        }

        cart.setQuantity(request.getQuantity());
        cart.setSubtotal(cart.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));

        mallCartMapper.updateById(cart);
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public void removeFromCart(Long id) {
        log.info("删除购物车项: {}", id);
        MallCart cart = mallCartMapper.selectById(id);
        if (cart == null) {
            throw BusinessException.notFound("购物车项不存在: " + id);
        }
        cart.setDeleted(1);
        mallCartMapper.updateById(cart);
    }

    @Override
    @Transactional
    public void clearCart() {
        log.info("清空购物车");
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        List<MallCart> cartItems = mallCartMapper.selectList(
                new LambdaQueryWrapper<MallCart>()
                        .eq(MallCart::getCustomerId, customerId)
                        .eq(MallCart::getTenantId, tenantId)
                        .eq(MallCart::getDeleted, 0)
        );

        for (MallCart item : cartItems) {
            item.setDeleted(1);
            mallCartMapper.updateById(item);
        }
    }

    @Override
    public void checkStock() {
        log.info("检查库存");
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        List<MallCart> cartItems = mallCartMapper.selectList(
                new LambdaQueryWrapper<MallCart>()
                        .eq(MallCart::getCustomerId, customerId)
                        .eq(MallCart::getTenantId, tenantId)
                        .eq(MallCart::getDeleted, 0)
                        .eq(MallCart::getChecked, true)
        );

        for (MallCart item : cartItems) {
            MallProduct product = mallProductMapper.selectOne(
                    new LambdaQueryWrapper<MallProduct>()
                            .eq(MallProduct::getProductId, item.getProductId())
                            .eq(MallProduct::getTenantId, tenantId)
            );
            if (product != null && product.getStockQuantity() < item.getQuantity()) {
                throw BusinessException.badRequest("商品库存不足: " + item.getProductName()
                        + ", 库存: " + product.getStockQuantity() + ", 需要: " + item.getQuantity());
            }
        }
    }

    private CartDTO convertToDTO(MallCart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setProductId(cart.getProductId());
        dto.setProductName(cart.getProductName());
        dto.setProductImage(cart.getProductImage());
        dto.setPrice(cart.getPrice());
        dto.setQuantity(cart.getQuantity());
        dto.setSubtotal(cart.getSubtotal());
        dto.setChecked(cart.getChecked());
        return dto;
    }
}
