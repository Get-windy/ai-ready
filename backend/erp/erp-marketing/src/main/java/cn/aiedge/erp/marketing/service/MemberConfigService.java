package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.MemberConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MemberConfigService extends IService<MemberConfig> {

    /** 读取当前租户的会员设置（无行时按默认值建行后返回，永不为 null） */
    MemberConfig getConfig();

    /** 保存当前租户的会员设置（单行 upsert） */
    MemberConfig saveConfig(MemberConfig config);
}
