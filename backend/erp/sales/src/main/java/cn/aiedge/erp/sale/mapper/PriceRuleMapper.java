package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.PriceRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 价格规则Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PriceRuleMapper extends BaseMapper<PriceRule> {

    /**
     * 根据策略ID查询规则列表
     *
     * @param strategyId 策略ID
     * @return 规则列表
     */
    @Select("SELECT * FROM erp_pricing_rule " +
            "WHERE deleted = 0 AND strategy_id = #{strategyId} " +
            "AND status = 'active' " +
            "ORDER BY sort_order ASC")
    List<PriceRule> selectByStrategyId(@Param("strategyId") Long strategyId);

    /**
     * 批量根据策略ID查询规则
     */
    List<PriceRule> selectByStrategyIds(@Param("strategyIds") List<Long> strategyIds);
}
