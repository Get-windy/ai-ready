package cn.aiedge.dms.channel.adapter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配送平台统一适配器接口
 * 每个外部平台独立实现此接口
 */
public interface DeliveryAdapter {

    /** 适配器唯一标识 */
    String getChannelCode();

    /**
     * 连通性自检（渠道管理页「连通性测试」调用）。
     * 默认视为「未实现」——各平台适配器接入后覆写为真实探活（鉴权接口/签名校验）。
     */
    default CheckResult check() {
        return new CheckResult(false, "适配器未实现连通性自检（尚未对接该平台）");
    }

    /**
     * 拉取该平台的配送员（渠道管理页「同步运力」调用）。
     * 默认返回空列表——适配器未对接时不得伪造运力数据。
     */
    default List<RemoteRider> fetchRiders() {
        return List.of();
    }

    record CheckResult(boolean success, String message) {
    }

    record RemoteRider(String realName, String phone, String vehicleType, String vehicleNo) {
    }

    /**
     * 创建配送单（渠道派单调用）。
     * 默认视为「未实现」——**必须如实失败**，绝不返回模拟单号，
     * 否则派单链路会被「假成功」污染（金标准红线：无桩、无模拟）。
     */
    default CreateResult createOrder(CreateRequest request) {
        return new CreateResult(false, null, notImplemented("下单"));
    }

    /** 查询配送状态（默认未实现，如实失败） */
    default QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "UNKNOWN", null, null, null, null);
    }

    /** 取消配送（默认未实现，如实失败） */
    default CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(false, notImplemented("取消"));
    }

    /** 预估配送费用（默认未实现，如实失败，不返回模拟报价） */
    default EstimateResult estimateFee(EstimateRequest request) {
        return new EstimateResult(false, null, notImplemented("费用预估"));
    }

    /** 查询骑手位置（默认未实现，如实失败，不返回模拟坐标） */
    default LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(false, null, null, notImplemented("位置查询"));
    }

    /** 未实现能力的统一说明（接入方覆写对应方法后自动消失） */
    default String notImplemented(String capability) {
        return "适配器 " + getChannelCode() + " 尚未实现「" + capability + "」能力（未对接该平台，不返回模拟数据）";
    }

    // ── 请求/响应 DTO ──

    record CreateRequest(
        String orderNo,
        String originAddress,
        BigDecimal originLat,
        BigDecimal originLng,
        String destAddress,
        BigDecimal destLat,
        BigDecimal destLng,
        String contactName,
        String contactPhone,
        String remark
    ) {}

    record CreateResult(
        boolean success,
        String channelOrderNo,
        String message
    ) {}

    record QueryResult(
        String channelOrderNo,
        String channelStatus,
        String riderName,
        String riderPhone,
        BigDecimal riderLat,
        BigDecimal riderLng
    ) {}

    record CancelResult(
        boolean success,
        String message
    ) {}

    record EstimateRequest(
        BigDecimal originLat,
        BigDecimal originLng,
        BigDecimal destLat,
        BigDecimal destLng,
        BigDecimal weight,
        BigDecimal volume
    ) {}

    record EstimateResult(
        boolean success,
        BigDecimal estimatedFee,
        String message
    ) {}

    record LocationResult(
        boolean success,
        BigDecimal lat,
        BigDecimal lng,
        String message
    ) {}
}
