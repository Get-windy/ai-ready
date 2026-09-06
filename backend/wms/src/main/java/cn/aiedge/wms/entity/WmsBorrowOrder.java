package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_borrow_order")
@Schema(description = "借进借出单")
public class WmsBorrowOrder extends BaseEntity {
    @Schema(description = "单号 JJ/JC+yyyyMMdd+3位流水")
    private String orderNo;
    @Schema(description = "方向 1-借进 2-借出")
    private Integer direction;
    @Schema(description = "往来单位ID")
    private Long partnerId;
    @Schema(description = "往来单位名称")
    private String partnerName;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "借出/借进日期")
    private LocalDate borrowDate;
    @Schema(description = "预计归还日期")
    private LocalDate expectedReturnDate;
    @Schema(description = "状态 0-草稿 1-待审批 2-已审批 3-部分归还 4-已归还 5-已取消")
    private Integer status;
    @Schema(description = "总数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已归还数量")
    private BigDecimal returnedQuantity;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "往来单位编号")
    private String partnerCode;
    @Schema(description = "经手人ID")
    private Long handlerId;
    @Schema(description = "经手人姓名")
    private String handlerName;
    @Schema(description = "部门ID")
    private Long deptId;
    @Schema(description = "部门名称")
    private String deptName;
    @Schema(description = "制单人")
    private String creatorName;
    @Schema(description = "记账人ID")
    private Long bookkeeperId;
    @Schema(description = "记账人")
    private String bookkeeperName;
    @Schema(description = "记账时间")
    private LocalDateTime bookkeepingTime;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "附件")
    private String attachment;
    @Schema(description = "打印次数")
    private Integer printCount;
    @Schema(description = "借进/借出金额")
    private BigDecimal borrowAmount;
    @Schema(description = "借进/借出数量")
    private BigDecimal borrowQuantity;
    @Schema(description = "未处理数量")
    private BigDecimal nonProcessedQuantity;
    @Schema(description = "未处理金额")
    private BigDecimal nonProcessedAmount;
    @Schema(description = "借转采购数量")
    private BigDecimal convertPurchaseQuantity;
    @Schema(description = "借转采购金额")
    private BigDecimal convertPurchaseAmount;
    @Schema(description = "总重量（kg）")
    private BigDecimal totalWeight;
    @Schema(description = "总体积（m³）")
    private BigDecimal totalVolume;
    @Schema(description = "红冲标记 0-否 1-是")
    private Integer redFlag;
}
