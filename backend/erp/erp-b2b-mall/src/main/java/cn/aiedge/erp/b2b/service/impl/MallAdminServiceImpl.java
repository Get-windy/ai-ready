package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.mapper.MallOrderItemMapper;
import cn.aiedge.erp.b2b.mapper.MallOrderMapper;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.mapper.ShopBannerMapper;
import cn.aiedge.erp.b2b.mapper.ShopConfigMapper;
import cn.aiedge.erp.b2b.mapper.ShopTemplateMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.MallOrder;
import cn.aiedge.erp.b2b.model.MallOrderItem;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopTemplate;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.service.MallAdminService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MallAdminServiceImpl implements MallAdminService {

    private final ShopConfigMapper shopConfigMapper;
    private final ShopUserMapper shopUserMapper;
    private final ShopBannerMapper shopBannerMapper;
    private final ShopTemplateMapper shopTemplateMapper;
    private final MallProductMapper mallProductMapper;
    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;

    private Long getCurrentTenantId() {
        return StpUtil.getLoginIdAsLong();
    }

    // ==================== 商城配置 ====================

    @Override
    public ShopConfig getConfig() {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ShopConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopConfig::getTenantId, tenantId);
        return shopConfigMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ShopConfig config) {
        Long tenantId = getCurrentTenantId();
        ShopConfig existing = shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>().eq(ShopConfig::getTenantId, tenantId));
        if (existing == null) {
            config.setId(null);
            config.setTenantId(tenantId);
            config.setStatus(1);
            shopConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setTenantId(tenantId);
            config.setUpdateBy(StpUtil.getLoginIdAsLong());
            config.setUpdateTime(LocalDateTime.now());
            shopConfigMapper.updateById(config);
        }
    }

    // ==================== 商城用户审核 ====================

    @Override
    public Page<ShopUser> pageUsers(Integer pageNum, Integer pageSize, String keyword,
                                    Integer auditStatus, Integer status) {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ShopUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopUser::getTenantId, tenantId);

        if (auditStatus != null) {
            wrapper.eq(ShopUser::getAuditStatus, auditStatus);
        }
        if (status != null) {
            wrapper.eq(ShopUser::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ShopUser::getUsername, keyword)
                    .or().like(ShopUser::getNickname, keyword)
                    .or().like(ShopUser::getPhone, keyword)
                    .or().like(ShopUser::getCompanyName, keyword));
        }

        wrapper.orderByDesc(ShopUser::getCreateTime);
        return shopUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveUser(Long userId) {
        Long tenantId = getCurrentTenantId();
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null || !user.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setAuditStatus(1);
        user.setAuditTime(LocalDateTime.now());
        user.setAuditBy(StpUtil.getLoginIdAsLong());
        user.setRejectReason(null);
        shopUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectUser(Long userId, String reason) {
        Long tenantId = getCurrentTenantId();
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null || !user.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setAuditStatus(2);
        user.setAuditTime(LocalDateTime.now());
        user.setAuditBy(StpUtil.getLoginIdAsLong());
        user.setRejectReason(reason);
        shopUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId, Integer status) {
        Long tenantId = getCurrentTenantId();
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null || !user.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setStatus(status);
        shopUserMapper.updateById(user);
    }

    // ==================== 轮播图管理 ====================

    @Override
    public List<ShopBanner> listBanners() {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ShopBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopBanner::getTenantId, tenantId);
        wrapper.orderByAsc(ShopBanner::getSortOrder);
        return shopBannerMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBanner(ShopBanner banner) {
        banner.setId(null);
        banner.setTenantId(getCurrentTenantId());
        banner.setCreateBy(StpUtil.getLoginIdAsLong());
        shopBannerMapper.insert(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(ShopBanner banner) {
        Long tenantId = getCurrentTenantId();
        ShopBanner existing = shopBannerMapper.selectById(banner.getId());
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("轮播图不存在");
        }
        banner.setTenantId(tenantId);
        banner.setUpdateBy(StpUtil.getLoginIdAsLong());
        banner.setUpdateTime(LocalDateTime.now());
        shopBannerMapper.updateById(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        Long tenantId = getCurrentTenantId();
        ShopBanner banner = shopBannerMapper.selectById(id);
        if (banner == null || !banner.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("轮播图不存在");
        }
        shopBannerMapper.deleteById(id);
    }

    // ==================== 商品管理 ====================

    @Override
    public Page<MallProduct> pageProducts(Integer pageNum, Integer pageSize, String keyword, String categoryId, String status) {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<MallProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallProduct::getTenantId, tenantId);
        wrapper.eq(MallProduct::getDeleted, 0);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MallProduct::getProductName, keyword).or()
                    .like(MallProduct::getProductCode, keyword).or()
                    .like(MallProduct::getProductId, keyword);
        }
        if (categoryId != null && !categoryId.isEmpty()) {
            wrapper.eq(MallProduct::getCategoryId, categoryId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MallProduct::getStatus, status);
        }
        wrapper.orderByDesc(MallProduct::getCreateTime);
        return mallProductMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(MallProduct product) {
        Long tenantId = getCurrentTenantId();
        if (product.getId() != null) {
            MallProduct existing = mallProductMapper.selectById(product.getId());
            if (existing == null || !existing.getTenantId().equals(tenantId)) {
                throw BusinessException.notFound("商品不存在");
            }
            product.setTenantId(tenantId);
            product.setUpdateBy(StpUtil.getLoginIdAsLong());
            mallProductMapper.updateById(product);
        } else {
            product.setId(null);
            product.setTenantId(tenantId);
            product.setCreateBy(StpUtil.getLoginIdAsLong());
            mallProductMapper.insert(product);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Long tenantId = getCurrentTenantId();
        MallProduct product = mallProductMapper.selectById(id);
        if (product == null || !product.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("商品不存在");
        }
        mallProductMapper.deleteById(id);
    }

    // ==================== 订单管理 ====================

    @Override
    public Page<MallOrder> pageOrders(Integer pageNum, Integer pageSize, String keyword, String orderStatus) {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getTenantId, tenantId);
        wrapper.eq(MallOrder::getDeleted, 0);

        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(MallOrder::getOrderStatus, orderStatus);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(MallOrder::getOrderNo, keyword)
                    .or().like(MallOrder::getCustomerName, keyword)
                    .or().like(MallOrder::getConsignee, keyword));
        }
        wrapper.orderByDesc(MallOrder::getCreateTime);
        return mallOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public MallOrder getOrderDetail(Long id) {
        Long tenantId = getCurrentTenantId();
        MallOrder order = mallOrderMapper.selectById(id);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setOrderItems(mallOrderItemMapper.selectList(
                new LambdaQueryWrapper<MallOrderItem>()
                        .eq(MallOrderItem::getOrderId, id)));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        Long tenantId = getCurrentTenantId();
        MallOrder order = mallOrderMapper.selectById(orderId);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!"PAID".equals(order.getOrderStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许审核通过");
        }
        order.setOrderStatus("APPROVED");
        mallOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId, String reason) {
        Long tenantId = getCurrentTenantId();
        MallOrder order = mallOrderMapper.selectById(orderId);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!"PAID".equals(order.getOrderStatus()) && !"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许驳回");
        }
        order.setOrderStatus("REJECTED");
        order.setRemark(reason);
        mallOrderMapper.updateById(order);
    }

    // ==================== 页面模板 ====================

    @Override
    public List<ShopTemplate> listTemplates() {
        LambdaQueryWrapper<ShopTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopTemplate::getStatus, 1);
        wrapper.orderByAsc(ShopTemplate::getId);
        return shopTemplateMapper.selectList(wrapper);
    }
}
