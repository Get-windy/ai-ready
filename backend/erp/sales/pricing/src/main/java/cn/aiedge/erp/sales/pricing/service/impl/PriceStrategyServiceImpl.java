package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.erp.sales.pricing.dto.PriceStrategyDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.repository.PriceStrategyRepository;
import cn.aiedge.erp.sales.pricing.service.IPriceStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 价格策略服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PriceStrategyServiceImpl implements IPriceStrategyService {
    
    private final PriceStrategyRepository priceStrategyRepository;
    
    @Override
    public PriceStrategy createStrategy(PriceStrategyDTO strategyDTO) {
        log.info("创建价格策略: {}", strategyDTO.getName());
        
        PriceStrategy strategy = new PriceStrategy();
        BeanUtils.copyProperties(strategyDTO, strategy, "id", "createTime", "updateTime");
        
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy updateStrategy(Long id, PriceStrategyDTO strategyDTO) {
        log.info("更新价格策略: id={}", id);
        
        PriceStrategy strategy = priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
        
        BeanUtils.copyProperties(strategyDTO, strategy, "id", "createTime", "updateTime");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public void deleteStrategy(Long id) {
        log.info("删除价格策略: id={}", id);
        
        PriceStrategy strategy = priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
        
        strategy.setDeleted(1);
        strategy.setUpdateTime(LocalDateTime.now());
        
        priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy getStrategy(Long id) {
        return priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
    }
    
    @Override
    public Page<PriceStrategy> listStrategies(Pageable pageable) {
        return priceStrategyRepository.findAll(pageable);
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByTenant(Long tenantId) {
        return priceStrategyRepository.findByTenantIdAndStatus(tenantId, "active");
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByCustomerLevel(String customerLevel) {
        return priceStrategyRepository.findByCustomerLevelAndStatus(customerLevel, "active");
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByProductCategory(Long productCategoryId) {
        return priceStrategyRepository.findByProductCategoryIdAndStatus(productCategoryId, "active");
    }
    
    @Override
    public PriceStrategy activateStrategy(Long id) {
        log.info("激活价格策略: id={}", id);
        
        PriceStrategy strategy = getStrategy(id);
        strategy.setStatus("active");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy deactivateStrategy(Long id) {
        log.info("停用价格策略: id={}", id);
        
        PriceStrategy strategy = getStrategy(id);
        strategy.setStatus("inactive");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy copyStrategy(Long sourceId, String newStrategyName) {
        log.info("复制价格策略: sourceId={}, newName={}", sourceId, newStrategyName);
        
        PriceStrategy sourceStrategy = getStrategy(sourceId);
        PriceStrategy newStrategy = new PriceStrategy();
        
        BeanUtils.copyProperties(sourceStrategy, newStrategy, "id", "name", "createTime", "updateTime");
        newStrategy.setName(newStrategyName);
        newStrategy.setCreateTime(LocalDateTime.now());
        newStrategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(newStrategy);
    }
    
    @Override
    public List<PriceStrategy> importStrategies(List<PriceStrategyDTO> strategyDTOs) {
        log.info("导入价格策略: count={}", strategyDTOs.size());
        
        return strategyDTOs.stream()
                .map(this::createStrategy)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PriceStrategyDTO> exportStrategies(List<Long> strategyIds) {
        log.info("导出价格策略: ids={}", strategyIds);
        
        return strategyIds.stream()
                .map(this::getStrategy)
                .map(strategy -> {
                    PriceStrategyDTO dto = new PriceStrategyDTO();
                    BeanUtils.copyProperties(strategy, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}