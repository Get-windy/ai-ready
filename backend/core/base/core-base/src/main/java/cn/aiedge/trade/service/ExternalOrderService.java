package cn.aiedge.trade.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.dto.ExternalOrderDTO;
import cn.aiedge.trade.entity.ExternalOrderRaw;

import java.util.List;

/**
 * 外部订单接入服务
 *
 * 核心职责：
 * 1. 接收外部平台订单（回调/拉取）
 * 2. 转换为内部订单格式
 * 3. 幂等处理防止重复入库
 * 4. 异步处理防止阻塞
 */
public interface ExternalOrderService {

    /**
     * 接收外部订单回调
     * @param channelCode 渠道编码
     * @param callbackData 回调数据
     * @return 处理结果
     */
    ExternalOrderRaw receiveCallback(String channelCode, String callbackData);

    /**
     * 从外部平台拉取订单
     * @param channelCode 渠道编码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 拉取数量
     */
    int pullOrders(String channelCode, String startTime, String endTime);

    /**
     * 处理待转换的原始订单
     * @param limit 处理数量限制
     * @return 处理成功数量
     */
    int processRawOrders(int limit);

    /**
     * 转换并入库订单
     * @param rawId 原始订单ID
     * @return 内部订单ID
     */
    Long convertAndSave(Long rawId);

    /**
     * 重试失败的订单
     * @param rawId 原始订单ID
     * @return 是否成功
     */
    boolean retry(Long rawId);

    /**
     * 分页查询原始订单
     */
    PageResult<ExternalOrderRaw> pageRawOrders(Integer pageNum, Integer pageSize, String channelCode, Integer status);

    /**
     * 分页查询原始订单（管理端扩展条件：渠道 + 状态 + 外部订单号 + 接收时间区间）
     *
     * @param externalOrderId 外部订单号（模糊匹配，可空）
     * @param startTime       接收时间起（可空，支持 yyyy-MM-dd / yyyy-MM-dd HH:mm:ss）
     * @param endTime         接收时间止（可空）
     */
    PageResult<ExternalOrderRaw> pageRawOrders(Integer pageNum, Integer pageSize, String channelCode, Integer status,
                                               String externalOrderId, String startTime, String endTime);

    /**
     * 查询待处理订单数量
     */
    int countPending(String channelCode);

    /**
     * 外部订单处理状态统计（真实聚合 SQL，非当前页口径）
     *
     * <p>返回：total 总笔数 / pendingCount 待处理(0) / processedCount 已处理(1已转换+2已入库) / failedCount 失败(3)。</p>
     */
    java.util.Map<String, Object> statExternalOrders();

    /**
     * 检查订单是否已存在（幂等校验）
     */
    boolean existsByExternalId(String channelCode, String externalOrderId);
}