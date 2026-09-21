package cn.aiedge.erp.b2b.support;

import cn.aiedge.erp.b2b.mapper.ShopConfigMapper;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 商城「游客访问」判定（B2B 租户商城模型）。
 *
 * <p><b>为什么需要它</b>：`tenant_shop_config` 里的 {@code allowGuest}（是否允许游客访问）
 * 与 {@code guestShowPrice}（游客是否显示价格）两个字段**早就存在**，前端「商城 → 商城设置 →
 * 店铺设置」的开关也早就就位并正常保存 —— 但后端**从来没有代码消费它们**，
 * 也就是说这两个开关此前是**空开关**：管理员来回切换、保存成功、商城行为毫无变化。
 * 本类把这两个开关接到真实行为上。</p>
 *
 * <p><b>「这是哪家店」怎么确定</b>：B2B 租户商城模式下，每个租户一家店。游客没有会话，
 * 因此租户按以下顺序解析：</p>
 * <ol>
 *   <li>登录用户的会话租户（买家/会员）；</li>
 *   <li>否则取请求头 {@code X-Tenant-Id}（商城 C 端带上「我在逛哪家店」）。</li>
 * </ol>
 * <p>两者都取不到 ⇒ 返回 {@code null}，调用方按「无法确定店铺」拒绝，而不是退化成查全表。</p>
 *
 * <p><b>⚠️ 安全边界（说清楚，别误以为它是鉴权）</b>：{@code X-Tenant-Id} 由客户端提供，
 * 因此**能改头就就能看别家店的公开商品信息**（商品名/图/规格，以及那家店自己设置为
 * 「游客可见」的价格）。它解决的是「游客怎么知道自己逛的是哪家店」，**不是**「谁能看谁的数据」。
 * 若将来要把租户目录当作商业机密，应改为按域名解析（需配 DNS / 反向代理），或对头值签名。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallGuestAccess {

    /** 与前端商城 C 端约定的店铺标识头 */
    public static final String TENANT_HEADER = "X-Tenant-Id";

    /** 店铺设置取值：允许游客访问 */
    private static final String ALLOW_GUEST_YES = "ALLOW";
    /** 店铺设置取值：游客可见价格 */
    private static final String GUEST_SHOW_PRICE_YES = "SHOW";

    private final ShopConfigMapper shopConfigMapper;

    /** 当前调用者是否为已登录用户（买家/会员），而非游客 */
    public boolean isLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            // 无 token / 会话异常一律按「游客」处理，不向上抛
            return false;
        }
    }

    /**
     * 解析当前访问的店铺租户 ID。取不到返回 {@code null}。
     */
    public Long currentShopTenantId() {
        try {
            Object tid = StpUtil.getSession().get("tenantId");
            if (tid instanceof Number) {
                return ((Number) tid).longValue();
            }
        } catch (Exception ignored) {
            // 游客没有会话，属正常路径
        }
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String raw = req.getHeader(TENANT_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("非法的 {} 头：{}", TENANT_HEADER, raw);
            return null;
        }
    }

    /** 读取指定店铺的配置；无配置行时返回 {@code null}（视为未开通商城）。 */
    public ShopConfig shopConfig(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>().eq(ShopConfig::getTenantId, tenantId));
    }

    /**
     * 游客是否有权浏览指定店铺。
     *
     * <p>已登录用户一律放行；游客需该店铺 {@code allowGuest = ALLOW}。
     * 无配置行时按「未开通商城」拒绝（fail-closed）。</p>
     */
    public boolean guestMayBrowse(ShopConfig config) {
        if (isLoggedIn()) {
            return true;
        }
        return config != null && ALLOW_GUEST_YES.equalsIgnoreCase(config.getAllowGuest());
    }

    /**
     * 当前调用者是否可见价格。
     *
     * <p>已登录用户一律可见（价格按客户等级另算，不在本判定范围）；游客需该店铺
     * {@code guestShowPrice = SHOW}。无配置行时**隐藏**（fail-closed，宁可不显示也不外泄）。</p>
     */
    public boolean priceVisible(ShopConfig config) {
        if (isLoggedIn()) {
            return true;
        }
        return config != null && GUEST_SHOW_PRICE_YES.equalsIgnoreCase(config.getGuestShowPrice());
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
