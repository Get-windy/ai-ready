package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送路线单（执行单）分页查询参数
 *
 * 对标状态：ql361 无「配送」模块，本页为本系统自主建模，查询条件按《配送路线单开发文档》§3.3 落地。
 * 查询项：筛选条件（路线编号/线路名称/配送员/车牌/起终点）+ 线路 + 配送员 + 车辆 +
 *        状态（多选）+ 计划配送日期区间 + 创建时间区间 + 显示已取消。
 */
@Data
public class DeliveryRouteQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 筛选条件：路线编号 / 线路名称 / 配送员 / 车牌号 / 起点 / 终点 模糊匹配 */
    private String keyword;

    /** 线路档案ID */
    private Long routeId;

    /** 配送员ID（可存 riderId 或员工 userId，按字符串等值匹配） */
    private String deliveryPersonId;

    /** 车辆ID */
    private Long vehicleId;

    /** 状态多选，逗号分隔：PLANNING,READY,IN_PROGRESS,COMPLETED,CANCELLED */
    private String status;

    private LocalDate planDateStart;

    private LocalDate planDateEnd;

    private LocalDateTime createTimeStart;

    private LocalDateTime createTimeEnd;

    /** 显示已取消 1=包含已取消路线（默认不显示） */
    private Integer showCancelled;

    private Integer pageNum;

    private Integer pageSize;

    /** 排序字段白名单：routeCode / planDate / status / totalDistance / totalPoints / createTime / startTime / completeTime */
    private String sortField;

    /** 排序方向：asc / desc（默认 desc） */
    private String sortOrder;
}
