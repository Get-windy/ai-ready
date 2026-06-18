package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.dto.PriceStrategyDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 价格策略服务接口
 */
public interface IPriceStrategyService {
    
    /**
     * 创建价格策略
     */
    PriceStrategy createStrategy(PriceStrategyDTO strategyDTO);
    
    /**
     * 更新价格策略
     */
    PriceStrategy updateStrategy(Long id, PriceStrategyDTO strategyDTO);
    
    /**
     * 删除价格策略（逻辑删除）
     */
    void deleteStrategy(Long id);
    
    /**
     * 获取价格策略详情
     */
    PriceStrategy getStrategy(Long id);
    
    /**
     * 分页查询价格策略
     */
    Page<PriceStrategy> listStrategies(Pageable pageable);
    
    /**
     * 查询租户的所有价格策略
     */
    List<PriceStrategy> listStrategiesByTenant(Long tenantId);
    
    /**
     * 根据客户等级查询适用的价格策略
     */
    List<PriceStrategy> listStrategiesByCustomerLevel(String customerLevel);
    
    /**
     * 根据产品类别查询适用的价格策略
     */
    List<PriceStrategy> listStrategiesByProductCategory(Long productCategoryId);
    
    /**
     * 激活价格策略
     */
    PriceStrategy activateStrategy(Long id);
    
    /**
     * 停用价格策略
     */
    PriceStrategy deactivateStrategy(Long id);
    
    /**
     * 复制价格策略
     */
    PriceStrategy copyStrategy(Long sourceId, String newStrategyName);
    
    /**
     * 批量导入价格策略
     */
    List<PriceStrategy> importStrategies(List<PriceStrategyDTO> strategyDTOs);
    
    /**
     * 导出价格策略
     */
    List<PriceStrategyDTO> exportStrategies(List<Long> strategyIds);
}