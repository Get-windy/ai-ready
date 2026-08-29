package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 辅助核算类型实体
 */
@Data
@TableName("finance_auxiliary_type")
@EqualsAndHashCode(callSuper = true)
public class FinanceAuxiliaryType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 辅助核算类型编码 (DEPT/PROJECT/CUSTOMER/SUPPLIER/EMPLOYEE/OTHER)
     */
    @TableField("type_code")
    private String typeCode;

    /**
     * 类型名称
     */
    @TableField("type_name")
    private String typeName;

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
