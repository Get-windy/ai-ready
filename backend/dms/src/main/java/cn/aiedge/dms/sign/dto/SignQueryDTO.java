package cn.aiedge.dms.sign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 签收台账查询条件（《签收管理开发文档》§3.3 查询条件）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "签收台账查询条件")
public class SignQueryDTO {

    private Integer pageNum;

    private Integer pageSize;

    @Schema(description = "关键词：任务编号 / 客户名称 / 订单号")
    private String keyword;

    @Schema(description = "任务编号（模糊）")
    private String taskNo;

    @Schema(description = "配送员ID（选择器）")
    private Long riderId;

    @Schema(description = "客户名称（模糊）")
    private String customerName;

    @Schema(description = "签收类型（多选，CSV 或重复参数；1-正常 2-部分 3-拒收）")
    private List<Integer> signTypes;

    @Schema(description = "审核状态（多选：0-待审核 1-已通过 2-已驳回）")
    private List<Integer> auditStatusList;

    @Schema(description = "签收时间-起（yyyy-MM-dd）")
    private String signTimeStart;

    @Schema(description = "签收时间-止（yyyy-MM-dd）")
    private String signTimeEnd;

    @Schema(description = "仅看超阈值（定位偏差超限）")
    private Boolean onlyWarning;

    @Schema(description = "有无手写签名：true-有 / false-无")
    private Boolean hasSignature;

    @Schema(description = "有无签收照片：true-有 / false-无")
    private Boolean hasPhoto;

    @Schema(description = "排序字段：signTime / locationDeviation / auditTime / createTime")
    private String sortField;

    @Schema(description = "排序方向：asc / desc（缺省 desc）")
    private String sortOrder;
}
