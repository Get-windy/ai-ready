package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.PriceStrategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 价格策略Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PriceStrategyMapper extends BaseMapper<PriceStrategy> {

    /**
     * 根据条件查询价格策略列表
     *
     * @param tenantId 租户ID
     * @param status 状态
     * @param strategyType 策略类型
     * @param customerLevel 客户等级
     * @return 策略列表
     */
    @Select("<script>" +
            "SELECT * FROM erp_pricing_strategy " +
            "WHERE deleted = 0 " +
            "<if test='tenantId != null'> AND tenant_id = #{tenantId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='strategyType != null'> AND strategy_type = #{strategyType} </if>" +
            "<if test='customerLevel != null'> AND customer_level = #{customerLevel} </if>" +
            "ORDER BY priority ASC, create_time DESC" +
            "</script>")
    List<PriceStrategy> selectByConditions(@Param("tenantId") Long tenantId,
                                           @Param("status") String status,
                                           @Param("strategyType") String strategyType,
                                           @Param("customerLevel") String customerLevel);

    /**
     * 分页查询价格策略
     */
    Page<PriceStrategy> selectPageList(Page<PriceStrategy> page,
                                       @Param("tenantId") Long tenantId,
                                       @Param("name") String name,
                                       @Param("status") String status,
                                       @Param("strategyType") String strategyType);

    /**
     * 查询生效中的策略（按优先级排序）
     */
    @Select("SELECT * FROM erp_pricing_strategy " +
            "WHERE deleted = 0 AND status = 'active' " +
            "AND (effective_start_time IS NULL OR effective_start_time &lt;= NOW()) " +
            "AND (effective_end_time IS NULL OR effective_end_time &gt;= NOW()) " +
            "ORDER BY priority ASC")
    List<PriceStrategy> selectActiveStrategies();
}
