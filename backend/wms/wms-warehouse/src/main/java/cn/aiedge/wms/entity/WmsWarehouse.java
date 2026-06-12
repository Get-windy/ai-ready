package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_warehouse")
@Schema(description = "仓库扩展信息")
public class WmsWarehouse extends BaseEntity {
    @Schema(description = "关联erp_warehouse.id")
    private Long warehouseId;
    @Schema(description = "仓库编码")
    private String warehouseCode;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "仓库类型 1-普通 2-冷库 3-危险品 4-保税")
    private Integer warehouseType;
    @Schema(description = "库区数量")
    private Integer zoneCount;
    @Schema(description = "货位数量")
    private Integer locationCount;
    @Schema(description = "总容量(m³)")
    private BigDecimal totalCapacity;
    @Schema(description = "已用容量")
    private BigDecimal usedCapacity;
    @Schema(description = "是否启用WMS管理 0-否 1-是")
    private Integer isWmsEnabled;
    @Schema(description = "备注")
    private String remark;
}
