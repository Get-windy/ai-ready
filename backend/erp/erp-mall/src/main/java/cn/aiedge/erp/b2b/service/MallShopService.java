package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.ShopConfigVO;

/**
 * 商城 C 端「店铺」服务。
 *
 * <p>只服务买家侧只读接口；管理端的店铺配置读写在 {@link MallAdminService}。</p>
 */
public interface MallShopService {

    /**
     * 当前访问店铺的对外配置（白名单字段）。
     *
     * <p>走 {@code MallGuestAccess.requireShop()} 统一准入：取不到店铺报 400、
     * 未开放游客报 403、无配置行 fail-closed。</p>
     */
    ShopConfigVO currentShopConfig();
}
