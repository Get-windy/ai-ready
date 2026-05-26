package cn.aiedge.statistics.service.impl;

import cn.aiedge.report.dto.*;
import cn.aiedge.report.service.IStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 统计分析服务实现
 * 注：实际实现需要连接数据库查询，这里提供基础框架
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements IStatisticsService {

    @Override
    public CustomerStatsDTO getCustomerStats(StatsQueryRequest request) {
        CustomerStatsDTO dto = new CustomerStatsDTO();
        dto.setTenantId(request.getTenantId());
        // 实际应从数据库查询
        dto.setTotalCustomers(1000);
        dto.setNewCustomers(50);
        dto.setActiveCustomers(800);
        dto.setLostCustomers(30);
        dto.setVipCustomers(100);

        // 阶段分布
        List<CustomerStatsDTO.StageDistribution> stages = new ArrayList<>();
        String[] stageNames = {"潜在", "意向", "成交", "流失"};
        int[] counts = {300, 400, 200, 100};
        for (int i = 0; i < stageNames.length; i++) {
            CustomerStatsDTO.StageDistribution sd = new CustomerStatsDTO.StageDistribution();
            sd.setStage(i + 1);
            sd.setStageName(stageNames[i]);
            sd.setCount(counts[i]);
            sd.setPercentage(BigDecimal.valueOf(counts[i] * 100.0 / 1000).setScale(2, RoundingMode.HALF_UP));
            stages.add(sd);
        }
        dto.setStageDistribution(stages);
        return dto;
    }

    @Override
    public OrderStatsDTO getOrderStats(StatsQueryRequest request) {
        OrderStatsDTO dto = new OrderStatsDTO();
        dto.setTenantId(request.getTenantId());
        // 实际应从数据库查询
        dto.setTotalOrders(500);
        dto.setCompletedOrders(350);
        dto.setPendingOrders(100);
        dto.setCancelledOrders(50);
        dto.setTotalAmount(BigDecimal.valueOf(1000000));
        dto.setActualAmount(BigDecimal.valueOf(950000));
        dto.setReceivedAmount(BigDecimal.valueOf(800000));
        dto.setUnReceivedAmount(BigDecimal.valueOf(150000));

        // 生成日趋势
        List<OrderStatsDTO.DailyStats> dailyStats = new ArrayList<>();
        LocalDate start = request.getStartDate() != null ? request.getStartDate().toLocalDate() : LocalDate.now().minusDays(7);
        LocalDate end = request.getEndDate() != null ? request.getEndDate().toLocalDate() : LocalDate.now();
        Random random = new Random();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            OrderStatsDTO.DailyStats ds = new OrderStatsDTO.DailyStats();
            ds.setDate(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            ds.setOrderCount(random.nextInt(20) + 1);
            ds.setAmount(BigDecimal.valueOf(random.nextInt(50000) + 5000));
            dailyStats.add(ds);
        }
        dto.setDailyStats(dailyStats);
        return dto;
    }

    @Override
    public SalesStatsDTO getSalesStats(StatsQueryRequest request) {
        SalesStatsDTO dto = new SalesStatsDTO();
        dto.setTenantId(request.getTenantId());
        // 实际应从数据库查询
        dto.setTotalSales(BigDecimal.valueOf(950000));
        dto.setTargetSales(BigDecimal.valueOf(1000000));
        dto.setAchievementRate(BigDecimal.valueOf(0.95));
        dto.setTotalDeals(350);
        dto.setAvgDealAmount(BigDecimal.valueOf(2714.29));

        // 销售员业绩
        List<SalesStatsDTO.SalesByOwner> salesByOwner = new ArrayList<>();
        String[] names = {"张三", "李四", "王五"};
        BigDecimal[] sales = {BigDecimal.valueOf(350000), BigDecimal.valueOf(320000), BigDecimal.valueOf(280000)};
        for (int i = 0; i < names.length; i++) {
            SalesStatsDTO.SalesByOwner so = new SalesStatsDTO.SalesByOwner();
            so.setOwnerId((long) (i + 1));
            so.setOwnerName(names[i]);
            so.setSales(sales[i]);
            so.setDealCount(100 + i * 20);
            salesByOwner.add(so);
        }
        dto.setSalesByOwner(salesByOwner);

        // 热销产品
        List<SalesStatsDTO.ProductSales> topProducts = new ArrayList<>();
        String[] products = {"产品A", "产品B", "产品C"};
        for (int i = 0; i < products.length; i++) {
            SalesStatsDTO.ProductSales ps = new SalesStatsDTO.ProductSales();
            ps.setProductId((long) (i + 1));
            ps.setProductName(products[i]);
            ps.setQuantity(100 * (i + 1));
            ps.setAmount(BigDecimal.valueOf(50000 * (i + 1)));
            topProducts.add(ps);
        }
        dto.setTopProducts(topProducts);
        return dto;
    }

    @Override
    public CustomerStatsDTO getCustomerTrend(StatsQueryRequest request) {
        return getCustomerStats(request);
    }

    @Override
    public OrderStatsDTO getOrderTrend(StatsQueryRequest request) {
        return getOrderStats(request);
    }
}
