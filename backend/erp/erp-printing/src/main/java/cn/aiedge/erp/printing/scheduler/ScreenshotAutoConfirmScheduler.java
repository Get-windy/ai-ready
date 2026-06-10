package cn.aiedge.erp.printing.scheduler;

import cn.aiedge.erp.printing.entity.v2.SysScreenshotTask;
import cn.aiedge.erp.printing.mapper.SysScreenshotTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 截图超时调度器
 *
 * <p>仅处理 MANUAL_CONFIRM 模式下超时未确认的截图任务。
 * 打印任务不受影响（截图服务独立于打印流程）。
 * AUTO_CONFIRM 模式的截图任务不由调度器管理（客户端自动截图，无确认等待）。</p>
 *
 * <p>执行频率：每 15 秒扫描一次，每次最多处理 50 个超时任务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "print.scheduler.auto-confirm.enabled", havingValue = "true", matchIfMissing = true)
public class ScreenshotAutoConfirmScheduler {

    private final SysScreenshotTaskMapper screenshotMapper;

    /** 每次最多处理的超时任务数 */
    private static final int MAX_BATCH_SIZE = 50;

    @Scheduled(fixedRate = 15_000)
    @Transactional
    public void autoConfirmExpiredScreenshots() {
        List<SysScreenshotTask> expiredTasks = screenshotMapper.selectExpiredManualConfirmTasks(MAX_BATCH_SIZE);

        if (expiredTasks.isEmpty()) {
            return;
        }

        log.info("截图超时调度器发现 {} 个 MANUAL_CONFIRM 超时截图任务", expiredTasks.size());

        for (SysScreenshotTask task : expiredTasks) {
            try {
                task.setStatus("FAILED");
                task.setErrorMessage("截图等待超时，用户未在限定时间内确认截图");
                screenshotMapper.updateById(task);

                log.info("截图任务超时已标记失败: screenshotId={}, taskCode={}",
                        task.getScreenshotId(), task.getTaskCode());
            } catch (Exception e) {
                log.error("处理截图超时任务失败: screenshotId={}", task.getScreenshotId(), e);
            }
        }
    }
}
