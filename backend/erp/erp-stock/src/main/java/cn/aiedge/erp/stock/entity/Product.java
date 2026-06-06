package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 产品实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product")
public class Product {

    /**
     * 产品ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 单位
     */
    private String unit;

    /**
     * 分类
     */
    private String category;

    /**
     * 规格
     */
    private String spec;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
