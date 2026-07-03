package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品单位字典实体（简单的单位名称列表）
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_unit_dict")
public class ProductUnitDict {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 单位名称 */
    private String unitName;

    /** 助记码(拼音首字母) */
    private String mnemonicCode;

    /** 单位类型 */
    private String unitType;

    /** 换算率（相对于基本单位） */
    private BigDecimal conversionRate;

    /** 备注 */
    private String remark;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 1启用 0停用 */
    private Integer status;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
