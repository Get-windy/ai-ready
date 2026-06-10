package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeStatisticsQueryRequest;
import cn.aiedge.erp.expense.dto.FeeStatisticsVO;
import cn.aiedge.erp.expense.entity.FeeApplication;
import cn.aiedge.erp.expense.entity.FeeReimbursement;
import cn.aiedge.erp.expense.entity.FeeStatistics;
import cn.aiedge.erp.expense.mapper.FeeApplicationMapper;
import cn.aiedge.erp.expense.mapper.FeeReimbursementMapper;
import cn.aiedge.erp.expense.mapper.FeeStatisticsMapper;
import cn.aiedge.erp.expense.service.FeeStatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 费用统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeStatisticsServiceImpl implements FeeStatisticsService {

    private final FeeStatisticsMapper statisticsMapper;
    private final FeeApplicationMapper applicationMapper;
    private final FeeReimbursementMapper reimbursementMapper;

    @Override
    public PageResult<FeeStatisticsVO> pageList(FeeStatisticsQueryRequest request) {
        LambdaQueryWrapper<FeeStatistics> wrapper = new LambdaQueryWrapper<>();

        if (request.getStatYear() != null) {
            wrapper.eq(FeeStatistics::getStatYear, request.getStatYear());
        }
        if (request.getStatMonth() != null) {
            wrapper.eq(FeeStatistics::getStatMonth, request.getStatMonth());
        }
        if (request.getDepartmentId() != null) {
            wrapper.eq(FeeStatistics::getDepartmentId, request.getDepartmentId());
        }
        if (request.getExpenseType() != null) {
            wrapper.eq(FeeStatistics::getExpenseType, request.getExpenseType());
        }

        wrapper.orderByDesc(FeeStatistics::getStatYear).orderByDesc(FeeStatistics::getStatMonth);

        Page<FeeStatistics> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FeeStatistics> result = statisticsMapper.selectPage(page, wrapper);

        List<FeeStatisticsVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public Map<String, Object> getSummaryData(Integer year, Integer month, Long departmentId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<FeeApplication> appWrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            appWrapper.eq(FeeApplication::getDepartmentId, departmentId);
        }
        if (year != null) {
            appWrapper.apply("EXTRACT(YEAR FROM apply_date) = {0}", year);
        }
        if (month != null) {
            appWrapper.apply("EXTRACT(MONTH FROM apply_date) = {0}", month);
        }
        List<FeeApplication> allApps = applicationMapper.selectList(appWrapper);

        // 统计申请单数量/金额
        long totalApplyCount = allApps.size();
        BigDecimal totalApplyAmount = allApps.stream()
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long approvedCount = allApps.stream().filter(a -> "APPROVED".equals(a.getStatus())).count();
        BigDecimal approvedAmount = allApps.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long rejectedCount = allApps.stream().filter(a -> "REJECTED".equals(a.getStatus())).count();
        BigDecimal rejectedAmount = allApps.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("totalApplyCount", totalApplyCount);
        result.put("totalApplyAmount", totalApplyAmount);
        result.put("approvedCount", approvedCount);
        result.put("approvedAmount", approvedAmount);
        result.put("rejectedCount", rejectedCount);
        result.put("rejectedAmount", rejectedAmount);

        // 报销统计
        LambdaQueryWrapper<FeeReimbursement> reimbWrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            reimbWrapper.eq(FeeReimbursement::getDepartmentId, departmentId);
        }
        if (year != null) {
            reimbWrapper.apply("EXTRACT(YEAR FROM reimbursement_date) = {0}", year);
        }
        if (month != null) {
            reimbWrapper.apply("EXTRACT(MONTH FROM reimbursement_date) = {0}", month);
        }
        List<FeeReimbursement> allReimbs = reimbursementMapper.selectList(reimbWrapper);

        long totalReimbCount = allReimbs.size();
        BigDecimal totalReimbAmount = allReimbs.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = allReimbs.stream().filter(r -> "COMPLETED".equals(r.getPaymentStatus())).count();
        BigDecimal paidAmount = allReimbs.stream()
                .filter(r -> "COMPLETED".equals(r.getPaymentStatus()))
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("totalReimbCount", totalReimbCount);
        result.put("totalReimbAmount", totalReimbAmount);
        result.put("paidCount", paidCount);
        result.put("paidAmount", paidAmount);

        return result;
    }

    @Override
    public List<FeeStatisticsVO> getByDepartment(Integer year, Integer month) {
        LambdaQueryWrapper<FeeStatistics> wrapper = new LambdaQueryWrapper<>();
        if (year != null) wrapper.eq(FeeStatistics::getStatYear, year);
        if (month != null) wrapper.eq(FeeStatistics::getStatMonth, month);
        wrapper.isNotNull(FeeStatistics::getDepartmentId)
               .orderByDesc(FeeStatistics::getApplyAmount);

        return statisticsMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeeStatisticsVO> getByExpenseType(Integer year, Integer month, Long departmentId) {
        LambdaQueryWrapper<FeeStatistics> wrapper = new LambdaQueryWrapper<>();
        if (year != null) wrapper.eq(FeeStatistics::getStatYear, year);
        if (month != null) wrapper.eq(FeeStatistics::getStatMonth, month);
        if (departmentId != null) wrapper.eq(FeeStatistics::getDepartmentId, departmentId);
        wrapper.isNotNull(FeeStatistics::getExpenseType)
               .orderByDesc(FeeStatistics::getApplyAmount);

        return statisticsMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshStatistics(Integer year, Integer month) {
        // 删除旧统计
        LambdaQueryWrapper<FeeStatistics> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FeeStatistics::getStatYear, year)
                     .eq(FeeStatistics::getStatMonth, month);
        statisticsMapper.delete(deleteWrapper);

        // 按费用类型统计
        LambdaQueryWrapper<FeeApplication> appQuery = new LambdaQueryWrapper<>();
        if (year != null) {
            appQuery.apply("EXTRACT(YEAR FROM apply_date) = {0}", year);
        }
        if (month != null) {
            appQuery.apply("EXTRACT(MONTH FROM apply_date) = {0}", month);
        }
        Map<String, List<FeeApplication>> byType = applicationMapper.selectList(appQuery).stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(FeeApplication::getExpenseType));

        for (Map.Entry<String, List<FeeApplication>> entry : byType.entrySet()) {
            FeeStatistics stat = buildStatistics(year, month, null, null, entry.getKey(), entry.getValue());
            if (stat != null) {
                statisticsMapper.insert(stat);
            }
        }

        log.info("刷新费用统计完成: year={}, month={}", year, month);
    }

    private FeeStatistics buildStatistics(Integer year, Integer month, Long deptId, String deptName,
                                          String expenseType, List<FeeApplication> apps) {
        if (apps == null || apps.isEmpty()) return null;

        FeeStatistics stat = new FeeStatistics();
        stat.setStatYear(year);
        stat.setStatMonth(month);
        stat.setStatDate(LocalDate.of(year, month, 1));
        stat.setDepartmentId(deptId);
        stat.setDepartmentName(deptName);
        stat.setExpenseType(expenseType);
        stat.setCreateBy(SecurityUtils.getCurrentUserId());

        stat.setApplyCount(apps.size());
        stat.setApplyAmount(apps.stream()
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stat.setApprovedCount((int) apps.stream().filter(a -> "APPROVED".equals(a.getStatus())).count());
        stat.setApprovedAmount(apps.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stat.setRejectedCount((int) apps.stream().filter(a -> "REJECTED".equals(a.getStatus())).count());
        stat.setRejectedAmount(apps.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return stat;
    }

    private FeeStatisticsVO convertToVO(FeeStatistics entity) {
        FeeStatisticsVO vo = new FeeStatisticsVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
