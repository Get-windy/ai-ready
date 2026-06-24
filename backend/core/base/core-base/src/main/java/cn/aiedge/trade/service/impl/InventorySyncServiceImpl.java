package cn.aiedge.trade.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import cn.aiedge.trade.channel.ExternalChannelAdapter;
import cn.aiedge.trade.entity.InventorySyncRecord;
import cn.aiedge.trade.mapper.InventorySyncRecordMapper;
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

    private final InventorySyncRecordMapper syncMapper;
    private final List<ExternalChannelAdapter> adapters;
    private final RedisTemplate<String, Object> redisTemplate;

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
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        if (adapter == null) {
            ProductSyncResult fail = new ProductSyncResult();
            fail.setSkuCode(skuCode);
            fail.setStatus(0);
            fail.setErrorMsg("不支持的渠道: " + channelCode);
            return fail;
        }

        ProductSyncResult result = adapter.pushInventory(skuCode, quantity);

        // 记录同步日志
        InventorySyncRecord record = new InventorySyncRecord();
        record.setChannelCode(channelCode);
        record.setSkuCode(skuCode);
        record.setSyncQty(quantity);
        record.setSyncType("PUSH");
        record.setSyncTime(LocalDateTime.now());
        record.setSyncStatus(result.getStatus() == 1 ? 1 : 2);
        record.setErrorMsg(result.getErrorMsg());
        syncMapper.insert(record);

        // 刷新缓存
        refreshCache(skuCode);

        log.info("推送库存到渠道: channel={}, sku={}, qty={}, result={}", channelCode, skuCode, quantity, result.getStatus());
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
                        fail.setErrorMsg("不支持的渠道");
                        return fail;
                    }));
        }

        return adapter.batchPushInventory(skuQuantities);
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
        ExternalChannelAdapter adapter = getAdapterMap().get(channelCode);
        if (adapter == null) {
            ProductSyncResult fail = new ProductSyncResult();
            fail.setSkuCode(skuCode);
            fail.setStatus(0);
            fail.setErrorMsg("不支持的渠道");
            return fail;
        }

        return adapter.updatePrice(skuCode, price);
    }
}