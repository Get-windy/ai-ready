package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockReplenishment;
import cn.aiedge.erp.stock.event.PurchaseOrderCreateEvent;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.StockReplenishmentMapper;
import cn.aiedge.erp.stock.service.StockReplenishmentService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReplenishmentServiceImpl extends ServiceImpl<StockReplenishmentMapper, StockReplenishment> implements StockReplenishmentService {

    private final StockMapper stockMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<StockReplenishment> pageList(String keyword, String priority, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockReplenishment> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, StockReplenishment::getProductCode, keyword)
                .or(w -> w.like(keyword != null, StockReplenishment::getProductName, keyword))
                .eq(priority != null, StockReplenishment::getPriority, priority)
                .eq(status != null, StockReplenishment::getStatus, status)
                .orderByAsc(StockReplenishment::getPriority)
                .orderByDesc(StockReplenishment::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StockReplenishment> generateSuggestions() {
        Long tenantId = StpUtil.getLoginIdAsLong();

        // 查询所有库存低于最低库存的产品
        List<Stock> lowStockList = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>()
                        .apply("quantity < min_stock")
                        .eq(Stock::getTenantId, tenantId)
        );

        if (lowStockList.isEmpty()) {
            log.info("没有需要补货的产品");
            return new ArrayList<>();
        }

        List<StockReplenishment> suggestions = new ArrayList<>();

        for (Stock stock : lowStockList) {
            // 计算缺货数量
            BigDecimal minStock = stock.getMinStock() != null ? stock.getMinStock() : BigDecimal.ZERO;
            BigDecimal shortageQty = minStock.subtract(stock.getQuantity());
            if (shortageQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 计算建议补货数量（按1.5倍安全库存补货，取整）
            BigDecimal suggestedQty = minStock.multiply(new BigDecimal("1.5"))
                    .subtract(stock.getQuantity())
                    .setScale(0, RoundingMode.UP);

            // 确定优先级
            String priority;
            BigDecimal ratio = stock.getQuantity().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.TEN
                    : minStock.divide(stock.getQuantity(), 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("3")) >= 0) {
                priority = "HIGH";
            } else if (ratio.compareTo(new BigDecimal("1.5")) >= 0) {
                priority = "MEDIUM";
            } else {
                priority = "LOW";
            }

            StockReplenishment suggestion = new StockReplenishment();
            suggestion.setTenantId(tenantId);
            suggestion.setProductCode(stock.getProductCode());
            suggestion.setProductName(stock.getProductName());
            suggestion.setProductUnit(stock.getUnit());
            suggestion.setWarehouseId(stock.getWarehouseId());
            suggestion.setWarehouseName(stock.getWarehouseName());
            suggestion.setCurrentQty(stock.getQuantity());
            suggestion.setSafetyStock(minStock);
            suggestion.setShortageQty(shortageQty);
            suggestion.setAvgDailySales(BigDecimal.ZERO);
            suggestion.setDaysOfStock(BigDecimal.ZERO);
            suggestion.setLeadTime(7);
            suggestion.setSuggestedQty(suggestedQty);
            suggestion.setPriority(priority);
            suggestion.setReason("当前库存 " + stock.getQuantity() + "，低于安全库存 " + minStock + "，缺少 " + shortageQty);
            suggestion.setStatus("PENDING");
            suggestion.setCreateTime(LocalDateTime.now());
            suggestion.setCreateBy(StpUtil.getLoginIdAsLong());

            this.save(suggestion);
            suggestions.add(suggestion);
        }

        log.info("生成补货建议 {} 条", suggestions.size());
        return suggestions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockReplenishment createOrder(Long suggestionId, Long supplierId) {
        StockReplenishment suggestion = this.getById(suggestionId);
        if (suggestion == null) {
            throw BusinessException.notFound("补货建议不存在");
        }
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw BusinessException.badRequest("只有待处理的补货建议可以创建订单");
        }

        // 更新补货建议状态
        suggestion.setStatus("ORDERED");
        suggestion.setSupplierId(supplierId);
        suggestion.setUpdateTime(LocalDateTime.now());
        suggestion.setUpdateBy(StpUtil.getLoginIdAsLong());
        this.updateById(suggestion);

        // 通过事件发布解耦创建采购订单，采购模块监听后创建实际采购订单
        eventPublisher.publishEvent(new PurchaseOrderCreateEvent(
            this, suggestion.getId(), supplierId,
            suggestion.getProductCode(), suggestion.getProductName(), suggestion.getSuggestedQty(),
            suggestion.getWarehouseId(), suggestion.getWarehouseName()));

        // 生成临时订单号并记录到补货建议（后续由采购模块实际创建后更新）
        String tempOrderNo = "PO-" + System.currentTimeMillis();
        suggestion.setCreatedOrderNo(tempOrderNo);
        this.updateById(suggestion);

        log.info("补货建议 {} 已转为采购订单: 供应商ID={}, 产品编码={}, 产品名称={}, "
                + "建议数量={}, 仓库ID={}, 仓库名称={}, 临时订单号={}, "
                + "已发布采购订单创建事件，待采购模块监听处理",
            suggestionId, supplierId,
            suggestion.getProductCode(), suggestion.getProductName(), suggestion.getSuggestedQty(),
            suggestion.getWarehouseId(), suggestion.getWarehouseName(), tempOrderNo);
        return suggestion;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockReplenishment ignoreSuggestion(Long id, String reason) {
        StockReplenishment suggestion = this.getById(id);
        if (suggestion == null) {
            throw BusinessException.notFound("补货建议不存在");
        }
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw BusinessException.badRequest("只有待处理的补货建议可以忽略");
        }

        suggestion.setStatus("IGNORED");
        suggestion.setRemark(reason);
        suggestion.setUpdateTime(LocalDateTime.now());
        suggestion.setUpdateBy(StpUtil.getLoginIdAsLong());
        this.updateById(suggestion);

        log.info("补货建议 {} 已忽略，原因: {}", id, reason);
        return suggestion;
    }
}
