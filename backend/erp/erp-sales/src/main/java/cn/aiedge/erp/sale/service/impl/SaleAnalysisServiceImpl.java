package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.CustomerActiveAnalysisDTO;
import cn.aiedge.erp.sale.dto.PromotionAnalysisDTO;
import cn.aiedge.erp.sale.mapper.SaleAnalysisMapper;
import cn.aiedge.erp.sale.service.SaleAnalysisService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleAnalysisServiceImpl implements SaleAnalysisService {

    private final SaleAnalysisMapper saleAnalysisMapper;

    @Override
    public Page<CustomerActiveAnalysisDTO> pageCustomerActive(long current, long size,
                                                              Integer days, Integer activeDays, Integer silentDays,
                                                              String keyword) {
        int d = days != null && days > 0 ? days : 30;
        int ad = activeDays != null && activeDays > 0 ? activeDays : 30;
        int sd = silentDays != null && silentDays > 0 ? silentDays : 90;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentSince = now.minusDays(d);
        LocalDateTime activeSince = now.minusDays(ad);
        LocalDateTime normalSince = now.minusDays(sd);

        Long tenantId = resolveTenantId();
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<CustomerActiveAnalysisDTO> page = new Page<>(current, size);
        return (Page<CustomerActiveAnalysisDTO>) saleAnalysisMapper.selectCustomerActivePage(
                page, tenantId, kw, recentSince, activeSince, normalSince);
    }

    @Override
    public PromotionAnalysisDTO promotionAnalysis(String startDate, String endDate) {
        LocalDateTime startTime = parseStart(startDate);
        LocalDateTime endTime = parseEnd(endDate);

        PromotionAnalysisDTO result = new PromotionAnalysisDTO();
        result.setCountByStatus(saleAnalysisMapper.selectPromotionCountByStatus());
        result.setCountByType(saleAnalysisMapper.selectPromotionCountByType());
        result.setMonthlyDistribution(saleAnalysisMapper.selectPromotionMonthlyDistribution(startTime, endTime));
        result.setActivities(saleAnalysisMapper.selectPromotionActivities(startTime, endTime));
        result.setDiscountOverview(saleAnalysisMapper.selectOrderDiscountOverview(startTime, endTime));
        return result;
    }

    /**
     * 从 Sa-Token Session 解析当前租户ID, 未登录/不可用时返回 null (与全局租户拦截器行为一致)
     */
    private Long resolveTenantId() {
        try {
            if (StpUtil.isLogin()) {
                Object tid = StpUtil.getSession().get("tenantId");
                if (tid instanceof Number) {
                    return ((Number) tid).longValue();
                }
                if (tid != null) {
                    return Long.parseLong(tid.toString());
                }
            }
        } catch (Exception e) {
            log.debug("解析租户ID失败, 不按租户过滤: {}", e.getMessage());
        }
        return null;
    }

    private LocalDateTime parseStart(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim()).atStartOfDay();
        } catch (Exception e) {
            log.warn("无效的开始日期格式: {}, 将跳过该过滤", dateStr);
            return null;
        }
    }

    private LocalDateTime parseEnd(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim()).atTime(23, 59, 59);
        } catch (Exception e) {
            log.warn("无效的结束日期格式: {}, 将跳过该过滤", dateStr);
            return null;
        }
    }
}
