package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交车（解绑）请求
 *
 * <p>与前端 {@code POST /api/dms/verification/binding/{id}/handover} 请求体对齐，
 * 处理人从登录态取，不由前端传入。</p>
 *
 * <p>收车检查随交车一并提交（业界 check-in 单证）：后端在同一事务内落一条
 * {@code inspectionType=2} 的巡检记录，检查异常时自动转《车辆维护》并置车辆维修中。</p>
 */
@Data
@Schema(description = "交车（解绑）请求")
public class HandoverDTO {

    @Schema(description = "交车里程（km）")
    private Integer handoverMileage;

    @Schema(description = "交车地点（文本）")
    private String handoverLocation;

    @Schema(description = "交车位置纬度")
    private BigDecimal handoverLat;

    @Schema(description = "交车位置经度")
    private BigDecimal handoverLng;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "收车后检查（强制：未提交禁止交车；类型缺省为 2-收车后检查）")
    private VehicleInspectionCreateDTO inspection;
}
