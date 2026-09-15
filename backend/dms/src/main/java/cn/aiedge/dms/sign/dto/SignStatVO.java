package cn.aiedge.dms.sign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 签收统计（《签收管理开发文档》§3.5 `/sign/stat`：签收率 / 超阈值率 / 拒收率）
 *
 * <p>口径：以**签收记录**为分母（同一任务多次签收各自计数），审核状态与类型按记录统计；
 * 比率均为百分数（保留 2 位小数）。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "签收统计")
public class SignStatVO {

    @Schema(description = "签收记录总数")
    private long total;

    @Schema(description = "待审核数")
    private long pending;

    @Schema(description = "已通过数")
    private long approved;

    @Schema(description = "已驳回数")
    private long rejected;

    @Schema(description = "正常签收数")
    private long normalCount;

    @Schema(description = "部分签收数")
    private long partialCount;

    @Schema(description = "拒收数")
    private long rejectCount;

    @Schema(description = "定位偏差超限数")
    private long warningCount;

    @Schema(description = "有手写签名数")
    private long signatureCount;

    @Schema(description = "有签收照片数")
    private long photoCount;

    @Schema(description = "审核通过率(%)")
    private BigDecimal approveRate;

    @Schema(description = "超阈值率(%)")
    private BigDecimal warningRate;

    @Schema(description = "拒收率(%)")
    private BigDecimal rejectRate;
}
