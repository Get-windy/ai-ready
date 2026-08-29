package cn.aiedge.erp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 辅助核算类型DTO
 */
@Data
public class FinanceAuxiliaryTypeDTO {

    private Long id;

    /**
     * 辅助核算类型编码
     */
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;

    /**
     * 类型名称
     */
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
