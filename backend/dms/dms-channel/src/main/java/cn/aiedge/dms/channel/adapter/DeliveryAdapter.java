package cn.aiedge.dms.channel.adapter;

import java.math.BigDecimal;

/**
 * 配送平台统一适配器接口
 * 每个外部平台独立实现此接口
 */
public interface DeliveryAdapter {

    /** 适配器唯一标识 */
    String getChannelCode();

    /** 创建配送单 */
    CreateResult createOrder(CreateRequest request);

    /** 查询配送状态 */
    QueryResult queryOrder(String channelOrderNo);

    /** 取消配送 */
    CancelResult cancelOrder(String channelOrderNo);

    /** 预估配送费用 */
    EstimateResult estimateFee(EstimateRequest request);

    /** 查询骑手位置 */
    LocationResult queryRiderLocation(String channelOrderNo);

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
