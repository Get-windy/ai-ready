package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_putaway_task")
@Schema(description = "上架任务")
public class WmsPutawayTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "来源 1-收货上架 2-移库上架")
    private Integer sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceId;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "总商品数")
    private Integer totalItems;
    @Schema(description = "总数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已上架数量")
    private BigDecimal putawayQuantity;
    @Schema(description = "状态 0-待上架 1-上架中 2-已完成 3-已取消")
    private Integer status;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "备注")
    private String remark;
}
