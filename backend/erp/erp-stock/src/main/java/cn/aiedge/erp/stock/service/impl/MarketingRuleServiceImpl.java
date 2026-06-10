package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.MarketingRule;
import cn.aiedge.erp.stock.mapper.MarketingRuleMapper;
import cn.aiedge.erp.stock.service.MarketingRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class MarketingRuleServiceImpl extends ServiceImpl<MarketingRuleMapper, MarketingRule> implements MarketingRuleService {
}
