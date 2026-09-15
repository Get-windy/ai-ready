package cn.aiedge.dms.orderpool.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.enums.PoolStatusEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.util.RetryUtils;
import cn.aiedge.dms.orderpool.dto.OrderPoolQuery;
import cn.aiedge.dms.orderpool.dto.OrderPoolRowVO;
import cn.aiedge.dms.orderpool.entity.DmsBid;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.mapper.DmsBidMapper;
import cn.aiedge.dms.orderpool.mapper.DmsOrderPoolMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.verification.service.KycService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 订单大厅服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPoolService {

    private final DmsOrderPoolMapper orderPoolMapper;
    private final DmsBidMapper bidMapper;
    private final DmsTaskMapper taskMapper;
    private final DmsRiderMapper riderMapper;
    private final KycService kycService;

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
     * 订单池台账多条件分页（联查配送任务主数据：任务编号/客户/取货收货地址/里程/货品金额/接单配送员）
     *
     * <p>任务号、订单号、客户为「先查任务再按 taskId 收窄」的两段式过滤，
     * 避免在池表上做跨表 join（池数据量小、任务表大，两段式反而更快且不引入方言 SQL）。</p>
     */
    public Page<OrderPoolRowVO> pageQuery(OrderPoolQuery query, int pageNum, int pageSize) {
        OrderPoolQuery q = query == null ? new OrderPoolQuery() : query;
        LambdaQueryWrapper<DmsOrderPool> wrapper = new LambdaQueryWrapper<>();

        // ── 任务侧条件 → 收窄 taskId 集合 ──
        boolean needTaskFilter = StringUtils.hasText(q.getTaskNo()) || StringUtils.hasText(q.getOrderNo())
                || StringUtils.hasText(q.getCustomerName()) || q.getOrderType() != null
                || q.getRiderId() != null || q.getRouteId() != null || StringUtils.hasText(q.getRouteArea())
                || StringUtils.hasText(q.getKeyword());
        if (needTaskFilter) {
            LambdaQueryWrapper<DmsTask> tw = new LambdaQueryWrapper<>();
            tw.like(StringUtils.hasText(q.getTaskNo()), DmsTask::getTaskNo, q.getTaskNo());
            tw.like(StringUtils.hasText(q.getOrderNo()), DmsTask::getOrderNo, q.getOrderNo());
            tw.like(StringUtils.hasText(q.getCustomerName()), DmsTask::getCustomerName, q.getCustomerName());
            tw.eq(q.getOrderType() != null, DmsTask::getOrderType, q.getOrderType());
            // 「接单配送员」= 任务上的骑手（抢单/强制分配/竞价结算都会回写该列）
            tw.eq(q.getRiderId() != null, DmsTask::getRiderId, q.getRiderId());
            // 「配送线路 / 配送区域」= 任务上的线路档案引用与区域快照（《线路开发文档》主数据）
            tw.eq(q.getRouteId() != null, DmsTask::getRouteId, q.getRouteId());
            tw.like(StringUtils.hasText(q.getRouteArea()), DmsTask::getRouteArea, q.getRouteArea());
            if (StringUtils.hasText(q.getKeyword())) {
                String kw = q.getKeyword();
                tw.and(w -> w.like(DmsTask::getTaskNo, kw)
                        .or().like(DmsTask::getOrderNo, kw)
                        .or().like(DmsTask::getCustomerName, kw));
            }
            List<Long> taskIds = taskMapper.selectList(tw).stream()
                    .map(DmsTask::getId).collect(Collectors.toList());
            if (taskIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(DmsOrderPool::getTaskId, taskIds);
        }

        // ── 池侧条件 ──
        wrapper.eq(q.getPoolStatus() != null, DmsOrderPool::getPoolStatus, q.getPoolStatus());
        wrapper.eq(q.getBidEnabled() != null, DmsOrderPool::getBidEnabled, q.getBidEnabled());
        wrapper.ge(q.getFeeMin() != null, DmsOrderPool::getDeliveryFee, q.getFeeMin());
        wrapper.le(q.getFeeMax() != null, DmsOrderPool::getDeliveryFee, q.getFeeMax());
        LocalDateTime start = parseDateTimeStart(q.getPublishTimeStart());
        LocalDateTime end = parseDateTimeEnd(q.getPublishTimeEnd());
        wrapper.ge(start != null, DmsOrderPool::getPublishedTime, start);
        wrapper.le(end != null, DmsOrderPool::getPublishedTime, end);
        wrapper.orderByDesc(DmsOrderPool::getPublishedTime).orderByDesc(DmsOrderPool::getId);

        Page<DmsOrderPool> poolPage = orderPoolMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<OrderPoolRowVO> result = new Page<>(pageNum, pageSize, poolPage.getTotal());
        List<DmsOrderPool> pools = poolPage.getRecords();
        if (pools.isEmpty()) {
            result.setRecords(new ArrayList<>());
            return result;
        }

        // ── 批量联查任务与配送员（含「已接单」池的接单配送员：取中标/抢单骑手）──
        List<Long> taskIds = pools.stream().map(DmsOrderPool::getTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, DmsTask> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            for (DmsTask t : taskMapper.selectBatchIds(taskIds)) {
                taskMap.put(t.getId(), t);
            }
        }
        List<Long> riderIds = taskMap.values().stream().map(DmsTask::getRiderId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, DmsRider> riderMap = new HashMap<>();
        if (!riderIds.isEmpty()) {
            for (DmsRider r : riderMapper.selectBatchIds(riderIds)) {
                riderMap.put(r.getId(), r);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<OrderPoolRowVO> rows = new ArrayList<>();
        for (DmsOrderPool pool : pools) {
            DmsTask task = pool.getTaskId() == null ? null : taskMap.get(pool.getTaskId());
            OrderPoolRowVO vo = new OrderPoolRowVO();
            vo.setId(pool.getId());
            vo.setTaskId(pool.getTaskId());
            vo.setDeliveryFee(pool.getDeliveryFee());
            vo.setBidEnabled(pool.getBidEnabled());
            vo.setBidStartPrice(pool.getBidStartPrice());
            vo.setBidCurrentPrice(pool.getBidCurrentPrice());
            vo.setBidCount(pool.getBidCount());
            vo.setPoolStatus(pool.getPoolStatus());
            vo.setPoolStatusText(poolStatusText(pool.getPoolStatus()));
            vo.setPublishedTime(pool.getPublishedTime());
            vo.setBidStartTime(pool.getBidStartTime());
            vo.setExpireTime(pool.getBidEndTime());
            vo.setOfflineReason(pool.getOfflineReason());
            vo.setOfflineTime(pool.getOfflineTime());
            vo.setCreateTime(pool.getCreateTime());
            if (pool.getBidEndTime() != null
                    && Objects.equals(pool.getPoolStatus(), PoolStatusEnum.BIDDING.getValue())) {
                vo.setRemainSeconds(Math.max(0, Duration.between(now, pool.getBidEndTime()).getSeconds()));
            }
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
                vo.setOrderNo(task.getOrderNo());
                vo.setOrderType(task.getOrderType());
                vo.setOrderTypeText(orderTypeText(task.getOrderType()));
                vo.setCustomerName(task.getCustomerName());
                vo.setSourceAddress(task.getSourceAddress());
                vo.setCustomerAddress(task.getCustomerAddress());
                vo.setEstimatedDistance(task.getEstimatedDistance());
                vo.setGoodsAmount(task.getGoodsAmount());
                vo.setRouteId(task.getRouteId());
                vo.setRouteArea(task.getRouteArea());
                vo.setRemark(task.getRemark());
                vo.setRiderId(task.getRiderId());
                if (task.getRiderId() != null) {
                    DmsRider rider = riderMap.get(task.getRiderId());
                    vo.setRiderName(rider != null ? rider.getRealName() : task.getRiderName());
                }
            }
            rows.add(vo);
        }
        result.setRecords(rows);
        return result;
    }

    /**
     * 下架（待抢单 / 竞价中 → 已下架；已下架幂等）
     */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long poolId, String reason) {
        DmsOrderPool pool = getById(poolId);
        Integer status = pool.getPoolStatus();
        if (Objects.equals(status, PoolStatusEnum.REMOVED.getValue())) {
            return; // 幂等
        }
        if (Objects.equals(status, PoolStatusEnum.ACCEPTED.getValue())) {
            throw new BusinessException("已接单的订单不可下架，请先在配送任务中改派或取消");
        }
        if (Objects.equals(status, PoolStatusEnum.EXPIRED.getValue())) {
            throw new BusinessException("已过期的订单不可下架");
        }
        pool.setPoolStatus(PoolStatusEnum.REMOVED.getValue());
        // 下架原因留痕（此前只写日志，页面填的原因静默丢失、事后无法追溯）
        if (StringUtils.hasText(reason)) {
            pool.setOfflineReason(reason.length() > 200 ? reason.substring(0, 200) : reason);
        }
        pool.setOfflineTime(LocalDateTime.now());
        orderPoolMapper.updateById(pool);
        log.info("Order pool {} offline, reason={}", poolId, reason);
    }

    /**
     * 强制分配（调度主管定向指派）：池 → 已接单，任务 → 已分配并绑定骑手
     *
     * <p>幂等：已接单且同一骑手时直接返回；已接单但骑手不同则拒绝。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceAssign(Long poolId, Long riderId, String riderName) {
        DmsOrderPool pool = getById(poolId);
        DmsTask task = taskMapper.selectById(pool.getTaskId());
        if (task == null) {
            throw BusinessException.notFound("配送任务不存在");
        }
        if (Objects.equals(pool.getPoolStatus(), PoolStatusEnum.ACCEPTED.getValue())) {
            if (Objects.equals(task.getRiderId(), riderId)) {
                return; // 幂等：同一骑手重复指派
            }
            throw new BusinessException("该订单已被其他配送员接单，无法强制分配");
        }
        if (Objects.equals(pool.getPoolStatus(), PoolStatusEnum.REMOVED.getValue())
                || Objects.equals(pool.getPoolStatus(), PoolStatusEnum.EXPIRED.getValue())) {
            throw new BusinessException("已下架/已过期的订单不可强制分配，请重新发布到池");
        }
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        // 资质门控与抢单一致：无资质不派单
        kycService.assertEligible(riderId);

        pool.setPoolStatus(PoolStatusEnum.ACCEPTED.getValue());
        orderPoolMapper.updateById(pool);

        task.setRiderId(riderId);
        task.setRiderName(rider != null ? rider.getRealName() : riderName);
        task.setStatus(TaskStatusEnum.ASSIGNED.getValue());
        task.setDispatchType(2); // 手动
        taskMapper.updateById(task);
        log.info("Order pool {} force assigned to rider {}", poolId, riderId);
    }

    private static String poolStatusText(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "待抢单";
            case 1 -> "竞价中";
            case 2 -> "已接单";
            case 3 -> "已过期";
            case 4 -> "已下架";
            default -> String.valueOf(status);
        };
    }

    private static String orderTypeText(Integer orderType) {
        if (orderType == null) {
            return "-";
        }
        return switch (orderType) {
            case 1 -> "销售配送";
            case 2 -> "调拨";
            case 3 -> "退货";
            default -> String.valueOf(orderType);
        };
    }

    private static LocalDateTime parseDateTimeStart(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim().substring(0, 10)).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTimeEnd(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim().substring(0, 10)).atTime(23, 59, 59);
        } catch (Exception e) {
            return null;
        }
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
        // 只有「待分配」的任务才可入池：已分配/在途/终态的任务若入池被抢，
        // 会把任务上的骑手直接改写成抢单人（等于把别人手上的单抢走）
        if (!Objects.equals(task.getStatus(), TaskStatusEnum.PENDING.getValue())) {
            throw new BusinessException("任务 " + task.getTaskNo() + " 当前为「"
                    + TaskStatusEnum.fromValue(task.getStatus() == null ? 0 : task.getStatus()).getDescription()
                    + "」，只有「待分配」的任务才能发布到订单池");
        }
        // 防重复入池：「待抢单 / 竞价中 / 已接单」都算生效中的池记录。
        // 尤其「已接单」必须拦住——否则新池被抢单时会覆盖任务上的骑手，形成「一人已接单、他人又抢走」的双接单。
        Long active = orderPoolMapper.selectCount(new LambdaQueryWrapper<DmsOrderPool>()
                .eq(DmsOrderPool::getTaskId, taskId)
                .in(DmsOrderPool::getPoolStatus,
                        PoolStatusEnum.PENDING_GRAB.getValue(), PoolStatusEnum.BIDDING.getValue(),
                        PoolStatusEnum.ACCEPTED.getValue()));
        if (active != null && active > 0) {
            throw new BusinessException("任务 " + task.getTaskNo() + " 已有生效中的订单池记录（待抢单/竞价中/已接单），请勿重复发布");
        }

        DmsOrderPool pool = new DmsOrderPool();
        pool.setTaskId(taskId);
        // 未指定配送费时沿用任务上的配送费（与页面提示「不填则沿用任务上的配送费」一致）
        pool.setDeliveryFee(deliveryFee != null ? deliveryFee : task.getDeliveryFee());
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
        // 参数边界：起拍价必须为正、时长 1~1440 分钟（否则会出现 0 元起拍 / 立即截止的竞价）
        if (startPrice == null || startPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("起拍价必须大于 0");
        }
        if (durationMinutes == null || durationMinutes <= 0 || durationMinutes > 1440) {
            throw new BusinessException("竞价时长必须在 1~1440 分钟之间");
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
     * 关闭竞价（竞价中 → 待抢单）：撤回本次竞价并作废全部报价
     *
     * <p>业务诉求（《订单池开发文档》§3.1 功能按钮「开启/关闭竞价」）：起拍价填错、
     * 或临时改回「先到先得」抢单模式时，需要把已发起的竞价撤回。
     * 报价按逻辑删除作废（保留审计痕迹），池上竞价字段清空、回到待抢单，可重新设置起拍价再开。</p>
     *
     * @return 本次作废的报价条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int disableBid(Long poolId) {
        DmsOrderPool pool = getById(poolId);
        if (!Objects.equals(pool.getPoolStatus(), PoolStatusEnum.BIDDING.getValue())) {
            throw new BusinessException("只有「竞价中」的订单才能关闭竞价");
        }
        List<DmsBid> bids = bidMapper.selectList(
                new LambdaQueryWrapper<DmsBid>().eq(DmsBid::getPoolId, poolId));
        for (DmsBid bid : bids) {
            bidMapper.deleteById(bid.getId());
        }
        // 注意：updateById 默认忽略 null 字段，清空竞价字段必须走 UpdateWrapper 显式 set(null)
        orderPoolMapper.update(null, new LambdaUpdateWrapper<DmsOrderPool>()
                .eq(DmsOrderPool::getId, poolId)
                .set(DmsOrderPool::getPoolStatus, PoolStatusEnum.PENDING_GRAB.getValue())
                .set(DmsOrderPool::getBidEnabled, 0)
                .set(DmsOrderPool::getBidCount, 0)
                .set(DmsOrderPool::getBidStartPrice, null)
                .set(DmsOrderPool::getBidCurrentPrice, null)
                .set(DmsOrderPool::getBidStartTime, null)
                .set(DmsOrderPool::getBidEndTime, null));
        log.info("Bidding disabled for pool {}, {} bids voided", poolId, bids.size());
        return bids.size();
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
        // 「无资质不接单」：实名认证未通过 / 证照过期一律拒绝抢单
        kycService.assertEligible(riderId);

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
            // 快照配送员姓名，保证台账「司机名称」列有值（与强制分配/竞价结算一致）
            task.setRiderName(rider.getRealName() != null ? rider.getRealName() : riderName);
            task.setStatus(TaskStatusEnum.ACCEPTED.getValue());
            task.setDispatchType(3); // 抢单
            taskMapper.updateById(task);
        }

        log.info("Rider {} ({}) grabbed order pool {}", riderId, riderName, poolId);
    }

    /**
     * 批量过期已到竞价截止时间的订单
     *
     * @return 本次过期的池数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int expirePools() {
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
        return expiredPools.size();
    }
}
