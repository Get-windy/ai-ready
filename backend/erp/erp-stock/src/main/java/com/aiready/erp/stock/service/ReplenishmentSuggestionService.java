package com.aiready.erp.stock.service;

import com.aiready.erp.stock.entity.StockEntity;
import com.aiready.erp.stock.entity.ProductEntity;
import com.aiready.erp.purchase.entity.PurchaseOrderEntity;
import com.aiready.erp.purchase.entity.PurchaseOrderItemEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReplenishmentSuggestionService {

    private final StockService stockService;
    private final ProductService productService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;

    public List<ReplenishmentSuggestion> generateSuggestions(Long warehouseId) {
        List<StockEntity> stocks = stockService.listByWarehouse(warehouseId);
        List<ReplenishmentSuggestion> suggestions = new ArrayList<>();

        for (StockEntity stock : stocks) {
            ProductEntity product = productService.getById(stock.getProductId());
            if (product == null) continue;

            ReplenishmentSuggestion suggestion = analyzeStock(stock, product, warehouseId);
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        suggestions.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
        return suggestions;
    }

    private ReplenishmentSuggestion analyzeStock(StockEntity stock, ProductEntity product, Long warehouseId) {
        BigDecimal currentQty = stock.getQuantity();
        BigDecimal safetyStock = product.getSafetyStock() != null ? product.getSafetyStock() : BigDecimal.ZERO;
        BigDecimal minOrderQty = product.getMinOrderQty() != null ? product.getMinOrderQty() : BigDecimal.ONE;

        if (currentQty.compareTo(safetyStock) > 0) {
            return null;
        }

        BigDecimal shortageQty = safetyStock.subtract(currentQty);
        BigDecimal avgDailySales = calculateAvgDailySales(product.getId(), warehouseId, 30);
        BigDecimal daysOfStock = currentQty.divide(avgDailySales, 2, RoundingMode.HALF_UP);

        int priority = calculatePriority(daysOfStock, avgDailySales, product.getPriority());
        BigDecimal suggestedQty = calculateSuggestedQty(shortageQty, avgDailySales, minOrderQty, product.getLeadTime());

        ReplenishmentSuggestion suggestion = new ReplenishmentSuggestion();
        suggestion.setProductId(product.getId());
        suggestion.setProductCode(product.getCode());
        suggestion.setProductName(product.getName());
        suggestion.setWarehouseId(warehouseId);
        suggestion.setCurrentQty(currentQty);
        suggestion.setSafetyStock(safetyStock);
        suggestion.setShortageQty(shortageQty);
        suggestion.setAvgDailySales(avgDailySales);
        suggestion.setDaysOfStock(daysOfStock.intValue());
        suggestion.setSuggestedQty(suggestedQty);
        suggestion.setPriority(priority);
        suggestion.setLeadTime(product.getLeadTime() != null ? product.getLeadTime() : 7);
        suggestion.setEstimatedArrival(calculateEstimatedArrival(product.getLeadTime()));
        suggestion.setReason(generateReason(daysOfStock, avgDailySales, shortageQty));
        suggestion.setCreateTime(LocalDateTime.now());

        return suggestion;
    }

    private BigDecimal calculateAvgDailySales(Long productId, Long warehouseId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        BigDecimal totalSales = salesOrderService.sumProductSales(productId, warehouseId, startDate, endDate);
        return totalSales.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
    }

    private int calculatePriority(BigDecimal daysOfStock, BigDecimal avgDailySales, Integer productPriority) {
        int score = 0;

        if (daysOfStock.compareTo(BigDecimal.ZERO) <= 0) {
            score += 100;
        } else if (daysOfStock.compareTo(BigDecimal.valueOf(3)) < 0) {
            score += 80;
        } else if (daysOfStock.compareTo(BigDecimal.valueOf(7)) < 0) {
            score += 60;
        } else if (daysOfStock.compareTo(BigDecimal.valueOf(14)) < 0) {
            score += 40;
        } else {
            score += 20;
        }

        if (avgDailySales.compareTo(BigDecimal.valueOf(10)) > 0) {
            score += 30;
        } else if (avgDailySales.compareTo(BigDecimal.valueOf(5)) > 0) {
            score += 20;
        } else if (avgDailySales.compareTo(BigDecimal.valueOf(1)) > 0) {
            score += 10;
        }

        if (productPriority != null) {
            score += productPriority * 5;
        }

        return Math.min(score, 100);
    }

    private BigDecimal calculateSuggestedQty(BigDecimal shortageQty, BigDecimal avgDailySales, BigDecimal minOrderQty, Integer leadTime) {
        int leadDays = leadTime != null ? leadTime : 7;
        BigDecimal leadTimeDemand = avgDailySales.multiply(BigDecimal.valueOf(leadDays));
        BigDecimal totalNeed = shortageQty.add(leadTimeDemand);

        BigDecimal safetyBuffer = avgDailySales.multiply(BigDecimal.valueOf(3));
        totalNeed = totalNeed.add(safetyBuffer);

        BigDecimal suggestedQty = totalNeed.divide(minOrderQty, 0, RoundingMode.UP).multiply(minOrderQty);

        BigDecimal maxOrderQty = minOrderQty.multiply(BigDecimal.valueOf(10));
        if (suggestedQty.compareTo(maxOrderQty) > 0) {
            suggestedQty = maxOrderQty;
        }

        return suggestedQty;
    }

    private LocalDateTime calculateEstimatedArrival(Integer leadTime) {
        int days = leadTime != null ? leadTime : 7;
        return LocalDateTime.now().plusDays(days + 2);
    }

    private String generateReason(BigDecimal daysOfStock, BigDecimal avgDailySales, BigDecimal shortageQty) {
        StringBuilder reason = new StringBuilder();

        if (daysOfStock.compareTo(BigDecimal.ZERO) <= 0) {
            reason.append("库存已耗尽，急需补货");
        } else if (daysOfStock.compareTo(BigDecimal.valueOf(3)) < 0) {
            reason.append("库存即将耗尽，建议立即补货");
        } else if (daysOfStock.compareTo(BigDecimal.valueOf(7)) < 0) {
            reason.append("库存低于安全库存，建议尽快补货");
        } else {
            reason.append("库存接近安全库存线，建议安排补货");
        }

        if (avgDailySales.compareTo(BigDecimal.valueOf(10)) > 0) {
            reason.append("；该产品日均销量较高(").append(avgDailySales).append("件/天)");
        }

        reason.append("；缺口").append(shortageQty).append("件");

        return reason.toString();
    }

    public ReplenishmentReport generateReport(Long warehouseId) {
        List<ReplenishmentSuggestion> suggestions = generateSuggestions(warehouseId);

        ReplenishmentReport report = new ReplenishmentReport();
        report.setWarehouseId(warehouseId);
        report.setTotalSuggestions(suggestions.size());
        report.setHighPriorityCount(suggestions.stream().filter(s -> s.getPriority() >= 80).count());
        report.setMediumPriorityCount(suggestions.stream().filter(s -> s.getPriority() >= 50 && s.getPriority() < 80).count());
        report.setLowPriorityCount(suggestions.stream().filter(s -> s.getPriority() < 50).count());
        report.setTotalShortageQty(suggestions.stream().map(ReplenishmentSuggestion::getShortageQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setTotalSuggestedQty(suggestions.stream().map(ReplenishmentSuggestion::getSuggestedQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setEstimatedCost(calculateEstimatedCost(suggestions));
        report.setCreateTime(LocalDateTime.now());

        return report;
    }

    private BigDecimal calculateEstimatedCost(List<ReplenishmentSuggestion> suggestions) {
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ReplenishmentSuggestion suggestion : suggestions) {
            ProductEntity product = productService.getById(suggestion.getProductId());
            if (product != null && product.getCostPrice() != null) {
                BigDecimal itemCost = product.getCostPrice().multiply(suggestion.getSuggestedQty());
                totalCost = totalCost.add(itemCost);
            }
        }
        return totalCost;
    }

    public PurchaseOrderEntity createPurchaseOrderFromSuggestions(List<Long> suggestionIds, Long supplierId) {
        List<ReplenishmentSuggestion> suggestions = suggestionIds.stream()
            .map(this::getSuggestionById)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            return null;
        }

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setSupplierId(supplierId);
        order.setStatus("draft");
        order.setCreateTime(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderItemEntity> items = new ArrayList<>();

        for (ReplenishmentSuggestion suggestion : suggestions) {
            ProductEntity product = productService.getById(suggestion.getProductId());
            if (product == null) continue;

            PurchaseOrderItemEntity item = new PurchaseOrderItemEntity();
            item.setProductId(product.getId());
            item.setQuantity(suggestion.getSuggestedQty());
            item.setUnitPrice(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            item.setAmount(item.getUnitPrice().multiply(item.getQuantity()));
            items.add(item);
            totalAmount = totalAmount.add(item.getAmount());
        }

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        return purchaseOrderService.save(order);
    }

    public void markSuggestionAsProcessed(Long suggestionId, Long purchaseOrderId) {
        ReplenishmentSuggestion suggestion = getSuggestionById(suggestionId);
        if (suggestion != null) {
            suggestion.setStatus("processed");
            suggestion.setPurchaseOrderId(purchaseOrderId);
            suggestion.setProcessTime(LocalDateTime.now());
            updateSuggestion(suggestion);
        }
    }

    public void ignoreSuggestion(Long suggestionId, String reason) {
        ReplenishmentSuggestion suggestion = getSuggestionById(suggestionId);
        if (suggestion != null) {
            suggestion.setStatus("ignored");
            suggestion.setIgnoreReason(reason);
            suggestion.setProcessTime(LocalDateTime.now());
            updateSuggestion(suggestion);
        }
    }

    public List<ReplenishmentSuggestion> getSuggestionsByStatus(String status) {
        return suggestionMapper.selectList(
            new LambdaQueryWrapper<ReplenishmentSuggestion>()
                .eq(ReplenishmentSuggestion::getStatus, status)
                .orderByDesc(ReplenishmentSuggestion::getPriority)
        );
    }

    public ReplenishmentSuggestion getSuggestionById(Long id) {
        return suggestionMapper.selectById(id);
    }

    public void updateSuggestion(ReplenishmentSuggestion suggestion) {
        suggestionMapper.updateById(suggestion);
    }
}