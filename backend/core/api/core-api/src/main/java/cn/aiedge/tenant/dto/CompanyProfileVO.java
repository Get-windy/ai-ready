package cn.aiedge.tenant.dto;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysTenantProfile;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 「企业信息」页（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）的**响应视图**。
 *
 * <p>存在的唯一理由：拆表（V11.420.0：企业档案列从 {@code sys_tenant} 迁到 1:1 子表
 * {@code sys_tenant_profile}）后，{@code GET /api/tenant/current} 的响应体由两张表拼成。
 * 本 VO **保留了拆表前 {@code SysTenant} 实体的全部字段名与类型**（含企业档案 15 列、
 * 以及平台侧只读列 id / tenantCode / level / expireTime / status / adminUserId / remark /
 * createTime / updateTime / deleted），使前端 {@code CompanyProfile} 类型与 E2E 断言
 * **无需任何改动**即可跑通 —— 接口契约不变，只是数据来源从一张表变成两张表。</p>
 *
 * <p>字段来源对照：</p>
 * <ul>
 *   <li>平台侧身份 / 生命周期列 ← {@code sys_tenant}（平台维护，本页只读展示）</li>
 *   <li>企业档案 15 列 + {@code logoUrl} ← {@code sys_tenant_profile}（本页可写）</li>
 * </ul>
 *
 * <p>⚠️ {@code establishDate} 的 {@code @JsonFormat("yyyy-MM-dd")} 必须在这里重新标注：
 * 该注解写在 {@link SysTenantProfile} 上只对「直接序列化实体」生效，本 VO 是独立类型，
 * 漏标注会让前端日期选择器收到 ISO 时间串（历史坑：拆表时容易漏）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CompanyProfileVO {

    // ── 平台侧身份 / 生命周期（sys_tenant，只读展示）──────────────────────────

    /** 租户 ID（雪花 ID，BIGINT：前端一律按字符串处理，禁止 Number(id)） */
    private Long id;

    /** 企业名称（= 租户名，必填；Tab① 与 Tab② 共用同一数据源） */
    private String tenantName;

    /** 租户编码（平台维护，只读） */
    private String tenantCode;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 企业邮箱 */
    private String contactEmail;

    /** 企业地址 */
    private String address;

    /** 租户管理员用户ID（平台维护，只读） */
    private Long adminUserId;

    /** 租户等级（basic / professional / enterprise，平台维护，只读） */
    private String level;

    /** 到期时间（平台维护，只读） */
    private LocalDateTime expireTime;

    /** 租户状态（0 正常 / 1 停用，平台维护，只读） */
    private Integer status;

    /** 备注（平台维护，只读） */
    private String remark;

    /** 注册时间（= create_time，只读） */
    private LocalDateTime createTime;

    /** 更新时间（只读） */
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0 正常 / 1 已删除；保留仅为与拆表前的实体响应逐字段对齐） */
    private Integer deleted;

    // ── 企业档案（sys_tenant_profile，本页可写）──────────────────────────────

    /** 统一社会信用代码（18 位） */
    private String creditCode;

    /** 企业类型 */
    private String companyType;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本（万元） */
    private BigDecimal registeredCapital;

    /** 所属行业 */
    private String industry;

    /** 企业规模 */
    private String companyScale;

    /** 成立日期（yyyy-MM-dd：必须在此重新标注，见类注释） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishDate;

    /** 经营范围 */
    private String businessScope;

    /** 联系人电话 */
    private String contactPersonPhone;

    /** 联系人邮箱 */
    private String contactPersonEmail;

    /** 纳税人识别号（开票资料） */
    private String taxNumber;

    /** 纳税人信息 - 地址 */
    private String taxpayerAddress;

    /** 纳税人信息 - 电话 */
    private String taxpayerPhone;

    /** 纳税人信息 - 开户行地址 */
    private String bankName;

    /** 纳税人信息 - 开户行账号 */
    private String bankAccount;

    /** 企业 LOGO 地址（上传端点返回的 url；未上传为 null） */
    private String logoUrl;

    /**
     * 合并两张表为一份响应。
     *
     * @param tenant  租户主表行（非空，调用方已做存在性校验）
     * @param profile 企业档案子表行；**允许为 null**（新租户尚未填过档案 → 档案字段一律返回 null）
     */
    public static CompanyProfileVO of(SysTenant tenant, SysTenantProfile profile) {
        CompanyProfileVO vo = new CompanyProfileVO()
                .setId(tenant.getId())
                .setTenantName(tenant.getTenantName())
                .setTenantCode(tenant.getTenantCode())
                .setContactPerson(tenant.getContactPerson())
                .setContactPhone(tenant.getContactPhone())
                .setContactEmail(tenant.getContactEmail())
                .setAddress(tenant.getAddress())
                .setAdminUserId(tenant.getAdminUserId())
                .setLevel(tenant.getLevel())
                .setExpireTime(tenant.getExpireTime())
                .setStatus(tenant.getStatus())
                .setRemark(tenant.getRemark())
                .setCreateTime(tenant.getCreateTime())
                .setUpdateTime(tenant.getUpdateTime())
                .setDeleted(tenant.getDeleted());

        if (profile != null) {
            vo.setCreditCode(profile.getCreditCode())
              .setCompanyType(profile.getCompanyType())
              .setLegalPerson(profile.getLegalPerson())
              .setRegisteredCapital(profile.getRegisteredCapital())
              .setIndustry(profile.getIndustry())
              .setCompanyScale(profile.getCompanyScale())
              .setEstablishDate(profile.getEstablishDate())
              .setBusinessScope(profile.getBusinessScope())
              .setContactPersonPhone(profile.getContactPersonPhone())
              .setContactPersonEmail(profile.getContactPersonEmail())
              .setTaxNumber(profile.getTaxNumber())
              .setTaxpayerAddress(profile.getTaxpayerAddress())
              .setTaxpayerPhone(profile.getTaxpayerPhone())
              .setBankName(profile.getBankName())
              .setBankAccount(profile.getBankAccount())
              .setLogoUrl(profile.getLogoUrl());
        }
        return vo;
    }
}
