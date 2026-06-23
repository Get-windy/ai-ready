package cn.aiedge.erp.pricing.mapper;

import cn.aiedge.erp.pricing.entity.PriceRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PriceRuleMapper extends BaseMapper<PriceRule> {

    List<PriceRule> selectByStrategyIds(@Param("strategyIds") List<Long> strategyIds);
}