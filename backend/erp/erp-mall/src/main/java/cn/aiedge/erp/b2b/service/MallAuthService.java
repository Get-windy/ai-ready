package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.IdentityDTO;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;

import java.util.List;

public interface MallAuthService {

    /**
     * 商城顾客登录（**入店**登录）。
     *
     * <p>⚠️ 必须知道"在登哪家店"：顾客是**系统级身份**（{@code shop_user}），
     * "他属不属于这家店"由 {@code shop_user_tenant} 表达 ——
     * 所以登录**之前**必须先做入店校验（在本店注册过且已通过审核），
     * 否则随便一个系统顾客都能登进任何一家店。</p>
     *
     * @param shopTenantId 本店租户（由 Controller 从会话 / {@code X-Tenant-Id} 解析），不可为空
     */
    LoginResponse login(LoginRequest request, Long shopTenantId);

    /**
     * 商城顾客注册（**两分支**，DOMAIN-MODEL §11.3 阶段 1）。
     *
     * <ul>
     *   <li><b>分支 A</b>：系统里还没有这个自然人 ⇒ 建系统顾客 + 本店关联；</li>
     *   <li><b>分支 B</b>：系统里已有（在别家店注册过）⇒ <b>必须自证身份（原密码）</b>，
     *       通过后**只建关联**、复用同一个系统顾客 —— 否则拿别人的用户名就能绑到新店（冒名绑定）。</li>
     * </ul>
     *
     * <p>是否需审核按**本店**的 {@code tenant_shop_config.reg_audit_required} 决定：
     * 1 = 待审核（PENDING），0 = 默认同意（直接 ACTIVE）。</p>
     *
     * @param shopTenantId 本店租户，不可为空
     */
    void register(LoginRequest request, Long shopTenantId);

    void logout();

    String refreshToken();

    /**
     * 获取当前用户可切换的所有身份。
     * 包含：个人会员身份 + 作为联系人的所有企业客户身份。
     */
    List<IdentityDTO> listIdentities();

    /**
     * 切换当前下单身份。
     *
     * @param partyId 目标身份的 biz_party.id
     */
    void switchIdentity(Long partyId);
}
