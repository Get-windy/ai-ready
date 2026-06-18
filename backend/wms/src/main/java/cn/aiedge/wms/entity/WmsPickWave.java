package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_pick_wave")
@Schema(description = "拣货波次")
public class WmsPickWave extends BaseEntity {
    @Schema(description = "波次号")
    private String waveNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "订单数")
    private Integer orderCount;
    @Schema(description = "商品项数")
    private Integer itemCount;
    @Schema(description = "总拣货数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已拣货数量")
    private BigDecimal pickedQuantity;
    @Schema(description = "状态 0-待分配 1-拣货中 2-已完成 3-已取消")
    private Integer status;
    @Schema(description = "优先级 1-普通 2-紧急 3-加急")
    private Integer priority;
    @Schema(description = "波次类型 1-按单波次 2-按商品聚合 3-按路线")
    private Integer waveType;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "备注")
    private String remark;
}
