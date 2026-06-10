package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.model.MallOrder;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopTemplate;
import cn.aiedge.erp.b2b.model.ShopUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 商城管理后台服务接口
 */
public interface MallAdminService {

    // ==================== 商城配置 ====================

    /**
     * 获取当前租户的商城配置
     */
    ShopConfig getConfig();

    /**
     * 更新商城配置
     */
    void updateConfig(ShopConfig config);

    // ==================== 商城用户审核 ====================

    /**
     * 分页查询商城用户
     */
    Page<ShopUser> pageUsers(Integer pageNum, Integer pageSize, String keyword, Integer auditStatus, Integer status);

    /**
     * 审核通过
     */
    void approveUser(Long userId);

    /**
     * 审核驳回
     */
    void rejectUser(Long userId, String reason);

    /**
     * 启用/禁用用户
     */
    void toggleUserStatus(Long userId, Integer status);

    // ==================== 轮播图管理 ====================

    /**
     * 获取轮播图列表（按排序升序）
     */
    List<ShopBanner> listBanners();

    /**
     * 创建轮播图
     */
    void createBanner(ShopBanner banner);

    /**
     * 更新轮播图
     */
    void updateBanner(ShopBanner banner);

    /**
     * 删除轮播图
     */
    void deleteBanner(Long id);

    // ==================== 页面模板 ====================

    /**
     * 获取启用的模板列表
     */
    List<ShopTemplate> listTemplates();

    // ==================== 商品管理 ====================

    /**
     * 分页查询商城商品
     */
    Page<MallProduct> pageProducts(Integer pageNum, Integer pageSize, String keyword, String categoryId, String status);

    /**
     * 创建/更新商品
     */
    void saveProduct(MallProduct product);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);

    // ==================== 订单管理 ====================

    /**
     * 分页查询商城订单（管理端）
     */
    Page<MallOrder> pageOrders(Integer pageNum, Integer pageSize, String keyword, String orderStatus);

    /**
     * 获取订单详情（含明细）
     */
    MallOrder getOrderDetail(Long id);

    /**
     * 管理端审核通过订单
     */
    void approveOrder(Long orderId);

    /**
     * 管理端审核驳回订单
     */
    void rejectOrder(Long orderId, String reason);
}
