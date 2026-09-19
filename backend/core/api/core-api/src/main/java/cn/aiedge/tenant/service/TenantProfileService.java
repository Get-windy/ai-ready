package cn.aiedge.tenant.service;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysTenantProfile;
import cn.aiedge.base.mapper.SysTenantProfileMapper;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.tenant.dto.CompanyProfileDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 租户企业档案服务（设置 → 系统配置 → 企业信息，菜单 80624 / {@code set:company-info}）
 *
 * <p><b>为什么需要它</b>：迁移 V11.420.0 把企业档案列从 {@code sys_tenant} 拆到 1:1 子表
 * {@code sys_tenant_profile} 后，「保存企业信息」从「写一张表」变成「写两张表」——
 * 必须放进**同一个事务**（子表写失败时主表的 tenant_name 也要回滚），故不留在控制器里拼 SQL。</p>
 *
 * <p><b>租户归属</b>：一律由调用方（{@code TenantController}）从会话取
 * （{@code SecurityContext.getCurrentTenantId()}）后传入，**不接受请求体/路径提供的租户 ID**。
 * 本类的每个读写都**显式**带 {@code tenant_id = tenantId} 条件，与多租户插件的自动注入形成双保险
 * （{@code sys_tenant_profile} 含 tenant_id 且不在忽略清单中；而 {@code sys_tenant} 无 tenant_id
 * 且在忽略清单中，只能靠显式条件）。</p>
 *
 * <p><b>清空语义</b>：两张表都用 {@link UpdateWrapper} 逐列显式 {@code set}
 * （不用 {@code updateById} —— 后者忽略 null，用户清空输入框后保存无效，即本页历史缺陷 P2-⑮），
 * 且空白串统一归一为 {@code null} 落库。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProfileService {

    private final TenantMapper tenantMapper;

    private final SysTenantProfileMapper profileMapper;

    /**
     * 读取某租户的企业档案行。
     *
     * @param tenantId 租户 ID（来自会话；为 null 直接返回 null，不查库）
     * @return 档案行；**可能为 null** —— 新租户尚未填过档案，或该租户行从未存在
     */
    public SysTenantProfile getByTenantId(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return profileMapper.selectOne(new LambdaQueryWrapper<SysTenantProfile>()
                .eq(SysTenantProfile::getTenantId, tenantId)
                // 表上有 uk_sys_tenant_profile_tenant(tenant_id) WHERE deleted = 0 兜底；此处再限 1 行，
                // 避免历史脏数据（重复行）导致 selectOne 抛 TooManyResultsException
                .last("LIMIT 1"));
    }

    /**
     * 保存某租户的企业档案（两张表，同一事务）。
     *
     * <p>分工：{@code tenant_name} / 联系人 / 联系电话 / 企业邮箱 / 企业地址属 {@code sys_tenant}
     * 的**既有 15 列**（拆表时留在主表，被租户注册、租户管理弹窗等多处引用，不搬）；
     * 其余 15 个档案列 + {@code logo_url} 写子表。子表行不存在时插入（首次保存）。</p>
     *
     * @param tenantId 租户 ID（来自会话，调用方已做存在性校验）
     * @param dto      白名单入参（不含 tenantCode / level / expireTime / status 等平台列）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long tenantId, CompanyProfileDTO.Save dto) {
        // ── ① 主表 sys_tenant：只写「既有 15 列」中本页可改的 5 列 ────────────────
        UpdateWrapper<SysTenant> tenantUpdate = new UpdateWrapper<>();
        tenantUpdate.eq("id", tenantId);
        tenantUpdate.set("tenant_name", dto.tenantName() == null ? null : dto.tenantName().trim());
        tenantUpdate.set("contact_person", trimToNull(dto.contactPerson()));
        tenantUpdate.set("contact_phone", trimToNull(dto.contactPhone()));
        tenantUpdate.set("contact_email", trimToNull(dto.contactEmail()));
        tenantUpdate.set("address", trimToNull(dto.address()));
        // wrapper 更新不会触发 FieldFill.INSERT_UPDATE 自动填充，这里显式写更新时间
        tenantUpdate.set("update_time", LocalDateTime.now());
        tenantMapper.update(null, tenantUpdate);

        // ── ② 子表 sys_tenant_profile：存在则逐列更新，不存在则插入 ──────────────
        SysTenantProfile exists = getByTenantId(tenantId);
        if (exists == null) {
            // 注意：BaseEntity 的 setter 是 void（无 @Accessors(chain=true)），此处不能用链式调用
            SysTenantProfile created = new SysTenantProfile();
            created.setTenantId(tenantId);
            created.setCreditCode(trimToNull(dto.creditCode()));
            created.setCompanyType(trimToNull(dto.companyType()));
            created.setLegalPerson(trimToNull(dto.legalPerson()));
            created.setRegisteredCapital(dto.registeredCapital());
            created.setIndustry(trimToNull(dto.industry()));
            created.setCompanyScale(trimToNull(dto.companyScale()));
            created.setEstablishDate(dto.establishDate());
            created.setBusinessScope(trimToNull(dto.businessScope()));
            created.setContactPersonPhone(trimToNull(dto.contactPersonPhone()));
            created.setContactPersonEmail(trimToNull(dto.contactPersonEmail()));
            created.setTaxNumber(trimToNull(dto.taxNumber()));
            created.setTaxpayerAddress(trimToNull(dto.taxpayerAddress()));
            created.setTaxpayerPhone(trimToNull(dto.taxpayerPhone()));
            created.setBankName(trimToNull(dto.bankName()));
            created.setBankAccount(trimToNull(dto.bankAccount()));
            created.setLogoUrl(trimToNull(dto.logoUrl()));
            profileMapper.insert(created);
            log.info("[公司档案] 首次建档 tenantId={}", tenantId);
            return;
        }

        UpdateWrapper<SysTenantProfile> profileUpdate = new UpdateWrapper<>();
        profileUpdate.eq("id", exists.getId());
        // 显式租户条件：多租户插件也会注入一次，双保险（本表不在 IGNORE_TENANT_TABLES 中）
        profileUpdate.eq("tenant_id", tenantId);
        // Tab① 企业信息
        profileUpdate.set("credit_code", trimToNull(dto.creditCode()));
        profileUpdate.set("company_type", trimToNull(dto.companyType()));
        profileUpdate.set("legal_person", trimToNull(dto.legalPerson()));
        profileUpdate.set("registered_capital", dto.registeredCapital());
        profileUpdate.set("industry", trimToNull(dto.industry()));
        profileUpdate.set("company_scale", trimToNull(dto.companyScale()));
        profileUpdate.set("establish_date", dto.establishDate());
        profileUpdate.set("business_scope", trimToNull(dto.businessScope()));
        profileUpdate.set("contact_person_phone", trimToNull(dto.contactPersonPhone()));
        profileUpdate.set("contact_person_email", trimToNull(dto.contactPersonEmail()));
        // Tab② 纳税人信息
        profileUpdate.set("tax_number", trimToNull(dto.taxNumber()));
        profileUpdate.set("taxpayer_address", trimToNull(dto.taxpayerAddress()));
        profileUpdate.set("taxpayer_phone", trimToNull(dto.taxpayerPhone()));
        profileUpdate.set("bank_name", trimToNull(dto.bankName()));
        profileUpdate.set("bank_account", trimToNull(dto.bankAccount()));
        // 企业标识
        profileUpdate.set("logo_url", trimToNull(dto.logoUrl()));
        profileUpdate.set("update_time", LocalDateTime.now());
        profileMapper.update(null, profileUpdate);
    }

    /** 空白串归一为 null（保证「清空」能真正落库为 NULL，而不是留一个空串） */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
