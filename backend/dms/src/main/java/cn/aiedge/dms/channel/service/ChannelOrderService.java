package cn.aiedge.dms.channel.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.dms.channel.adapter.DeliveryAdapter;
import cn.aiedge.dms.channel.dto.ChannelCallbackRequest;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.entity.DmsChannelCallbackLog;
import cn.aiedge.dms.channel.entity.DmsChannelOrder;
import cn.aiedge.dms.channel.mapper.DmsChannelCallbackLogMapper;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.channel.mapper.DmsChannelOrderMapper;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.event.service.EventService;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.entity.DmsTaskLog;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.task.service.TaskLogService;
import cn.aiedge.dms.task.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道对外下单与回调服务
 *
 * <p>落地《渠道管理开发文档》§7.1「派单接线」与 §7.3「回调安全」：</p>
 * <ol>
 *   <li><b>下单</b>：{@link #pushOrder} 走渠道适配器 {@code createOrder}，幂等键
 *       {@code taskNo:channelId}（库内部分唯一索引）保证「同任务同渠道只下一次」，
 *       失败按配置重试（指数退避），台账落 {@code dms_channel_order}；适配器未接通时**如实失败**，
 *       不伪造单号。</li>
 *   <li><b>回调</b>：{@link #handleCallback} 做 HMAC-SHA256 验签（密钥取渠道凭据解密后的
 *       appSecret/signKey）+ 时间戳容差 + nonce 防重放，全部留痕 {@code dms_channel_callback_log}，
 *       并按外部单号回写台账与任务状态（状态仅允许**前进**，终态不回写）。</li>
 * </ol>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelOrderService {

    /** 下单失败重试次数 */
    private static final String CFG_PUSH_RETRY = "dms.channel.push.retry";
    /** 渠道不可用时是否降级自有运力（由调度侧消费，本服务只提供查询口径） */
    private static final String CFG_PUSH_FALLBACK = "dms.channel.push.fallback";
    /** 回调时间戳容差（秒） */
    private static final String CFG_CALLBACK_TOLERANCE = "dms.channel.callback.tolerance.seconds";

    private static final int DEFAULT_PUSH_RETRY = 2;
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;
    /** 重试间隔基数（毫秒，指数退避 150 / 300 / 600 …，上限 1s） */
    private static final long BACKOFF_BASE_MILLIS = 150L;
    private static final long BACKOFF_MAX_MILLIS = 1000L;
    private static final int LAST_ERROR_MAX = 500;
    private static final int PAYLOAD_MAX = 2000;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DmsChannelOrderMapper orderMapper;
    private final DmsChannelCallbackLogMapper callbackLogMapper;
    private final DmsChannelMapper channelMapper;
    private final ChannelService channelService;
    private final DmsTaskMapper taskMapper;
    private final TaskLogService taskLogService;
    /** 任务终态后的运力释放（渠道回传「已完成 / 已取消」时回置自有配送员占用） */
    private final TaskService taskService;
    private final EventService eventService;
    private final ConfigService configService;

    // ══════════════════════════════════════════════════════════
    // 结果模型
    // ══════════════════════════════════════════════════════════

    /** 下单结果 */
    public record PushResult(boolean success, boolean reused, Long taskId, String taskNo, Long channelId,
                             String channelCode, String channelName, String channelOrderNo,
                             int attempts, Integer orderStatus, String message) {
    }

    /** 回调处理结果 */
    public record CallbackResult(boolean accepted, boolean processed, String result, String message,
                                 String channelCode, String channelOrderNo, String taskNo, Integer taskStatus) {
    }

    // ══════════════════════════════════════════════════════════
    // 一、渠道派单（对外下单）
    // ══════════════════════════════════════════════════════════

    /**
     * 向外部渠道下单（幂等 + 重试 + 台账）
     *
     * @param taskId       配送任务ID
     * @param channelId    渠道ID
     * @param dispatchType 派单方式（1-自动调度 2-手工指派，用于复盘口径）
     * @param reason       派单原因（写调度审计，可空）
     */
    @Transactional(rollbackFor = Exception.class)
    public PushResult pushOrder(Long taskId, Long channelId, int dispatchType, String reason) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("配送任务不存在: " + taskId);
        }
        DmsChannel channel = channelService.getById(channelId);
        if (Integer.valueOf(0).equals(channel.getStatus())) {
            throw new DmsBusinessException("渠道「" + channel.getChannelName() + "」已停用，不可派单");
        }

        String idemKey = task.getTaskNo() + ":" + channel.getId();
        // ① 幂等命中：已有成功台账 → 直接复用原单号，不重复外呼
        DmsChannelOrder exist = orderMapper.selectByIdemKey(idemKey);
        if (exist != null && exist.getChannelOrderNo() != null
                && !Integer.valueOf(DmsChannelOrder.STATUS_FAILED).equals(exist.getOrderStatus())) {
            return new PushResult(true, true, task.getId(), task.getTaskNo(), channel.getId(),
                    channel.getChannelCode(), channel.getChannelName(), exist.getChannelOrderNo(),
                    nz(exist.getAttempts()), exist.getOrderStatus(),
                    "该任务已向渠道「" + channel.getChannelName() + "」下单（幂等命中），外部单号 "
                            + exist.getChannelOrderNo());
        }

        if (!Integer.valueOf(DmsConstants.TASK_PENDING).equals(task.getStatus())) {
            throw new DmsBusinessException("任务状态不允许渠道派单（当前 "
                    + TaskStatusEnum.fromValue(nz(task.getStatus())).getDescription() + "）");
        }

        // ② 适配器解析（软失败：未接通如实返回原因，不伪造成功）
        DeliveryAdapter adapter = channelService.resolveAdapter(channel);
        if (adapter == null) {
            String message = "渠道「" + channel.getChannelName() + "」未接通："
                    + (hasText(channel.getAdapterBean())
                    ? "适配器 " + channel.getAdapterBean() + " 未启用或不存在（请确认 dms.channel.*.enabled 开关）"
                    : "未配置适配器 Bean")
                    + "；当前可用适配器：" + channelService.availableAdapterNames();
            saveOrder(exist, task, channel, idemKey, null, DmsChannelOrder.STATUS_FAILED, 0, message);
            log.warn("渠道派单失败（适配器未接通）: taskNo={}, channel={}", task.getTaskNo(), channel.getChannelCode());
            return new PushResult(false, false, task.getId(), task.getTaskNo(), channel.getId(),
                    channel.getChannelCode(), channel.getChannelName(), null, 0,
                    DmsChannelOrder.STATUS_FAILED, message);
        }

        // ③ 外呼（失败按配置重试，指数退避）
        DeliveryAdapter.CreateRequest request = buildCreateRequest(task, channel);
        int maxRetry = Math.max(cfgInt(CFG_PUSH_RETRY, DEFAULT_PUSH_RETRY), 0);
        int attempts = 0;
        String lastError = null;
        DeliveryAdapter.CreateResult result = null;
        for (int i = 0; i <= maxRetry; i++) {
            attempts++;
            try {
                result = adapter.createOrder(request);
                if (result != null && result.success() && hasText(result.channelOrderNo())) {
                    lastError = null;
                    break;
                }
                lastError = result == null ? "适配器未返回结果"
                        : (hasText(result.message()) ? result.message() : "适配器未返回外部单号");
            } catch (Exception e) {
                result = null;
                lastError = "适配器调用异常：" + e.getMessage();
                log.warn("渠道下单异常: taskNo={}, channel={}, attempt={}", task.getTaskNo(),
                        channel.getChannelCode(), attempts, e);
            }
            if (i < maxRetry) {
                sleep(BACKOFF_BASE_MILLIS << Math.min(i, 3));
            }
        }

        boolean success = result != null && result.success() && hasText(result.channelOrderNo());
        String channelOrderNo = success ? result.channelOrderNo() : null;
        saveOrder(exist, task, channel, idemKey, channelOrderNo,
                success ? DmsChannelOrder.STATUS_SUBMITTED : DmsChannelOrder.STATUS_FAILED,
                attempts, success ? null : lastError);

        if (!success) {
            String message = "渠道「" + channel.getChannelName() + "」下单失败（已尝试 " + attempts + " 次）：" + lastError;
            log.warn("渠道派单失败: taskNo={}, channel={}, attempts={}, err={}",
                    task.getTaskNo(), channel.getChannelCode(), attempts, lastError);
            return new PushResult(false, false, task.getId(), task.getTaskNo(), channel.getId(),
                    channel.getChannelCode(), channel.getChannelName(), null, attempts,
                    DmsChannelOrder.STATUS_FAILED, message);
        }

        // ④ 任务流转到「已分配」并写调度审计（外部平台承接，无本系统配送员）
        task.setStatus(DmsConstants.TASK_ASSIGNED);
        task.setDispatchType(dispatchType);
        task.setDispatchTime(LocalDateTime.now());
        taskMapper.updateById(task);
        taskLogService.record(task.getId(), task.getTaskNo(),
                dispatchType == 2 ? DmsTaskLog.ACTION_ASSIGN : DmsTaskLog.ACTION_AUTO_ASSIGN,
                null, null, "渠道派单：" + channel.getChannelName() + "（" + channel.getChannelCode()
                        + "），外部单号 " + channelOrderNo
                        + (hasText(reason) ? "；" + reason : ""));
        publishDispatchEvent(task, channel, channelOrderNo);

        log.info("渠道派单成功: taskNo={}, channel={}, channelOrderNo={}, attempts={}",
                task.getTaskNo(), channel.getChannelCode(), channelOrderNo, attempts);
        return new PushResult(true, false, task.getId(), task.getTaskNo(), channel.getId(),
                channel.getChannelCode(), channel.getChannelName(), channelOrderNo, attempts,
                DmsChannelOrder.STATUS_SUBMITTED, "已向渠道「" + channel.getChannelName() + "」下单，外部单号 "
                + channelOrderNo);
    }

    /** 渠道不可用时是否降级自有运力（调度侧消费；缺失/异常回落 true） */
    public boolean fallbackEnabled() {
        return cfgBool(CFG_PUSH_FALLBACK, true);
    }

    /** 外部单台账分页（按渠道） */
    public Page<DmsChannelOrder> orderPage(Long channelId, Integer orderStatus, int pageNum, int pageSize) {
        channelService.getById(channelId); // 渠道不存在直接报错，避免「空列表」误导
        LambdaQueryWrapper<DmsChannelOrder> wrapper = new LambdaQueryWrapper<DmsChannelOrder>()
                .eq(DmsChannelOrder::getChannelId, channelId)
                .eq(orderStatus != null, DmsChannelOrder::getOrderStatus, orderStatus)
                .orderByDesc(DmsChannelOrder::getId);
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 回调日志分页（按渠道） */
    public Page<DmsChannelCallbackLog> callbackLogPage(Long channelId, int pageNum, int pageSize) {
        channelService.getById(channelId);
        LambdaQueryWrapper<DmsChannelCallbackLog> wrapper = new LambdaQueryWrapper<DmsChannelCallbackLog>()
                .eq(DmsChannelCallbackLog::getChannelId, channelId)
                .orderByDesc(DmsChannelCallbackLog::getId);
        return callbackLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    // ══════════════════════════════════════════════════════════
    // 二、外部平台回调（验签 + 防重放 + 留痕 + 回写）
    // ══════════════════════════════════════════════════════════

    /** 回调入口：解析渠道 → 切租户上下文 → 处理 */
    public CallbackResult handleCallback(ChannelCallbackRequest req) {
        if (req == null || !hasText(req.getChannelCode())) {
            throw new DmsBusinessException("回调缺少 channelCode");
        }
        if (!hasText(req.getChannelOrderNo())) {
            throw new DmsBusinessException("回调缺少 channelOrderNo（外部平台单号）");
        }
        String code = req.getChannelCode().trim();
        List<DmsChannel> candidates = channelService.findByCodeAcrossTenants(code);
        if (candidates.isEmpty()) {
            throw new DmsBusinessException("渠道不存在: " + code);
        }
        DmsChannel channel;
        if (req.getTenantId() != null) {
            channel = candidates.stream()
                    .filter(c -> req.getTenantId().equals(c.getTenantId()))
                    .findFirst()
                    .orElseThrow(() -> new DmsBusinessException(
                            "渠道不存在: " + code + "（tenantId=" + req.getTenantId() + "）"));
        } else if (candidates.size() == 1) {
            channel = candidates.get(0);
        } else {
            throw new DmsBusinessException("渠道编码 " + code + " 在多个租户下存在，回调必须携带 tenantId");
        }

        Long tenantId = channel.getTenantId() == null ? 0L : channel.getTenantId();
        // 无登录会话：显式指定租户上下文，保证后续读写落在渠道所属租户
        MyBatisPlusConfig.setTempTenantId(tenantId);
        try {
            return doHandleCallback(req, channel, tenantId);
        } finally {
            MyBatisPlusConfig.clearTempTenantId();
        }
    }

    /**
     * 回调处理主体。
     *
     * <p>⚠️ 刻意**不加 `@Transactional`**：被拒回调需要「先写日志再抛异常」（日志必须落库供对账），
     * 若整体事务化会把拒绝留痕一起回滚。台账/任务回写均为幂等写，平台重投（换 nonce）可自愈。</p>
     */
    private CallbackResult doHandleCallback(ChannelCallbackRequest req, DmsChannel channel, Long tenantId) {
        String channelOrderNo = req.getChannelOrderNo().trim();

        // ① nonce 防重放：同渠道同 nonce 已处理过 → 只更新原行标记，不再回写业务
        if (hasText(req.getNonce())) {
            DmsChannelCallbackLog processed = callbackLogMapper.selectOne(
                    new LambdaQueryWrapper<DmsChannelCallbackLog>()
                            .eq(DmsChannelCallbackLog::getChannelId, channel.getId())
                            .eq(DmsChannelCallbackLog::getNonce, req.getNonce().trim())
                            .orderByDesc(DmsChannelCallbackLog::getId)
                            .last("LIMIT 1"));
            if (processed != null) {
                processed.setReplayed(1);
                processed.setProcessResult(DmsChannelCallbackLog.RESULT_REPLAY);
                processed.setProcessMessage("重复投递（nonce=" + req.getNonce() + "），已忽略");
                callbackLogMapper.updateById(processed);
                log.warn("渠道回调重放已忽略: channel={}, nonce={}, channelOrderNo={}",
                        channel.getChannelCode(), req.getNonce(), channelOrderNo);
                return new CallbackResult(true, false, DmsChannelCallbackLog.RESULT_REPLAY,
                        "重复回调已忽略（防重放命中）", channel.getChannelCode(), channelOrderNo,
                        processed.getTaskNo(), null);
            }
        }

        // ② 时间戳容差 + ③ 验签（密钥取解密后的渠道凭据）
        String rejectReason = verifyRequest(req, channel);
        if (rejectReason != null) {
            writeLog(channel, tenantId, req, channelOrderNo, 0, DmsChannelCallbackLog.RESULT_REJECT,
                    rejectReason, null, null);
            throw new DmsBusinessException("回调校验失败：" + rejectReason);
        }

        // ④ 台账 upsert（外部平台先建单时按回调新建）+ 任务状态回写
        DmsChannelOrder order = orderMapper.selectByChannelOrderNo(channel.getId(), channelOrderNo);
        DmsTask task = resolveTask(order, req);
        Integer externalStatus = externalOrderStatus(req.getStatus());
        String message;
        Integer taskStatus = null;

        if (order == null && task != null) {
            // 同一任务同渠道可能已有**历史失败台账**（下单失败后平台侧先建单再回调）：
            // 复用该行，避免撞幂等键唯一索引「uk_dms_channel_order_idem」
            order = orderMapper.selectByIdemKey(task.getTaskNo() + ":" + channel.getId());
        }
        if (order == null) {
            order = new DmsChannelOrder();
            order.setChannelId(channel.getId());
            order.setChannelCode(channel.getChannelCode());
            order.setChannelOrderNo(channelOrderNo);
            order.setIdemKey(externalIdemKey(task, channel, channelOrderNo));
            order.setOrderStatus(DmsChannelOrder.STATUS_SUBMITTED);
            order.setAttempts(0);
            order.setSubmitTime(LocalDateTime.now());
        } else if (!hasText(order.getChannelOrderNo())) {
            // 失败台账补上平台返回的外部单号（台账以首次下单/回调到的单号为准）
            order.setChannelOrderNo(channelOrderNo);
            order.setSubmitTime(LocalDateTime.now());
        }
        if (task != null) {
            order.setTaskId(task.getId());
            order.setTaskNo(task.getTaskNo());
        }
        if (externalStatus != null) {
            order.setOrderStatus(externalStatus);
        }
        order.setCallbackTime(LocalDateTime.now());
        order.setRemark(trim("回调 " + nvlText(req.getEventType(), "status") + " → " + nvlText(req.getStatus(), "-"), 500));
        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }

        message = "已更新外部单 " + channelOrderNo + " 状态为 " + nvlText(req.getStatus(), "-");
        if (task != null) {
            Integer target = mapTaskStatus(req.getStatus());
            if (target == null) {
                message += "；任务未回写（平台状态无法映射：'" + req.getStatus() + "'）";
            } else if (canApply(task.getStatus(), target)) {
                applyTaskStatus(task, target, req);
                taskStatus = target;
                message += "；任务 " + task.getTaskNo() + " → "
                        + TaskStatusEnum.fromValue(target).getDescription();
            } else {
                message += "；任务状态未回写（" + TaskStatusEnum.fromValue(nz(task.getStatus())).getDescription()
                        + " → " + TaskStatusEnum.fromValue(target).getDescription() + " 不允许）";
            }
        } else {
            message += "；未关联到本系统任务（未提供 taskNo 且台账无任务）";
        }

        // 回调到达即证明对接可用：刷新渠道对接状态
        DmsChannel patch = new DmsChannel();
        patch.setId(channel.getId());
        patch.setLinkStatus(1);
        patch.setLastTestTime(LocalDateTime.now());
        patch.setLastTestResult(trim("回调正常：" + nvlText(req.getEventType(), req.getStatus()), 500));
        channelMapper.updateById(patch);

        writeLog(channel, tenantId, req, channelOrderNo, 1, DmsChannelCallbackLog.RESULT_OK, message,
                task, taskStatus);
        publishCallbackEvent(channel, req, task, taskStatus);
        log.info("渠道回调处理完成: channel={}, channelOrderNo={}, status={}, taskNo={}",
                channel.getChannelCode(), channelOrderNo, req.getStatus(), task == null ? null : task.getTaskNo());
        return new CallbackResult(true, true, DmsChannelCallbackLog.RESULT_OK, message,
                channel.getChannelCode(), channelOrderNo, task == null ? null : task.getTaskNo(), taskStatus);
    }

    /** 校验链：时间戳容差 → 回调密钥存在 → 签名一致。返回 null 表示通过，否则为拒绝原因。 */
    private String verifyRequest(ChannelCallbackRequest req, DmsChannel channel) {
        if (req.getTimestamp() == null) {
            return "缺少时间戳 timestamp";
        }
        long millis = req.getTimestamp() < 1_000_000_000_000L ? req.getTimestamp() * 1000 : req.getTimestamp();
        long toleranceMillis = Math.max(cfgInt(CFG_CALLBACK_TOLERANCE, DEFAULT_TOLERANCE_SECONDS), 0) * 1000L;
        long drift = Math.abs(System.currentTimeMillis() - millis);
        if (toleranceMillis > 0 && drift > toleranceMillis) {
            return "时间戳已过期（偏差 " + drift / 1000 + " 秒 > 容差 " + toleranceMillis / 1000 + " 秒）";
        }
        if (!hasText(req.getSign())) {
            return "缺少签名 sign";
        }
        String secret = callbackSecret(channel);
        if (!hasText(secret)) {
            return "渠道未配置回调密钥（对接配置中的 appSecret / signKey），已拒绝接收";
        }
        String expected = sign(req.getChannelCode(), req.getTimestamp(), req.getNonce(),
                req.getChannelOrderNo(), req.getStatus(), secret);
        if (!expected.equalsIgnoreCase(req.getSign().trim())) {
            return "签名不匹配";
        }
        return null;
    }

    /**
     * 回调签名口径（对外文档化、适配器按此实现）：
     * {@code HEX(HMAC-SHA256(secret, channelCode + timestamp + nonce + channelOrderNo + status))}
     */
    public static String sign(String channelCode, Long timestamp, String nonce,
                              String channelOrderNo, String status, String secret) {
        // 拼接口径：null 视为空串；timestamp 用其十进制字符串（秒/毫秒以请求原值为准）
        String base = (channelCode == null ? "" : channelCode)
                + (timestamp == null ? "" : timestamp.toString())
                + (nonce == null ? "" : nonce)
                + (channelOrderNo == null ? "" : channelOrderNo)
                + (status == null ? "" : status);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(base.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (Exception e) {
            throw new DmsBusinessException("回调签名计算失败：" + e.getMessage());
        }
    }

    /** 回调密钥：优先 signKey，其次 appSecret（取渠道凭据的**解密后**明文，不出服务端） */
    private String callbackSecret(DmsChannel channel) {
        String plain = channelService.decryptConfig(channel);
        if (!hasText(plain)) {
            return null;
        }
        Map<String, String> config = flatJson(plain);
        for (String key : List.of("signKey", "sign_key", "appSecret", "app_secret", "secret")) {
            for (Map.Entry<String, String> entry : config.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key) && hasText(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /** 原始状态 → 台账单状态 */
    private Integer externalOrderStatus(String status) {
        if (!hasText(status)) {
            return null;
        }
        return switch (status.trim().toUpperCase()) {
            case "ACCEPTED", "WAITING_PICKUP", "PICKED_UP", "DISPATCHING", "RIDER_ACCEPTED" ->
                    DmsChannelOrder.STATUS_ACCEPTED;
            case "DELIVERING", "TRANSPORTING", "SHIPPED" -> DmsChannelOrder.STATUS_DELIVERING;
            case "COMPLETED", "FINISHED", "DONE", "SIGNED" -> DmsChannelOrder.STATUS_COMPLETED;
            case "CANCELLED", "CANCELED", "CANCEL" -> DmsChannelOrder.STATUS_CANCELLED;
            case "EXCEPTION", "FAILED", "ERROR" -> DmsChannelOrder.STATUS_FAILED;
            default -> null;
        };
    }

    /** 原始状态 → 本系统任务状态（无法映射返回 null） */
    private Integer mapTaskStatus(String status) {
        Integer orderStatus = externalOrderStatus(status);
        if (orderStatus == null) {
            return null;
        }
        return switch (orderStatus) {
            case DmsChannelOrder.STATUS_ACCEPTED -> TaskStatusEnum.ACCEPTED.getValue();
            case DmsChannelOrder.STATUS_DELIVERING -> TaskStatusEnum.DELIVERING.getValue();
            case DmsChannelOrder.STATUS_COMPLETED -> TaskStatusEnum.COMPLETED.getValue();
            case DmsChannelOrder.STATUS_CANCELLED -> TaskStatusEnum.CANCELLED.getValue();
            case DmsChannelOrder.STATUS_FAILED -> TaskStatusEnum.EXCEPTION.getValue();
            default -> null;
        };
    }

    /**
     * 状态是否可回写：仅允许**前进**，终态（已完成/已取消）不再变更，
     * 取消/异常作为旁支终态可从任何未完成态进入。
     */
    private boolean canApply(Integer current, Integer target) {
        if (current == null || target == null) {
            return false;
        }
        if (current == TaskStatusEnum.COMPLETED.getValue() || current == TaskStatusEnum.CANCELLED.getValue()) {
            return false;
        }
        if (target == TaskStatusEnum.CANCELLED.getValue() || target == TaskStatusEnum.EXCEPTION.getValue()) {
            return current < TaskStatusEnum.COMPLETED.getValue();
        }
        return target > current;
    }

    private void applyTaskStatus(DmsTask task, int target, ChannelCallbackRequest req) {
        task.setStatus(target);
        // 外部平台配送员姓名快照（rider_name 是持久列；电话为非持久展示字段，记入调度审计留痕）
        if (hasText(req.getRiderName())) {
            task.setRiderName(req.getRiderName().trim());
        }
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case DmsConstants.TASK_ACCEPTED -> task.setLoadTime(now);
            case DmsConstants.TASK_DELIVERING -> task.setDeliveryTime(now);
            case DmsConstants.TASK_COMPLETED, DmsConstants.TASK_SIGNED -> task.setCompletedTime(now);
            default -> { /* 取消/异常无专用时间列 */ }
        }
        taskMapper.updateById(task);
        // 渠道侧「已完成 / 已取消」= 本系统任务终态 → 释放自有配送员占用（外部平台单 riderId 为空，自动跳过）
        if (target == DmsConstants.TASK_COMPLETED || target == DmsConstants.TASK_CANCELLED) {
            taskService.releaseRiderIfIdle(task.getRiderId());
        }
        StringBuilder reason = new StringBuilder("渠道回传：")
                .append(nvlText(req.getEventType(), "status")).append(" → ").append(req.getStatus());
        if (hasText(req.getRiderName())) {
            reason.append("；平台配送员 ").append(req.getRiderName());
            if (hasText(req.getRiderPhone())) {
                reason.append("（").append(req.getRiderPhone()).append("）");
            }
        }
        taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_CHANNEL,
                null, null, reason.toString());
    }

    /** 台账/请求 → 本系统任务：优先台账关联，其次按 taskNo 解析 */
    private DmsTask resolveTask(DmsChannelOrder order, ChannelCallbackRequest req) {
        if (order != null && order.getTaskId() != null) {
            DmsTask byOrder = taskMapper.selectById(order.getTaskId());
            if (byOrder != null) {
                return byOrder;
            }
        }
        if (hasText(req.getTaskNo())) {
            return taskMapper.selectOne(new LambdaQueryWrapper<DmsTask>()
                    .eq(DmsTask::getTaskNo, req.getTaskNo().trim())
                    .last("LIMIT 1"));
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════
    // 内部工具
    // ══════════════════════════════════════════════════════════

    private void saveOrder(DmsChannelOrder exist, DmsTask task, DmsChannel channel, String idemKey,
                           String channelOrderNo, int status, int attempts, String lastError) {
        DmsChannelOrder row = exist == null ? new DmsChannelOrder() : exist;
        if (row.getId() == null) {
            row.setChannelId(channel.getId());
            row.setChannelCode(channel.getChannelCode());
            row.setTaskId(task.getId());
            row.setTaskNo(task.getTaskNo());
            row.setIdemKey(idemKey);
        }
        row.setOrderStatus(status);
        row.setAttempts(attempts);
        row.setLastError(trim(lastError, LAST_ERROR_MAX));
        if (hasText(channelOrderNo)) {
            row.setChannelOrderNo(channelOrderNo);
            row.setSubmitTime(LocalDateTime.now());
        }
        if (row.getId() == null) {
            orderMapper.insert(row);
        } else {
            orderMapper.updateById(row);
        }
    }

    private DeliveryAdapter.CreateRequest buildCreateRequest(DmsTask task, DmsChannel channel) {
        return new DeliveryAdapter.CreateRequest(
                task.getTaskNo(),
                task.getSourceAddress(),
                task.getSourceLat(),
                task.getSourceLng(),
                task.getCustomerAddress(),
                task.getCustomerLat(),
                task.getCustomerLng(),
                task.getCustomerName(),
                task.getCustomerPhone(),
                trim("渠道:" + channel.getChannelName() + (hasText(task.getRemark()) ? "；" + task.getRemark() : ""),
                        200));
    }

    private void writeLog(DmsChannel channel, Long tenantId, ChannelCallbackRequest req, String channelOrderNo,
                          int signOk, String result, String message, DmsTask task, Integer taskStatus) {
        try {
            DmsChannelCallbackLog entity = new DmsChannelCallbackLog();
            entity.setTenantId(tenantId);
            entity.setChannelId(channel.getId());
            entity.setChannelCode(channel.getChannelCode());
            entity.setChannelOrderNo(trim(channelOrderNo, 64));
            if (task != null) {
                entity.setTaskId(task.getId());
                entity.setTaskNo(task.getTaskNo());
            } else if (hasText(req.getTaskNo())) {
                entity.setTaskNo(trim(req.getTaskNo(), 64));
            }
            entity.setEventType(trim(req.getEventType(), 32));
            entity.setExternalStatus(trim(req.getStatus(), 32));
            entity.setNonce(trim(req.getNonce(), 64));
            entity.setSignOk(signOk);
            entity.setReplayed(0);
            entity.setProcessResult(result);
            entity.setProcessMessage(trim(message, LAST_ERROR_MAX));
            entity.setPayload(trim(req.getPayload(), PAYLOAD_MAX));
            entity.setReceiveTime(LocalDateTime.now());
            callbackLogMapper.insert(entity);
        } catch (Exception e) {
            // 留痕失败不能打断回调主流程（唯一索引冲突=同一 nonce 并发，忽略即可）
            log.warn("渠道回调日志写入失败: channel={}, nonce={}, err={}",
                    channel.getChannelCode(), req.getNonce(), e.getMessage());
        }
    }

    private void publishCallbackEvent(DmsChannel channel, ChannelCallbackRequest req, DmsTask task, Integer taskStatus) {
        try {
            String payload = "{\"channelCode\":\"" + esc(channel.getChannelCode())
                    + "\",\"channelOrderNo\":\"" + esc(req.getChannelOrderNo())
                    + "\",\"status\":\"" + esc(req.getStatus())
                    + "\",\"taskNo\":\"" + (task == null ? "" : esc(task.getTaskNo()))
                    + "\",\"taskStatus\":" + (taskStatus == null ? "null" : taskStatus) + "}";
            eventService.publishEvent("CHANNEL_CALLBACK", "dms:channel", payload);
        } catch (Exception e) {
            log.warn("渠道回调事件外发失败: channel={}", channel.getChannelCode(), e);
        }
    }

    private void publishDispatchEvent(DmsTask task, DmsChannel channel, String channelOrderNo) {
        try {
            String payload = "{\"taskId\":" + task.getId()
                    + ",\"taskNo\":\"" + esc(task.getTaskNo())
                    + "\",\"channelId\":" + channel.getId()
                    + ",\"channelCode\":\"" + esc(channel.getChannelCode())
                    + "\",\"channelOrderNo\":\"" + esc(channelOrderNo) + "\"}";
            eventService.publishEvent("DISPATCH_ASSIGNED", "dms:dispatch", payload);
        } catch (Exception e) {
            log.warn("渠道派单事件外发失败: taskNo={}", task.getTaskNo(), e);
        }
    }

    /** 台账幂等键（无任务时按外部单号构造，保证唯一索引可用） */
    private String externalIdemKey(DmsTask task, DmsChannel channel, String channelOrderNo) {
        return (task == null ? "EXT:" + channelOrderNo : task.getTaskNo()) + ":" + channel.getId();
    }

    /** 扁平化 JSON（取 key → 文本值；嵌套对象取子字段，够用且不引入强类型配置类） */
    private Map<String, String> flatJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        collectJson(json, map, 0);
        return map;
    }

    private void collectJson(String json, Map<String, String> target, int depth) {
        if (depth > 3 || !hasText(json)) {
            return;
        }
        try {
            JsonNode root = JSON.readTree(json);
            root.fields().forEachRemaining(entry -> {
                if (entry.getValue().isValueNode()) {
                    target.putIfAbsent(entry.getKey(), entry.getValue().asText());
                } else if (entry.getValue().isContainerNode()) {
                    collectJson(entry.getValue().toString(), target, depth + 1);
                }
            });
        } catch (Exception e) {
            log.debug("渠道配置解析失败（忽略）：{}", e.getMessage());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String nvlText(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Integer cfgInt(String key, int defaultValue) {
        try {
            Integer value = configService.getInteger(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean cfgBool(String key, boolean defaultValue) {
        try {
            Boolean value = configService.getBoolean(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
