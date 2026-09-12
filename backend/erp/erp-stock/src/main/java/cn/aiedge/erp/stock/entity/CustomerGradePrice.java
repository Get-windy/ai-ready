package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 级别指定价设置（商品价格管理 → 子标签「级别指定价设置」）
 * <p>
 * 一行 = 客户级别 ×（商品 | 分类）的指导价规则；对标「商品」列可填商品或商品分类。
 */
@Data
@Accessors(chain = true)
@TableName("erp_customer_grade_price")
public class CustomerGradePrice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 客户级别名称 */
    private String gradeName;

    private Long gradeId;

    /** 商品（填分类时为空） */
    private Long productId;

    private String productCode;

    private String productName;

    /** 商品分类（填商品时为空） */
    private Long categoryId;

    private String categoryName;

    private Long unitId;

    private String unitName;

    /** 基础价类型：零售价 / 批发价 / 价格等级名 */
    private String basePriceType;

    /** 计算符：+ - * / */
    private String calcOperator;

    /** 计算数 */
    private BigDecimal calcValue;

    /** 价格规则展示文本，如「餐饮店-2」「指定价 3.34」 */
    private String priceRule;

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
}
