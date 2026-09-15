package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 配送单列表查询条件
 *
 * <p>对齐《配送单开发文档》§2.1「页面配置 → 查询条件」。</p>
 */
@Data
@Schema(description = "配送单查询条件")
public class DmsTaskQuery {

    @Schema(description = "任务编号（模糊）")
    private String taskNo;

    @Schema(description = "来源单据编号（模糊）")
    private String sourceBillNo;

    @Schema(description = "订单号（模糊）")
    private String orderNo;

    @Schema(description = "客户名称（模糊）")
    private String customerName;

    @Schema(description = "客户ID（往来单位选择器）")
    private Long customerId;

    @Schema(description = "收货人 / 联系电话（模糊）")
    private String receiverKeyword;

    @Schema(description = "配送状态（可多选；执行态 0-8）")
    private List<Integer> statusList;

    @Schema(description = "优先级：1-普通 2-紧急 3-加急")
    private Integer priority;

    @Schema(description = "订单类型：1-销售配送 2-调拨 3-退货")
    private Integer orderType;

    @Schema(description = "仅看未分配（配送员为空）")
    private Boolean unassigned;

    @Schema(description = "仅看异常任务（status=8）")
    private Boolean abnormal;

    @Schema(description = "仅看超时在途任务（在途且已过要求送达时间）")
    private Boolean overdue;

    @Schema(description = "配送司机ID")
    private Long riderId;

    @Schema(description = "配送车辆ID")
    private Long vehicleId;

    @Schema(description = "送货员ID")
    private Long deliverymanId;

    @Schema(description = "配送线路ID")
    private Long routeId;

    @Schema(description = "配送区域（模糊）")
    private String routeArea;

    @Schema(description = "制单人（模糊）")
    private String creatorName;

    @Schema(description = "备注（模糊）")
    private String remark;

    @Schema(description = "指定配送日期-起（yyyy-MM-dd）")
    private String deliveryDateStart;

    @Schema(description = "指定配送日期-止（yyyy-MM-dd）")
    private String deliveryDateEnd;

    @Schema(description = "制单时间-起（yyyy-MM-dd）")
    private String createTimeStart;

    @Schema(description = "制单时间-止（yyyy-MM-dd）")
    private String createTimeEnd;

    @Schema(description = "显示红冲（含已取消/异常任务）")
    private Boolean showRed;

    @Schema(description = "关键字（任务编号/订单号/客户，通用检索）")
    private String keyword;

    @Schema(description = "排序字段（白名单：priority/goodsAmount/totalQuantity/deliveryFee/deadlineTime/dispatchTime/completedTime/deliveryDate/createTime/taskNo/status）")
    private String sortField;

    @Schema(description = "排序方向：asc / desc")
    private String sortOrder;
}
