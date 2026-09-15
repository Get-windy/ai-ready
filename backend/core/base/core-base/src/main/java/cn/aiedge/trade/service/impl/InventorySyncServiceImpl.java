package cn.aiedge.trade.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import cn.aiedge.trade.channel.ExternalChannelAdapter;
import cn.aiedge.trade.entity.InventorySyncRecord;
import cn.aiedge.trade.mapper.InventorySyncRecordMapper;
import cn.aiedge.trade.monitor.ApiCallDirection;
import cn.aiedge.trade.monitor.ApiCallLogRecorder;
import cn.aiedge.trade.monitor.ErrorCategory;
import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import cn.aiedge.trade.service.InventorySyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySyncServiceImpl implements InventorySyncService {

    /** 出站调用日志的接口标识（与《API监控》「接口调用日志」按接口聚合口径一致） */
    private static final String OUT_PATH_SYNC = "/channel/inventory/sync";
    private static final String OUT_NAME_SYNC = "渠道库存推送";
    private static final String OUT_PATH_PRICE = "/channel/product/price";
    private static final String OUT_NAME_PRICE = "渠道价格推送";

    private final InventorySyncRecordMapper syncMapper;
    private final List<ExternalChannelAdapter> adapters;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiCallLogRecorder apiCallLogRecorder;

    private static final String CACHE_PREFIX = "inventory:";
    private static final int CACHE_EXPIRE_SECONDS = 30;

    private Map<String, ExternalChannelAdapter> adapterMap;

    private Map<String, ExternalChannelAdapter> getAdapterMap() {
        if (adapterMap == null) {
            adapterMap = adapters.stream()
                    .collect(Collectors.toMap(ExternalChannelAdapter::getChannelCode, Function.identity()));
        }
        return adapterMap;
    }

    @Override
    public InventoryQueryResult queryInventory(String skuCode, Long warehouseId) {
        String cacheKey = CACHE_PREFIX + skuCode + ":" + (warehouseId != null ? warehouseId : "all");

        // 先查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof InventoryQueryResult) {
            return (InventoryQueryResult) cached;
        }

        // 查数据库 - 实际应该调用库存服务
        InventoryQueryResult result = new InventoryQueryResult();
        result.setSkuCode(skuCode);
        result.setQuantity(100); // 模拟数据
        result.setAvailableQuantity(100);
        result.setLockedQuantity(0);
        result.setQueryTimestamp(System.currentTimeMillis());
        result.setSuccess(true);

        // 写缓存
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return result;
    }

    @Override
    public Map<String, InventoryQueryResult> batchQueryInventory(List<String> skuCodes, Long warehouseId) {
        Map<String, InventoryQueryResult> results = new java.util.HashMap<>();
        for (String skuCode : skuCodes) {
            results.put(skuCode, queryInventory(skuCode, warehouseId));
        }
        return results;
    }

    @Override
    public ProductSyncResult pushToChannel(String channelCode, String skuCode, Integer quantity) {
        ProductSyncResult result = invokeChannel(channelCode, skuCode, quantity);

        // 记录同步日志（按 SKU 记账；product_id 允许为空，见迁移 V11.360.0）
        writeSyncRecord(channelCode, skuCode, quantity, result);

        // 刷新缓存
        refreshCache(skuCode);

        log.info("推送库存到渠道: channel={}, sku={}, qty={}, result={}", channelCode, skuCode, quantity, result.getStatus());
        return result;
    }

    @Override
    public ProductSyncResult invokeChannel(String channelCode, String skuCode, Integer quantity) {
        long startMs = System.currentTimeMillis();
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        ProductSyncResult result;
        if (adapter == null) {
            result = new ProductSyncResult();
            result.setSkuCode(skuCode);
            result.setStatus(0);
            result.setErrorCode("CHANNEL_NOT_SUPPORTED");
            result.setErrorMsg("不支持的渠道: " + channelCode);
        } else {
            result = adapter.pushInventory(skuCode, quantity);
        }
        recordOutbound(channelCode, skuCode, OUT_PATH_SYNC, OUT_NAME_SYNC, result,
                (int) Math.max(System.currentTimeMillis() - startMs, 0));
        return result;
    }

    @Override
    public Map<String, ProductSyncResult> batchPushToChannel(String channelCode, Map<String, Integer> skuQuantities) {
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        if (adapter == null) {
            return skuQuantities.keySet().stream()
                    .collect(Collectors.toMap(k -> k, k -> {
                        ProductSyncResult fail = new ProductSyncResult();
                        fail.setSkuCode(k);
                        fail.setStatus(0);
                        fail.setErrorCode("CHANNEL_NOT_SUPPORTED");
                        fail.setErrorMsg("不支持的渠道");
                        // 未知渠道同样落一条失败同步记录 + 出站调用日志，保证「失败可查、可重试」
                        recordOutbound(channelCode, k, OUT_PATH_SYNC, OUT_NAME_SYNC, fail, 0);
                        return fail;
                    }));
        }

        // 逐 SKU 走单条通道：与适配器内部实现一致，但可获得**逐 SKU 的同步记录与调用日志**
        Map<String, ProductSyncResult> results = new java.util.LinkedHashMap<>();
        skuQuantities.forEach((sku, qty) -> results.put(sku, pushToChannel(channelCode, sku, qty)));
        return results;
    }

    /**
     * 写库存同步记录（状态口径：1 成功 / 2 失败）+ 失败原因分类
     */
    private void writeSyncRecord(String channelCode, String skuCode, Integer quantity, ProductSyncResult result) {
        boolean success = result != null && Integer.valueOf(1).equals(result.getStatus());
        InventorySyncRecord record = new InventorySyncRecord();
        record.setChannelCode(channelCode);
        record.setSkuCode(skuCode);
        record.setSyncQty(quantity);
        record.setSyncType("PUSH");
        record.setSyncTime(LocalDateTime.now());
        record.setSyncStatus(success ? 1 : 2);
        record.setErrorMsg(success || result == null ? (result == null ? "渠道无返回" : null) : result.getErrorMsg());
        record.setErrorCategory(success ? null : ErrorCategory.classify(record.getErrorMsg()).name());
        record.setRetryCount(0);
        syncMapper.insert(record);
    }

    /**
     * 写出站调用日志（方向 OUT）——《API监控》「接口调用日志」的外部调用来源
     *
     * <p>口径：只落 SKU 与耗时/状态/错误摘要，不落请求体。</p>
     */
    private void recordOutbound(String channelCode, String skuCode, String apiPath, String apiName,
                               ProductSyncResult result, int costMs) {
        boolean success = result != null && Integer.valueOf(1).equals(result.getStatus());
        ApiAccessLog row = apiCallLogRecorder.build(
                ApiCallDirection.OUT.name(), channelCode, apiPath, apiName, "POST",
                success, success ? 200 : 500, costMs,
                success ? null : (result == null ? null : result.getErrorCode()),
                success ? null : (result == null ? "渠道无返回" : result.getErrorMsg()));
        row.setRequestId(apiCallLogRecorder.newRequestId());
        row.setRequestParams("skuCode=" + skuCode);
        row.setAccessTime(LocalDateTime.now());
        apiCallLogRecorder.record(row);
    }

    @Override
    public int fullSyncToChannel(String channelCode) {
        // 查询所有SKU并同步
        // 实际应该调用商品服务获取SKU列表
        List<String> allSkus = java.util.Arrays.asList("SKU001", "SKU002", "SKU003");

        int count = 0;
        for (String sku : allSkus) {
            InventoryQueryResult inventory = queryInventory(sku, null);
            ProductSyncResult result = pushToChannel(channelCode, sku, inventory.getAvailableQuantity());
            if (result.getStatus() == 1) {
                count++;
            }
        }

        log.info("全量同步库存: channel={}, count={}", channelCode, count);
        return count;
    }

    @Override
    public void refreshCache(String skuCode) {
        String pattern = CACHE_PREFIX + skuCode + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    @Override
    public void refreshAllCache() {
        redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
    }

    @Override
    public PageResult<InventorySyncRecord> pageSyncRecords(Integer pageNum, Integer pageSize, String channelCode, String skuCode) {
        LambdaQueryWrapper<InventorySyncRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channelCode != null, InventorySyncRecord::getChannelCode, channelCode);
        wrapper.eq(skuCode != null, InventorySyncRecord::getSkuCode, skuCode);
        wrapper.orderByDesc(InventorySyncRecord::getSyncTime);

        Page<InventorySyncRecord> page = syncMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<InventorySyncRecord> listFailedSyncs(String channelCode) {
        LambdaQueryWrapper<InventorySyncRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channelCode != null, InventorySyncRecord::getChannelCode, channelCode);
        wrapper.eq(InventorySyncRecord::getSyncStatus, 2);
        wrapper.orderByDesc(InventorySyncRecord::getSyncTime);
        return syncMapper.selectList(wrapper);
    }

    @Override
    public ProductSyncResult updatePrice(String channelCode, String skuCode, BigDecimal price) {
        long startMs = System.currentTimeMillis();
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        ProductSyncResult result;
        if (adapter == null) {
            result = new ProductSyncResult();
            result.setSkuCode(skuCode);
            result.setStatus(0);
            result.setErrorCode("CHANNEL_NOT_SUPPORTED");
            result.setErrorMsg("不支持的渠道: " + channelCode);
        } else {
            result = adapter.updatePrice(skuCode, price);
        }
        recordOutbound(channelCode, skuCode, OUT_PATH_PRICE, OUT_NAME_PRICE, result,
                (int) Math.max(System.currentTimeMillis() - startMs, 0));
        return result;
    }
}