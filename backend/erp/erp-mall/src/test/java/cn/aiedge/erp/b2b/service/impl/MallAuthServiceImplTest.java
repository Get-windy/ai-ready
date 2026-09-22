package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.model.ShopUserTenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商城「入店校验」的真值表（DOMAIN-MODEL §11.3 阶段 1）。
 *
 * <p>为什么单独把这个判定拿出来测：登录链路的其余部分要动 Sa-Token 的**静态**方法
 * （{@code StpUtil.login/getSession}），单测里只能靠 mock 静态（本仓未装 mockito-inline），
 * 所以把"能不能进这家店"这条**纯判定**抽成可测方法，把装配部分留给真机脚本
 * （{@code tools/verify-mall-shop-entry.cjs}）去验 —— 两边各测自己擅长的。</p>
 *
 * <h3>这条判定为什么值得单独钉死</h3>
 * 顾客是**系统级身份**（{@code shop_user}），"他属不属于这家店"由 {@code shop_user_tenant} 表达。
 * 一旦这里放行了不该放的状态：<b>任何一个系统顾客都能登进任何一家店</b>（跨租户入口失守）；
 * 反之若把状态混成一句"登录失败"，顾客不知道是该去注册、还是等审核、还是被拒了。
 */
class MallAuthServiceImplTest {

    private static final Long SHOP = 990021L;

    private final MallAuthServiceImpl service = new MallAuthServiceImpl(
            null, null, null, null, null, null);   // 本测试只测纯判定，不碰任何依赖

    private static ShopUserTenant link(Integer status, String rejectReason) {
        ShopUserTenant link = new ShopUserTenant();
        link.setShopUserId(1L);
        link.setTenantId(SHOP);
        link.setStatus(status);
        link.setRejectReason(rejectReason);
        return link;
    }

    // ══════════════════════ 拿不到"哪家店" ⇒ 一律拒绝 ══════════════════════

    @Test
    @DisplayName("没有店铺标识 ⇒ 拒（绝不放行成「看全表」或「猜一家店」）")
    void missingShopTenantIsRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> MallAuthServiceImpl.requireShopTenant(null));
        assertTrue(ex.getMessage().contains("哪家店铺"), ex.getMessage());

        assertEquals(SHOP, MallAuthServiceImpl.requireShopTenant(SHOP), "有值时必须原样返回");
    }

    // ══════════════════════ 四种状态分开说 ══════════════════════

    @Test
    @DisplayName("没有本店关联 ⇒ 提示「还没有在本店注册」（不是含糊的「登录失败」）")
    void noLinkMeansNotRegisteredHere() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(null, SHOP));
        assertTrue(ex.getMessage().contains("还没有在本店注册"), ex.getMessage());
    }

    @Test
    @DisplayName("待审核 ⇒ 拒，且提示「等待店铺审核」而不是「没注册」")
    void pendingIsRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(link(ShopUserTenant.STATUS_PENDING, null), SHOP));
        assertTrue(ex.getMessage().contains("等待店铺审核"), ex.getMessage());
    }

    @Test
    @DisplayName("已拒绝 ⇒ 拒，且把拒绝理由带给顾客")
    void rejectedCarriesReason() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(
                        link(ShopUserTenant.STATUS_REJECTED, "资料不全，请补充营业执照"), SHOP));
        assertTrue(ex.getMessage().contains("已被本店拒绝"), ex.getMessage());
        assertTrue(ex.getMessage().contains("资料不全，请补充营业执照"),
                "拒绝理由必须透出，否则顾客只能反复试：" + ex.getMessage());
    }

    @Test
    @DisplayName("已解除 ⇒ 拒，并提示可重新注册")
    void revokedIsRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(link(ShopUserTenant.STATUS_REVOKED, null), SHOP));
        assertTrue(ex.getMessage().contains("已被解除"), ex.getMessage());
    }

    @Test
    @DisplayName("正常 ⇒ 放行")
    void activeIsAllowed() {
        assertDoesNotThrow(
                () -> service.assertShopEntryAllowed(link(ShopUserTenant.STATUS_ACTIVE, null), SHOP));
    }

    // ══════════════════════ 停用是**第二个维度**（审核通过 ≠ 一定放行） ══════════════════════

    @Test
    @DisplayName("审核通过但**本店已停用** ⇒ 拒（两个维度正交：停用不改审核记录）")
    void activeButDisabledIsRejected() {
        ShopUserTenant disabled = link(ShopUserTenant.STATUS_ACTIVE, null);
        disabled.setEnabled(ShopUserTenant.ENABLED_NO);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(disabled, SHOP));
        assertTrue(ex.getMessage().contains("已被停用"), ex.getMessage());
        assertEquals(ShopUserTenant.STATUS_ACTIVE, disabled.getStatus(),
                "停用**不该**改动审核状态（status 仍是「已通过」）");
    }

    @Test
    @DisplayName("本店启用 ⇒ 放行（enabled=1 与缺值都按启用处理）")
    void enabledYesIsAllowed() {
        ShopUserTenant on = link(ShopUserTenant.STATUS_ACTIVE, null);
        on.setEnabled(ShopUserTenant.ENABLED_YES);
        assertDoesNotThrow(() -> service.assertShopEntryAllowed(on, SHOP));

        ShopUserTenant absent = link(ShopUserTenant.STATUS_ACTIVE, null);
        absent.setEnabled(null);   // 理论上不会出现（列 NOT NULL），但缺值按启用、绝不按停用
        assertDoesNotThrow(() -> service.assertShopEntryAllowed(absent, SHOP));
    }

    @Test
    @DisplayName("状态为 null / 未知值 ⇒ **不放行**（fail-closed，宁可让顾客问一句）")
    void unknownStatusIsRejected() {
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(link(null, null), SHOP));
        assertTrue(ex1.getMessage().contains("正在等待店铺审核"), ex1.getMessage());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> service.assertShopEntryAllowed(link(99, null), SHOP));
        assertTrue(ex2.getMessage().contains("状态异常"), ex2.getMessage());
    }
}
