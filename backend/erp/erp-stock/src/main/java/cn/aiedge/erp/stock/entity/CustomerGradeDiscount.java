package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户级别折扣设置（商品价格管理 → 子标签「客户级别折扣设置」）
 * <p>
 * 一行 = 一个客户级别（如「A餐饮客户」）的订货价规则：订货价 = 基础价（批发价/零售价/某价格等级） ×|+ 计算数。
 * 级别名称与客户档案 {@code biz_party.party_level} / 客户级别主数据同源，不另建级别表。
 */
@Data
@Accessors(chain = true)
@TableName("erp_customer_grade_discount")
public class CustomerGradeDiscount {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 客户级别 ID（可空，级别以名称与客户档案对齐） */
    private Long gradeId;

    /** 客户级别名称 */
    private String gradeName;

    /** 基础价类型：零售价 / 批发价 / 最低售价 / 8 个价格等级名 */
    private String basePriceType;

    /** 计算符：* + - / */
    private String calcOperator;

    /** 计算数 */
    private BigDecimal calcValue;

    /** 规则预览文本，如「客户级别【重点客户01】的订货价格=重点|vip01*1」 */
    private String previewText;

    private String remark;

    private Integer status;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 最后修改人姓名（查询时联表填充，不落库） */
    @TableField(exist = false)
    private String lastModifierName;
}
