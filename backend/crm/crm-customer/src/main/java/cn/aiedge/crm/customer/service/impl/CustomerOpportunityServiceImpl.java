package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.customer.entity.CustomerOpportunity;
import cn.aiedge.crm.customer.mapper.CustomerOpportunityMapper;
import cn.aiedge.crm.customer.service.CustomerOpportunityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOpportunityServiceImpl extends ServiceImpl<CustomerOpportunityMapper, CustomerOpportunity> implements CustomerOpportunityService {

    @Override
    public CustomerOpportunity getByOpportunityCode(String opportunityCode) {
        LambdaQueryWrapper<CustomerOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerOpportunity::getOpportunityCode, opportunityCode);
        wrapper.eq(CustomerOpportunity::getDeleted, 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Page<CustomerOpportunity> pageList(String keyword, Long customerId, Integer opportunityStage,
                                                Integer status, Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<CustomerOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerOpportunity::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(CustomerOpportunity::getOpportunityName, keyword)
                    .or().like(CustomerOpportunity::getCustomerName, keyword));
        }
        
        if (customerId != null) {
            wrapper.eq(CustomerOpportunity::getCustomerId, customerId);
        }
        
        if (opportunityStage != null) {
            wrapper.eq(CustomerOpportunity::getOpportunityStage, opportunityStage);
        }
        
        if (status != null) {
            wrapper.eq(CustomerOpportunity::getStatus, status);
        }
        
        if (salesPersonId != null) {
            wrapper.eq(CustomerOpportunity::getSalesPersonId, salesPersonId);
        }
        
        wrapper.orderByDesc(CustomerOpportunity::getCreatedAt);
        
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<CustomerOpportunity> listByCustomerId(Long customerId) {
        LambdaQueryWrapper<CustomerOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerOpportunity::getDeleted, 0);
        wrapper.eq(CustomerOpportunity::getCustomerId, customerId);
        wrapper.orderByDesc(CustomerOpportunity::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<CustomerOpportunity> listBySalesPersonId(Long salesPersonId) {
        LambdaQueryWrapper<CustomerOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerOpportunity::getDeleted, 0);
        wrapper.eq(CustomerOpportunity::getSalesPersonId, salesPersonId);
        wrapper.orderByDesc(CustomerOpportunity::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public CustomerOpportunity advanceStage(Long opportunityId) {
        CustomerOpportunity opportunity = baseMapper.selectById(opportunityId);
        if (opportunity == null || opportunity.getDeleted() == 1) {
            throw new RuntimeException("商机不存在: " + opportunityId);
        }
        
        int currentStage = opportunity.getOpportunityStage();
        if (currentStage >= 5) {
            throw new RuntimeException("商机已处于最终阶段");
        }
        
        int nextStage = currentStage + 1;
        opportunity.setOpportunityStage(nextStage);
        opportunity.setOpportunityStageDesc(getStageDesc(nextStage));
        
        int probability = getProbabilityByStage(nextStage);
        opportunity.setProbability(probability);
        
        baseMapper.updateById(opportunity);
        log.info("商机阶段推进: {} -> {}", opportunity.getOpportunityCode(), nextStage);
        return opportunity;
    }

    @Override
    @Transactional
    public CustomerOpportunity winOpportunity(Long opportunityId, BigDecimal actualAmount) {
        CustomerOpportunity opportunity = baseMapper.selectById(opportunityId);
        if (opportunity == null || opportunity.getDeleted() == 1) {
            throw new RuntimeException("商机不存在: " + opportunityId);
        }
        
        opportunity.setOpportunityStage(5);
        opportunity.setOpportunityStageDesc("成交");
        opportunity.setStatus(2);
        opportunity.setStatusDesc("赢单");
        opportunity.setActualAmount(actualAmount);
        opportunity.setActualCloseDate(LocalDate.now());
        opportunity.setProbability(100);
        
        baseMapper.updateById(opportunity);
        log.info("商机赢单: {}, 金额: {}", opportunity.getOpportunityCode(), actualAmount);
        return opportunity;
    }

    @Override
    @Transactional
    public CustomerOpportunity loseOpportunity(Long opportunityId, String loseReason) {
        CustomerOpportunity opportunity = baseMapper.selectById(opportunityId);
        if (opportunity == null || opportunity.getDeleted() == 1) {
            throw new RuntimeException("商机不存在: " + opportunityId);
        }
        
        opportunity.setStatus(3);
        opportunity.setStatusDesc("输单");
        opportunity.setLoseReason(loseReason);
        opportunity.setActualCloseDate(LocalDate.now());
        opportunity.setProbability(0);
        
        baseMapper.updateById(opportunity);
        log.info("商机输单: {}, 原因: {}", opportunity.getOpportunityCode(), loseReason);
        return opportunity;
    }

    @Override
    public Map<String, Object> getOpportunityStatistics(Long salesPersonId) {
        LambdaQueryWrapper<CustomerOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerOpportunity::getDeleted, 0);
        if (salesPersonId != null) {
            wrapper.eq(CustomerOpportunity::getSalesPersonId, salesPersonId);
        }
        
        List<CustomerOpportunity> opportunities = baseMapper.selectList(wrapper);
        
        BigDecimal totalEstimated = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        int totalCount = opportunities.size();
        int winCount = 0;
        int loseCount = 0;
        int activeCount = 0;
        
        for (CustomerOpportunity opp : opportunities) {
            totalEstimated = totalEstimated.add(opp.getEstimatedAmount() != null ? opp.getEstimatedAmount() : BigDecimal.ZERO);
            if (opp.getActualAmount() != null) {
                totalActual = totalActual.add(opp.getActualAmount());
            }
            
            if (opp.getStatus() == 2) {
                winCount++;
            } else if (opp.getStatus() == 3) {
                loseCount++;
            } else {
                activeCount++;
            }
        }
        
        BigDecimal winRate = totalCount > 0 
                ? BigDecimal.valueOf(winCount * 100).divide(BigDecimal.valueOf(winCount + loseCount), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("winCount", winCount);
        statistics.put("loseCount", loseCount);
        statistics.put("activeCount", activeCount);
        statistics.put("totalEstimated", totalEstimated);
        statistics.put("totalActual", totalActual);
        statistics.put("winRate", winRate);
        
        return statistics;
    }

    @Override
    public String generateOpportunityCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "OPP-" + dateStr + String.format("%04d", count + 1);
    }

    private String getStageDesc(int stage) {
        switch (stage) {
            case 1: return "初步接触";
            case 2: return "需求确认";
            case 3: return "方案报价";
            case 4: return "商务谈判";
            case 5: return "成交";
            default: return "未知";
        }
    }

    private int getProbabilityByStage(int stage) {
        switch (stage) {
            case 1: return 10;
            case 2: return 30;
            case 3: return 50;
            case 4: return 70;
            case 5: return 100;
            default: return 0;
        }
    }
}