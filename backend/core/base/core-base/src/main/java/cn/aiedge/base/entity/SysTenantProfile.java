package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 租户企业档案实体（1:1 于 {@link SysTenant}）
 *
 * <p>落库表 {@code sys_tenant_profile}（迁移 V11.420.0）。页面：设置 → 系统配置 → 企业信息
 * （菜单 80624 / {@code set:company-info}），由 {@code GET/PUT /api/tenant/current} 读写。</p>
 *
 * <p><b>为什么要拆出这张表</b>：上一轮（V11.396.0）为本页 15 个表单项直接给 {@code sys_tenant}
 * 加了 15 列，使该表涨到 30 列，违反《开发技术规范》「单表 ≤25 列 / 禁止上帝表」。
 * 本表承接**全部档案类字段**（Tab① 企业档案 10 列 + Tab② 纳税人信息 5 列 + LOGO），
 * {@code sys_tenant} 退回 15 列，只留身份 / 生命周期 / 平台维护类字段。</p>
 *
 * <p><b>与 {@code sys_tenant} 的两点关键差异（勿踩）</b>：</p>
 * <ul>
 *   <li>本表**有 {@code tenant_id}**（1:1 指向 {@code sys_tenant.id}），且**不在**
 *       {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 中 → 全局多租户插件会自动注入
 *       {@code tenant_id = 会话租户}。控制器另按会话租户显式过滤 + 显式写入租户 ID，
 *       两处口径一致，不依赖调用方传参。</li>
 *   <li>记录可能**暂不存在**（新注册租户尚未填过档案）→ 读取返回 null 由上层补空，
 *       首次保存时由 {@code TenantProfileService} 插入（不用「读时建行」，避免写放大）。</li>
 * </ul>
 *
 * <p>命名口径：15 个档案列**逐字沿用** V11.396.0 在 {@code sys_tenant} 上的列名，
 * 拆表不改名，故 {@code GET /api/tenant/current} 的响应字段名与拆表前**完全一致**
 * （前端与 E2E 无需改动）。纳税人识别号列名为 {@code tax_number}（全库词根：
 * {@code biz_party} / {@code erp_supplier} / {@code crm_customer} 均为 {@code tax_number}），
 * 不用开发文档 §8.4 的拟名 {@code taxpayer_id}，该偏离已在文档 §13.1 / §13.4-2 登记。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_profile")
public class SysTenantProfile extends BaseEntity {

    // ═══════════════════════════════════════════════════════════════════════
    // Tab① 企业信息（企业档案 10 列）
    // ═══════════════════════════════════════════════════════════════════════

    /** 统一社会信用代码（18 位） */
    private String creditCode;

    /** 企业类型（有限责任公司 / 股份有限公司 / 合伙企业 / 个体工商户，前端硬编码字面量） */
    private String companyType;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本（万元） */
    private BigDecimal registeredCapital;

    /** 所属行业 */
    private String industry;

    /** 企业规模 */
    private String companyScale;

    /** 成立日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishDate;

    /** 经营范围 */
    private String businessScope;

    /** 联系人电话 */
    private String contactPersonPhone;

    /** 联系人邮箱 */
    private String contactPersonEmail;

    // ═══════════════════════════════════════════════════════════════════════
    // Tab② 纳税人信息（开票资料 5 列）
    // ═══════════════════════════════════════════════════════════════════════

    /** 纳税人识别号（开票资料） */
    private String taxNumber;

    /** 纳税人信息 - 地址（开票地址） */
    private String taxpayerAddress;

    /** 纳税人信息 - 电话（开票电话） */
    private String taxpayerPhone;

    /** 纳税人信息 - 开户行地址 */
    private String bankName;

    /** 纳税人信息 - 开户行账号 */
    private String bankAccount;

    // ═══════════════════════════════════════════════════════════════════════
    // 企业标识
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 企业 LOGO 访问地址
     *
     * <p>由通用上传端点 {@code POST /api/file/upload} 返回（形如 {@code /api/file/view/yyyy/MM/dd/xxx.png}），
     * 只存 URL 不存二进制。对应开发文档 §13.4-4 登记的「LOGO 未做」缺口。</p>
     */
    private String logoUrl;
}
