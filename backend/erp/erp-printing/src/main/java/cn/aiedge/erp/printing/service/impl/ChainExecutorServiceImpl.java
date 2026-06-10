package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.common.cache.RedisCache;
import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.*;
import cn.aiedge.erp.printing.mapper.*;
import cn.aiedge.erp.printing.mq.PrintTaskProducer;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.service.ChainExecutorService;
import cn.aiedge.erp.printing.service.ScreenshotService;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChainExecutorServiceImpl implements ChainExecutorService {

    private final SysPrintChainMapper chainMapper;
    private final SysPrintChainItemMapper chainItemMapper;
    private final SysPrintTaskMapper taskMapper;
    private final SysPrintTemplateMapper templateMapper;
    private final SysPrintClientMapper clientMapper;
    private final SysScreenshotTaskMapper screenshotTaskMapper;
    private final ScreenshotService screenshotService;
    private final ObjectMapper objectMapper;
    private final PrintTaskProducer taskProducer;
    private final RedisCache redisCache;

    /** 客户端并发锁前缀 */
    private static final String LOCK_KEY_CLIENT_PREFIX = "print:client:lock:";
    /** 客户端锁超时秒数 */
    private static final long CLIENT_LOCK_SECONDS = 30;

    @Override
    @Transactional
    public List<PrintTaskVO> executeChain(ChainTaskExecuteRequest request, Long tenantId, Long userId) {
        // 1. 获取链路
        SysPrintChain chain = chainMapper.selectById(request.getChainId());
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("打印链路不存在");
        }
        if (!"ACTIVE".equals(chain.getStatus())) {
            throw BusinessException.forbidden("打印链路已禁用");
        }

        // 2. 获取链路明细（按 step_order 排序）
        List<SysPrintChainItem> items = chainItemMapper.selectByChainIdOrdered(request.getChainId());
        if (items.isEmpty()) {
            throw BusinessException.badRequest("打印链路没有配置步骤");
        }

        // 3. 确定执行范围
        int startStep = request.getStartStep() != null ? request.getStartStep() : 1;
        int endStep = request.getEndStep() != null ? request.getEndStep() : items.size();

        // 4. 序列化 data_json
        String dataJsonStr;
        try {
            Map<String, Object> data = request.getDataJson();
            if (data == null) data = new HashMap<>();
            dataJsonStr = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw BusinessException.internalError("单据数据序列化失败");
        }

        // 5. 为每个步骤创建任务
        List<SysPrintTask> createdTasks = new ArrayList<>();

        for (SysPrintChainItem item : items) {
            if (item.getStepOrder() < startStep || item.getStepOrder() > endStep) {
                continue;
            }

            SysPrintTask task = buildTask(item, chain, request, dataJsonStr, tenantId, userId);
            taskMapper.insert(task);
            createdTasks.add(task);
        }

        // 6. 自动启动第一个步骤
        if (!createdTasks.isEmpty()) {
            SysPrintTask firstTask = createdTasks.get(0);
            processTask(firstTask);
        }

        return createdTasks.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void executeStep(Long taskId) {
        SysPrintTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        processTask(task);
    }

    @Override
    @Transactional
    public void confirmScreenshot(Long taskId) {
        SysPrintTask task = taskMapper.selectById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        Long screenshotId = task.getScreenshotId();
        if (screenshotId == null) {
            throw BusinessException.badRequest("该任务没有关联的截图，无需确认");
        }

        // 仅完成截图任务，不改变打印任务状态（打印已独立执行）
        screenshotService.completeScreenshot(screenshotId, null, null, null, null, null);

        task.setResultLog((task.getResultLog() != null ? task.getResultLog() : "")
                + "\n[截图] 用户已确认截图");
        taskMapper.updateById(task);

        log.info("截图已确认: taskId={}, screenshotId={}", taskId, screenshotId);
    }

    @Override
    @Transactional
    public void cancelWaitingTask(Long taskId) {
        SysPrintTask task = taskMapper.selectById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        task.setStatus("CANCELLED");
        task.setCompleteTime(LocalDateTime.now());
        task.setResultLog((task.getResultLog() != null ? task.getResultLog() : "") + "\n[用户取消]");
        taskMapper.updateById(task);
    }

    private SysPrintTask buildTask(SysPrintChainItem item, SysPrintChain chain,
                                    ChainTaskExecuteRequest request, String dataJson,
                                    Long tenantId, Long userId) {
        SysPrintTask task = new SysPrintTask();
        task.setTaskCode("PT" + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        task.setTenantId(tenantId);
        task.setPageCode(chain.getPageCode());
        task.setDocumentType(request.getDocumentType());
        task.setDocumentId(request.getDocumentId());
        task.setDocumentNo(request.getDocumentNo());
        task.setDataJson(dataJson);

        // 链路关联
        task.setChainId(chain.getChainId());
        task.setChainItemId(item.getItemId());
        task.setStepOrder(item.getStepOrder());

        // 执行目标
        task.setClientId(item.getClientId());
        task.setPrinterName(item.getPrinterName());

        // 模板
        task.setTemplateId(item.getTemplateId());

        // 初始状态：始终 PENDING，打印立即执行，截图作为独立并行流程不影响任务状态
        task.setStatus("PENDING");

        task.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        task.setMaxRetry(3);
        task.setSubmitTime(LocalDateTime.now());
        task.setCreatedBy(userId);

        return task;
    }

    /**
     * 处理单个任务：打印优先执行，截图作为独立并行流程
     *
     * <p>核心原则：截图服务不影响打印流程正常执行。
     * <ul>
     *   <li>DISABLED：仅打印</li>
     *   <li>MANUAL_CONFIRM：打印 + 创建截图任务（等待用户手动截图上传）</li>
     *   <li>AUTO_CONFIRM：打印 + 自动截图（客户端自动完成，无需等待）</li>
     * </ul>
     */
    private void processTask(SysPrintTask task) {
        // 0. 确定截图模式
        SysPrintChainItem item = chainItemMapper.selectById(task.getChainItemId());
        String screenshotMode = (item != null) ? item.getScreenshotMode() : "DISABLED";

        // 1. 始终优先执行打印（截图不阻塞打印）
        try {
            renderAndDispatch(task);
        } catch (Exception e) {
            log.error("渲染分发失败: {}", task.getTaskId(), e);
            markFailed(task, e.getMessage());
            return;
        }

        // 2. 截图作为独立并行流程（不影响打印状态）
        if (item != null && !"DISABLED".equals(screenshotMode)) {
            try {
                createIndependentScreenshot(task, item);
            } catch (Exception e) {
                log.warn("创建截图任务失败（不影响打印）: taskId={}", task.getTaskId(), e);
            }
        }
    }

    /**
     * 创建独立截图任务（不阻塞打印流程）
     * 截图任务独立于打印任务运行，状态互不影响
     */
    private void createIndependentScreenshot(SysPrintTask task, SysPrintChainItem item) {
        Map<String, Object> data = parseDataJson(task.getDataJson());
        ScreenshotCreateRequest ssRequest = new ScreenshotCreateRequest();
        ssRequest.setTemplateId(task.getTemplateId());
        ssRequest.setDataJson(data);
        ssRequest.setPageCode(task.getPageCode());

        var ssVO = screenshotService.createScreenshotTask(ssRequest, task.getTenantId(), task.getCreatedBy());

        // 关联截图任务到打印任务（仅用于查询关联，不改变打印任务状态）
        task.setScreenshotId(ssVO.getScreenshotId());
        task.setResultLog((task.getResultLog() != null ? task.getResultLog() : "")
                + "\n[截图] 截图任务已创建（独立于打印流程）");
        taskMapper.updateById(task);

        // 服务端预渲染 HTML
        screenshotService.executeScreenshot(ssVO.getScreenshotId());

        String mode = item.getScreenshotMode();
        log.info("独立截图任务已创建: taskId={}, screenshotId={}, mode={}",
                task.getTaskId(), ssVO.getScreenshotId(), mode);
    }

    /**
     * 渲染模板 HTML 并分发到客户端
     */
    private void renderAndDispatch(SysPrintTask task) throws Exception {
        // 1. 标记为队列中
        task.setStatus("QUEUED");
        taskMapper.updateById(task);

        // 2. 渲染（预渲染 HTML 在 data_json 中标记，最终客户端渲染）
        // 实际客户端收到任务后，会使用本地渲染引擎进行最终渲染
        // 这里也可以直接预渲染成 HTML 存到 result_log 或扩展字段

        // 3. 分发到客户端
        dispatchToClient(task);
    }

    /**
     * 通过 RabbitMQ 将任务分派到客户端队列
     * 使用 Redis 分布式锁确保同一客户端同一时刻只派发一个任务
     * 锁在分派完成后立即释放（MQ 队列保证客户端侧串行处理）
     */
    private void dispatchToClient(SysPrintTask task) {
        Long clientId = task.getClientId();
        if (clientId == null) {
            markFailed(task, "未指定客户端");
            return;
        }

        // 并发控制：同一客户端只能有一个任务在派发中
        String lockKey = LOCK_KEY_CLIENT_PREFIX + clientId;
        boolean locked = redisCache.setIfAbsent(lockKey, task.getTaskCode(), CLIENT_LOCK_SECONDS, TimeUnit.SECONDS);
        if (!locked) {
            String currentTask = redisCache.get(lockKey);
            log.warn("客户端 {} 已有任务 {} 在执行，跳过分发 taskId={}", clientId, currentTask, task.getTaskId());
            task.setStatus("QUEUED");
            task.setResultLog((task.getResultLog() != null ? task.getResultLog() : "")
                    + "\n[等待] 客户端正在处理其他任务，排队中");
            taskMapper.updateById(task);
            return;
        }

        try {
            task.setStatus("PRINTING");
            task.setStartTime(LocalDateTime.now());
            taskMapper.updateById(task);

            // 通过 MQ 推送到客户端
            taskProducer.sendTaskToClient(task, clientId);

            log.info("任务 {} 已通过 MQ 分派到客户端 clientId={}", task.getTaskCode(), clientId);
        } finally {
            // 无论分派成功与否均释放锁，避免锁泄漏
            // 客户端侧串行处理由 MQ 队列 (print.task.{clientId}) 保证
            redisCache.delete(lockKey);
        }
    }

    private void markFailed(SysPrintTask task, String error) {
        task.setStatus("FAILED");
        task.setErrorMessage(error);
        task.setCompleteTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private Map<String, Object> parseDataJson(String dataJson) {
        try {
            if (dataJson != null && !dataJson.isEmpty()) {
                return objectMapper.readValue(dataJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("parseDataJson解析失败, dataJson长度={}", dataJson != null ? dataJson.length() : 0, e);
        }
        return new HashMap<>();
    }

    private PrintTaskVO toVO(SysPrintTask task) {
        PrintTaskVO vo = new PrintTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTenantId(task.getTenantId());
        vo.setPageCode(task.getPageCode());
        vo.setDocumentType(task.getDocumentType());
        vo.setDocumentId(task.getDocumentId());
        vo.setDocumentNo(task.getDocumentNo());
        vo.setChainId(task.getChainId());
        vo.setChainItemId(task.getChainItemId());
        vo.setStepOrder(task.getStepOrder());
        vo.setClientId(task.getClientId());
        vo.setPrinterName(task.getPrinterName());
        vo.setTemplateId(task.getTemplateId());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setResultLog(task.getResultLog());
        vo.setSubmitTime(task.getSubmitTime());
        vo.setStartTime(task.getStartTime());
        vo.setCompleteTime(task.getCompleteTime());
        vo.setScreenshotId(task.getScreenshotId());

        // 填充截图任务状态
        if (task.getScreenshotId() != null) {
            SysScreenshotTask ssTask = screenshotTaskMapper.selectById(task.getScreenshotId());
            if (ssTask != null) {
                vo.setScreenshotStatus(ssTask.getStatus());
            }
        }

        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        // 解析 data_json
        Map<String, Object> data = parseDataJson(task.getDataJson());
        vo.setDataJson(data);

        // 填充关联名称
        if (task.getChainId() != null) {
            SysPrintChain chain = chainMapper.selectById(task.getChainId());
            if (chain != null) vo.setChainName(chain.getChainName());
        }
        if (task.getTemplateId() != null) {
            SysPrintTemplate template = templateMapper.selectById(task.getTemplateId());
            if (template != null) vo.setTemplateName(template.getTemplateName());
        }
        if (task.getClientId() != null) {
            SysPrintClient client = clientMapper.selectById(task.getClientId());
            if (client != null) vo.setClientName(client.getClientName());
        }

        return vo;
    }
}
