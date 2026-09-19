package cn.aiedge.erp.marketing.job;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.common.lock.DistributedLock;
import cn.aiedge.common.lock.DistributedLockFactory;
import cn.aiedge.erp.marketing.mapper.MarketingTenantMapper;
import cn.aiedge.erp.marketing.service.PointsLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 营销·积分到期处理（定时作业）
 *
 * <p>把《会员设置》页的「立即执行过期」定时化：每日把已到期积分批次的剩余清零、写 {@code EXPIRE} 流水
 * （任务行 {@code job_key = mkt.member.pointsExpire}）。</p>
 *
 * <p>为什么必须定时：积分有效期是**时间驱动**的规则，靠人工点按钮必然漏；
 * 而「过期」只有在批次账上才算得准（过期的是某批次剩余，不是账户总额）。</p>
 *
 * <p><b>安全闸门</b>：默认关闭（迁移 {@code enabled = 0}）+ Redis 锁多实例互斥。
 * 本作业**无演练开关**：过期是"到期即应发生"的确定性事实，演练没有业务意义。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPointsExpireJob implements JobHandler {

    public static final String KEY = "mkt.member.pointsExpire";
    private static final String LOCK_KEY = "mkt:job:lock:" + KEY;
    private static final long LEASE_MS = 300_000L;

    private final PointsLedgerService pointsLedgerService;
    private final MarketingTenantMapper marketingTenantMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String name() {
        return "营销·积分到期处理";
    }

    @Override
    public String execute(String params) {
        DistributedLock lock = DistributedLockFactory.createRedisLock(redisTemplate, LOCK_KEY, LEASE_MS);
        if (!lock.tryLock()) {
            log.info("[{}] 另一实例正在执行，本轮跳过", KEY);
            return "跳过：另一实例正在执行（锁 " + LOCK_KEY + " 被占用）";
        }
        try {
            List<Long> tenantIds = marketingTenantMapper.selectActiveTenantIds();
            if (tenantIds.isEmpty()) return "无到期积分批次，跳过";

            int tenants = 0;
            int members = 0;
            BigDecimal expired = BigDecimal.ZERO;
            for (Long tenantId : tenantIds) {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                try {
                    Map<String, BigDecimal> detail = pointsLedgerService.expireDue(LocalDate.now());
                    members += detail.size();
                    for (BigDecimal v : detail.values()) expired = expired.add(v);
                    tenants++;
                } catch (Exception e) {
                    log.warn("[{}] 租户 {} 积分过期处理失败", KEY, tenantId, e);
                } finally {
                    MyBatisPlusConfig.clearTempTenantId();
                }
            }
            String summary = String.format("积分过期处理完成：%d 个租户，%d 位会员，共过期 %s 分",
                    tenants, members, expired.stripTrailingZeros().toPlainString());
            log.info("[{}] {}", KEY, summary);
            return summary;
        } finally {
            lock.unlock();
        }
    }
}
