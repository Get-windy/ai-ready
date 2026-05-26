package cn.aiedge.erp.sales.pricing.repository;

import cn.aiedge.erp.sales.pricing.entity.PriceRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 价格规则Repository接口
 */
@Repository
@Mapper
public interface PriceRuleRepository extends JpaRepository<PriceRule, Long>, 
                                             JpaSpecificationExecutor<PriceRule>,
                                             BaseMapper<PriceRule> {
    
    /**
     * 根据策略ID查询规则
     */
    List<PriceRule> findByStrategyId(Long strategyId);
    
    /**
     * 根据策略ID和状态查询规则
     */
    List<PriceRule> findByStrategyIdAndStatus(Long strategyId, String status);
    
    /**
     * 根据规则类型查询
     */
    List<PriceRule> findByRuleType(String ruleType);
    
    /**
     * 根据策略ID删除所有规则
     */
    void deleteByStrategyId(Long strategyId);
    
    /**
     * 根据策略ID查询激活的规则（按优先级排序）
     */
    List<PriceRule> findByStrategyIdAndStatusOrderByPriorityAsc(Long strategyId, String status);
}