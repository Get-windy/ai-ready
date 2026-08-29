package cn.aiedge.erp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 辅助核算项目DTO
 */
@Data
public class FinanceAuxiliaryItemDTO {

    private Long id;

    /**
     * 关联辅助核算类型ID
     */
    @NotNull(message = "辅助核算类型ID不能为空")
    private Long auxiliaryTypeId;

    /**
     * 项目编码
     */
    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    /**
     * 上级ID(支持层级)
     */
    private Long parentId;

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
