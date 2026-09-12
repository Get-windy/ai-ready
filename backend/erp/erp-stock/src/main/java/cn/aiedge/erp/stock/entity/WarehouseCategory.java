package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓库分类实体（仓库规划左侧分类树）
 *
 * @author AI-Ready Team
 * @since 11.156.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_warehouse_category")
public class WarehouseCategory {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 分类编码（层级编号） */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 上级分类ID（0=顶级） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 层级（1=顶级） */
    private Integer categoryLevel;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // ── 非持久化字段 ──

    /** 子分类（树形结构用） */
    @TableField(exist = false)
    private List<WarehouseCategory> children;

    /** 分类下仓库数量（统计用） */
    @TableField(exist = false)
    private Integer warehouseCount;
}
