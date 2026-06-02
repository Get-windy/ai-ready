package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetCategory;
import cn.aiedge.erp.fixedasset.model.FixedAssetDepreciation;
import cn.aiedge.erp.fixedasset.repository.FixedAssetCategoryRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetDepreciationRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 固定资产报表服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetReportServiceImpl implements FixedAssetReportService {

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetCategoryRepository categoryRepository;
    private final FixedAssetDepreciationRepository depreciationRepository;

    @Override
    public List<Map<String, Object>> getDepreciationSummary(String year) {
        String yearStr = (year != null) ? year : String.valueOf(LocalDate.now().getYear());
        List<FixedAssetDepreciation> records = depreciationRepository.findAll();

        // Group by period (month)
        List<FixedAsset> allAssets = fixedAssetRepository.findAll();
        BigDecimal totalOriginalValue = allAssets.stream()
            .map(a -> a.getOriginalValue() != null ? a.getOriginalValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulatedDepreciation = allAssets.stream()
            .map(a -> a.getAccumulatedDepreciation() != null ? a.getAccumulatedDepreciation() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetValue = allAssets.stream()
            .map(a -> a.getNetValue() != null ? a.getNetValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("year", yearStr);
        summary.put("totalOriginalValue", totalOriginalValue);
        summary.put("totalAccumulatedDepreciation", totalAccumulatedDepreciation);
        summary.put("totalNetValue", totalNetValue);
        summary.put("assetCount", allAssets.size());

        // Monthly breakdown
        Map<String, List<FixedAssetDepreciation>> byPeriod = records.stream()
            .filter(r -> r.getPeriod() != null && r.getPeriod().startsWith(yearStr))
            .collect(Collectors.groupingBy(FixedAssetDepreciation::getPeriod));

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            String monthKey = yearStr + "-" + String.format("%02d", i);
            List<FixedAssetDepreciation> monthRecords = byPeriod.getOrDefault(monthKey, Collections.emptyList());
            Map<String, Object> monthMap = new LinkedHashMap<>();
            monthMap.put("period", monthKey);
            monthMap.put("count", monthRecords.size());
            monthMap.put("totalAmount", monthRecords.stream()
                .map(r -> r.getPeriodAmount() != null ? r.getPeriodAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthlyData.add(monthMap);
        }
        summary.put("monthlyData", monthlyData);

        return Collections.singletonList(summary);
    }

    @Override
    public List<Map<String, Object>> getAssetLedger(String assetCode, String departmentId) {
        List<FixedAsset> assets;
        if (assetCode != null && !assetCode.isBlank()) {
            assets = fixedAssetRepository.findByAssetCode(assetCode).stream().collect(Collectors.toList());
        } else if (departmentId != null && !departmentId.isBlank()) {
            assets = fixedAssetRepository.findByDepartmentId(departmentId);
        } else {
            assets = fixedAssetRepository.findAll();
        }

        return assets.stream().map(a -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("assetCode", a.getAssetCode());
            entry.put("assetName", a.getAssetName());
            entry.put("categoryName", a.getCategoryName());
            entry.put("purchaseDate", a.getPurchaseDate());
            entry.put("originalValue", a.getOriginalValue());
            entry.put("accumulatedDepreciation", a.getAccumulatedDepreciation());
            entry.put("netValue", a.getNetValue());
            entry.put("monthlyDepreciation", a.getMonthlyDepreciation());
            entry.put("status", a.getStatus());
            entry.put("departmentName", a.getDepartmentName());
            entry.put("custodianName", a.getCustodianName());
            entry.put("location", a.getLocation());
            return entry;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAgeAnalysis() {
        List<FixedAsset> assets = fixedAssetRepository.findAll();
        LocalDate now = LocalDate.now();

        List<Map<String, Object>> ageGroups = new ArrayList<>();

        // < 1 year
        Map<String, Object> group1 = buildAgeGroup(assets, now, 0, 1, "1年以内");
        ageGroups.add(group1);

        // 1-3 years
        Map<String, Object> group2 = buildAgeGroup(assets, now, 1, 3, "1-3年");
        ageGroups.add(group2);

        // 3-5 years
        Map<String, Object> group3 = buildAgeGroup(assets, now, 3, 5, "3-5年");
        ageGroups.add(group3);

        // 5-10 years
        Map<String, Object> group4 = buildAgeGroup(assets, now, 5, 10, "5-10年");
        ageGroups.add(group4);

        // > 10 years
        Map<String, Object> group5 = buildAgeGroup(assets, now, 10, Integer.MAX_VALUE, "10年以上");
        ageGroups.add(group5);

        return ageGroups;
    }

    private Map<String, Object> buildAgeGroup(List<FixedAsset> assets, LocalDate now,
                                               int minYears, int maxYears, String label) {
        List<FixedAsset> filtered = assets.stream()
            .filter(a -> a.getPurchaseDate() != null)
            .filter(a -> {
                long years = java.time.temporal.ChronoUnit.YEARS.between(a.getPurchaseDate(), now);
                return years >= minYears && years < maxYears;
            })
            .collect(Collectors.toList());

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("label", label);
        group.put("count", filtered.size());
        group.put("originalValue", filtered.stream()
            .map(a -> a.getOriginalValue() != null ? a.getOriginalValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        group.put("netValue", filtered.stream()
            .map(a -> a.getNetValue() != null ? a.getNetValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        return group;
    }

    @Override
    public List<Map<String, Object>> getCategorySummary() {
        List<FixedAsset> assets = fixedAssetRepository.findAll();
        List<FixedAssetCategory> categories = categoryRepository.findAll();

        Map<Long, String> categoryMap = categories.stream()
            .collect(Collectors.toMap(FixedAssetCategory::getId, FixedAssetCategory::getCategoryName));

        Map<Long, List<FixedAsset>> byCategory = assets.stream()
            .filter(a -> a.getCategoryId() != null)
            .collect(Collectors.groupingBy(FixedAsset::getCategoryId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<FixedAsset>> entry : byCategory.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", entry.getKey());
            item.put("categoryName", categoryMap.getOrDefault(entry.getKey(), "未知分类"));
            item.put("count", entry.getValue().size());
            item.put("originalValue", entry.getValue().stream()
                .map(a -> a.getOriginalValue() != null ? a.getOriginalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("netValue", entry.getValue().stream()
                .map(a -> a.getNetValue() != null ? a.getNetValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }

        return result;
    }
}
