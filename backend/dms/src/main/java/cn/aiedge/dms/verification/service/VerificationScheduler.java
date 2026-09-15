package cn.aiedge.dms.verification.service;

import cn.aiedge.dms.verification.vo.ScanResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 人车核验 / 实名认证定时任务
 *
 * <p>修复原「预警永不产生」的硬伤：{@code detect*Anomaly} 此前无任何调用方，
 * 现由本调度器真实驱动（{@code core-api} 的 SchedulerConfig 已开启 @EnableScheduling）。</p>
 *
 * <ul>
 *   <li>每 5 分钟：批量核验全部「绑定中」记录 → 绑定超时 / 异常滞留 / 人车分离</li>
 *   <li>每天 07:10：证照与平台背书到期扫描 → 标注证照状态、产生到期预警</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationScheduler {

    private final VerificationService verificationService;
    private final KycService kycService;

    /**
     * 批量核验：扫描「绑定中」人车关系，产生超时/滞留/分离预警
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void scanActiveBindings() {
        try {
            ScanResultVO result = verificationService.batchVerification();
            if (result.getTimeoutAlerts() + result.getStayAlerts() + result.getSeparationAlerts() > 0) {
                log.warn("人车核验扫描完成: 扫描 {} 条绑定, 超时 {} / 滞留 {} / 分离 {} (去重跳过 {})",
                        result.getScannedBindings(), result.getTimeoutAlerts(), result.getStayAlerts(),
                        result.getSeparationAlerts(), result.getSkippedDuplicated());
            }
        } catch (Exception e) {
            log.error("人车核验定时扫描失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 证照 / 资格到期扫描（默认提前 30 天，租户可配 kyc.cert.expire.warn.days）
     */
    @Scheduled(cron = "0 10 7 * * ?")
    public void scanCertExpiry() {
        try {
            int alerts = kycService.scanExpiry(null);
            log.info("证照到期扫描完成, 新增预警 {} 条", alerts);
        } catch (Exception e) {
            log.error("证照到期定时扫描失败: {}", e.getMessage(), e);
        }
    }
}
