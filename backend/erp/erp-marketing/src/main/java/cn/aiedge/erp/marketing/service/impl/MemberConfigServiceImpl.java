package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.marketing.entity.MemberConfig;
import cn.aiedge.erp.marketing.mapper.MemberConfigMapper;
import cn.aiedge.erp.marketing.service.MemberConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class MemberConfigServiceImpl extends ServiceImpl<MemberConfigMapper, MemberConfig>
        implements MemberConfigService {

    /** 对标实测默认值（2026-09-15 抓取时点，见《会员设置开发文档》§2） */
    private MemberConfig defaultConfig() {
        return new MemberConfig()
                .setMemberEnabled(1)
                .setAutoUpgradeEnabled(0)
                .setPointsRewardEnabled(1)
                .setRegisterPoints(BigDecimal.ZERO)
                .setBirthdayMultiple(BigDecimal.ONE)
                .setConsumePointsEnabled(1)
                .setConsumePointsMode("BY_AMOUNT")
                .setAmountPerPoint(new BigDecimal("100"))
                .setPointsByDiscount(0)
                .setPointsRoundRule("ROUND")
                .setApplySceneOffline(0)
                .setApplySceneMall(0)
                .setSigninEnabled(1)
                .setSigninFirstPoints(3)
                .setSigninIncrement(1)
                .setSigninMaxPoints(8)
                .setCashDeductEnabled(1)
                .setPointsPerYuan(new BigDecimal("100"))
                .setMaxDeductPercent(new BigDecimal("100"));
    }

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberConfig getConfig() {
        Long tenantId = currentTenantId();
        MemberConfig existing = getOne(new LambdaQueryWrapper<MemberConfig>()
                .eq(MemberConfig::getTenantId, tenantId)
                .orderByAsc(MemberConfig::getId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        MemberConfig created = defaultConfig()
                .setTenantId(tenantId)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        save(created);
        log.info("[会员设置] 租户 {} 首次读取，已按对标默认值建行 id={}", tenantId, created.getId());
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberConfig saveConfig(MemberConfig config) {
        MemberConfig current = getConfig();
        config.setId(current.getId());
        config.setTenantId(current.getTenantId());
        config.setUpdateTime(LocalDateTime.now());
        updateById(config);
        return getById(current.getId());
    }
}
