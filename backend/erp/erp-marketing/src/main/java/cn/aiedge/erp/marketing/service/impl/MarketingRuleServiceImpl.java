package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.MarketingRule;
import cn.aiedge.erp.marketing.mapper.MarketingRuleMapper;
import cn.aiedge.erp.marketing.service.MarketingRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MarketingRuleServiceImpl extends ServiceImpl<MarketingRuleMapper, MarketingRule>
        implements MarketingRuleService {
}
