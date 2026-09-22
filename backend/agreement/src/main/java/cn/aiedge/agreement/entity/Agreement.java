package cn.aiedge.agreement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 协议主档（一张表 + {@code agreementType} 承载三类协议，裁定①）。
 *
 * <p><b>⚠️ 本表是「系统级」（裁定⑥）</b>：{@code tenant_id} <b>恒为 0</b>，
 * 四张协议表都登记在 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 里，
 * 所以租户拦截器在本表上<b>不生效</b>。</p>
 *
 * <p>⇒ 「谁能看到这份协议」完全由 {@code partyATenantId} / {@code partyBTenantId} 显式判定，
 * 判定条件<b>收敛在唯一一处</b>：{@code cn.aiedge.agreement.domain.AgreementVisibility}。
 * 任何 Service 方法都<b>不许</b>自己再写一遍 {@code party_a_tenant_id = ? OR party_b_tenant_id = ?}
 * ——漏一处就是跨租户数据泄露（这是裁定⑥点名要求防的事）。</p>
 *
 * <p><b>⚠️ 显式写 {@code tenant_id = 0}</b>：MyBatis-Plus 的 {@code MetaObjectHandler.insertFill}
 * 在实体 tenantId 为 null 时会填入<b>会话租户</b>，于是协议主档会落到某个租户头上、
 * 另一端租户立刻查不到。因此创建主档时必须显式 set 0（见 {@code AgreementServiceImpl#create}）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement")
public class Agreement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，**恒为 0**。不要用它判断归属，见类注释。 */
    private Long tenantId;

    /** 协议编号（复用系统唯一号段服务 {@code BizNumberGeneratorService}，不自造） */
    private String agreementNo;

    /** 协议类型：PLATFORM_SERVICE / DISTRIBUTION / GOODS_FRAMEWORK / CONSUMER_PROMISE */
    private String agreementType;

    private String title;

    /** 甲方：主体 + 该主体所属租户（两端对称，裁定②） */
    private Long partyAId;
    private Long partyATenantId;

    /** 乙方：主体 + 该主体所属租户；消费者单方承诺时为「不特定消费者」，允许为空 */
    private Long partyBId;
    private Long partyBTenantId;

    /** 当前生效版本 ID（agreement_version 中 status=1 的那一条）；未生效的草稿协议为空 */
    private Long currentVersionId;

    /** 0=DRAFT 洽谈中 / 1=ACTIVE 生效 / 2=SUSPENDED 暂停 / 3=TERMINATED 终止 */
    private Integer status;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    /** 平台终止留痕（㉝ 清退权） */
    private Long terminatedBy;
    private LocalDateTime terminatedAt;
    private String terminateReason;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
