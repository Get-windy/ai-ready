package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.AddonRule;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AddonRuleService extends IService<AddonRule> {

    /**
     * 启用规则
     */
    void enable(Long id);

    /**
     * 停用规则
     */
    void disable(Long id);
}
