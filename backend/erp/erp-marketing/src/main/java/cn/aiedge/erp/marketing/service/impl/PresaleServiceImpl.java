package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.Presale;
import cn.aiedge.erp.marketing.mapper.PresaleMapper;
import cn.aiedge.erp.marketing.service.PresaleService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresaleServiceImpl extends ServiceImpl<PresaleMapper, Presale>
        implements PresaleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Presale presale = mustGet(id);
        if (presale.getStatus() == Presale.STATUS_ACTIVE) {
            throw new IllegalArgumentException("预售活动已发布，无需重复发布");
        }
        if (presale.getStatus() == Presale.STATUS_FINISHED) {
            throw new IllegalArgumentException("预售活动已结束，不能重新发布");
        }
        // 校验时间窗口
        LocalDateTime now = LocalDateTime.now();
        if (presale.getStartTime() == null || presale.getEndTime() == null) {
            throw new IllegalArgumentException("开始/结束时间不能为空");
        }
        if (!presale.getStartTime().isBefore(presale.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        if (!presale.getEndTime().isAfter(now)) {
            throw new IllegalArgumentException("结束时间必须晚于当前时间");
        }
        // 校验库存与价格
        if (presale.getStockLimit() == null || presale.getStockLimit() <= 0) {
            throw new IllegalArgumentException("预售库存必须大于0");
        }
        if (presale.getDepositAmount() == null || presale.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("定金金额必须大于0");
        }
        if (presale.getFinalAmount() == null || presale.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("尾款金额必须大于0");
        }

        presale.setStatus(Presale.STATUS_ACTIVE);
        updateById(presale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Presale presale = mustGet(id);
        if (presale.getStatus() != Presale.STATUS_PENDING && presale.getStatus() != Presale.STATUS_ACTIVE) {
            throw new IllegalArgumentException("仅未开始或进行中的预售活动可以取消");
        }
        presale.setStatus(Presale.STATUS_CANCELLED);
        updateById(presale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishExpired() {
        update(new LambdaUpdateWrapper<Presale>()
                .eq(Presale::getStatus, Presale.STATUS_ACTIVE)
                .lt(Presale::getEndTime, LocalDateTime.now())
                .set(Presale::getStatus, Presale.STATUS_FINISHED));
    }

    private Presale mustGet(Long id) {
        Presale presale = getById(id);
        if (presale == null) {
            throw new IllegalArgumentException("预售活动不存在: " + id);
        }
        return presale;
    }
}
