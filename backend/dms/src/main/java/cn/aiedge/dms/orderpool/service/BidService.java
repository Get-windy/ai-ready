package cn.aiedge.dms.orderpool.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.PoolStatusEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.orderpool.entity.DmsBid;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.mapper.DmsBidMapper;
import cn.aiedge.dms.orderpool.mapper.DmsOrderPoolMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.verification.service.KycService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞价服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final DmsBidMapper bidMapper;
    private final DmsOrderPoolMapper orderPoolMapper;
    private final DmsTaskMapper taskMapper;
    private final KycService kycService;

    /**
     * 根据订单大厅 ID 查询竞价记录列表
     */
    public List<DmsBid> getBidsByPoolId(Long poolId) {
        return bidMapper.findByPoolId(poolId);
    }

    /**
     * 创建竞价记录
     *
     * @param poolId    订单大厅 ID
     * @param riderId   骑手 ID
     * @param riderName 骑手名称
     * @param price     出价
     * @return 竞价记录
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsBid createBid(Long poolId, Long riderId, String riderName, BigDecimal price) {
        DmsOrderPool pool = orderPoolMapper.selectById(poolId);
        if (pool == null) {
            throw BusinessException.notFound("订单大厅条目不存在");
        }
        if (PoolStatusEnum.BIDDING.getValue() != pool.getPoolStatus()) {
            throw new BusinessException("当前不在竞价状态");
        }
        if (pool.getBidStartPrice() != null && price.compareTo(pool.getBidStartPrice()) > 0) {
            throw new BusinessException("出价不能高于起拍价");
        }
        // 竞价截止后不再收单（否则结算前还能压价，破坏「截止时间统一」的公平性）
        if (pool.getBidEndTime() != null && LocalDateTime.now().isAfter(pool.getBidEndTime())) {
            throw new BusinessException("竞价已截止，无法再出价");
        }

        // 同一配送员对同一池只保留一条有效报价：重复出价视为「改价」，不再新增记录
        // （否则同一人可用多次出价刷高「竞价数」、在竞价详情里占据多个名次，破坏竞价公平）
        DmsBid bid = bidMapper.findActiveBid(poolId, riderId);
        if (bid != null) {
            bid.setRiderName(riderName);
            bid.setBidPrice(price);
            bid.setBidTime(LocalDateTime.now());
            bidMapper.updateById(bid);
        } else {
            bid = new DmsBid();
            bid.setPoolId(poolId);
            bid.setTaskId(pool.getTaskId());
            bid.setRiderId(riderId);
            bid.setRiderName(riderName);
            bid.setBidPrice(price);
            bid.setBidTime(LocalDateTime.now());
            bid.setIsWin(0);
            bidMapper.insert(bid);
        }

        // 大厅「竞价数 / 当前价」一律按有效报价重算，避免加加减减产生漂移
        refreshPoolBidSummary(pool);

        log.info("Rider {} ({}) placed bid {} on pool {}", riderId, riderName, price, poolId);
        return bid;
    }

    /**
     * 按有效（未取消）报价重算池上的「竞价数」与「当前价」
     *
     * <p>当前价 = 最低有效报价；无人出价时回退到起拍价。</p>
     */
    private void refreshPoolBidSummary(DmsOrderPool pool) {
        Long activeCount = bidMapper.selectCount(
                new LambdaQueryWrapper<DmsBid>().eq(DmsBid::getPoolId, pool.getId()));
        pool.setBidCount(activeCount == null ? 0 : activeCount.intValue());
        DmsBid lowest = bidMapper.findLowestBid(pool.getId());
        pool.setBidCurrentPrice(lowest != null ? lowest.getBidPrice() : pool.getBidStartPrice());
        orderPoolMapper.updateById(pool);
    }

    /**
     * 取消竞价
     *
     * @param bidId   竞价记录 ID
     * @param riderId 骑手 ID（用于校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelBid(Long bidId, Long riderId) {
        DmsBid bid = bidMapper.selectById(bidId);
        if (bid == null) {
            throw BusinessException.notFound("竞价记录不存在");
        }
        if (!bid.getRiderId().equals(riderId)) {
            throw new BusinessException("只能取消自己的竞价");
        }
        if (Integer.valueOf(1).equals(bid.getIsWin())) {
            throw new BusinessException("已中标的竞价不可取消");
        }

        // 只有「竞价中」的池才有报价可撤：已结算/已下架/已过期后再撤会与中标结果、竞价数口径打架
        DmsOrderPool pool = orderPoolMapper.selectById(bid.getPoolId());
        if (pool == null) {
            throw BusinessException.notFound("订单大厅条目不存在");
        }
        if (!Integer.valueOf(PoolStatusEnum.BIDDING.getValue()).equals(pool.getPoolStatus())) {
            throw new BusinessException("该订单已结束竞价，无法取消出价");
        }

        bidMapper.deleteById(bidId);

        // 取消后重算竞价数与当前价（当前价回退到剩余最低报价，无人报价则回退起拍价）
        refreshPoolBidSummary(pool);

        log.info("Bid {} cancelled by rider {}", bidId, riderId);
    }

    /**
     * 结算竞价：选择最低出价者中标
     *
     * @param poolId 订单大厅 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleBid(Long poolId) {
        DmsOrderPool pool = orderPoolMapper.selectById(poolId);
        if (pool == null) {
            throw BusinessException.notFound("订单大厅条目不存在");
        }
        if (PoolStatusEnum.BIDDING.getValue() != pool.getPoolStatus()) {
            throw new BusinessException("当前状态不可结算竞价");
        }

        List<DmsBid> bids = bidMapper.findByPoolId(poolId);
        if (bids.isEmpty()) {
            throw new BusinessException("暂无竞价记录，无法结算");
        }

        // 出价最低者中标（findByPoolId 已按 bid_price ASC 排序，且已剔除取消的报价）
        DmsBid winner = bids.get(0);
        // 与抢单/强制分配一致：中标人同样要过实名资质门控（否则「竞价中标」成了绕过资质的口子）
        kycService.assertEligible(winner.getRiderId());
        winner.setIsWin(1);
        bidMapper.updateById(winner);

        // 更新大厅状态：中标价即当前价（列表「当前价/中标价」列结算后显示中标价）
        pool.setPoolStatus(PoolStatusEnum.ACCEPTED.getValue());
        pool.setBidCurrentPrice(winner.getBidPrice());
        orderPoolMapper.updateById(pool);

        // 中标结果与任务指派同事务写入（避免「中标了但任务没派出去」）
        DmsTask task = taskMapper.selectById(pool.getTaskId());
        if (task == null) {
            throw BusinessException.notFound("配送任务不存在，无法完成中标指派");
        }
        task.setRiderId(winner.getRiderId());
        task.setRiderName(winner.getRiderName());
        task.setStatus(TaskStatusEnum.ASSIGNED.getValue());
        task.setDispatchType(4); // 竞价
        taskMapper.updateById(task);

        log.info("Bid settled for pool {}, winner riderId={}, price={}",
                poolId, winner.getRiderId(), winner.getBidPrice());
    }
}
