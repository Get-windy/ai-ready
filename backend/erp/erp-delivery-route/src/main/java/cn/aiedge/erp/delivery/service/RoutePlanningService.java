package cn.aiedge.erp.delivery.service;

import cn.aiedge.erp.delivery.dto.DeliveryEtaNotifyQueryDTO;
import cn.aiedge.erp.delivery.dto.ExpeditePointDTO;
import cn.aiedge.erp.delivery.dto.RouteEtaVO;
import cn.aiedge.erp.delivery.entity.DeliveryEtaNotify;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 配送路线规划 / 催单调整 / ETA 预估
 *
 * 地图能力统一由 `dms-delivery` 提供（含未配置 Key 的降级直线估算），本模块只消费。
 */
public interface RoutePlanningService {

    /** 按地图能力对点位重排，回填顺序 / 里程 / 时长 / 分段距离 */
    Map<String, Object> planOrder(Long routeId);

    /** 催单：把指定点位移到目标序号，并可按地图能力重排其后点位 */
    Map<String, Object> expeditePoint(Long routeId, Long pointId, ExpeditePointDTO dto);

    /** 预估各剩余点位的到达时间（沿既定顺序累计，不重排） */
    RouteEtaVO calcEta(Long routeId);

    /** 生成 ETA 通知「待发送」记录（通道未接入，先保留能力） */
    Map<String, Object> notifyEta(Long routeId, List<Long> pointIds, String channel);

    /** ETA 通知台账分页 */
    Page<DeliveryEtaNotify> pageNotify(DeliveryEtaNotifyQueryDTO query);

    /** 通知状态回写（人工标记已发送 / 标记失败 / 作废） */
    int updateNotifyStatus(List<Long> ids, String status, String errorMsg);

    /** 尝试发送（通道未接入时明确返回未配置，且不篡改记录状态） */
    Map<String, Object> sendNotify(List<Long> ids);

    /** 把「待发送且未投递」的通知补投递到消息底座（幂等，可指定路线单） */
    Map<String, Object> dispatchPending(Long routeId);
}
