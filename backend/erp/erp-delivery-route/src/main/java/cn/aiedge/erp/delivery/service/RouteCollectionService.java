package cn.aiedge.erp.delivery.service;

import cn.aiedge.erp.delivery.dto.AddRoutePointsDTO;
import cn.aiedge.erp.delivery.dto.AutoCollectQueryDTO;
import cn.aiedge.erp.delivery.dto.AutoCollectResultVO;
import cn.aiedge.erp.delivery.dto.DeliveryDemandVO;

import java.util.List;
import java.util.Map;

/**
 * 配送需求归集
 *
 * · 围栏自动归集：命中围栏的销售出库单/销售订单自动入线（可干跑预览）；
 * · 手动添加：不受围栏限制，运营可把围栏外订单补进指定路线单。
 */
public interface RouteCollectionService {

    /** 围栏归集（dryRun=true 时仅预览不落库） */
    AutoCollectResultVO autoCollect(AutoCollectQueryDTO query);

    /** 手动添加配送点位（不校验围栏） */
    Map<String, Object> addPoints(Long routeId, AddRoutePointsDTO dto);

    /** 查询可入线的配送需求（供手动选择弹窗） */
    List<DeliveryDemandVO> listDemands(String source, String keyword, Integer limit);

    /** 补录客户配送坐标（biz_party.latitude/longitude），供围栏归集与地图规划使用 */
    Map<String, Object> saveCustomerGeo(Long customerId, java.math.BigDecimal latitude, java.math.BigDecimal longitude);

    /** 客户配送坐标清单（归集弹窗展示「哪些客户还没坐标」） */
    List<Map<String, Object>> listCustomerGeos(Integer limit);
}
