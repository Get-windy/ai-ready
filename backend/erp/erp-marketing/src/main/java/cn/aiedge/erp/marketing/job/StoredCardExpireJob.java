package cn.aiedge.erp.marketing.job;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.common.lock.DistributedLock;
import cn.aiedge.common.lock.DistributedLockFactory;
import cn.aiedge.erp.marketing.entity.StoredCard;
import cn.aiedge.erp.marketing.entity.StoredCardFlow;
import cn.aiedge.erp.marketing.mapper.StoredCardFlowMapper;
import cn.aiedge.erp.marketing.mapper.StoredCardMapper;
import cn.aiedge.erp.marketing.mapper.MarketingTenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销·储值卡到期处理（定时作业）
 *
 * <p>把《储值卡》页的到期规则定时化：每日把已过有效期且仍为 ACTIVE 的卡置 {@code EXPIRED}
 * 并写一条 {@code ADJUST} 流水留痕（任务行 {@code job_key = mkt.storedCard.expire}）。</p>
 *
 * <p><b>只改状态、不动余额</b>：预付卡余额属消费者已付款项，系统不做过期没收；
 * 余额处置（退款/续期）必须由人工按合规流程处理（法释〔2025〕4 号）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoredCardExpireJob implements JobHandler {

    public static final String KEY = "mkt.storedCard.expire";
    private static final String LOCK_KEY = "mkt:job:lock:" + KEY;
    private static final long LEASE_MS = 300_000L;

    private final StoredCardMapper storedCardMapper;
    private final StoredCardFlowMapper storedCardFlowMapper;
    private final MarketingTenantMapper marketingTenantMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String name() {
        return "营销·储值卡到期处理";
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
            if (tenantIds.isEmpty()) return "无带有效期的储值卡，跳过";

            int tenants = 0;
            int expired = 0;
            for (Long tenantId : tenantIds) {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                try {
                    expired += expireOneTenant();
                    tenants++;
                } catch (Exception e) {
                    log.warn("[{}] 租户 {} 储值卡到期处理失败", KEY, tenantId, e);
                } finally {
                    MyBatisPlusConfig.clearTempTenantId();
                }
            }
            String summary = String.format("储值卡到期处理完成：%d 个租户，置为已过期 %d 张（余额保留，退款按合规流程人工处理）",
                    tenants, expired);
            log.info("[{}] {}", KEY, summary);
            return summary;
        } finally {
            lock.unlock();
        }
    }

    private int expireOneTenant() {
        List<StoredCard> due = storedCardMapper.selectList(new LambdaQueryWrapper<StoredCard>()
                .eq(StoredCard::getStatus, StoredCard.STATUS_ACTIVE)
                .isNotNull(StoredCard::getExpireTime)
                .le(StoredCard::getExpireTime, LocalDateTime.now()));
        LocalDateTime now = LocalDateTime.now();
        for (StoredCard card : due) {
            storedCardMapper.update(null, new LambdaUpdateWrapper<StoredCard>()
                    .eq(StoredCard::getId, card.getId())
                    .eq(StoredCard::getStatus, StoredCard.STATUS_ACTIVE)
                    .set(StoredCard::getStatus, StoredCard.STATUS_EXPIRED)
                    .set(StoredCard::getUpdateTime, now));
            storedCardFlowMapper.insert(new StoredCardFlow()
                    .setTenantId(card.getTenantId())
                    .setCardId(card.getId())
                    .setCardNo(card.getCardNo())
                    .setPartnerId(card.getPartnerId())
                    .setFlowType(StoredCardFlow.ADJUST)
                    .setAmount(BigDecimal.ZERO)
                    .setBonusAmount(BigDecimal.ZERO)
                    .setBalanceAfter(card.getBalance())
                    .setHandlerName("系统定时任务")
                    .setRemark("卡片到期置为已过期（有效期至 " + card.getExpireTime() + "，余额保留）")
                    .setDeleted(0)
                    .setCreateTime(now));
        }
        return due.size();
    }
}
