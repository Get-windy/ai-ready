package cn.aiedge.dms.orderpool.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.enums.PoolStatusEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.util.RetryUtils;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.mapper.DmsOrderPoolMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单大厅服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPoolService {

    private final DmsOrderPoolMapper orderPoolMapper;
    private final DmsTaskMapper taskMapper;
    private final DmsRiderMapper riderMapper;

    /**
     * 分页查询订单大厅
     */
    public IPage<DmsOrderPool> page(Page<DmsOrderPool> page, LambdaQueryWrapper<DmsOrderPool> wrapper) {
        return orderPoolMapper.selectPage(page, wrapper);
    }

    /**
     * 根据 ID 获取订单大厅条目，不存在则抛异常
     */
    public DmsOrderPool getById(Long id) {
        DmsOrderPool pool = orderPoolMapper.selectById(id);
        if (pool == null) {
            throw BusinessException.notFound("订单大厅条目不存在");
        }
        return pool;
    }

    /**
     * 发布任务到订单大厅
     *
     * @param taskId     任务 ID
     * @param deliveryFee 配送费
     * @return 订单大厅条目
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsOrderPool publishToPool(Long taskId, BigDecimal deliveryFee) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("配送任务不存在");
        }

        DmsOrderPool pool = new DmsOrderPool();
        pool.setTaskId(taskId);
        pool.setDeliveryFee(deliveryFee);
        pool.setBidEnabled(0);
        pool.setBidCount(0);
        pool.setPoolStatus(PoolStatusEnum.PENDING_GRAB.getValue());
        pool.setPublishedTime(LocalDateTime.now());
        orderPoolMapper.insert(pool);
        log.info("Task {} published to order pool, poolId={}", taskId, pool.getId());
        return pool;
    }

    /**
     * 启用竞价
     *
     * @param poolId           订单大厅 ID
     * @param startPrice       起拍价
     * @param durationMinutes  竞价时长（分钟）
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableBid(Long poolId, BigDecimal startPrice, Integer durationMinutes) {
        DmsOrderPool pool = getById(poolId);
        if (PoolStatusEnum.PENDING_GRAB.getValue() != pool.getPoolStatus()) {
            throw new BusinessException("当前状态不可启用竞价");
        }
        pool.setBidEnabled(1);
        pool.setBidStartPrice(startPrice);
        pool.setBidCurrentPrice(startPrice);
        pool.setBidStartTime(LocalDateTime.now());
        pool.setBidEndTime(LocalDateTime.now().plusMinutes(durationMinutes));
        pool.setPoolStatus(PoolStatusEnum.BIDDING.getValue());
        orderPoolMapper.updateById(pool);
        log.info("Bidding enabled for pool {}, startPrice={}, duration={}min", poolId, startPrice, durationMinutes);
    }

    /**
     * 抢单（支持乐观锁重试）
     *
     * @param poolId    订单大厅 ID
     * @param riderId   骑手 ID
     * @param riderName 骑手名称
     */
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long poolId, Long riderId, String riderName) {
        // 校验骑手状态
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw BusinessException.notFound("骑手不存在");
        }
        if (rider.getStatus() != DmsConstants.RIDER_STATUS_IDLE) {
            throw new BusinessException("骑手当前不可接单，状态：" +
                    (rider.getStatus() == DmsConstants.RIDER_STATUS_BUSY ? "忙碌" :
                     rider.getStatus() == DmsConstants.RIDER_STATUS_OFFLINE ? "离线" : "休息"));
        }

        // 乐观锁重试抢单
        boolean grabbed = RetryUtils.retry3(() -> {
            DmsOrderPool pool = orderPoolMapper.selectById(poolId);
            if (pool == null) {
                return false;
            }
            if (PoolStatusEnum.PENDING_GRAB.getValue() != pool.getPoolStatus()) {
                return false; // 状态已变更，放弃
            }
            pool.setPoolStatus(PoolStatusEnum.ACCEPTED.getValue());
            return orderPoolMapper.updateById(pool) > 0;
        });

        if (!grabbed) {
            throw new BusinessException("订单已被其他人抢走，请刷新后重试");
        }

        // 更新任务状态及骑手信息
        DmsOrderPool freshPool = orderPoolMapper.selectById(poolId);
        DmsTask task = freshPool != null ? taskMapper.selectById(freshPool.getTaskId()) : null;
        if (task != null) {
            task.setRiderId(riderId);
            task.setStatus(TaskStatusEnum.ACCEPTED.getValue());
            task.setDispatchType(3); // 抢单
            taskMapper.updateById(task);
        }

        log.info("Rider {} ({}) grabbed order pool {}", riderId, riderName, poolId);
    }

    /**
     * 批量过期已到竞价截止时间的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void expirePools() {
        LambdaQueryWrapper<DmsOrderPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsOrderPool::getPoolStatus, PoolStatusEnum.BIDDING.getValue())
                .le(DmsOrderPool::getBidEndTime, LocalDateTime.now());

        List<DmsOrderPool> expiredPools = orderPoolMapper.selectList(wrapper);
        for (DmsOrderPool pool : expiredPools) {
            pool.setPoolStatus(PoolStatusEnum.EXPIRED.getValue());
            orderPoolMapper.updateById(pool);
        }
        if (!expiredPools.isEmpty()) {
            log.info("Expired {} order pools", expiredPools.size());
        }
    }
}
