package cn.aiedge.dms.task.service;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.dto.BatchResultVO;
import cn.aiedge.dms.task.dto.DmsTaskDetailDTO;
import cn.aiedge.dms.task.dto.DmsTaskItemRowDTO;
import cn.aiedge.dms.task.dto.DmsTaskQuery;
import cn.aiedge.dms.task.dto.DmsTaskSaveDTO;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.entity.DmsTaskDoc;
import cn.aiedge.dms.task.entity.DmsTaskItem;
import cn.aiedge.dms.task.entity.DmsTaskLog;
import cn.aiedge.dms.task.mapper.DmsTaskDocMapper;
import cn.aiedge.dms.task.mapper.DmsTaskItemMapper;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 配送任务（配送单）服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    /** 配送单号前缀（《配送单开发文档》§4.1 统一号段：PSD-YYYYMMDD-序号） */
    private static final String NO_PREFIX = "PSD";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** 在途任务状态（已分配/已接单/取货中/配送中）——负载与超时口径与 DispatchService 一致 */
    private static final List<Integer> ACTIVE_STATUS = List.of(
            TaskStatusEnum.ASSIGNED.getValue(), TaskStatusEnum.ACCEPTED.getValue(),
            TaskStatusEnum.PICKING_UP.getValue(), TaskStatusEnum.DELIVERING.getValue());

    private final DmsTaskMapper taskMapper;
    private final DmsTaskItemMapper taskItemMapper;
    private final DmsTaskDocMapper taskDocMapper;
    private final SysUserMapper sysUserMapper;
    private final DmsRiderMapper riderMapper;
    private final TaskLogService taskLogService;

    // ═══════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════

    /**
     * 分页查询配送任务
     */
    public IPage<DmsTask> page(Page<DmsTask> page, LambdaQueryWrapper<DmsTask> wrapper) {
        return taskMapper.selectPage(page, wrapper);
    }

    /**
     * 配送单多条件分页（按单据视图）
     *
     * <p>《调度任务开发文档》§3.2 调度工作台需要的「配送员姓名/电话、车辆、负载、距目的地、是否超时」
     * 由 {@link #enrich(List)} 联查/聚合填充到非持久化字段，前端一屏拿全，免 N+1 请求。</p>
     */
    public Page<DmsTask> pageQuery(DmsTaskQuery query, int pageNum, int pageSize) {
        Page<DmsTask> page = new Page<>(pageNum, pageSize);
        Page<DmsTask> result = taskMapper.selectPage(page, buildWrapper(query));
        enrich(result.getRecords());
        return result;
    }

    /**
     * 调度工作台展示字段富化（配送员电话 / 在途负载 / 距目的地 / 超时标记）
     *
     * <p>只读聚合，每次分页 3 条 SQL 以内（配送员批量、负载批量、按记录算距离），不做逐行查询。</p>
     */
    public void enrich(List<DmsTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> riderIds = tasks.stream().map(DmsTask::getRiderId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsRider> riders = new LinkedHashMap<>();
        if (!riderIds.isEmpty()) {
            for (DmsRider rider : riderMapper.selectBatchIds(riderIds)) {
                riders.put(rider.getId(), rider);
            }
        }
        Map<Long, Integer> load = activeTaskCounts(riderIds);
        LocalDateTime now = LocalDateTime.now();
        for (DmsTask task : tasks) {
            DmsRider rider = task.getRiderId() == null ? null : riders.get(task.getRiderId());
            if (rider != null) {
                task.setRiderPhone(rider.getPhone());
                // 名称快照缺失（历史数据）时用档案名兜底，列表不再出现裸 ID
                if (!StringUtils.hasText(task.getRiderName())) {
                    task.setRiderName(rider.getRealName());
                }
            }
            task.setActiveTaskCount(task.getRiderId() == null ? 0 : load.getOrDefault(task.getRiderId(), 0));
            task.setDistanceKm(distanceKm(task, rider));
            task.setOverdue(isOverdue(task, now));
        }
    }

    /** 在途任务数（按配送员分组，一次 SQL） */
    private Map<Long, Integer> activeTaskCounts(Set<Long> riderIds) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        if (riderIds.isEmpty()) {
            return map;
        }
        List<DmsTask> active = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getRiderId, riderIds)
                .in(DmsTask::getStatus, ACTIVE_STATUS)
                .select(DmsTask::getRiderId, DmsTask::getId));
        for (DmsTask task : active) {
            map.merge(task.getRiderId(), 1, Integer::sum);
        }
        return map;
    }

    /** 距目的地直线距离（km）：配送员当前位置 → 收货地址坐标；任一侧无坐标返回 null（前端显示 -） */
    private BigDecimal distanceKm(DmsTask task, DmsRider rider) {
        if (rider == null || rider.getCurrentLat() == null || rider.getCurrentLng() == null
                || task.getCustomerLat() == null || task.getCustomerLng() == null) {
            return null;
        }
        double meters = GeoUtils.distanceMeters(
                rider.getCurrentLat().doubleValue(), rider.getCurrentLng().doubleValue(),
                task.getCustomerLat().doubleValue(), task.getCustomerLng().doubleValue());
        return BigDecimal.valueOf(meters / 1000d).setScale(2, RoundingMode.HALF_UP);
    }

    /** 超时口径：在途（已分配~配送中）且已过要求送达时间 */
    private boolean isOverdue(DmsTask task, LocalDateTime now) {
        return task.getStatus() != null && ACTIVE_STATUS.contains(task.getStatus())
                && task.getDeadlineTime() != null && task.getDeadlineTime().isBefore(now);
    }

    /**
     * 配送单多条件分页（按明细视图：一个商品行 = 一行）
     *
     * <p>分页按「配送任务」计，明细在页内展开；无明细的任务保留一行（明细列留空），
     * 保证台账不丢单。</p>
     */
    public Page<DmsTaskItemRowDTO> pageDetail(DmsTaskQuery query, int pageNum, int pageSize) {
        Page<DmsTask> taskPage = pageQuery(query, pageNum, pageSize);
        List<DmsTask> tasks = taskPage.getRecords();
        Page<DmsTaskItemRowDTO> result = new Page<>(pageNum, pageSize, taskPage.getTotal());
        if (tasks.isEmpty()) {
            result.setRecords(new ArrayList<>());
            return result;
        }
        List<Long> taskIds = tasks.stream().map(DmsTask::getId).collect(Collectors.toList());
        List<DmsTaskItem> items = taskItemMapper.selectList(new LambdaQueryWrapper<DmsTaskItem>()
                .in(DmsTaskItem::getTaskId, taskIds)
                .orderByAsc(DmsTaskItem::getTaskId)
                .orderByAsc(DmsTaskItem::getLineNo));
        Map<Long, List<DmsTaskItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(DmsTaskItem::getTaskId, LinkedHashMap::new, Collectors.toList()));

        List<DmsTaskItemRowDTO> rows = new ArrayList<>();
        for (DmsTask task : tasks) {
            List<DmsTaskItem> lines = grouped.get(task.getId());
            if (lines == null || lines.isEmpty()) {
                rows.add(toRow(task, null));
            } else {
                for (DmsTaskItem line : lines) {
                    rows.add(toRow(task, line));
                }
            }
        }
        result.setRecords(rows);
        return result;
    }

    /**
     * 导出行（不分页）
     */
    public List<DmsTask> exportList(DmsTaskQuery query) {
        return taskMapper.selectList(buildWrapper(query)
                .orderByDesc(DmsTask::getDeliveryDate)
                .orderByDesc(DmsTask::getCreateTime));
    }

    private LambdaQueryWrapper<DmsTask> buildWrapper(DmsTaskQuery query) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            query = new DmsTaskQuery();
        }
        wrapper.like(StringUtils.hasText(query.getTaskNo()), DmsTask::getTaskNo, query.getTaskNo());
        wrapper.like(StringUtils.hasText(query.getSourceBillNo()), DmsTask::getSourceBillNo, query.getSourceBillNo());
        wrapper.like(StringUtils.hasText(query.getOrderNo()), DmsTask::getOrderNo, query.getOrderNo());
        wrapper.like(StringUtils.hasText(query.getCustomerName()), DmsTask::getCustomerName, query.getCustomerName());
        wrapper.like(StringUtils.hasText(query.getRouteArea()), DmsTask::getRouteArea, query.getRouteArea());
        wrapper.like(StringUtils.hasText(query.getCreatorName()), DmsTask::getCreatorName, query.getCreatorName());
        wrapper.like(StringUtils.hasText(query.getRemark()), DmsTask::getRemark, query.getRemark());
        if (StringUtils.hasText(query.getReceiverKeyword())) {
            String kw = query.getReceiverKeyword();
            wrapper.and(w -> w.like(DmsTask::getCustomerPhone, kw)
                    .or().like(DmsTask::getCustomerAddress, kw));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(DmsTask::getTaskNo, kw)
                    .or().like(DmsTask::getOrderNo, kw)
                    .or().like(DmsTask::getCustomerName, kw)
                    .or().like(DmsTask::getSourceBillNo, kw));
        }
        boolean explicitStatus = query.getStatusList() != null && !query.getStatusList().isEmpty();
        if (explicitStatus) {
            wrapper.in(DmsTask::getStatus, query.getStatusList());
        }
        // 「显示红冲」未勾选时隐藏已取消任务（对标：被红冲的任务/单据默认隐藏）；显式按状态筛选时以筛选为准
        if (!Boolean.TRUE.equals(query.getShowRed()) && !explicitStatus) {
            wrapper.ne(DmsTask::getStatus, TaskStatusEnum.CANCELLED.getValue());
        }
        wrapper.eq(query.getRiderId() != null, DmsTask::getRiderId, query.getRiderId());
        wrapper.eq(query.getVehicleId() != null, DmsTask::getVehicleId, query.getVehicleId());
        wrapper.eq(query.getDeliverymanId() != null, DmsTask::getDeliverymanId, query.getDeliverymanId());
        wrapper.eq(query.getRouteId() != null, DmsTask::getRouteId, query.getRouteId());
        wrapper.eq(query.getCustomerId() != null, DmsTask::getCustomerId, query.getCustomerId());
        wrapper.eq(query.getPriority() != null, DmsTask::getPriority, query.getPriority());
        wrapper.eq(query.getOrderType() != null, DmsTask::getOrderType, query.getOrderType());
        // 有无配送员：仅看未分配
        // 有无配送员：true=仅未分配，false=仅已分配（两个方向都要落到 SQL，否则「仅已分配」等于不筛）
        if (Boolean.TRUE.equals(query.getUnassigned())) {
            wrapper.isNull(DmsTask::getRiderId);
        } else if (Boolean.FALSE.equals(query.getUnassigned())) {
            wrapper.isNotNull(DmsTask::getRiderId);
        }
        // 是否异常：true=仅异常(status=8)，false=排除异常
        if (Boolean.TRUE.equals(query.getAbnormal())) {
            wrapper.eq(DmsTask::getStatus, TaskStatusEnum.EXCEPTION.getValue());
        } else if (Boolean.FALSE.equals(query.getAbnormal())) {
            wrapper.ne(DmsTask::getStatus, TaskStatusEnum.EXCEPTION.getValue());
        }
        // 超时在途：在途状态 且 已过要求送达时间
        if (Boolean.TRUE.equals(query.getOverdue())) {
            wrapper.in(DmsTask::getStatus, ACTIVE_STATUS)
                    .isNotNull(DmsTask::getDeadlineTime)
                    .lt(DmsTask::getDeadlineTime, LocalDateTime.now());
        }
        LocalDate dateStart = parseDate(query.getDeliveryDateStart());
        LocalDate dateEnd = parseDate(query.getDeliveryDateEnd());
        wrapper.ge(dateStart != null, DmsTask::getDeliveryDate, dateStart);
        wrapper.le(dateEnd != null, DmsTask::getDeliveryDate, dateEnd);
        LocalDateTime createStart = parseDateTimeStart(query.getCreateTimeStart());
        LocalDateTime createEnd = parseDateTimeEnd(query.getCreateTimeEnd());
        wrapper.ge(createStart != null, DmsTask::getCreateTime, createStart);
        wrapper.le(createEnd != null, DmsTask::getCreateTime, createEnd);
        applySort(wrapper, query);
        return wrapper;
    }

    /**
     * 服务端排序（白名单，未命中回落默认「指定配送日期 → 制单时间」倒序）
     *
     * <p>列名来自前端表头，直接拼 SQL 会有注入风险，故只认白名单键。</p>
     */
    private void applySort(LambdaQueryWrapper<DmsTask> wrapper, DmsTaskQuery query) {
        String field = query.getSortField();
        boolean asc = !"desc".equalsIgnoreCase(query.getSortOrder());
        if (StringUtils.hasText(field)) {
            switch (field) {
                case "priority" -> wrapper.orderBy(true, asc, DmsTask::getPriority);
                case "goodsAmount" -> wrapper.orderBy(true, asc, DmsTask::getGoodsAmount);
                case "totalQuantity" -> wrapper.orderBy(true, asc, DmsTask::getTotalQuantity);
                case "deliveryFee" -> wrapper.orderBy(true, asc, DmsTask::getDeliveryFee);
                case "deadlineTime" -> wrapper.orderBy(true, asc, DmsTask::getDeadlineTime);
                case "dispatchTime" -> wrapper.orderBy(true, asc, DmsTask::getDispatchTime);
                case "completedTime" -> wrapper.orderBy(true, asc, DmsTask::getCompletedTime);
                case "deliveryDate" -> wrapper.orderBy(true, asc, DmsTask::getDeliveryDate);
                case "createTime" -> wrapper.orderBy(true, asc, DmsTask::getCreateTime);
                case "taskNo" -> wrapper.orderBy(true, asc, DmsTask::getTaskNo);
                case "status" -> wrapper.orderBy(true, asc, DmsTask::getStatus);
                default -> { /* 非白名单列：走默认排序 */ }
            }
        }
        wrapper.orderByDesc(DmsTask::getDeliveryDate).orderByDesc(DmsTask::getCreateTime);
    }

    // ═══════════════════════════════════════════════
    // 详情
    // ═══════════════════════════════════════════════

    /**
     * 根据 ID 获取配送任务，不存在则抛异常
     */
    public DmsTask getById(Long id) {
        DmsTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("配送任务不存在");
        }
        return task;
    }

    /**
     * 配送单详情（头 + 商品明细）
     */
    public DmsTaskDetailDTO getDetail(Long id) {
        DmsTask task = getById(id);
        DmsTaskDetailDTO dto = new DmsTaskDetailDTO();
        BeanUtils.copyProperties(task, dto);
        dto.setItems(getItems(id));
        dto.setSourceDocs(getSourceDocs(id));
        return dto;
    }

    /**
     * 获取指定配送单的商品明细（无上游单据的临时配送才有；有上游单据时明细由上游单据穿透查看）
     */
    public List<DmsTaskItem> getItems(Long taskId) {
        return taskItemMapper.selectList(new LambdaQueryWrapper<DmsTaskItem>()
                .eq(DmsTaskItem::getTaskId, taskId)
                .orderByAsc(DmsTaskItem::getLineNo));
    }

    /**
     * 获取配送单的来源上游单据（表头数量/金额/重量/体积的聚合来源）
     */
    public List<DmsTaskDoc> getSourceDocs(Long taskId) {
        return taskDocMapper.selectList(new LambdaQueryWrapper<DmsTaskDoc>()
                .eq(DmsTaskDoc::getTaskId, taskId)
                .orderByAsc(DmsTaskDoc::getId));
    }

    /** 整体替换来源上游单据（对标「任务 ↔ 单据」1:N 关系） */
    private void replaceSourceDocs(Long taskId, List<DmsTaskDoc> docs) {
        taskDocMapper.delete(new LambdaQueryWrapper<DmsTaskDoc>().eq(DmsTaskDoc::getTaskId, taskId));
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (DmsTaskDoc doc : docs) {
            doc.setId(null);
            doc.setTaskId(taskId);
            doc.setTenantId(null);
            taskDocMapper.insert(doc);
        }
    }

    // ═══════════════════════════════════════════════
    // 保存
    // ═══════════════════════════════════════════════

    /**
     * 创建配送任务，初始状态为待分配（保留旧入口）
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsTask create(DmsTask task) {
        task.setStatus(TaskStatusEnum.PENDING.getValue());
        if (!StringUtils.hasText(task.getTaskNo())) {
            task.setTaskNo(generateTaskNo());
        }
        if (task.getPrintCount() == null) {
            task.setPrintCount(0);
        }
        taskMapper.insert(task);
        log.info("Task created: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return task;
    }

    /**
     * 保存配送单（头 + 商品明细）
     *
     * <p>单号权威在后端号段：沿用前端从 /next-no 取到的号码；同号已存在或为空则重新分配。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsTask save(DmsTaskSaveDTO dto) {
        DmsTask task;
        boolean isCreate = dto.getId() == null;
        if (isCreate) {
            task = new DmsTask();
            task.setStatus(TaskStatusEnum.PENDING.getValue());
        } else {
            task = getById(dto.getId());
            assertEditable(task);
        }

        task.setDeliveryDate(dto.getDeliveryDate() != null ? dto.getDeliveryDate() : LocalDate.now());
        task.setOrderType(dto.getOrderType() != null ? dto.getOrderType() : 1);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        task.setOrderNo(dto.getOrderNo());
        task.setSourceBillNo(dto.getSourceBillNo());
        task.setSourceWarehouseId(dto.getSourceWarehouseId());
        task.setSourceAddress(dto.getSourceAddress());
        task.setCustomerId(dto.getCustomerId());
        task.setCustomerName(dto.getCustomerName());
        task.setCustomerPhone(dto.getCustomerPhone());
        task.setCustomerAddress(dto.getCustomerAddress());
        task.setDeadlineTime(dto.getDeadlineTime());
        task.setRouteId(dto.getRouteId());
        task.setRouteArea(dto.getRouteArea());
        task.setRiderId(dto.getRiderId());
        task.setRiderName(dto.getRiderName());
        task.setVehicleId(dto.getVehicleId());
        task.setVehicleName(dto.getVehicleName());
        task.setDeliverymanId(dto.getDeliverymanId());
        task.setDeliverymanName(dto.getDeliverymanName());
        task.setDeliveryFee(nvl(dto.getDeliveryFee()));
        task.setCollectOnDelivery(nvl(dto.getCollectOnDelivery()));
        if (dto.getEstimatedDistance() != null) {
            task.setEstimatedDistance(dto.getEstimatedDistance());
        }
        task.setRemark(dto.getRemark());
        if (StringUtils.hasText(dto.getTaskNo())) {
            task.setTaskNo(dto.getTaskNo());
        }
        if (!StringUtils.hasText(task.getTaskNo())
                || (isCreate && lambdaCountByTaskNo(task.getTaskNo()) > 0)) {
            task.setTaskNo(generateTaskNo());
        }
        if (task.getPrintCount() == null) {
            task.setPrintCount(0);
        }
        if (isCreate) {
            // 制单人以登录用户为准（前端传值仅作兜底）
            task.setCreatorName(StringUtils.hasText(currentUserName()) && !"系统".equals(currentUserName())
                    ? currentUserName()
                    : (StringUtils.hasText(dto.getCreatorName()) ? dto.getCreatorName() : currentUserName()));
        }

        // ═══ 口径分流（业界：配送单=运输执行单，货值与库存口径归上游单据） ═══
        // ① 有来源上游单据（销售出库单等）→ 表头发货数量/金额/重量/体积一律由上游单据聚合，
        //    本单不再录入货物金额，商品明细改为「查看」时穿透上游单据；
        // ② 无来源单据（临时配送）→ 保留本单货物明细（定位为装载/货物描述）。
        List<DmsTaskDoc> sourceDocs = dto.getSourceDocs() == null ? new ArrayList<>() : dto.getSourceDocs();
        boolean bySourceDocs = !sourceDocs.isEmpty();

        // 明细 → 合计（仅临时配送场景生效；有上游单据时 items 为空）
        List<DmsTaskItem> items = dto.getItems() == null ? new ArrayList<>() : dto.getItems();
        BigDecimal totalQty = ZERO;
        BigDecimal totalAmount = ZERO;
        BigDecimal totalWeight = ZERO;
        BigDecimal totalVolume = ZERO;
        int lineNo = 1;
        for (DmsTaskItem item : items) {
            item.setId(null);
            item.setTaskId(null);
            item.setLineNo(lineNo++);
            BigDecimal qty = nvl(item.getQuantity());
            BigDecimal price = nvl(item.getUnitPrice());
            item.setQuantity(qty);
            item.setUnitPrice(price);
            item.setAmount(qty.multiply(price).setScale(4, java.math.RoundingMode.HALF_UP));
            item.setWeight(nvl(item.getWeight()));
            item.setVolume(nvl(item.getVolume()));
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(item.getAmount());
            totalWeight = totalWeight.add(item.getWeight());
            totalVolume = totalVolume.add(item.getVolume());
        }
        task.setTotalItems(items.size());

        if (bySourceDocs) {
            BigDecimal docQty = ZERO;
            BigDecimal docAmount = ZERO;
            BigDecimal docWeight = ZERO;
            BigDecimal docVolume = ZERO;
            int docBox = 0;
            List<String> docNos = new ArrayList<>();
            for (DmsTaskDoc doc : sourceDocs) {
                doc.setId(null);
                doc.setTaskId(null);
                doc.setQuantity(nvl(doc.getQuantity()));
                doc.setAmount(nvl(doc.getAmount()));
                doc.setWeight(nvl(doc.getWeight()));
                doc.setVolume(nvl(doc.getVolume()));
                doc.setBoxCount(doc.getBoxCount() == null ? 0 : doc.getBoxCount());
                if (StringUtils.hasText(doc.getDocNo())) {
                    docNos.add(doc.getDocNo().trim());
                }
                docQty = docQty.add(doc.getQuantity());
                docAmount = docAmount.add(doc.getAmount());
                docWeight = docWeight.add(doc.getWeight());
                docVolume = docVolume.add(doc.getVolume());
                docBox += doc.getBoxCount();
            }
            task.setTotalQuantity(docQty);
            task.setGoodsAmount(docAmount);
            task.setTotalWeight(docWeight);
            task.setTotalVolume(docVolume);
            // 配送单量 = 上游单据数（真实计数，不再靠字符串解析）
            task.setOrderCount(docNos.size());
            if (!docNos.isEmpty()) {
                // 回写来源单据编号，兼容旧字段展示与「配送单据编号」检索
                task.setSourceBillNo(String.join(",", docNos));
            }
            // 装箱数量：默认取上游单据箱数合计，允许表头手工覆盖
            task.setBoxQuantity(dto.getBoxQuantity() != null
                    ? nvl(dto.getBoxQuantity())
                    : BigDecimal.valueOf(docBox));
        } else {
            task.setTotalQuantity(totalQty);
            task.setGoodsAmount(totalAmount);
            task.setTotalWeight(totalWeight);
            task.setTotalVolume(totalVolume);
            // 配送单量：本任务承载的来源单据数（按来源单据编号去重，无来源单时为 1）
            task.setOrderCount(countSourceBills(task.getSourceBillNo()));
            task.setBoxQuantity(nvl(dto.getBoxQuantity()));
        }

        // 退货类型的配送单：带回的退货计入退货三列（真实来源，非估算）
        BigDecimal returnBaseQty = bySourceDocs ? nvl(task.getTotalQuantity()) : totalQty;
        BigDecimal returnBaseAmount = bySourceDocs ? nvl(task.getGoodsAmount()) : totalAmount;
        if (Objects.equals(task.getOrderType(), 3)) {
            task.setReturnOrderCount(bySourceDocs ? Math.max(task.getOrderCount() == null ? 0 : task.getOrderCount(), 1) : 1);
            task.setReturnQuantity(returnBaseQty);
            task.setReturnAmount(returnBaseAmount);
        } else if (task.getReturnOrderCount() == null) {
            task.setReturnOrderCount(0);
            task.setReturnQuantity(ZERO);
            task.setReturnAmount(ZERO);
        }
        task.setDepositAmount(nvl(dto.getDepositAmount()));

        if (isCreate) {
            taskMapper.insert(task);
        } else {
            taskMapper.updateById(task);
        }
        // 有上游单据：单据明细全部由上游穿透，本单不再保存商品明细
        replaceSourceDocs(task.getId(), bySourceDocs ? sourceDocs : new ArrayList<>());
        replaceItems(task.getId(), bySourceDocs ? new ArrayList<>() : items);
        log.info("Task saved: id={}, taskNo={}, sourceDocs={}, items={}",
                task.getId(), task.getTaskNo(), sourceDocs.size(), bySourceDocs ? 0 : items.size());
        return task;
    }

    /**
     * 更新配送任务（保留旧入口）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(DmsTask task) {
        getById(task.getId()); // 确保存在
        taskMapper.updateById(task);
        log.info("Task updated: id={}", task.getId());
    }

    /**
     * 删除配送单（逻辑删除；已进入配送执行的任务不允许删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsTask task = getById(id);
        Integer status = task.getStatus();
        boolean deletable = Objects.equals(status, TaskStatusEnum.PENDING.getValue())
                || Objects.equals(status, TaskStatusEnum.ASSIGNED.getValue())
                || Objects.equals(status, TaskStatusEnum.CANCELLED.getValue());
        if (!deletable) {
            throw BusinessException.badRequest("配送单已进入配送执行，不能删除");
        }
        taskItemMapper.delete(new LambdaQueryWrapper<DmsTaskItem>().eq(DmsTaskItem::getTaskId, id));
        taskDocMapper.delete(new LambdaQueryWrapper<DmsTaskDoc>().eq(DmsTaskDoc::getTaskId, id));
        taskMapper.deleteById(id);
        log.info("Task deleted: id={}, taskNo={}", id, task.getTaskNo());
    }

    /**
     * 审核（待分配 → 已分配）
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id) {
        DmsTask task = getById(id);
        if (!Objects.equals(task.getStatus(), TaskStatusEnum.PENDING.getValue())) {
            throw BusinessException.badRequest("仅「待分配」的配送单可审核");
        }
        task.setStatus(TaskStatusEnum.ASSIGNED.getValue());
        taskMapper.updateById(task);
        log.info("Task audited: id={}", id);
    }

    /**
     * 反审核（已分配 → 待分配；已被调度指派司机的不可反审核）
     */
    @Transactional(rollbackFor = Exception.class)
    public void unaudit(Long id) {
        DmsTask task = getById(id);
        if (!Objects.equals(task.getStatus(), TaskStatusEnum.ASSIGNED.getValue())) {
            throw BusinessException.badRequest("仅「已分配」的配送单可反审核");
        }
        if (task.getRiderId() != null) {
            throw BusinessException.badRequest("已指派配送司机的配送单不能反审核，请先取消指派");
        }
        task.setStatus(TaskStatusEnum.PENDING.getValue());
        taskMapper.updateById(task);
        log.info("Task unaudited: id={}", id);
    }

    /**
     * 打印次数 +1
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementPrintCount(Long id) {
        DmsTask task = getById(id);
        task.setPrintCount((task.getPrintCount() == null ? 0 : task.getPrintCount()) + 1);
        taskMapper.updateById(task);
    }

    // ═══════════════════════════════════════════════
    // 状态流转（原有能力）
    // ═══════════════════════════════════════════════

    /**
     * 更新任务状态，校验状态转换合法性
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer fromStatus, Integer toStatus) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        TaskStatusEnum target = TaskStatusEnum.fromValue(toStatus);

        if (!task.getStatus().equals(fromStatus)) {
            throw new BusinessException(
                    String.format("任务状态已变更，当前状态：%s，期望状态：%s",
                            current.getDescription(),
                            TaskStatusEnum.fromValue(fromStatus).getDescription()));
        }

        if (!current.canTransitionTo(target)) {
            throw new BusinessException(
                    String.format("无效的状态转换：%s -> %s", current.getDescription(), target.getDescription()));
        }

        task.setStatus(toStatus);
        taskMapper.updateById(task);
        log.info("Task {} status updated: {} -> {}", id, current.getDescription(), target.getDescription());
    }

    /**
     * 取消任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        cancel(id, null);
    }

    /**
     * 取消任务（带原因，写调度审计）
     *
     * <p>取消「已分配」任务时同步释放配送员占用（否则配送员被永久置为忙碌）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        if (!current.canTransitionTo(TaskStatusEnum.CANCELLED)) {
            throw new BusinessException("当前状态不可取消：" + current.getDescription());
        }
        Long fromRiderId = task.getRiderId();
        task.setStatus(TaskStatusEnum.CANCELLED.getValue());
        taskMapper.updateById(task);
        if (fromRiderId != null) {
            releaseRiderIfIdle(fromRiderId);
        }
        taskLogService.record(id, task.getTaskNo(), DmsTaskLog.ACTION_CANCEL,
                fromRiderId, null, StringUtils.hasText(reason) ? reason : "调度取消任务");
        log.info("Task {} cancelled", id);
    }

    /**
     * 标记任务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void markException(Long id) {
        markException(id, null);
    }

    /**
     * 标记任务异常（带原因，写调度审计）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markException(Long id, String reason) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        if (!current.canTransitionTo(TaskStatusEnum.EXCEPTION)) {
            throw new BusinessException("当前状态不可标记异常：" + current.getDescription());
        }
        task.setStatus(TaskStatusEnum.EXCEPTION.getValue());
        taskMapper.updateById(task);
        taskLogService.record(id, task.getTaskNo(), DmsTaskLog.ACTION_EXCEPTION,
                task.getRiderId(), task.getRiderId(),
                StringUtils.hasText(reason) ? reason : "调度标记异常，待人工介入");
        log.info("Task {} marked as exception", id);
    }

    /**
     * 批量取消（《调度任务开发文档》§3.4 功能按钮：批量取消）
     *
     * <p>逐单执行并逐单反馈：不可取消的任务（如已接单/已完成）只记失败原因，不影响其余单据。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchCancel(List<Long> taskIds, String reason) {
        BatchResultVO result = new BatchResultVO();
        if (taskIds == null || taskIds.isEmpty()) {
            return result;
        }
        result.setTotal(taskIds.size());
        for (Long taskId : taskIds) {
            DmsTask task = null;
            try {
                task = taskMapper.selectById(taskId);
                if (task == null) {
                    result.markFailed(taskId, null, "任务不存在或已删除");
                    continue;
                }
                cancel(taskId, reason);
                result.markSuccess();
            } catch (Exception e) {
                result.markFailed(taskId, task == null ? null : task.getTaskNo(), e.getMessage());
            }
        }
        log.info("批量取消完成: 成功 {}/{}", result.getSuccess(), result.getTotal());
        return result;
    }

    /**
     * 批量记录打印次数（《调度任务开发文档》§3.4：批量打印配送单后回写打印次数）
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchIncrementPrintCount(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long taskId : taskIds) {
            DmsTask task = taskMapper.selectById(taskId);
            if (task == null) {
                continue;
            }
            incrementPrintCount(taskId);
            count++;
        }
        return count;
    }

    /**
     * 占用配送员运力：置「忙碌(2)」（休息中不覆盖）
     *
     * <p>用于「已交付但退回在途」的场景：签收被驳回 / 待审核签收被删除 → 任务退回「配送中(4)」，
     * 该配送员名下重新有在途任务，需要重新占用（与 {@link #releaseRiderIfIdle} 配对，
     * 维持「空闲 ⟺ 名下无在途任务」的不变式，自动派单候选池口径才自洽）。</p>
     */
    public void markRiderBusy(Long riderId) {
        if (riderId == null) {
            return;
        }
        try {
            DmsRider rider = riderMapper.selectById(riderId);
            if (rider != null && !Objects.equals(rider.getStatus(), DmsConstants.RIDER_STATUS_REST)
                    && !Objects.equals(rider.getStatus(), DmsConstants.RIDER_STATUS_BUSY)) {
                rider.setStatus(DmsConstants.RIDER_STATUS_BUSY);
                riderMapper.updateById(rider);
                log.info("配送员运力重新占用: riderId={} → 忙碌", riderId);
            }
        } catch (Exception e) {
            log.warn("占用配送员运力失败: riderId={}", riderId, e);
        }
    }

    /**
     * 释放配送员占用：该配送员名下**再无在途任务**时回置「空闲」（休息中的不被覆盖）
     *
     * <p>任务进入终态（已完成 / 已取消）或改派给他人后**必须**调用一次，否则配送员会永久停在
     * 「忙碌(2)」；而派单策略默认 {@code dms.dispatch.require.online=true} 时「忙碌」不再进入
     * 自动派单候选池 —— 运力池会随完成单量单调收缩，最终「无人可派」。</p>
     *
     * <p>仅在途计数（已分配/已接单/取货中/配送中，与 {@link #ACTIVE_STATUS} 同口径）为 0 时回置，
     * 避免「取消其中一单」把名下还有多单在途的配送员误置空闲。</p>
     */
    public void releaseRiderIfIdle(Long riderId) {
        if (riderId == null) {
            return;
        }
        try {
            long active = taskMapper.selectCount(new LambdaQueryWrapper<DmsTask>()
                    .eq(DmsTask::getRiderId, riderId)
                    .in(DmsTask::getStatus, ACTIVE_STATUS));
            if (active > 0) {
                return;
            }
            DmsRider rider = riderMapper.selectById(riderId);
            if (rider != null && !Objects.equals(rider.getStatus(), DmsConstants.RIDER_STATUS_REST)) {
                rider.setStatus(DmsConstants.RIDER_STATUS_IDLE);
                riderMapper.updateById(rider);
                log.info("配送员运力已释放: riderId={}, 在途 0 单 → 空闲", riderId);
            }
        } catch (Exception e) {
            log.warn("释放配送员占用失败: riderId={}", riderId, e);
        }
    }

    /**
     * 根据骑手 ID 获取任务列表
     */
    public List<DmsTask> getByRiderId(Long riderId, Integer status) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsTask::getRiderId, riderId);
        if (status != null) {
            wrapper.eq(DmsTask::getStatus, status);
        }
        wrapper.orderByDesc(DmsTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    /**
     * 统计骑手活跃任务数量（已接单、取货中、配送中）
     */
    public long getActiveTaskCount(Long riderId) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsTask::getRiderId, riderId)
                .in(DmsTask::getStatus,
                        TaskStatusEnum.ACCEPTED.getValue(),
                        TaskStatusEnum.PICKING_UP.getValue(),
                        TaskStatusEnum.DELIVERING.getValue());
        return taskMapper.selectCount(wrapper);
    }

    // ═══════════════════════════════════════════════
    // 号段与内部工具
    // ═══════════════════════════════════════════════

    /**
     * 生成下一配送单号：PSD-YYYYMMDD-序号（三位序号）
     */
    public String generateTaskNo() {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String prefix = NO_PREFIX + "-" + dateStr + "-";
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DmsTask::getTaskNo, prefix)
                .eq(DmsTask::getDeleted, 0)
                .orderByDesc(DmsTask::getTaskNo)
                .last("LIMIT 1");
        DmsTask last = taskMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getTaskNo() != null) {
            String tail = last.getTaskNo().substring(last.getTaskNo().lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    private long lambdaCountByTaskNo(String taskNo) {
        return taskMapper.selectCount(new LambdaQueryWrapper<DmsTask>().eq(DmsTask::getTaskNo, taskNo));
    }

    private void replaceItems(Long taskId, List<DmsTaskItem> items) {
        taskItemMapper.delete(new LambdaQueryWrapper<DmsTaskItem>().eq(DmsTaskItem::getTaskId, taskId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (DmsTaskItem item : items) {
            item.setId(null);
            item.setTaskId(taskId);
            item.setLineNo(line++);
            item.setTenantId(null);
            taskItemMapper.insert(item);
        }
    }

    private void assertEditable(DmsTask task) {
        Integer status = task.getStatus();
        boolean editable = Objects.equals(status, TaskStatusEnum.PENDING.getValue())
                || Objects.equals(status, TaskStatusEnum.ASSIGNED.getValue());
        if (!editable) {
            throw BusinessException.badRequest("配送单已进入配送执行，不能修改");
        }
    }

    private int countSourceBills(String sourceBillNo) {
        if (!StringUtils.hasText(sourceBillNo)) {
            return 1;
        }
        Map<String, Boolean> distinct = new HashMap<>();
        for (String part : sourceBillNo.split("[,，、;；\\s]+")) {
            if (StringUtils.hasText(part)) {
                distinct.put(part.trim(), Boolean.TRUE);
            }
        }
        return Math.max(distinct.size(), 1);
    }

    private DmsTaskItemRowDTO toRow(DmsTask task, DmsTaskItem item) {
        DmsTaskItemRowDTO row = new DmsTaskItemRowDTO();
        row.setTaskId(task.getId());
        row.setTaskNo(task.getTaskNo());
        row.setDeliveryDate(task.getDeliveryDate());
        row.setStatus(task.getStatus());
        row.setOrderNo(task.getOrderNo());
        row.setCustomerName(task.getCustomerName());
        row.setRiderName(task.getRiderName());
        row.setVehicleName(task.getVehicleName());
        if (item != null) {
            row.setId(item.getId());
            row.setLineNo(item.getLineNo());
            row.setProductCode(item.getProductCode());
            row.setProductName(item.getProductName());
            row.setBarcode(item.getBarcode());
            row.setSpec(item.getSpec());
            row.setUnit(item.getUnit());
            row.setQuantity(item.getQuantity());
            row.setUnitPrice(item.getUnitPrice());
            row.setAmount(item.getAmount());
            row.setWeight(item.getWeight());
            row.setVolume(item.getVolume());
            row.setRemark(item.getRemark());
        }
        return row;
    }

    /** 当前登录用户真实姓名（昵称优先，其次用户名） */
    private String currentUserName() {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        if (userId == null) {
            return "系统";
        }
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                if (StringUtils.hasText(user.getNickname())) {
                    return user.getNickname();
                }
                if (StringUtils.hasText(user.getUsername())) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("查询当前用户姓名失败: userId={}", userId, e);
        }
        return String.valueOf(userId);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private static LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTimeStart(String text) {
        LocalDate date = parseDate(text);
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime parseDateTimeEnd(String text) {
        LocalDate date = parseDate(text);
        return date == null ? null : date.atTime(23, 59, 59);
    }

    /** 供导出排序使用（保留显式 Comparator 导入以支持自定义排序扩展） */
    static Comparator<DmsTask> byDeliveryDateDesc() {
        return Comparator.comparing(DmsTask::getDeliveryDate,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
