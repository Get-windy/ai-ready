package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.CommissionRule;
import cn.aiedge.erp.marketing.mapper.CommissionRuleMapper;
import cn.aiedge.erp.marketing.service.CommissionRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommissionRuleServiceImpl extends ServiceImpl<CommissionRuleMapper, CommissionRule>
        implements CommissionRuleService {
}
