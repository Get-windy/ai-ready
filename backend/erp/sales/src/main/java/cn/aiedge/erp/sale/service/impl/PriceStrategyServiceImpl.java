package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.dto.PriceRuleDTO;
import cn.aiedge.erp.sale.dto.PriceStrategyDTO;
import cn.aiedge.erp.sale.entity.PriceRule;
import cn.aiedge.erp.sale.entity.PriceStrategy;
import cn.aiedge.erp.sale.mapper.PriceRuleMapper;
import cn.aiedge.erp.sale.mapper.PriceStrategyMapper;
import cn.aiedge.erp.sale.service.IPriceStrategyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 价格策略服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PriceStrategyServiceImpl extends ServiceImpl<PriceStrategyMapper, PriceStrategy>
        implements IPriceStrategyService {

    private final PriceStrategyMapper priceStrategyMapper;
    private final PriceRuleMapper priceRuleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStrategy(PriceStrategyDTO dto) {
        PriceStrategy strategy = new PriceStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategy.setStatus("draft");
        priceStrategyMapper.insert(strategy);

        // 保存关联规则
        if (!CollectionUtils.isEmpty(dto.getRules())) {
            saveRules(strategy.getId(), dto.getRules());
        }
        return strategy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStrategy(Long id, PriceStrategyDTO dto) {
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            throw BusinessException.notFound("价格策略不存在");
        }
        BeanUtils.copyProperties(dto, strategy, "id", "createTime", "createBy");
        priceStrategyMapper.updateById(strategy);

        // 更新关联规则
        if (!CollectionUtils.isEmpty(dto.getRules())) {
            // 删除旧规则
            priceRuleMapper.delete(new LambdaQueryWrapper<PriceRule>()
                    .eq(PriceRule::getStrategyId, id));
            // 保存新规则
            saveRules(id, dto.getRules());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategy(Long id) {
        priceStrategyMapper.deleteById(id);
        // 级联删除规则
        priceRuleMapper.delete(new LambdaQueryWrapper<PriceRule>()
                .eq(PriceRule::getStrategyId, id));
    }

    @Override
    public PriceStrategyDTO getStrategyDetail(Long id) {
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            return null;
        }
        PriceStrategyDTO dto = new PriceStrategyDTO();
        BeanUtils.copyProperties(strategy, dto);

        // 查询关联规则
        List<PriceRule> rules = priceRuleMapper.selectByStrategyId(id);
        if (!CollectionUtils.isEmpty(rules)) {
            dto.setRules(rules.stream().map(this::convertToRuleDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    @Override
    public Page<PriceStrategyDTO> pageStrategies(Page<PriceStrategyDTO> page, Long tenantId,
                                                 String name, String status, String strategyType) {
        LambdaQueryWrapper<PriceStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, PriceStrategy::getTenantId, tenantId)
                .like(StringUtils.hasText(name), PriceStrategy::getName, name)
                .eq(StringUtils.hasText(status), PriceStrategy::getStatus, status)
                .eq(StringUtils.hasText(strategyType), PriceStrategy::getStrategyType, strategyType)
                .orderByDesc(PriceStrategy::getCreateTime);

        Page<PriceStrategy> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<PriceStrategy> resultPage = priceStrategyMapper.selectPage(entityPage, wrapper);

        List<PriceStrategyDTO> records = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<PriceStrategyDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    @Override
    public List<PriceStrategyDTO> getActiveStrategies() {
        List<PriceStrategy> strategies = priceStrategyMapper.selectActiveStrategies();
        return strategies.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateStrategy(Long id) {
        PriceStrategy strategy = new PriceStrategy();
        strategy.setId(id);
        strategy.setStatus("active");
        priceStrategyMapper.updateById(strategy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateStrategy(Long id) {
        PriceStrategy strategy = new PriceStrategy();
        strategy.setId(id);
        strategy.setStatus("inactive");
        priceStrategyMapper.updateById(strategy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyStrategy(Long id) {
        PriceStrategyDTO source = getStrategyDetail(id);
        if (source == null) {
            throw BusinessException.notFound("原策略不存在");
        }
        source.setId(null);
        source.setName(source.getName() + " - 副本");
        source.setStatus("draft");
        return createStrategy(source);
    }

    private void saveRules(Long strategyId, List<PriceRuleDTO> rules) {
        for (PriceRuleDTO ruleDTO : rules) {
            PriceRule rule = new PriceRule();
            BeanUtils.copyProperties(ruleDTO, rule);
            rule.setStrategyId(strategyId);
            rule.setStatus("active");
            priceRuleMapper.insert(rule);
        }
    }

    private PriceStrategyDTO convertToDTO(PriceStrategy strategy) {
        PriceStrategyDTO dto = new PriceStrategyDTO();
        BeanUtils.copyProperties(strategy, dto);
        return dto;
    }

    private PriceRuleDTO convertToRuleDTO(PriceRule rule) {
        PriceRuleDTO dto = new PriceRuleDTO();
        BeanUtils.copyProperties(rule, dto);
        return dto;
    }
}
