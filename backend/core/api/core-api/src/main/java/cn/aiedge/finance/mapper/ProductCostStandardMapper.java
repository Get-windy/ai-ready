package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.ProductCostStandard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ProductCostStandardMapper extends BaseMapper<ProductCostStandard> {

    @Select("SELECT COALESCE(SUM(material_cost),0) materialCost, COALESCE(SUM(labor_cost),0) laborCost, " +
            "COALESCE(SUM(overhead_cost),0) overheadCost, COALESCE(SUM(total_cost),0) totalCost " +
            "FROM erp_product_cost_standard WHERE deleted=0 AND tenant_id=#{tenantId}")
    Map<String, Object> selectStats(Long tenantId);
}
