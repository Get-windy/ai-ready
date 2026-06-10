package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品分类实体
 *
 * @author AI-Ready Team
 * @since 1.5.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_category")
public class ProductCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 父级ID(0=根节点) */
    private Long parentId;

    /** 层级(1/2/3...) */
    private Integer categoryLevel;

    /** 排序 */
    private Integer sortOrder;

    /** 图标 */
    private String icon;

    /** 状态: 1启用 0停用 */
    private Integer status;

    /** 描述 */
    private String description;

    /** 备注 */
    private String remark;

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

    // ── 非持久化字段 ──

    /** 子分类列表(树形结构用) */
    @TableField(exist = false)
    private List<ProductCategory> children;

    /** 产品数量(统计用) */
    @TableField(exist = false)
    private Integer productCount;
}
