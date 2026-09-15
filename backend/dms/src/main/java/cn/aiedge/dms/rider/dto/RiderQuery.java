package cn.aiedge.dms.rider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 配送员查询条件（金标准多条件分页）
 */
@Data
@Schema(description = "配送员查询条件")
public class RiderQuery {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 20;

    @Schema(description = "关键字（配送员编号/姓名/手机号，模糊）")
    private String keyword;

    @Schema(description = "姓名（模糊）")
    private String realName;

    @Schema(description = "手机号（模糊）")
    private String phone;

    @Schema(description = "配送员类型（多选）：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机")
    private List<Integer> riderTypes;

    @Schema(description = "归属渠道ID（外部平台配送员）")
    private Long channelId;

    @Schema(description = "所属部门ID（企业员工）")
    private Long deptId;

    @Schema(description = "状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer status;

    @Schema(description = "审核状态：0-待审核 1-已通过 2-已拒绝")
    private Integer verifyStatus;

    @Schema(description = "在线状态（心跳派生）：1-在线 0-离线")
    private Integer onlineStatus;

    @Schema(description = "资质到期提醒：N 天内到期或已过期（传 1 启用，默认 30 天）")
    private Integer qualifyExpireAlert;

    @Schema(description = "资质到期提醒天数（默认 30）")
    private Integer qualifyExpireDays;

    @Schema(description = "创建时间起（yyyy-MM-dd）")
    private LocalDate createTimeStart;

    @Schema(description = "创建时间止（yyyy-MM-dd）")
    private LocalDate createTimeEnd;

    @Schema(description = "排序字段：riderNo/realName/createTime/ratingScore/totalOrders/punctualRate")
    private String sortField;

    @Schema(description = "排序方向：asc/desc")
    private String sortOrder;
}
