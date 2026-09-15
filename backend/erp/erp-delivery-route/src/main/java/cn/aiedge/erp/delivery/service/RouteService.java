package cn.aiedge.erp.delivery.service;

import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 配送路线单（执行单）服务
 *
 * 红线：本服务管理的是**配送执行单**（谁跑、跑到哪、开始/完成/取消），
 *      线路档案主数据见 {@code cn.aiedge.erp.delivery.route.RouteMasterService}（erp_route）。
 */
public interface RouteService {

    // ==================== 台账（金标准骨架） ====================

    /** 多条件分页 */
    Page<DeliveryRouteVO> pageRoutes(DeliveryRouteQueryDTO query);

    /** 多条件全量查询（导出用） */
    List<DeliveryRouteVO> listRoutes(DeliveryRouteQueryDTO query);

    /** 详情（含点位明细） */
    DeliveryRouteVO getRouteVO(Long routeId);

    /** 生成下一个路线编号 PSXL-YYYYMMDD-序号 */
    String nextNo();

    /** 手工建单（不依赖高德 key） */
    DeliveryRouteVO createRoute(DeliveryRouteSaveDTO dto);

    /** 修改（仅规划中/待出发可改，点位整体替换） */
    DeliveryRouteVO updateRoute(Long routeId, DeliveryRouteSaveDTO dto);

    /** 批量状态流转（action: start / complete / cancel） */
    Map<String, Object> batchStatus(List<Long> ids, String action, String reason);

    /** 点位签收（多点签收，逐点独立） */
    void signPoint(Long routeId, Long pointId, RoutePointSignDTO dto);

    /** 追加配送点位（围栏归集 / 手动添加共用；返回追加后的完整点位列表） */
    List<RoutePointResult> appendPoints(Long routeId, List<RoutePointSaveDTO> points);

    /** 配送员当前进行中的路线（司机端/实时跟踪消费） */
    DeliveryRoute getActiveRouteByPerson(String deliveryPersonId);

    // ==================== 状态流转 ====================

    void startRoute(Long routeId);

    void completeRoute(Long routeId, String reason);

    void cancelRoute(Long routeId, String reason);
}
