package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_check_task")
@Schema(description = "盘点任务")
public class WmsCheckTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "盘点类型 1-明盘 2-盲盘")
    private Integer checkType;
    @Schema(description = "盘点范围 1-全库盘点 2-指定货位 3-指定商品 4-指定批次")
    private Integer scopeType;
    @Schema(description = "总商品数")
    private Integer totalItems;
    @Schema(description = "已盘点数")
    private Integer checkedItems;
    @Schema(description = "差异数")
    private Integer diffItems;
    @Schema(description = "状态 0-待盘点 1-盘点中 2-已完成 3-已审核 4-已取消")
    private Integer status;
    @Schema(description = "盘点期间锁定库位 0-否 1-是")
    private Integer lockLocation;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "复盘人")
    private Long checkerId;
    @Schema(description = "复盘人姓名")
    private String checkerName;
    @Schema(description = "审核人")
    private Long approvedBy;
    @Schema(description = "审核时间")
    private LocalDateTime approvedTime;
    @Schema(description = "备注")
    private String remark;
}
