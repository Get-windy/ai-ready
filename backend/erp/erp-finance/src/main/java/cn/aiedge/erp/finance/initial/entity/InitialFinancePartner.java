package cn.aiedge.erp.finance.initial.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务期初（按往来单位）实体 —— 表 {@code erp_initial_finance_partner}（迁移 V11.399.0）。
 *
 * <p>承载 ql361「设置 → 期初录入 → 财务期初」中 **2 个「按往来单位」Tab** 的期初余额：
 * 应付期初（应付金额 / 预付金额）与应收期初（默认经手人 / 应收金额 / 预收金额），
 * 由 {@code initialType} 区分。</p>
 *
 * <p><b>为什么与「按科目」分表</b>（开发文档 §7.3 路线 B）：应付 Tab 无「默认经手人」列、
 * 应收 Tab 无「预付金额」列，两者结构互斥；混表会产生稀疏列且无法表达
 * 「应收必须有客户」这类约束。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_initial_finance_partner")
public class InitialFinancePartner implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 期初类型：应付期初 */
    public static final String TYPE_PAYABLE = "PAYABLE";
    /** 期初类型：应收期初 */
    public static final String TYPE_RECEIVABLE = "RECEIVABLE";

    /** 主键（雪花 ID，前端一律按字符串处理） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 期初类型：PAYABLE 应付 / RECEIVABLE 应收 */
    private String initialType;

    /** 期初年度（2000–2099） */
    private Integer periodYear;

    /** 往来单位 ID（往来单位档案 biz_party.id，数据源＝/erp/md/customer/list） */
    private Long partnerId;

    /** 供应商编号 / 客户编号（快照，对标列） */
    private String partnerCode;

    /** 供应商名称 / 客户名称（快照，对标列） */
    private String partnerName;

    /** 默认经手人（**仅应收期初有值**，应付 Tab 无此列） */
    private String defaultHandler;

    /** 应付金额（应付 Tab） */
    private BigDecimal payableAmount;

    /** 预付金额（应付 Tab） */
    private BigDecimal prepayAmount;

    /** 应收金额（应收 Tab） */
    private BigDecimal receivableAmount;

    /** 预收金额（应收 Tab） */
    private BigDecimal advanceAmount;

    /** 逻辑删除 0-正常 1-已删除 */
    @TableLogic
    private Integer deleted = 0;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    private Long createBy;

    /** 更新人 */
    private Long updateBy;
}
