package cn.aiedge.erp.sales.pricing.repository;

import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格策略Repository接口
 */
@Repository
@Mapper
public interface PriceStrategyRepository extends JpaRepository<PriceStrategy, Long>, 
                                                JpaSpecificationExecutor<PriceStrategy>,
                                                BaseMapper<PriceStrategy> {
    
    /**
     * 根据租户ID和状态查询价格策略
     */
    List<PriceStrategy> findByTenantIdAndStatus(Long tenantId, String status);
    
    /**
     * 查询指定时间范围内生效的价格策略
     */
    @Query("SELECT ps FROM PriceStrategy ps WHERE ps.tenantId = :tenantId " +
           "AND ps.status = 'active' " +
           "AND (ps.effectiveStartTime IS NULL OR ps.effectiveStartTime <= :currentTime) " +
           "AND (ps.effectiveEndTime IS NULL OR ps.effectiveEndTime >= :currentTime)")
    List<PriceStrategy> findActiveStrategiesByTenantAndTime(
            @Param("tenantId") Long tenantId, 
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 根据客户等级查询适用的价格策略
     */
    List<PriceStrategy> findByCustomerLevelAndStatus(String customerLevel, String status);
    
    /**
     * 根据产品类别查询适用的价格策略
     */
    List<PriceStrategy> findByProductCategoryIdAndStatus(Long productCategoryId, String status);
    
    /**
     * 查询指定租户所有有效的价格策略（包含时间有效性）
     */
    default List<PriceStrategy> findAllActiveStrategies(Long tenantId) {
        return findActiveStrategiesByTenantAndTime(tenantId, LocalDateTime.now());
    }
}