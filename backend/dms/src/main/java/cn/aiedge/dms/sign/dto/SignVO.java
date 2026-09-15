package cn.aiedge.dms.sign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签收台账行（《签收管理开发文档》§3.2 列配置）
 *
 * <p>字段名与 `DmsSign` 实体保持一致（`locationDeviation` / `locationWarning` / `remark` 等），
 * 追加联查与派生的展示字段，保证老调用方（按任务查签收）拿到的字段不丢。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "签收记录")
public class SignVO {

    private Long id;

    private Long taskId;

    @Schema(description = "任务编号（联查 dms_task.task_no）")
    private String taskNo;

    @Schema(description = "任务状态（0-8 执行态）")
    private Integer taskStatus;

    private String taskStatusText;

    private String orderNo;

    private String sourceBillNo;

    @Schema(description = "配送员ID")
    private Long riderId;

    private String riderName;

    private String riderPhone;

    @Schema(description = "配送车辆（车牌号快照）")
    private String vehicleName;

    @Schema(description = "客户ID")
    private Long customerId;

    private String customerName;

    private String customerPhone;

    @Schema(description = "签收类型：1-正常签收 2-部分签收 3-拒收")
    private Integer signType;

    private String signTypeText;

    @Schema(description = "实际签收数量（部分签收）")
    private BigDecimal actualQuantity;

    @Schema(description = "应签收数量快照")
    private BigDecimal plannedQuantity;

    private String photoUrls;

    @Schema(description = "照片数量")
    private Integer photoCount;

    private String signatureUrl;

    @Schema(description = "是否有手写签名")
    private Boolean hasSignature;

    private BigDecimal signLat;

    private BigDecimal signLng;

    private BigDecimal customerLat;

    private BigDecimal customerLng;

    private BigDecimal newCustomerLat;

    private BigDecimal newCustomerLng;

    @Schema(description = "定位偏差（米）")
    private BigDecimal locationDeviation;

    @Schema(description = "定位偏差告警：0-正常 1-超限")
    private Integer locationWarning;

    @Schema(description = "本次生效的偏差阈值（米）")
    private BigDecimal deviationThresh;

    @Schema(description = "签收备注（拒收原因等）")
    private String remark;

    private LocalDateTime signTime;

    @Schema(description = "审核状态：0-待审核 1-已通过 2-已驳回")
    private Integer auditStatus;

    private String auditStatusText;

    private Long auditBy;

    private String auditByName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "代收货款金额（联查任务，审核通过后进入《收款管理》）")
    private BigDecimal collectOnDelivery;

    @Schema(description = "配送费（联查任务，签收通过后进入《配送结算》）")
    private BigDecimal deliveryFee;
}
