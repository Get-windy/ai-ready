package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 人车核验查询条件（三个 Tab 共用；各 Tab 取用自己相关的字段）
 *
 * <p>分页参数与全站统一为 {@code page / size}（修复原前端 current/pageSize
 * 与后端 page/size 不一致导致分页恒第 1 页的问题）。</p>
 */
@Data
@Schema(description = "人车核验查询条件")
public class VerificationQueryDTO {

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

    // ── 绑定记录 ──
    @Schema(description = "配送员姓名（模糊）")
    private String riderName;

    @Schema(description = "车牌号（模糊）")
    private String plateNo;

    @Schema(description = "绑定状态：0-绑定中 1-已交车 2-异常解绑")
    private Integer status;

    // ── 预警记录 ──
    @Schema(description = "预警类型：1-人车分离 2-异常滞留 … 7-证照到期")
    private Integer alertType;

    @Schema(description = "预警级别：1-提示 2-警告 3-严重")
    private Integer alertLevel;

    @Schema(description = "处理状态：0-待处理 1-已确认 2-已忽略 3-已处理")
    private Integer handleStatus;

    // ── 巡检记录 ──
    @Schema(description = "巡检类型：1-出车前 2-收车后 3-随机抽检 4-定期检查")
    private Integer inspectionType;

    @Schema(description = "巡检结果：1-通过 0-未通过 2-不通过")
    private Integer result;

    // ── 通用 ──
    @Schema(description = "开始日期（含）")
    private LocalDate startDate;

    @Schema(description = "结束日期（含）")
    private LocalDate endDate;
}
