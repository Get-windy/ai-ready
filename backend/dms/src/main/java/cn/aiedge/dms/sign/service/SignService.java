package cn.aiedge.dms.sign.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.dms.common.enums.SignAuditStatusEnum;
import cn.aiedge.dms.common.enums.SignTypeEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.event.service.EventService;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.sign.dto.SignAuditDTO;
import cn.aiedge.dms.sign.dto.SignQueryDTO;
import cn.aiedge.dms.sign.dto.SignStatVO;
import cn.aiedge.dms.sign.dto.SignSubmitDTO;
import cn.aiedge.dms.sign.dto.SignVO;
import cn.aiedge.dms.sign.entity.DmsSign;
import cn.aiedge.dms.sign.mapper.DmsSignMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.task.service.TaskService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 签收服务（配送 → 配送跟踪 → 签收管理）
 *
 * <p>《签收管理开发文档》落地口径：</p>
 * <ul>
 *   <li>§3.4 审核流转：提交 → 待审核(0) → 通过(1) → 任务「已完成(6)」；驳回(2) → 任务退回「配送中(4)」允许重新签收；</li>
 *   <li>§3.6.1 四要素：照片 / 手写签名 / 定位 / 时间戳；<b>拒收必须拍照 + 备注</b>，<b>部分签收必须给实际数量</b>；</li>
 *   <li>§3.6.2 偏差阈值配置化：读《配送参数》{@code dms.sign.deviation.threshold}，超阈值只<b>标记待复核</b>、不拒绝签收；</li>
 *   <li>§3.6.5 任务状态联动：未审核通过前任务不得进入「已完成」；</li>
 *   <li>§3.6.9 幂等：同任务存在「待审核」记录时拒绝重复提交（驳回后可重新签收）。</li>
 * </ul>
 *
 * <p>联动出口：审核事件经 `dms_event_outbox` 外发（`SIGN_SUBMITTED / SIGN_APPROVED / SIGN_REJECTED`），
 * 消费方为《配送结算》（按签收计费）与《收款管理》（代收货款）。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {

    /** 偏差阈值配置键（《配送参数》） */
    public static final String CFG_DEVIATION_THRESHOLD = "dms.sign.deviation.threshold";

    /** 偏差阈值缺省值（米） */
    private static final int DEFAULT_THRESHOLD = 100;

    private static final String EVENT_SIGN_SUBMITTED = "SIGN_SUBMITTED";
    private static final String EVENT_SIGN_APPROVED = "SIGN_APPROVED";
    private static final String EVENT_SIGN_REJECTED = "SIGN_REJECTED";

    private final DmsSignMapper signMapper;
    private final DmsTaskMapper taskMapper;
    private final DmsRiderMapper riderMapper;
    private final SysUserMapper sysUserMapper;
    private final ConfigService configService;
    private final EventService eventService;
    /** 任务终态后的运力释放（审核通过 → 已完成：名下无在途单则回置空闲） */
    private final TaskService taskService;

    // ==================== 提交签收 ====================

    @Transactional(rollbackFor = Exception.class)
    public SignVO submit(SignSubmitDTO dto) {
        DmsTask task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + dto.getTaskId());
        }
        SignTypeEnum signType = SignTypeEnum.fromValue(dto.getSignType());
        if (signType == null) {
            throw new DmsBusinessException("非法签收类型: " + dto.getSignType());
        }
        // 幂等判断必须在状态判断之前：首次提交后任务已是「已签收(5)」，
        // 若先判任务状态，重复提交会被报成「任务状态不允许签收」，掩盖真实原因（重复提交）
        Long pending = signMapper.selectCount(new LambdaQueryWrapper<DmsSign>()
                .eq(DmsSign::getTaskId, task.getId())
                .eq(DmsSign::getAuditStatus, SignAuditStatusEnum.PENDING.getValue()));
        if (pending != null && pending > 0) {
            throw new DmsBusinessException("该任务已提交签收且待审核，请勿重复提交");
        }
        if (task.getStatus() == null || task.getStatus() != TaskStatusEnum.DELIVERING.getValue()) {
            throw new DmsBusinessException("任务状态不允许签收，当前状态: " + taskStatusText(task.getStatus()));
        }
        if (signType == SignTypeEnum.REJECT) {
            if (!StringUtils.hasText(dto.getPhotoUrls())) {
                throw new DmsBusinessException("拒收必须上传照片凭证");
            }
            if (!StringUtils.hasText(dto.getRemark())) {
                throw new DmsBusinessException("拒收必须填写拒收原因");
            }
        }
        if (signType == SignTypeEnum.PARTIAL
                && (dto.getActualQuantity() == null || dto.getActualQuantity().signum() < 0)) {
            throw new DmsBusinessException("部分签收必须填写实际签收数量");
        }

        // 客户坐标：司机端通常不掌握客户坐标（也不应自行回传），缺省回落到任务上的客户坐标快照。
        // 否则司机端提交时偏差恒为 0、「超阈值待复核」形同虚设（§3.6.2）。
        BigDecimal customerLat = dto.getCustomerLat() != null ? dto.getCustomerLat() : task.getCustomerLat();
        BigDecimal customerLng = dto.getCustomerLng() != null ? dto.getCustomerLng() : task.getCustomerLng();

        double threshold = resolveThreshold(dto.getDeviationThresh());
        double deviation = calculateDeviation(dto.getSignLat(), dto.getSignLng(), customerLat, customerLng);
        int warning = deviation > threshold ? 1 : 0;

        DmsSign sign = new DmsSign();
        sign.setTaskId(task.getId());
        sign.setTenantId(task.getTenantId());
        sign.setSignType(signType.getValue());
        sign.setPhotoUrls(dto.getPhotoUrls());
        sign.setSignatureUrl(dto.getSignatureUrl());
        sign.setSignLat(dto.getSignLat());
        sign.setSignLng(dto.getSignLng());
        sign.setCustomerLat(customerLat);
        sign.setCustomerLng(customerLng);
        sign.setLocationDeviation(BigDecimal.valueOf(deviation).setScale(2, RoundingMode.HALF_UP));
        sign.setLocationWarning(warning);
        sign.setActualQuantity(dto.getActualQuantity());
        sign.setPlannedQuantity(task.getTotalQuantity());
        sign.setDeviationThresh(BigDecimal.valueOf(threshold).setScale(2, RoundingMode.HALF_UP));
        sign.setRemark(dto.getRemark());
        sign.setSignTime(LocalDateTime.now());
        sign.setAuditStatus(SignAuditStatusEnum.PENDING.getValue());
        signMapper.insert(sign);

        // 任务：配送中(4) → 已签收(5)；审核通过后才置「已完成(6)」
        taskMapper.update(null, new LambdaUpdateWrapper<DmsTask>()
                .eq(DmsTask::getId, task.getId())
                .set(DmsTask::getStatus, TaskStatusEnum.SIGNED.getValue())
                .set(DmsTask::getDeliveryTime, LocalDateTime.now()));

        // 交付动作已完成 → 释放运力：审核是后台动作（可能滞后数小时），若不在此释放，
        // 配送员会在「忙碌」上一直等审核，自动派单候选池（只取空闲）实际被签收台账拖住。
        // 驳回/删除会把任务退回「配送中(4)」，那时再重新占用（见 audit / delete）。
        taskService.releaseRiderIfIdle(task.getRiderId());

        if (warning == 1) {
            log.warn("签收定位偏差超限: taskId={}, deviation={}米, 阈值={}米", task.getId(), deviation, threshold);
        }
        publishEvent(EVENT_SIGN_SUBMITTED, task, sign, null);
        log.info("签收提交成功: taskId={}, signType={}, deviation={}米", task.getId(), signType.getDescription(), deviation);
        return detail(sign.getId());
    }

    // ==================== 台账查询 ====================

    public Page<SignVO> page(SignQueryDTO query) {
        SignQueryDTO q = query != null ? query : new SignQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<DmsSign> entityPage = signMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<SignVO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(toVOList(entityPage.getRecords()));
        return result;
    }

    /** 导出用全量列表 */
    public List<SignVO> list(SignQueryDTO query) {
        return toVOList(signMapper.selectList(buildWrapper(query != null ? query : new SignQueryDTO())));
    }

    public SignVO detail(Long id) {
        DmsSign sign = signMapper.selectById(id);
        if (sign == null) {
            throw new DmsBusinessException("签收记录不存在: " + id);
        }
        return toVOList(List.of(sign)).get(0);
    }

    /** 按任务查最新一条签收（兼容旧接口 `GET /{taskId}`） */
    public SignVO getByTaskId(Long taskId) {
        DmsSign sign = signMapper.selectOne(new LambdaQueryWrapper<DmsSign>()
                .eq(DmsSign::getTaskId, taskId)
                .orderByDesc(DmsSign::getCreateTime)
                .last("LIMIT 1"));
        return sign == null ? null : toVOList(List.of(sign)).get(0);
    }

    /** 查询条件装配（§3.3） */
    private LambdaQueryWrapper<DmsSign> buildWrapper(SignQueryDTO q) {
        LambdaQueryWrapper<DmsSign> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            Set<Long> ids = matchTaskIds(w -> w.like(DmsTask::getTaskNo, kw)
                    .or().like(DmsTask::getOrderNo, kw)
                    .or().like(DmsTask::getCustomerName, kw));
            wrapper.and(w -> {
                w.like(DmsSign::getRemark, kw);
                if (!ids.isEmpty()) {
                    w.or().in(DmsSign::getTaskId, ids);
                }
            });
        }
        if (StringUtils.hasText(q.getTaskNo())) {
            wrapper.in(DmsSign::getTaskId, orNone(matchTaskIds(w -> w.like(DmsTask::getTaskNo, q.getTaskNo().trim()))));
        }
        if (StringUtils.hasText(q.getCustomerName())) {
            wrapper.in(DmsSign::getTaskId,
                    orNone(matchTaskIds(w -> w.like(DmsTask::getCustomerName, q.getCustomerName().trim()))));
        }
        if (q.getRiderId() != null) {
            wrapper.in(DmsSign::getTaskId, orNone(matchTaskIds(w -> w.eq(DmsTask::getRiderId, q.getRiderId()))));
        }
        wrapper.in(q.getSignTypes() != null && !q.getSignTypes().isEmpty(), DmsSign::getSignType, q.getSignTypes());
        wrapper.in(q.getAuditStatusList() != null && !q.getAuditStatusList().isEmpty(),
                DmsSign::getAuditStatus, q.getAuditStatusList());
        if (StringUtils.hasText(q.getSignTimeStart())) {
            wrapper.ge(DmsSign::getSignTime, LocalDate.parse(q.getSignTimeStart().trim()).atStartOfDay());
        }
        if (StringUtils.hasText(q.getSignTimeEnd())) {
            wrapper.le(DmsSign::getSignTime, LocalDate.parse(q.getSignTimeEnd().trim()).atTime(LocalTime.MAX));
        }
        if (Boolean.TRUE.equals(q.getOnlyWarning())) {
            wrapper.eq(DmsSign::getLocationWarning, 1);
        }
        if (q.getHasSignature() != null) {
            if (q.getHasSignature()) {
                wrapper.isNotNull(DmsSign::getSignatureUrl).ne(DmsSign::getSignatureUrl, "");
            } else {
                wrapper.and(w -> w.isNull(DmsSign::getSignatureUrl).or().eq(DmsSign::getSignatureUrl, ""));
            }
        }
        if (q.getHasPhoto() != null) {
            if (q.getHasPhoto()) {
                wrapper.isNotNull(DmsSign::getPhotoUrls)
                        .ne(DmsSign::getPhotoUrls, "").ne(DmsSign::getPhotoUrls, "[]");
            } else {
                wrapper.and(w -> w.isNull(DmsSign::getPhotoUrls)
                        .or().eq(DmsSign::getPhotoUrls, "").or().eq(DmsSign::getPhotoUrls, "[]"));
            }
        }
        applySort(wrapper, q);
        return wrapper;
    }

    /** 按任务条件反查任务ID集合（**不用 inSql**：手写 SQL 不带租户条件，避免跨租户漏行） */
    private Set<Long> matchTaskIds(Consumer<LambdaQueryWrapper<DmsTask>> consumer) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        consumer.accept(wrapper);
        wrapper.select(DmsTask::getId);
        return taskMapper.selectList(wrapper).stream()
                .map(DmsTask::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 空集合 → 恒假条件（`IN ()` 不是合法 SQL） */
    private List<Long> orNone(Set<Long> ids) {
        return ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids);
    }

    private void applySort(LambdaQueryWrapper<DmsSign> wrapper, SignQueryDTO q) {
        boolean asc = "asc".equalsIgnoreCase(q.getSortOrder());
        String field = q.getSortField() == null ? "" : q.getSortField();
        switch (field) {
            case "signTime" -> wrapper.orderBy(true, asc, DmsSign::getSignTime);
            case "locationDeviation" -> wrapper.orderBy(true, asc, DmsSign::getLocationDeviation);
            case "auditTime" -> wrapper.orderBy(true, asc, DmsSign::getAuditTime);
            case "createTime" -> wrapper.orderBy(true, asc, DmsSign::getCreateTime);
            default -> wrapper.orderByDesc(DmsSign::getSignTime);
        }
    }

    // ==================== 审核 ====================

    @Transactional(rollbackFor = Exception.class)
    public SignVO audit(Long id, SignAuditDTO dto) {
        DmsSign sign = signMapper.selectById(id);
        if (sign == null) {
            throw new DmsBusinessException("签收记录不存在: " + id);
        }
        SignAuditStatusEnum target = resolveAuditTarget(dto);
        SignAuditStatusEnum current = SignAuditStatusEnum.fromValue(sign.getAuditStatus());
        if (current == null || !current.canTransitionTo(target)) {
            throw new DmsBusinessException("该签收记录已审核（" + auditStatusText(sign.getAuditStatus()) + "），不可重复审核");
        }

        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        String auditorName = currentUserName(auditorId);
        signMapper.update(null, new LambdaUpdateWrapper<DmsSign>()
                .eq(DmsSign::getId, id)
                .set(DmsSign::getAuditStatus, target.getValue())
                .set(DmsSign::getAuditBy, auditorId)
                .set(DmsSign::getAuditByName, auditorName)
                .set(DmsSign::getAuditTime, LocalDateTime.now())
                .set(DmsSign::getAuditRemark, dto.getAuditRemark()));

        DmsTask task = taskMapper.selectById(sign.getTaskId());
        if (task != null) {
            if (target == SignAuditStatusEnum.APPROVED) {
                // 通过：已签收(5) → 已完成(6)
                taskMapper.update(null, new LambdaUpdateWrapper<DmsTask>()
                        .eq(DmsTask::getId, task.getId())
                        .set(DmsTask::getStatus, TaskStatusEnum.COMPLETED.getValue())
                        .set(DmsTask::getCompletedTime, LocalDateTime.now()));
                // 任务终态 → 释放运力：该配送员名下再无在途单据时回置「空闲」，否则其不再参与自动派单
                taskService.releaseRiderIfIdle(task.getRiderId());
            } else {
                // 驳回：退回配送中(4)，允许重新签收
                taskMapper.update(null, new LambdaUpdateWrapper<DmsTask>()
                        .eq(DmsTask::getId, task.getId())
                        .set(DmsTask::getStatus, TaskStatusEnum.DELIVERING.getValue()));
                // 任务回到在途 → 重新占用运力（提交签收时已释放；驳回说明交付未成立）
                taskService.markRiderBusy(task.getRiderId());
            }
        }
        publishEvent(target == SignAuditStatusEnum.APPROVED ? EVENT_SIGN_APPROVED : EVENT_SIGN_REJECTED,
                task, sign, dto.getAuditRemark());
        log.info("签收审核: id={}, taskId={}, 结论={}, 审核人={}", id, sign.getTaskId(),
                target.getDescription(), auditorName);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchAudit(List<Long> ids, SignAuditDTO dto) {
        if (ids == null || ids.isEmpty()) {
            throw new DmsBusinessException("请先勾选要审核的签收记录");
        }
        resolveAuditTarget(dto);
        int done = 0;
        for (Long id : new ArrayList<>(new LinkedHashSet<>(ids))) {
            audit(id, dto);
            done++;
        }
        return done;
    }

    /** 审核结论校验（驳回原因必填） */
    private SignAuditStatusEnum resolveAuditTarget(SignAuditDTO dto) {
        SignAuditStatusEnum target = SignAuditStatusEnum.fromValue(dto.getAuditStatus());
        if (target == null || target == SignAuditStatusEnum.PENDING) {
            throw new DmsBusinessException("审核结论只能是「通过」或「驳回」");
        }
        if (target == SignAuditStatusEnum.REJECTED && !StringUtils.hasText(dto.getAuditRemark())) {
            throw new DmsBusinessException("驳回必须填写驳回原因");
        }
        return target;
    }

    /**
     * 删除签收记录（仅「待审核」可删，用于误提交清理；已审核记录保留审计留痕）
     *
     * <p>删除后任务退回「配送中」，允许重新签收。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsSign sign = signMapper.selectById(id);
        if (sign == null) {
            throw new DmsBusinessException("签收记录不存在: " + id);
        }
        if (!Objects.equals(sign.getAuditStatus(), SignAuditStatusEnum.PENDING.getValue())) {
            throw new DmsBusinessException("已审核的签收记录不可删除（审计留痕）");
        }
        signMapper.update(null, new LambdaUpdateWrapper<DmsSign>()
                .eq(DmsSign::getId, id)
                .set(DmsSign::getDeleted, 1));
        DmsTask deletedTask = taskMapper.selectById(sign.getTaskId());
        taskMapper.update(null, new LambdaUpdateWrapper<DmsTask>()
                .eq(DmsTask::getId, sign.getTaskId())
                .set(DmsTask::getStatus, TaskStatusEnum.DELIVERING.getValue()));
        // 任务回到在途 → 重新占用运力（与「提交签收即释放」配对）
        if (deletedTask != null) {
            taskService.markRiderBusy(deletedTask.getRiderId());
        }
        log.info("删除签收记录: id={}, taskId={}", id, sign.getTaskId());
    }

    // ==================== 统计 / 导出 ====================

    public SignStatVO stat(SignQueryDTO query) {
        List<DmsSign> rows = signMapper.selectList(buildWrapper(query != null ? query : new SignQueryDTO()));
        SignStatVO vo = new SignStatVO();
        vo.setTotal(rows.size());
        vo.setPending(rows.stream().filter(r -> eq(r.getAuditStatus(), SignAuditStatusEnum.PENDING)).count());
        vo.setApproved(rows.stream().filter(r -> eq(r.getAuditStatus(), SignAuditStatusEnum.APPROVED)).count());
        vo.setRejected(rows.stream().filter(r -> eq(r.getAuditStatus(), SignAuditStatusEnum.REJECTED)).count());
        vo.setNormalCount(rows.stream().filter(r -> eq(r.getSignType(), SignTypeEnum.NORMAL)).count());
        vo.setPartialCount(rows.stream().filter(r -> eq(r.getSignType(), SignTypeEnum.PARTIAL)).count());
        vo.setRejectCount(rows.stream().filter(r -> eq(r.getSignType(), SignTypeEnum.REJECT)).count());
        vo.setWarningCount(rows.stream().filter(r -> Integer.valueOf(1).equals(r.getLocationWarning())).count());
        vo.setSignatureCount(rows.stream().filter(r -> StringUtils.hasText(r.getSignatureUrl())).count());
        vo.setPhotoCount(rows.stream()
                .filter(r -> StringUtils.hasText(r.getPhotoUrls()) && !"[]".equals(r.getPhotoUrls().trim())).count());
        vo.setApproveRate(rate(vo.getApproved(), vo.getTotal()));
        vo.setWarningRate(rate(vo.getWarningCount(), vo.getTotal()));
        vo.setRejectRate(rate(vo.getRejectCount(), vo.getTotal()));
        return vo;
    }

    public void export(SignQueryDTO query, HttpServletResponse response) throws IOException {
        List<SignVO> rows = list(query);
        String fileName = "签收台账_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"任务编号", "配送员", "配送员电话", "客户", "客户电话", "签收类型",
                "应签收数量", "实际签收数量", "签收时间", "定位偏差(米)", "是否超阈值", "偏差阈值(米)",
                "照片数", "手写签名", "审核状态", "审核人", "审核时间", "审核意见", "签收备注", "车辆"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("签收台账");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (SignVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(vo.getTaskNo()), nullSafe(vo.getRiderName()), nullSafe(vo.getRiderPhone()),
                        nullSafe(vo.getCustomerName()), nullSafe(vo.getCustomerPhone()), nullSafe(vo.getSignTypeText()),
                        num(vo.getPlannedQuantity()), num(vo.getActualQuantity()),
                        timeText(vo.getSignTime()), num(vo.getLocationDeviation()),
                        Integer.valueOf(1).equals(vo.getLocationWarning()) ? "是" : "否",
                        num(vo.getDeviationThresh()),
                        vo.getPhotoCount() == null ? "0" : String.valueOf(vo.getPhotoCount()),
                        Boolean.TRUE.equals(vo.getHasSignature()) ? "有" : "无",
                        nullSafe(vo.getAuditStatusText()), nullSafe(vo.getAuditByName()),
                        timeText(vo.getAuditTime()), nullSafe(vo.getAuditRemark()),
                        nullSafe(vo.getRemark()), nullSafe(vo.getVehicleName()),
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    // ==================== 私有辅助 ====================

    private List<SignVO> toVOList(List<DmsSign> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> taskIds = rows.stream().map(DmsSign::getTaskId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsTask> taskMap = taskIds.isEmpty() ? Map.of()
                : taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(DmsTask::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
        Set<Long> riderIds = taskMap.values().stream().map(DmsTask::getRiderId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsRider> riderMap = riderIds.isEmpty() ? Map.of()
                : riderMapper.selectBatchIds(riderIds).stream()
                .collect(Collectors.toMap(DmsRider::getId, r -> r, (a, b) -> a, LinkedHashMap::new));

        return rows.stream().map(sign -> {
            DmsTask task = sign.getTaskId() == null ? null : taskMap.get(sign.getTaskId());
            DmsRider rider = task == null || task.getRiderId() == null ? null : riderMap.get(task.getRiderId());
            return toVO(sign, task, rider);
        }).toList();
    }

    private SignVO toVO(DmsSign sign, DmsTask task, DmsRider rider) {
        SignVO vo = new SignVO();
        vo.setId(sign.getId());
        vo.setTaskId(sign.getTaskId());
        if (task != null) {
            vo.setTaskNo(task.getTaskNo());
            vo.setTaskStatus(task.getStatus());
            vo.setTaskStatusText(taskStatusText(task.getStatus()));
            vo.setOrderNo(task.getOrderNo());
            vo.setSourceBillNo(task.getSourceBillNo());
            vo.setRiderId(task.getRiderId());
            vo.setRiderName(task.getRiderName());
            vo.setVehicleName(task.getVehicleName());
            vo.setCustomerId(task.getCustomerId());
            vo.setCustomerName(task.getCustomerName());
            vo.setCustomerPhone(task.getCustomerPhone());
            vo.setCollectOnDelivery(task.getCollectOnDelivery());
            vo.setDeliveryFee(task.getDeliveryFee());
        }
        if (rider != null) {
            if (!StringUtils.hasText(vo.getRiderName())) {
                vo.setRiderName(rider.getRealName());
            }
            vo.setRiderPhone(rider.getPhone());
        }
        vo.setSignType(sign.getSignType());
        SignTypeEnum st = SignTypeEnum.fromValue(sign.getSignType());
        vo.setSignTypeText(st == null ? null : st.getDescription());
        vo.setActualQuantity(sign.getActualQuantity());
        vo.setPlannedQuantity(sign.getPlannedQuantity());
        vo.setPhotoUrls(sign.getPhotoUrls());
        vo.setPhotoCount(countPhotos(sign.getPhotoUrls()));
        vo.setSignatureUrl(sign.getSignatureUrl());
        vo.setHasSignature(StringUtils.hasText(sign.getSignatureUrl()));
        vo.setSignLat(sign.getSignLat());
        vo.setSignLng(sign.getSignLng());
        vo.setCustomerLat(sign.getCustomerLat());
        vo.setCustomerLng(sign.getCustomerLng());
        vo.setNewCustomerLat(sign.getNewCustomerLat());
        vo.setNewCustomerLng(sign.getNewCustomerLng());
        vo.setLocationDeviation(sign.getLocationDeviation());
        vo.setLocationWarning(sign.getLocationWarning());
        vo.setDeviationThresh(sign.getDeviationThresh());
        vo.setRemark(sign.getRemark());
        vo.setSignTime(sign.getSignTime());
        vo.setAuditStatus(sign.getAuditStatus());
        vo.setAuditStatusText(auditStatusText(sign.getAuditStatus()));
        vo.setAuditBy(sign.getAuditBy());
        vo.setAuditByName(sign.getAuditByName());
        vo.setAuditTime(sign.getAuditTime());
        vo.setAuditRemark(sign.getAuditRemark());
        vo.setCreateTime(sign.getCreateTime());
        vo.setUpdateTime(sign.getUpdateTime());
        return vo;
    }

    /** 照片数：兼容 JSON 数组串与逗号分隔串 */
    private int countPhotos(String photoUrls) {
        if (!StringUtils.hasText(photoUrls)) {
            return 0;
        }
        String text = photoUrls.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text.isEmpty() ? 0 : text.split(",").length;
    }

    /** 偏差阈值：请求值 > 配送参数配置 > 常量缺省 */
    private double resolveThreshold(BigDecimal fromRequest) {
        if (fromRequest != null && fromRequest.signum() > 0) {
            return fromRequest.doubleValue();
        }
        try {
            Integer configured = configService.getInteger(MyBatisPlusConfig.getCurrentTenantIdValue(),
                    CFG_DEVIATION_THRESHOLD);
            if (configured != null && configured > 0) {
                return configured;
            }
        } catch (Exception e) {
            log.debug("签收偏差阈值未配置，使用缺省 {} 米（key={}）", DEFAULT_THRESHOLD, CFG_DEVIATION_THRESHOLD);
        }
        return DEFAULT_THRESHOLD;
    }

    private void publishEvent(String eventType, DmsTask task, DmsSign sign, String remark) {
        try {
            String payload = "{\"signId\":" + sign.getId()
                    + ",\"taskId\":" + sign.getTaskId()
                    + ",\"taskNo\":\"" + nullSafe(task == null ? null : task.getTaskNo()) + "\""
                    + ",\"signType\":" + sign.getSignType()
                    + ",\"auditStatus\":" + sign.getAuditStatus()
                    + ",\"collectOnDelivery\":" + (task == null || task.getCollectOnDelivery() == null
                            ? "0" : task.getCollectOnDelivery().toPlainString())
                    + ",\"deliveryFee\":" + (task == null || task.getDeliveryFee() == null
                            ? "0" : task.getDeliveryFee().toPlainString())
                    + ",\"remark\":\"" + nullSafe(remark) + "\"}";
            eventService.publishEvent(eventType, "dms:sign", payload);
        } catch (Exception e) {
            // 事件外发失败不得阻断签收主流程（事件可重试）
            log.warn("签收事件外发失败: type={}, signId={}", eventType, sign.getId(), e);
        }
    }

    /** 当前登录用户真实姓名（昵称优先，其次用户名） */
    private String currentUserName(Long userId) {
        if (userId == null) {
            return "系统";
        }
        try {
            var user = sysUserMapper.selectById(userId);
            if (user != null) {
                if (StringUtils.hasText(user.getNickname())) {
                    return user.getNickname();
                }
                if (StringUtils.hasText(user.getUsername())) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("查询审核人姓名失败: userId={}", userId, e);
        }
        return String.valueOf(userId);
    }

    private String taskStatusText(Integer status) {
        return TaskStatusEnum.fromValue(status == null ? 0 : status).getDescription();
    }

    private String auditStatusText(Integer status) {
        SignAuditStatusEnum e = SignAuditStatusEnum.fromValue(status);
        return e == null ? "-" : e.getDescription();
    }

    private boolean eq(Integer value, SignTypeEnum target) {
        return value != null && value == target.getValue();
    }

    private boolean eq(Integer value, SignAuditStatusEnum target) {
        return value != null && value == target.getValue();
    }

    private BigDecimal rate(long numerator, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String timeText(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    /** 两点间距离（Haversine 公式，米） */
    private double calculateDeviation(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double radLng1 = Math.toRadians(lng1.doubleValue());
        double radLng2 = Math.toRadians(lng2.doubleValue());
        double dLat = radLat2 - radLat1;
        double dLng = radLng2 - radLng1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }
}
