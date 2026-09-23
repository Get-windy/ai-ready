package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserTenantMapper;
import cn.aiedge.erp.party.service.CustomerGradeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 买家账号列表（{@code /erp/mall/admin/user/page}）的**参数拆箱回归**。
 *
 * <h3>为什么要有这条测试（不是凑数）</h3>
 * 「本店启用/停用」下沉到关联表时，过滤条件写成了：
 * <pre>{@code
 * Integer enabledFilter = Boolean.FALSE.equals(showDisabled)
 *         ? ShopUserTenant.ENABLED_YES   // ← int 基本类型
 *         : status;                      // ← Integer（前端默认不传 = null）
 * }</pre>
 * 三元表达式遇到「一边 int、一边 Integer」会做**二元数值提升**，类型变成 {@code int}，
 * 于是**两个分支都会把 `status` 拆箱** ⇒ {@code status == null} 时直接
 * {@code NullPointerException} ⇒ **该端点恒 500**（买家账号、买家申请管理两页都打不开）。
 *
 * <p>这类缺陷的可怕之处：**编译通过、单测（若有）也只看得到"有数据时"的路径**，
 * 而线上/页面默认就是**不传**这个参数。⇒ 用一条"**参数全空**"的用例把它钉死，
 * 谁再把三元的类型写回 int，这条测试就会红。</p>
 */
class MallAdminUserPageTest {

    private static final Long TENANT = 1L;

    /** 按真实构造顺序装配；本用例只走"列表条件组装"这条路，其余协作者传 null 即可 */
    private MallAdminServiceImpl service(ShopUserTenantMapper linkMapper, ShopUserMapper userMapper,
                                         SecurityContext securityContext) {
        return new MallAdminServiceImpl(
                null,                 // shopConfigMapper
                userMapper,           // shopUserMapper
                linkMapper,           // shopUserTenantMapper
                null, null, null, null, null, null, null, null, null, null,   // 其余 mapper
                securityContext,
                mock(CustomerGradeService.class),
                mock(PlatformTransactionManager.class));
    }

    @Test
    @DisplayName("status 与 showDisabled **都不传**（前端默认）⇒ 不得 NPE，且「停用」条件不下发")
    void nullStatusAndShowDisabledMustNotThrow() {
        ShopUserTenantMapper linkMapper = mock(ShopUserTenantMapper.class);
        ShopUserMapper userMapper = mock(ShopUserMapper.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getCurrentTenantId()).thenReturn(TENANT);
        when(linkMapper.selectByTenant(anyLong(), isNull(), isNull())).thenReturn(List.of());

        MallAdminServiceImpl svc = service(linkMapper, userMapper, securityContext);

        // 关键：三个"可选过滤"参数全为 null —— 这正是页面默认形态
        assertDoesNotThrow(() -> svc.pageUsers(1, 20, null, null, null, null, null, null, null, null),
                "status/showDisabled 都不传时不得 NPE（三元把 Integer 拆箱就是这个坑）");

        // 且语义仍是"不过滤停用"：enabled 条件必须传 null 下去，而不是被塞成 0/1
        verify(linkMapper).selectByTenant(eq(TENANT), isNull(), isNull());
    }

    @Test
    @DisplayName("showDisabled=false（取消勾选「显示停用」）⇒ 强制只看启用（enabled=1 下发）")
    void showDisabledFalseForcesEnabledOnly() {
        ShopUserTenantMapper linkMapper = mock(ShopUserTenantMapper.class);
        ShopUserMapper userMapper = mock(ShopUserMapper.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getCurrentTenantId()).thenReturn(TENANT);
        when(linkMapper.selectByTenant(anyLong(), isNull(), any())).thenReturn(List.of());

        MallAdminServiceImpl svc = service(linkMapper, userMapper, securityContext);
        svc.pageUsers(1, 20, null, null, null, null, null, null, null, false);

        verify(linkMapper).selectByTenant(eq(TENANT), isNull(), eq(1));
    }

    @Test
    @DisplayName("status=0（显式只看停用）⇒ 原样下发，不被 showDisabled 覆盖")
    void explicitStatusIsPassedThrough() {
        ShopUserTenantMapper linkMapper = mock(ShopUserTenantMapper.class);
        ShopUserMapper userMapper = mock(ShopUserMapper.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getCurrentTenantId()).thenReturn(TENANT);
        when(linkMapper.selectByTenant(anyLong(), isNull(), any())).thenReturn(List.of());

        MallAdminServiceImpl svc = service(linkMapper, userMapper, securityContext);
        svc.pageUsers(1, 20, null, null, 0, null, null, null, null, null);

        verify(linkMapper).selectByTenant(eq(TENANT), isNull(), eq(0));
    }

    @Test
    @DisplayName("没有本店顾客 ⇒ 返回空页（不是 null，也不去查 shop_user）")
    void emptyLinksReturnEmptyPage() {
        ShopUserTenantMapper linkMapper = mock(ShopUserTenantMapper.class);
        ShopUserMapper userMapper = mock(ShopUserMapper.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getCurrentTenantId()).thenReturn(TENANT);
        when(linkMapper.selectByTenant(anyLong(), any(), any())).thenReturn(List.of());

        MallAdminServiceImpl svc = service(linkMapper, userMapper, securityContext);

        var page = svc.pageUsers(1, 20, null, null, null, null, null, null, null, null);
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getRecords().size());
    }
}
