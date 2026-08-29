package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 辅助核算项目实体
 */
@Data
@TableName("finance_auxiliary_item")
@EqualsAndHashCode(callSuper = true)
public class FinanceAuxiliaryItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联辅助核算类型ID
     */
    @TableField("auxiliary_type_id")
    private Long auxiliaryTypeId;

    /**
     * 项目编码
     */
    @TableField("item_code")
    private String itemCode;

    /**
     * 项目名称
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 上级ID(支持层级)
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}
