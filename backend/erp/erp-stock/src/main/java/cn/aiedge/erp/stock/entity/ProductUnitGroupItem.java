package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品单位组成员（组内单位及换算率）
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_unit_group_item")
public class ProductUnitGroupItem {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 所属单位组ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /** 引用的单位字典ID（erp_product_unit_dict.id） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    /** 单位类型: SMALL小单位 MEDIUM中单位 LARGE大单位（对标表单固定 3 行） */
    private String unitType;

    /** 单位名称快照 */
    private String unitName;

    /** 换算关系（相对于小单位的倍数，小单位固定为 1） */
    private BigDecimal conversionRate;

    /** 组内排序 */
    private Integer sortOrder;

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
