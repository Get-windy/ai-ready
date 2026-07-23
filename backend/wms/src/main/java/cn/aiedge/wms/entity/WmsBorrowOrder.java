package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
