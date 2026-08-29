package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.SmartReplenishVO;
import cn.aiedge.erp.stock.mapper.SmartReplenishMapper;
import cn.aiedge.erp.stock.service.SmartReplenishService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 智能补货查询ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartReplenishServiceImpl implements SmartReplenishService {

    private final SmartReplenishMapper smartReplenishMapper;

    @Override
    public IPage<SmartReplenishVO> page(String startDate, String endDate,
                                        Integer stockDays, Long warehouseId,
                                        String productKeyword, String supplierName,
                                        Long categoryId, BigDecimal minPlanQty,
                                        int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        int days = calcDays(startDate, endDate);
        int stockDaysVal = stockDays != null ? stockDays : 0;
        return smartReplenishMapper.selectSmartReplenishPage(new Page<>(pageNum, pageSize),
                tenantId, startDate, endDate, days, stockDaysVal,
                warehouseId, productKeyword, supplierName, categoryId, minPlanQty);
    }

    /**
     * 计算销售日期区间天数（含首尾）。区间非法或未提供时返回 0，用于日均销量分母保护。
     */
    private int calcDays(String startDate, String endDate) {
        if (startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
            return 0;
        }
        try {
            LocalDate s = LocalDate.parse(startDate);
            LocalDate e = LocalDate.parse(endDate);
            if (e.isBefore(s)) {
                return 0;
            }
            return (int) (ChronoUnit.DAYS.between(s, e) + 1);
        } catch (Exception ex) {
            return 0;
        }
    }
}
