package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.FlashSale;
import cn.aiedge.erp.marketing.entity.FlashSaleOrder;
import cn.aiedge.erp.marketing.mapper.FlashSaleMapper;
import cn.aiedge.erp.marketing.mapper.FlashSaleOrderMapper;
import cn.aiedge.erp.marketing.service.FlashSaleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale>
        implements FlashSaleService {

    private final FlashSaleOrderMapper flashSaleOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        FlashSale flashSale = mustGet(id);
        if (flashSale.getStatus() == FlashSale.STATUS_PUBLISHED) {
            throw new IllegalArgumentException("场次已发布，无需重复发布");
        }
        if (flashSale.getStatus() == FlashSale.STATUS_FINISHED) {
            throw new IllegalArgumentException("场次已结束，不能重新发布");
        }
        // 校验时间窗口
        LocalDateTime now = LocalDateTime.now();
        if (flashSale.getStartTime() == null || flashSale.getEndTime() == null) {
            throw new IllegalArgumentException("开始/结束时间不能为空");
        }
        if (!flashSale.getStartTime().isBefore(flashSale.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        if (!flashSale.getEndTime().isAfter(now)) {
            throw new IllegalArgumentException("结束时间必须晚于当前时间");
        }
        // 校验库存与价格
        if (flashSale.getStockLimit() == null || flashSale.getStockLimit() <= 0) {
            throw new IllegalArgumentException("秒杀限量库存必须大于0");
        }
        if (flashSale.getFlashPrice() == null || flashSale.getFlashPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("秒杀价必须大于0");
        }
        if (flashSale.getOriginalPrice() != null
                && flashSale.getFlashPrice().compareTo(flashSale.getOriginalPrice()) > 0) {
            throw new IllegalArgumentException("秒杀价不能高于原价");
        }

        recalcSoldCount(id);
        flashSale.setStatus(FlashSale.STATUS_PUBLISHED);
        updateById(flashSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        FlashSale flashSale = mustGet(id);
        if (flashSale.getStatus() != FlashSale.STATUS_PUBLISHED) {
            throw new IllegalArgumentException("仅已发布的场次可以取消");
        }
        flashSale.setStatus(FlashSale.STATUS_CANCELLED);
        updateById(flashSale);
    }

    @Override
    public IPage<FlashSaleOrder> pageParticipants(Long sessionId, Integer pageNum, Integer pageSize) {
        recalcSoldCount(sessionId);
        Page<FlashSaleOrder> page = new Page<>(pageNum, pageSize);
        return flashSaleOrderMapper.selectPage(page, new LambdaQueryWrapper<FlashSaleOrder>()
                .eq(FlashSaleOrder::getSessionId, sessionId)
                .orderByDesc(FlashSaleOrder::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalcSoldCount(Long sessionId) {
        List<FlashSaleOrder> orders = flashSaleOrderMapper.selectList(new LambdaQueryWrapper<FlashSaleOrder>()
                .eq(FlashSaleOrder::getSessionId, sessionId)
                .ne(FlashSaleOrder::getStatus, FlashSaleOrder.STATUS_CANCELLED));
        int sold = orders.stream()
                .map(FlashSaleOrder::getQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();
        update(new LambdaUpdateWrapper<FlashSale>()
                .eq(FlashSale::getId, sessionId)
                .set(FlashSale::getSoldCount, sold));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishExpired() {
        update(new LambdaUpdateWrapper<FlashSale>()
                .eq(FlashSale::getStatus, FlashSale.STATUS_PUBLISHED)
                .lt(FlashSale::getEndTime, LocalDateTime.now())
                .set(FlashSale::getStatus, FlashSale.STATUS_FINISHED));
    }

    private FlashSale mustGet(Long id) {
        FlashSale flashSale = getById(id);
        if (flashSale == null) {
            throw new IllegalArgumentException("秒杀场次不存在: " + id);
        }
        return flashSale;
    }
}
