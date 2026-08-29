package cn.aiedge.erp.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一采购单据查询参数DTO
 * 对应采购单据查询页的查询条件（18个搜索字段）
 * 分页参数 current/size 与 pageNum/pageSize 双兼容。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "统一采购单据查询参数DTO")
public class UnifiedPurchaseDocQueryDTO {

    // ═══ 分页参数 ═══
    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    @Schema(description = "页码别名", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小别名", defaultValue = "20")
    private Integer pageSize = 20;

    // ═══ 日期与单据基本 ═══
    @Schema(description = "单据日期开始（yyyy-MM-dd）")
    private String dateStart;

    @Schema(description = "单据日期结束（yyyy-MM-dd）")
    private String dateEnd;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "单据类型（INBOUND/RETURN/EXCHANGE，空=全部）")
    private String documentType;

    // ═══ 供应商 ═══
    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "供应商编号")
    private String supplierCode;

    // ═══ 人员 & 部门 ═══
    @Schema(description = "经手人")
    private String handlerName;

    @Schema(description = "部门")
    private String departmentName;

    @Schema(description = "制单人")
    private String creatorName;

    @Schema(description = "记账人")
    private String bookkeeperName;

    // ═══ 仓库 ═══
    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    // ═══ 状态 ═══
    @Schema(description = "结算状态")
    private String settlementStatus;

    @Schema(description = "单据状态")
    private Integer status;

    // ═══ 来源订单 ═══
    @Schema(description = "来源订单编号")
    private String sourceOrder;

    // ═══ 备注 ═══
    @Schema(description = "单据备注")
    private String remark;

    // ═══ 自定义字段(数字范围) ═══
    @Schema(description = "自定义字段1(数字)最小值")
    private BigDecimal extNum1Start;

    @Schema(description = "自定义字段1(数字)最大值")
    private BigDecimal extNum1End;

    @Schema(description = "自定义字段2(数字)最小值")
    private BigDecimal extNum2Start;

    @Schema(description = "自定义字段2(数字)最大值")
    private BigDecimal extNum2End;

    // ═══ 自定义字段(文本) ═══
    @Schema(description = "自定义字段3(文本)")
    private String extText1;

    @Schema(description = "自定义字段4(文本)")
    private String extText2;

    @Schema(description = "自定义字段5(文本)")
    private String extText3;

    // ═══ 其他 ═══
    @Schema(description = "是否显示红冲（红冲单 status=3）")
    private Boolean showRed;

    // ═══ 自定义getter保证分页参数兼容 ═══
    public Long getCurrent() {
        return current != null ? current : (pageNum != null ? (long) pageNum : 1L);
    }

    public Long getSize() {
        return size != null ? size : (pageSize != null ? (long) pageSize : 20L);
    }

    public Integer getPageNum() {
        if (pageNum == null && current != null) {
            pageNum = current.intValue();
        }
        return pageNum != null ? pageNum : 1;
    }

    public Integer getPageSize() {
        if (pageSize == null && size != null) {
            pageSize = size.intValue();
        }
        return pageSize != null ? pageSize : 20;
    }
}
