package cn.aiedge.dms.orderpool.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.PoolStatusEnum;
import cn.aiedge.dms.orderpool.entity.DmsBid;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.mapper.DmsBidMapper;
import cn.aiedge.dms.orderpool.mapper.DmsOrderPoolMapper;
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
        if (price.compareTo(pool.getBidStartPrice()) > 0) {
            throw new BusinessException("出价不能高于起拍价");
        }

        DmsBid bid = new DmsBid();
        bid.setPoolId(poolId);
        bid.setTaskId(pool.getTaskId());
        bid.setRiderId(riderId);
        bid.setRiderName(riderName);
        bid.setBidPrice(price);
        bid.setBidTime(LocalDateTime.now());
        bid.setIsWin(0);
        bidMapper.insert(bid);

        // 更新大厅竞价值
        pool.setBidCount(pool.getBidCount() != null ? pool.getBidCount() + 1 : 1);
        pool.setBidCurrentPrice(price);
        orderPoolMapper.updateById(pool);

        log.info("Rider {} ({}) placed bid {} on pool {}", riderId, riderName, price, poolId);
        return bid;
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

        bidMapper.deleteById(bidId);

        // 更新大厅竞价次数
        DmsOrderPool pool = orderPoolMapper.selectById(bid.getPoolId());
        if (pool != null && pool.getBidCount() != null && pool.getBidCount() > 0) {
            pool.setBidCount(pool.getBidCount() - 1);
            orderPoolMapper.updateById(pool);
        }

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

        // 出价最低者中标（findByPoolId 已按 bid_price ASC 排序）
        DmsBid winner = bids.get(0);
        winner.setIsWin(1);
        bidMapper.updateById(winner);

        // 更新大厅状态
        pool.setPoolStatus(PoolStatusEnum.ACCEPTED.getValue());
        orderPoolMapper.updateById(pool);

        log.info("Bid settled for pool {}, winner riderId={}, price={}",
                poolId, winner.getRiderId(), winner.getBidPrice());
    }
}
