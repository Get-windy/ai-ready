package cn.aiedge.trade.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.channel.CallbackPayloadParser;
import cn.aiedge.trade.channel.ExternalChannelAdapter;
import cn.aiedge.trade.dto.ExternalOrderDTO;
import cn.aiedge.trade.entity.ExternalChannelConfig;
import cn.aiedge.trade.entity.ExternalOrderRaw;
import cn.aiedge.trade.mapper.ExternalChannelConfigMapper;
import cn.aiedge.trade.mapper.ExternalOrderRawMapper;
import cn.aiedge.trade.monitor.TimeParsers;
import cn.aiedge.trade.service.ExternalOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalOrderServiceImpl implements ExternalOrderService {

    private final ExternalOrderRawMapper rawMapper;
    private final ExternalChannelConfigMapper configMapper;
    private final List<ExternalChannelAdapter> adapters;

    private Map<String, ExternalChannelAdapter> adapterMap;

    private Map<String, ExternalChannelAdapter> getAdapterMap() {
        if (adapterMap == null) {
            adapterMap = adapters.stream()
                    .collect(Collectors.toMap(ExternalChannelAdapter::getChannelCode, Function.identity()));
        }
        return adapterMap;
    }

    @Override
    @Transactional
    public ExternalOrderRaw receiveCallback(String channelCode, String callbackData) {
        // 幂等校验 - 先解析订单ID
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的渠道: " + channelCode);
        }

        // 验证签名
        // if (!adapter.verifyCallbackSignature(callbackData, signature)) {
        //     throw new SecurityException("回调签名验证失败");
        // }

        ExternalOrderDTO dto = adapter.handleOrderCallback(callbackData);

        // 外部订单号缺失时：先从原始报文兜底提取，仍缺则给出**明确业务错误**，
        // 避免直接落到 external_order_raw.external_order_id NOT NULL 约束（表现为 500）
        String externalOrderId = dto == null ? null : dto.getExternalOrderId();
        if (!StringUtils.hasText(externalOrderId)) {
            externalOrderId = CallbackPayloadParser.extractOrderId(callbackData);
        }
        if (!StringUtils.hasText(externalOrderId)) {
            throw new IllegalArgumentException("回调报文缺少外部订单号（externalOrderId）");
        }
        if (dto == null) {
            dto = new ExternalOrderDTO();
        }
        dto.setExternalOrderId(externalOrderId);

        // 检查是否已存在
        if (existsByExternalId(channelCode, externalOrderId)) {
            log.warn("订单已存在,忽略重复回调: {}", externalOrderId);
            return rawMapper.selectOne(new LambdaQueryWrapper<ExternalOrderRaw>()
                    .eq(ExternalOrderRaw::getChannelCode, channelCode)
                    .eq(ExternalOrderRaw::getExternalOrderId, externalOrderId));
        }

        // 保存原始数据
        ExternalOrderRaw raw = new ExternalOrderRaw();
        raw.setChannelCode(channelCode);
        raw.setExternalOrderId(externalOrderId);
        raw.setRawData(callbackData);
        raw.setReceiveTime(LocalDateTime.now());
        raw.setProcessStatus(0);
        raw.setRetryCount(0);
        rawMapper.insert(raw);

        log.info("接收外部订单回调: channel={}, externalOrderId={}", channelCode, dto.getExternalOrderId());
        return raw;
    }

    @Override
    public int pullOrders(String channelCode, String startTime, String endTime) {
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的渠道: " + channelCode);
        }

        ExternalChannelConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<ExternalChannelConfig>()
                        .eq(ExternalChannelConfig::getChannelCode, channelCode));

        if (config == null || config.getSyncEnabled() != 1) {
            log.warn("渠道未配置或未启用同步: {}", channelCode);
            return 0;
        }

        // 初始化/刷新Token
        if (!adapter.isConnected()) {
            adapter.initialize(parseConfig(config));
        }

        List<ExternalOrderDTO> orders = adapter.pullOrders(startTime, endTime, 100);
        int count = 0;

        for (ExternalOrderDTO dto : orders) {
            if (!existsByExternalId(channelCode, dto.getExternalOrderId())) {
                ExternalOrderRaw raw = new ExternalOrderRaw();
                raw.setChannelCode(channelCode);
                raw.setExternalOrderId(dto.getExternalOrderId());
                raw.setRawData(dto.getRawJson());
                raw.setReceiveTime(LocalDateTime.now());
                raw.setProcessStatus(0);
                rawMapper.insert(raw);
                count++;
            }
        }

        // 更新同步时间
        config.setLastSyncTime(LocalDateTime.now());
        configMapper.updateById(config);

        log.info("拉取外部订单: channel={}, count={}", channelCode, count);
        return count;
    }

    @Override
    @Transactional
    public int processRawOrders(int limit) {
        List<ExternalOrderRaw> pending = rawMapper.selectPending(0, limit);
        int successCount = 0;

        for (ExternalOrderRaw raw : pending) {
            try {
                Long internalId = convertAndSave(raw.getId());
                if (internalId != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("处理订单失败: rawId={}, error={}", raw.getId(), e.getMessage());
                raw.setProcessStatus(3);
                raw.setErrorMsg(e.getMessage());
                raw.setRetryCount(raw.getRetryCount() + 1);
                rawMapper.updateById(raw);
            }
        }

        return successCount;
    }

    @Override
    @Transactional
    public Long convertAndSave(Long rawId) {
        ExternalOrderRaw raw = rawMapper.selectById(rawId);
        if (raw == null) {
            throw new IllegalArgumentException("原始订单不存在: " + rawId);
        }

        ExternalChannelAdapter adapter = getAdapterMap().get(raw.getChannelCode());
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的渠道: " + raw.getChannelCode());
        }

        // 解析原始数据
        ExternalOrderDTO dto = adapter.handleOrderCallback(raw.getRawData());

        // TODO: 转换为内部SaleOrder并保存
        // 这里需要调用销售订单服务创建订单
        // SaleOrder order = orderConverter.convert(dto);
        // order.setOrderSource(getOrderSource(raw.getChannelCode()));
        // saleOrderService.create(order);

        // 模拟生成内部订单ID
        Long internalOrderId = System.currentTimeMillis();

        // 更新状态
        raw.setProcessStatus(2);
        raw.setInternalOrderId(internalOrderId);
        rawMapper.updateById(raw);

        log.info("订单转换入库成功: rawId={}, internalOrderId={}", rawId, internalOrderId);
        return internalOrderId;
    }

    @Override
    @Transactional
    public boolean retry(Long rawId) {
        ExternalOrderRaw raw = rawMapper.selectById(rawId);
        if (raw == null) {
            throw new IllegalArgumentException("原始订单不存在: " + rawId);
        }
        if (raw.getRetryCount() >= 5) {
            log.warn("订单重试次数超限: rawId={}", rawId);
            return false;
        }

        raw.setProcessStatus(0);
        raw.setErrorMsg(null);
        rawMapper.updateById(raw);

        return true;
    }

    @Override
    public PageResult<ExternalOrderRaw> pageRawOrders(Integer pageNum, Integer pageSize, String channelCode, Integer status) {
        return pageRawOrders(pageNum, pageSize, channelCode, status, null, null, null);
    }

    @Override
    public PageResult<ExternalOrderRaw> pageRawOrders(Integer pageNum, Integer pageSize, String channelCode, Integer status,
                                                      String externalOrderId, String startTime, String endTime) {
        LambdaQueryWrapper<ExternalOrderRaw> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(channelCode), ExternalOrderRaw::getChannelCode, channelCode);
        wrapper.eq(status != null, ExternalOrderRaw::getProcessStatus, status);
        wrapper.like(StringUtils.hasText(externalOrderId), ExternalOrderRaw::getExternalOrderId, externalOrderId);
        LocalDateTime from = TimeParsers.parse(startTime);
        LocalDateTime to = TimeParsers.parse(endTime);
        wrapper.ge(from != null, ExternalOrderRaw::getReceiveTime, from);
        // 结束时间只传日期（00:00:00）时按「含当日」处理，与库存同步台账口径一致
        wrapper.lt(to != null, ExternalOrderRaw::getReceiveTime,
                to != null && to.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? to.plusDays(1) : to);
        wrapper.orderByDesc(ExternalOrderRaw::getReceiveTime).orderByDesc(ExternalOrderRaw::getId);

        Page<ExternalOrderRaw> page = rawMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public int countPending(String channelCode) {
        return rawMapper.countPending(channelCode);
    }

    @Override
    public Map<String, Object> statExternalOrders() {
        long pending = 0L;
        long processed = 0L;
        long failed = 0L;
        // 单条 GROUP BY 聚合（tenant_id 由租户插件注入），不在应用层按当前页累加
        for (Map<String, Object> row : rawMapper.countByProcessStatus()) {
            long count = row.get("count") instanceof Number ? ((Number) row.get("count")).longValue() : 0L;
            int status = row.get("processStatus") instanceof Number ? ((Number) row.get("processStatus")).intValue() : -1;
            switch (status) {
                case 0 -> pending += count;
                case 1, 2 -> processed += count; // 1已转换 / 2已入库 均属「已处理」
                case 3 -> failed += count;
                default -> pending += count; // 状态为空的历史数据按「待处理」计
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", pending + processed + failed);
        out.put("pendingCount", pending);
        out.put("processedCount", processed);
        out.put("failedCount", failed);
        return out;
    }

    @Override
    public boolean existsByExternalId(String channelCode, String externalOrderId) {
        return rawMapper.existsByExternalId(channelCode, externalOrderId);
    }

    private Map<String, String> parseConfig(ExternalChannelConfig config) {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("apiEndpoint", config.getApiEndpoint());
        map.put("appId", config.getAppId());
        map.put("appSecret", config.getAppSecret());
        map.put("accessToken", config.getAccessToken());
        // 可扩展解析configJson
        return map;
    }

    private Integer getOrderSource(String channelCode) {
        // 渠道编码转换为订单来源
        switch (channelCode) {
            case "TAOBAO": return 2;
            case "JD": return 2;
            case "PDD": return 2;
            case "DOUYIN": return 2;
            case "WECHAT_MINI": return 5;
            case "SELF_MALL": return 2;
            case "POS": return 3;
            case "ERP_API": return 1;
            default: return 2;
        }
    }
}