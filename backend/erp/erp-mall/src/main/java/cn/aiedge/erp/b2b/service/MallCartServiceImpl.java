package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.b2b.dto.CartAddRequest;
import cn.aiedge.erp.b2b.dto.CartDTO;
import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpProductMallMapper;
import cn.aiedge.erp.b2b.mapper.MallCartMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.MallCart;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallCartServiceImpl implements MallCartService {

    private final MallCartMapper mallCartMapper;
    private final ErpProductMallMapper erpProductMallMapper;
    private final ShopUserMapper shopUserMapper;

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
    public PageResult<CartDTO> pageCart(Integer pageNum, Integer pageSize, Long customerId,
                                        String memberKeyword, String productKeyword) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 20 : pageSize;
        Long tenantId = getTenantId();
        log.info("分页查询购物车: pageNum={}, pageSize={}, customerId={}, memberKeyword={}, productKeyword={}",
                current, size, customerId, memberKeyword, productKeyword);

        LambdaQueryWrapper<MallCart> wrapper = new LambdaQueryWrapper<MallCart>()
                .eq(MallCart::getDeleted, 0)
                .eq(MallCart::getTenantId, tenantId)
                .eq(customerId != null, MallCart::getCustomerId, customerId);

        // 商品关键字：商品名称 / 商品编码（mall_cart.product_id 存的是商品编码）
        if (StringUtils.hasText(productKeyword)) {
            String kw = productKeyword.trim();
            wrapper.and(w -> w.like(MallCart::getProductName, kw)
                    .or().like(MallCart::getProductId, kw));
        }

        // 会员关键字：先按 shop_user（登录名/昵称/手机号/公司名）解析出会员ID集合，
        // 再以 customer_id IN (...) 过滤——mall_cart 只有 customer_id，无会员名称列，不新增列。
        if (StringUtils.hasText(memberKeyword)) {
            String kw = memberKeyword.trim();
            List<Long> memberIds = shopUserMapper.selectList(new LambdaQueryWrapper<ShopUser>()
                            .eq(ShopUser::getDeleted, 0)
                            .and(w -> w.like(ShopUser::getUsername, kw)
                                    .or().like(ShopUser::getNickname, kw)
                                    .or().like(ShopUser::getPhone, kw)
                                    .or().like(ShopUser::getCompanyName, kw)))
                    .stream().map(ShopUser::getId).filter(java.util.Objects::nonNull).toList();
            if (memberIds.isEmpty()) {
                // 无匹配会员 → 空分页（避免退化为全量返回）
                return PageResult.empty(current, size);
            }
            wrapper.in(MallCart::getCustomerId, memberIds);
        }

        wrapper.orderByDesc(MallCart::getCreateTime).orderByDesc(MallCart::getId);
        Page<MallCart> page = mallCartMapper.selectPage(new Page<>(current, size), wrapper);

        Map<Long, ShopUser> memberMap = loadMembers(page.getRecords());
        Map<String, ErpProductMall> productMap = loadProducts(page.getRecords());
        List<CartDTO> records = page.getRecords().stream()
                .map(item -> convertToDTO(item, memberMap.get(item.getCustomerId()),
                        productMap.get(item.getProductId())))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), current, size);
    }

    /** 一次性载入本页涉及商品（v_mall_product），避免逐行查询（N+1）；键为商品编码 */
    private Map<String, ErpProductMall> loadProducts(List<MallCart> items) {
        Set<String> productIds = items.stream()
                .map(MallCart::getProductId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) {
            return new HashMap<>();
        }
        return erpProductMallMapper.selectList(new LambdaQueryWrapper<ErpProductMall>()
                        .in(ErpProductMall::getProductId, productIds))
                .stream()
                .collect(Collectors.toMap(ErpProductMall::getProductId, p -> p, (a, b) -> a));
    }

    /** 一次性载入本页涉及会员，避免逐行查询（N+1） */
    private Map<Long, ShopUser> loadMembers(List<MallCart> items) {
        Set<Long> customerIds = items.stream()
                .map(MallCart::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (customerIds.isEmpty()) {
            return new HashMap<>();
        }
        return shopUserMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(ShopUser::getId, u -> u, (a, b) -> a));
    }

    @Override
    @Transactional
    public CartDTO addToCart(CartAddRequest request) {
        log.info("添加购物车: productId={}, quantity={}", request.getProductId(), request.getQuantity());
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        // Check if product exists
        ErpProductMall product = erpProductMallMapper.selectOne(
                new LambdaQueryWrapper<ErpProductMall>()
                        .eq(ErpProductMall::getProductId, request.getProductId())
                        .eq(ErpProductMall::getDeleted, 0)
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
        cart.setChecked(1);

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
        // ⚠️ @TableLogic 下必须走 deleteById（否则"删了还在"）—— 与 removeBatch 的 deleteBatchIds 同一口径
        mallCartMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Long> validIds = ids.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return 0;
        }
        log.info("批量删除购物车项: ids={}", validIds);
        // deleteBatchIds 走 @TableLogic 逻辑删除，且租户条件由租户插件注入
        return mallCartMapper.deleteBatchIds(validIds);
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

        // 同上：逻辑删除要走 MP 的删除入口
        for (MallCart item : cartItems) {
            mallCartMapper.deleteById(item.getId());
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
                        // 只校验**已勾选**的行参与结算（列由 V11.516.0 补上，值 1/0）
                        .eq(MallCart::getChecked, 1)
        );

        for (MallCart item : cartItems) {
            ErpProductMall product = erpProductMallMapper.selectOne(
                    new LambdaQueryWrapper<ErpProductMall>()
                            .eq(ErpProductMall::getProductId, item.getProductId())
                            .eq(ErpProductMall::getTenantId, tenantId)
            );
            if (product != null && product.getStockQuantity() < item.getQuantity()) {
                throw BusinessException.badRequest("商品库存不足: " + item.getProductName()
                        + ", 库存: " + product.getStockQuantity() + ", 需要: " + item.getQuantity());
            }
        }
    }

    private CartDTO convertToDTO(MallCart cart) {
        return convertToDTO(cart, null, null);
    }

    /** 管理端分页用：附带会员昵称/账号、商品编码/规格/单位与加入时间（member/product 为空则仅缺对应字段） */
    private CartDTO convertToDTO(MallCart cart, ShopUser member, ErpProductMall product) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setProductId(cart.getProductId());
        dto.setProductName(cart.getProductName());
        dto.setProductImage(cart.getProductImage());
        dto.setPrice(cart.getPrice());
        dto.setQuantity(cart.getQuantity());
        dto.setSubtotal(cart.getSubtotal());
        dto.setTotalPrice(cart.getSubtotal());
        // 对外仍是 Boolean（前端语义），列里存的是 integer 0/1
        dto.setChecked(Integer.valueOf(1).equals(cart.getChecked()));
        dto.setCustomerId(cart.getCustomerId());
        dto.setCreateTime(cart.getCreateTime());
        // mall_cart.product_id 存的就是商品编码；无商品命中时回退为 product_id，避免列留空
        dto.setProductCode(product != null && StringUtils.hasText(product.getProductCode())
                ? product.getProductCode() : cart.getProductId());
        if (product != null) {
            dto.setSpecification(product.getSpecification());
            dto.setUnitName(product.getUnitName());
        }
        if (member != null) {
            dto.setMemberName(StringUtils.hasText(member.getNickname())
                    ? member.getNickname() : member.getCompanyName());
            dto.setMemberAccount(StringUtils.hasText(member.getUsername())
                    ? member.getUsername() : member.getPhone());
        }
        return dto;
    }
}
