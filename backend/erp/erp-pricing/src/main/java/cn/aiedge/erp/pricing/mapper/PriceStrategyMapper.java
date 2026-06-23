package cn.aiedge.erp.pricing.mapper;

import cn.aiedge.erp.pricing.entity.PriceStrategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PriceStrategyMapper extends BaseMapper<PriceStrategy> {

    List<PriceStrategy> selectPageList(@Param("tenantId") Long tenantId,
                                       @Param("name") String name,
                                       @Param("status") String status,
                                       @Param("strategyType") String strategyType);
}