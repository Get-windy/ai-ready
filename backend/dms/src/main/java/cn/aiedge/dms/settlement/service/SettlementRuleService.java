package cn.aiedge.dms.settlement.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.settlement.entity.DmsSettlementRule;
import cn.aiedge.dms.settlement.mapper.DmsSettlementRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 配送计费规则服务（配送结算 §3.1 计费规则模型）
 *
 * <p>规则维度：结算对象（配送员/渠道）× 适用渠道 × 适用线路 × 生效期 × 优先级；
 * 计价方式：按单 / 按距离 / 按重量 / 组合。规则表为空或未命中时，算费回落
 * 《配送参数》的全局缺省费率（`dms.settlement.*`），保证任何环境都能算得出费。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementRuleService {

    private final DmsSettlementRuleMapper ruleMapper;

    /** 规则分页（多条件） */
    public IPage<DmsSettlementRule> page(String ruleCode, String ruleName, Integer targetType,
                                         Integer status, long current, long size) {
        LambdaQueryWrapper<DmsSettlementRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(ruleCode), DmsSettlementRule::getRuleCode, ruleCode);
        wrapper.like(StringUtils.hasText(ruleName), DmsSettlementRule::getRuleName, ruleName);
        wrapper.eq(targetType != null, DmsSettlementRule::getTargetType, targetType);
        wrapper.eq(status != null, DmsSettlementRule::getStatus, status);
        wrapper.orderByAsc(DmsSettlementRule::getPriority).orderByDesc(DmsSettlementRule::getId);
        return ruleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /** 启用规则列表（供生成结算单时展示将采用的规则） */
    public List<DmsSettlementRule> enabledRules(Integer targetType) {
        return ruleMapper.selectList(new LambdaQueryWrapper<DmsSettlementRule>()
                .eq(DmsSettlementRule::getStatus, 1)
                .eq(targetType != null, DmsSettlementRule::getTargetType, targetType)
                .orderByAsc(DmsSettlementRule::getPriority));
    }

    public DmsSettlementRule getById(Long id) {
        DmsSettlementRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.notFound("计费规则不存在: " + id);
        }
        return rule;
    }

    /** 新增规则 */
    @Transactional(rollbackFor = Exception.class)
    public DmsSettlementRule create(DmsSettlementRule rule) {
        normalize(rule);
        if (!StringUtils.hasText(rule.getRuleCode())) {
            rule.setRuleCode(nextCode());
        } else if (existsCode(rule.getRuleCode(), null)) {
            throw BusinessException.badRequest("规则编码已存在：" + rule.getRuleCode());
        }
        rule.setId(null);
        if (rule.getStatus() == null) {
            rule.setStatus(1);
        }
        ruleMapper.insert(rule);
        log.info("计费规则已新增: code={}, name={}, targetType={}", rule.getRuleCode(), rule.getRuleName(), rule.getTargetType());
        return rule;
    }

    /** 修改规则（含生效期/费率调整；历史结算单已存快照，不受影响） */
    @Transactional(rollbackFor = Exception.class)
    public DmsSettlementRule update(Long id, DmsSettlementRule rule) {
        DmsSettlementRule exist = getById(id);
        normalize(rule);
        if (StringUtils.hasText(rule.getRuleCode()) && existsCode(rule.getRuleCode(), id)) {
            throw BusinessException.badRequest("规则编码已存在：" + rule.getRuleCode());
        }
        rule.setId(id);
        rule.setRuleCode(StringUtils.hasText(rule.getRuleCode()) ? rule.getRuleCode() : exist.getRuleCode());
        rule.setStatus(rule.getStatus() == null ? exist.getStatus() : rule.getStatus());
        ruleMapper.updateById(rule);
        log.info("计费规则已修改: id={}, code={}", id, rule.getRuleCode());
        return getById(id);
    }

    /** 启停规则 */
    @Transactional(rollbackFor = Exception.class)
    public DmsSettlementRule updateStatus(Long id, Integer status) {
        DmsSettlementRule exist = getById(id);
        exist.setStatus(status == null ? (Integer.valueOf(1).equals(exist.getStatus()) ? 0 : 1) : status);
        ruleMapper.updateById(exist);
        return exist;
    }

    /** 删除规则（物理删除：规则不参与审计留痕，历史结算单已存快照） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsSettlementRule exist = getById(id);
        ruleMapper.deleteById(id);
        log.info("计费规则已删除: id={}, code={}", id, exist.getRuleCode());
    }

    /**
     * 命中规则（算费引擎入口）
     *
     * <p>筛选：启用 + 生效期内 + 结算对象类型匹配 + （渠道/线路为空或不限时匹配任务）；
     * 排序：优先级升序 → 指定渠道者优先 → 指定线路者优先 → 新规则优先。</p>
     *
     * @return 命中的规则；无命中返回 {@code null}（调用方回落全局缺省费率）
     */
    public DmsSettlementRule match(Integer targetType, Long channelId, Long routeId) {
        LocalDate today = LocalDate.now();
        List<DmsSettlementRule> all = ruleMapper.selectList(new LambdaQueryWrapper<DmsSettlementRule>()
                .eq(DmsSettlementRule::getStatus, 1));
        List<DmsSettlementRule> candidates = new ArrayList<>();
        for (DmsSettlementRule r : all) {
            if (r.getTargetType() != null && targetType != null && !r.getTargetType().equals(targetType)) {
                continue;
            }
            if (r.getChannelId() != null && !r.getChannelId().equals(channelId)) {
                continue;
            }
            if (r.getRouteId() != null && !r.getRouteId().equals(routeId)) {
                continue;
            }
            if (r.getEffectiveStart() != null && today.isBefore(r.getEffectiveStart())) {
                continue;
            }
            if (r.getEffectiveEnd() != null && today.isAfter(r.getEffectiveEnd())) {
                continue;
            }
            candidates.add(r);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparingInt((DmsSettlementRule r) -> r.getPriority() == null ? 100 : r.getPriority())
                .thenComparing(r -> r.getChannelId() != null ? 0 : 1)
                .thenComparing(r -> r.getRouteId() != null ? 0 : 1)
                .thenComparing(Comparator.comparingLong((DmsSettlementRule r) -> r.getId() == null ? 0L : r.getId()).reversed()));
        return candidates.get(0);
    }

    /** 校验与缺省值 */
    private void normalize(DmsSettlementRule rule) {
        if (!StringUtils.hasText(rule.getRuleName())) {
            throw BusinessException.badRequest("规则名称不能为空");
        }
        if (rule.getTargetType() == null) {
            rule.setTargetType(1);
        }
        if (rule.getBillingType() == null) {
            rule.setBillingType(2);
        }
        if (rule.getSettleCycle() == null) {
            rule.setSettleCycle(1);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        rule.setBaseFee(nvl(rule.getBaseFee()));
        rule.setFreeDistanceKm(nvl(rule.getFreeDistanceKm()));
        rule.setPerKmRate(nvl(rule.getPerKmRate()));
        rule.setPerKgRate(nvl(rule.getPerKgRate()));
        rule.setTimeSurchargeRate(nvl(rule.getTimeSurchargeRate()));
        rule.setUrgentSurcharge(nvl(rule.getUrgentSurcharge()));
        if (rule.getEffectiveStart() != null && rule.getEffectiveEnd() != null
                && rule.getEffectiveStart().isAfter(rule.getEffectiveEnd())) {
            throw BusinessException.badRequest("生效开始日期不能晚于结束日期");
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean existsCode(String ruleCode, Long excludeId) {
        return ruleMapper.selectCount(new LambdaQueryWrapper<DmsSettlementRule>()
                .eq(DmsSettlementRule::getRuleCode, ruleCode)
                .ne(excludeId != null, DmsSettlementRule::getId, excludeId)) > 0;
    }

    /** 规则编码 JSR + 3 位序号 */
    private String nextCode() {
        DmsSettlementRule last = ruleMapper.selectOne(new LambdaQueryWrapper<DmsSettlementRule>()
                .likeRight(DmsSettlementRule::getRuleCode, "JSR")
                .orderByDesc(DmsSettlementRule::getRuleCode)
                .last("LIMIT 1"));
        int seq = 1;
        if (last != null && StringUtils.hasText(last.getRuleCode())) {
            try {
                seq = Integer.parseInt(last.getRuleCode().substring(3)) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return "JSR" + String.format("%03d", seq);
    }
}
