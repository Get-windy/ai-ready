package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商品品牌实体
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_brand")
public class ProductBrand {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 品牌名称 */
    private String brandName;

    /** 助记码(拼音首字母) */
    private String mnemonicCode;

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
