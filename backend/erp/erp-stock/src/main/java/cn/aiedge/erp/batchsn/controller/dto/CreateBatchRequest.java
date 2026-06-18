package cn.aiedge.erp.batchsn.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建批次请求DTO
 *
 * @author team-member
 * @date 2026-05-05
 */
@Data
public class CreateBatchRequest {

    /**
     * 批次编号
     */
    @NotBlank(message = "批次编号不能为空")
    @Size(min = 1, max = 50, message = "批次编号长度必须在1-50个字符之间")
    private String batchNo;

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /**
     * 产品编码
     */
    @NotBlank(message = "产品编码不能为空")
    @Size(min = 1, max = 50, message = "产品编码长度必须在1-50个字符之间")
    private String productCode;

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    @Size(min = 1, max = 100, message = "产品名称长度必须在1-100个字符之间")
    private String productName;

    /**
     * 规格型号
     */
    @Size(max = 200, message = "规格型号长度不能超过200个字符")
    private String specification;

    /**
     * 计量单位
     */
    @Size(max = 20, message = "计量单位长度不能超过20个字符")
    private String unit;

    /**
     * 生产日期
     */
    @NotNull(message = "生产日期不能为空")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    private LocalDate expirationDate;

    /**
     * 批次状态
     */
    @Size(max = 20, message = "批次状态长度不能超过20个字符")
    private String batchStatus;

    /**
     * 总数量
     */
    @NotNull(message = "总数量不能为空")
    @PositiveOrZero(message = "总数量必须大于等于0")
    private BigDecimal totalQuantity;

    /**
     * 可用数量
     */
    @NotNull(message = "可用数量不能为空")
    @PositiveOrZero(message = "可用数量必须大于等于0")
    private BigDecimal availableQuantity;

    /**
     * 预留数量
     */
    @NotNull(message = "预留数量不能为空")
    @PositiveOrZero(message = "预留数量必须大于等于0")
    private BigDecimal reservedQuantity;

    /**
     * 来源类型
     */
    @Size(max = 20, message = "来源类型长度不能超过20个字符")
    private String sourceType;

    /**
     * 来源参考号
     */
    @Size(max = 50, message = "来源参考号长度不能超过50个字符")
    private String sourceRefNo;

    /**
     * 仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /**
     * 仓库名称
     */
    @Size(max = 100, message = "仓库名称长度不能超过100个字符")
    private String warehouseName;

    /**
     * 库位ID
     */
    private Long locationId;

    /**
     * 质检状态
     */
    @Size(max = 20, message = "质检状态长度不能超过20个字符")
    private String qualityStatus;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}