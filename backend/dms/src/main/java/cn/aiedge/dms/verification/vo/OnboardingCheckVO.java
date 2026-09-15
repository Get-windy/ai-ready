package cn.aiedge.dms.verification.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 准入核验行（人证 × 车证一屏）
 *
 * <p>解决的问题：新骑手/新运力上岗时，要同时看「人合规」与「车合规」，还要看这台车**最近一次出车检查**是否通过。
 * 数据全部来自既有表（KYC 台账 / 证照 / 人车绑定 / 车辆 / 车辆巡检），<b>零新表、零重复采集</b>。</p>
 *
 * <p>结论分两层：{@code eligible}=可否接单（只看人）；{@code canDrive}=可否出车（人 + 当前绑定车辆 + 最近检查）。</p>
 */
@Data
public class OnboardingCheckVO {

    // ── 人 ──
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private Integer riderType;
    private String riderTypeText;
    /** 认证状态：null=未提交 */
    private Integer verifyStatus;
    private String verifyStatusText;
    /** 逾期证照数（已过期） */
    private Integer expiredCertCount;
    /** 到期预警证照数（提醒期内） */
    private Integer expiringCertCount;
    /** 平台背书/资质有效期 */
    private LocalDate endorseExpireDate;
    /** 可否接单（人） */
    private Boolean eligible;

    // ── 车（当前绑定） ──
    private Long vehicleId;
    private String plateNo;
    private String vehicleStatusText;
    /** 车证（保险/年检/营运证）最早到期日 */
    private LocalDate vehicleCertEarliestExpire;
    /** 车证是否齐全有效 */
    private Boolean vehicleCertOk;

    // ── 最近一次出车检查 ──
    private Integer lastInspectionResult;
    private String lastInspectionTime;
    private String lastInspectionTypeText;

    /** 可否出车（人 + 车 + 最近检查） */
    private Boolean canDrive;

    /** 汇总阻塞原因（人/车/检查；多原因以「；」连接） */
    private String blockReasons;
}
