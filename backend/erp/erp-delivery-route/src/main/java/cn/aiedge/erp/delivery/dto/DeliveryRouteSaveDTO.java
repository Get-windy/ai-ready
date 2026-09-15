package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 配送路线单（执行单）新增 / 修改入参
 *
 * 新增路线为「手工建单」：不依赖高德 key（AmapService 仅用于 optimize/reoptimize/navigation）。
 * 点位为空或坐标缺失时仍可建单，后续可调用 /{id}/optimize 重新规划。
 */
@Data
public class DeliveryRouteSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 路线编号；留空则按 PSXL-YYYYMMDD-序号 自动生成 */
    private String routeCode;

    /** 线路档案ID（可空；填写时回填线路名称/类型快照） */
    private Long routeId;

    /** 配送员ID（必填） */
    private String deliveryPersonId;

    private String deliveryPersonName;

    private Long vehicleId;

    private String vehicleNo;

    /** 绑定电子围栏ID（dms_geo_fence，可空；档案在《路线规划》维护） */
    private Long fenceId;

    /** 是否参与围栏自动归集 1=参与（默认） 0=仅手工维护 */
    private Integer autoCollect;

    /** 显式解绑围栏（fenceId 为空且本标记为 true 时清空绑定） */
    private Boolean clearFence;

    /** 计划配送日期 */
    private LocalDate planDate;

    /** 起点地址；留空取首个点位地址 */
    private String startPoint;

    private String startLatitude;

    private String startLongitude;

    private String remark;

    /** 点位明细（至少 1 个） */
    private List<RoutePointSaveDTO> points;
}
