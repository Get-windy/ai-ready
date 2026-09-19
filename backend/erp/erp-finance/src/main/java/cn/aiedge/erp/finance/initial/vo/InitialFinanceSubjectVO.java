package cn.aiedge.erp.finance.initial.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务期初（按科目）列表行。
 *
 * <p>⚠️ {@link JsonInclude.Include#NON_NULL} 是**刻意**的：银行现金/固定资产两个 Tab 无
 * 「借贷方向」列，若把 null 也序列化出来，验收断言「银行现金期初返回字段不含 direction」
 * 就会失败。NON_NULL 让无值字段从 JSON 中整体消失，字段集与对标列集严格一致。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitialFinanceSubjectVO {

    /** 主键（Long 由全局 Jackson 配置序列化为字符串，前端不会丢精度） */
    private Long id;

    /** 期初类型 */
    private String initialType;

    /** 期初年度 */
    private Integer periodYear;

    /** 会计科目 ID */
    private Long subjectId;

    /** 科目编号（对标列「科目编号」） */
    private String subjectCode;

    /** 科目名称（对标列「科目名称」） */
    private String subjectName;

    /** 借贷方向（仅资产负债期初；DEBIT 借方 / CREDIT 贷方） */
    private String direction;

    /** 期初金额（对标列「期初金额」） */
    private BigDecimal openingAmount;

    /** 创建时间（已格式化为 yyyy-MM-dd HH:mm:ss，避免前端二次解析） */
    private String createTime;
}
