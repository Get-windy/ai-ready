package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 产品等级实体
 *
 * @author AI-Ready Team
 * @since 1.5.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_grade")
public class ProductGrade {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 等级编码 A/B/C */
    private String gradeCode;

    /** 等级名称 A级/B级/C级 */
    private String gradeName;

    /** 等级数值(越大越高) */
    private Integer gradeLevel;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 1启用 0停用 */
    private Integer status;

    /** 描述 */
    private String description;

    /** 备注 */
    private String remark;

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
