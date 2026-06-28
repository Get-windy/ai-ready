package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.ProfitAnalysis;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ProfitAnalysisMapper extends BaseMapper<ProfitAnalysis> {

    @Select("SELECT COALESCE(SUM(sale_amount),0) totalRevenue, COALESCE(SUM(gross_profit),0) totalProfit " +
            "FROM erp_profit_analysis WHERE deleted=0 AND tenant_id=#{tenantId}")
    Map<String, Object> selectStats(Long tenantId);
}
