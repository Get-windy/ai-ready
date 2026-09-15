package cn.aiedge.dms.verification.vo;

import lombok.Data;

/**
 * 批量核验扫描结果
 *
 * <p>修复原 {@code batchVerification} 空循环：现在真实扫描全部「绑定中」记录，
 * 依次执行 绑定超时 / 异常滞留 / 人车分离 三类异常检测，并回传统计。</p>
 */
@Data
public class ScanResultVO {

    /** 扫描的活跃绑定数 */
    private int scannedBindings;

    /** 新增「绑定超时」预警数 */
    private int timeoutAlerts;

    /** 新增「异常滞留」预警数 */
    private int stayAlerts;

    /** 新增「人车分离」预警数 */
    private int separationAlerts;

    /** 新增「证照到期」预警数 */
    private int certExpiringAlerts;

    /** 已存在未处理同类预警而跳过的次数（去重） */
    private int skippedDuplicated;

    public void incTimeout() {
        timeoutAlerts++;
    }

    public void incStay() {
        stayAlerts++;
    }

    public void incSeparation() {
        separationAlerts++;
    }

    public void incCert() {
        certExpiringAlerts++;
    }

    public void incSkipped() {
        skippedDuplicated++;
    }
}
