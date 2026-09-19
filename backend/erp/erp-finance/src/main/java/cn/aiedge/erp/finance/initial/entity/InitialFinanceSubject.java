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
 * 财务期初（按科目）实体 —— 表 {@code erp_initial_finance_subject}（迁移 V11.399.0）。
 *
 * <p>承载 ql361「设置 → 期初录入 → 财务期初」中 **3 个「按科目」Tab** 的期初余额：
 * 银行现金期初 / 固定资产期初 / 资产负债期初（由 {@code initialType} 区分）。</p>
 *
 * <p><b>为什么不用 remark</b>：开发文档 §3.7 明确「备注」是本系统自加、对标无该列 → 不落库。</p>
 *
 * <p><b>主键策略</b>：{@link IdType#ASSIGN_ID}（应用侧雪花），因此建表脚本**不需要序列** ——
 * 与 CRM 三表「缺主键序列导致建档必 400」的坑相反，此处是刻意避开。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_initial_finance_subject")
public class InitialFinanceSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 期初类型：银行现金期初 */
    public static final String TYPE_BANK_CASH = "BANK_CASH";
    /** 期初类型：固定资产期初 */
    public static final String TYPE_FIXED_ASSET = "FIXED_ASSET";
    /** 期初类型：资产负债期初 */
    public static final String TYPE_BALANCE_SHEET = "BALANCE_SHEET";

    /** 借贷方向：借方 */
    public static final String DIRECTION_DEBIT = "DEBIT";
    /** 借贷方向：贷方 */
    public static final String DIRECTION_CREDIT = "CREDIT";

    /** 主键（雪花 ID，前端一律按字符串处理） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID（多租户拦截器自动注入，表**有** tenant_id 故不加入 IGNORE_TENANT_TABLES） */
    private Long tenantId;

    /** 期初类型：BANK_CASH 银行现金 / FIXED_ASSET 固定资产 / BALANCE_SHEET 资产负债 */
    private String initialType;

    /** 期初年度（2000–2099，对标无该列，为「当前会计年」的显式化） */
    private Integer periodYear;

    /** 会计科目 ID（finance_account_subject.id） */
    private Long subjectId;

    /** 科目编号（快照，对标列「科目编号」） */
    private String subjectCode;

    /** 科目名称（快照，对标列「科目名称」） */
    private String subjectName;

    /** 借贷方向 DEBIT 借方 / CREDIT 贷方（**仅资产负债期初有值**，其余两类为 null） */
    private String direction;

    /** 期初金额（对标列「期初金额」，允许负数） */
    private BigDecimal openingAmount;

    /** 逻辑删除 0-正常 1-已删除（@TableLogic：deleteById 走 UPDATE 而非物理删除） */
    @TableLogic
    private Integer deleted = 0;

    /** 创建时间（由 MetaObjectHandler 自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（由 MetaObjectHandler 自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    private Long createBy;

    /** 更新人 */
    private Long updateBy;
}
