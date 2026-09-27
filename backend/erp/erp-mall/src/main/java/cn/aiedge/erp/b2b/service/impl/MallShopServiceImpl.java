package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.erp.b2b.dto.ShopConfigVO;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.service.MallShopService;
import cn.aiedge.erp.b2b.support.MallGuestAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商城 C 端店铺服务实现。
 *
 * <p>无缓存：店铺配置是单行小表（{@code tenant_shop_config}，一个租户一行），
 * 走主键/租户索引查询成本极低；加缓存反而要处理"后台改完 C 端看不到"的一致性问题。
 * 若后续压测显示需要，再做带失效的缓存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallShopServiceImpl implements MallShopService {

    private final MallGuestAccess guestAccess;

    @Override
    public ShopConfigVO currentShopConfig() {
        // 与商品列表/标签同一套准入（400 无法确定店铺 / 403 未开放游客 / fail-closed）
        ShopConfig config = guestAccess.requireShop();
        log.debug("下发店铺配置: tenantId={}, shopName={}", config.getTenantId(), config.getShopName());
        // 白名单构造：凭据类字段（appsecret / mch_key）永不出现在返回值里
        return ShopConfigVO.from(config);
    }
}
