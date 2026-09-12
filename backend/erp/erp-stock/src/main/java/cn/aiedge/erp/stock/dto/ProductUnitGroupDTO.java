package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品单位组新增/修改 DTO
 *
 * <p>对标「单位组新增编辑」表单（2026-09-11 实测）只有 3 行单位配置，
 * 无名称/助记码/备注输入项，故 DTO 仅承载状态与单位明细。
 */
@Data
@Schema(description = "商品单位组")
public class ProductUnitGroupDTO {

    @Schema(description = "状态: 1启用 0停用")
    private Integer status;

    @Schema(description = "单位明细（小单位/中单位/大单位）")
    private List<ProductUnitGroupItemDTO> items;
}
