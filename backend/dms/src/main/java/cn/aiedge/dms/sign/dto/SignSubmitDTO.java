package cn.aiedge.dms.sign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提交签收请求（配送员端 → 服务端；《签收管理开发文档》§3.6.1 签收四要素）
 *
 * <p>四要素：照片 `photoUrls` + 手写签名 `signatureUrl` + 定位 `signLat/signLng` + 时间戳（服务端生成）。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "提交签收请求")
public class SignSubmitDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID")
    private Long taskId;

    @NotNull(message = "签收类型不能为空")
    @Schema(description = "签收类型：1-正常签收 2-部分签收 3-拒收")
    private Integer signType;

    @Schema(description = "照片URL列表(JSON数组)")
    private String photoUrls;

    @Schema(description = "手写签名图片URL")
    private String signatureUrl;

    @Schema(description = "签收纬度")
    private BigDecimal signLat;

    @Schema(description = "签收经度")
    private BigDecimal signLng;

    @Schema(description = "客户纬度")
    private BigDecimal customerLat;

    @Schema(description = "客户经度")
    private BigDecimal customerLng;

    @Schema(description = "偏差阈值(米)，缺省读配送参数 dms.sign.deviation.threshold（默认 100）")
    private BigDecimal deviationThresh;

    @Schema(description = "实际签收数量（部分签收必填）")
    private BigDecimal actualQuantity;

    @Schema(description = "签收备注（拒收必填，说明拒收原因）")
    private String remark;
}
