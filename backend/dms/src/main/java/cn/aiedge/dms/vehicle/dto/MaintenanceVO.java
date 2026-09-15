package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 维保记录视图对象（列表/详情/导出统一口径）
 *
 * <p>在实体基础上补齐三类「展示必需」信息：</p>
 * <ol>
 *   <li><b>车辆快照</b>：车牌号/品牌/型号/车型（维保表只存 vehicleId，禁止前端手输 ID）；</li>
 *   <li><b>维保前里程</b>：维保表无该列，按「同车上一笔维保的维保后里程」推导（首笔为空）；</li>
 *   <li><b>到期提醒</b>：剩余天数 / 剩余公里 / 到期状态，供「到期提醒」页签与列表着色。</li>
 * </ol>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "维保记录")
public class MaintenanceVO {

    private Long id;

    @Schema(description = "维保单号")
    private String maintNo;

    private Long vehicleId;

    @Schema(description = "车牌号（联查车辆档案）")
    private String plateNo;

    private String vehicleBrand;

    private String vehicleModel;

    @Schema(description = "车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车")
    private Integer vehicleType;

    @Schema(description = "维保类型：1-保养 2-维修 3-年检 4-保险 5-事故 6-其他")
    private Integer maintType;

    @Schema(description = "维保类型名称（服务端统一口径）")
    private String maintTypeText;

    @Schema(description = "维保日期")
    private LocalDate maintDate;

    @Schema(description = "维保内容描述")
    private String maintContent;

    @Schema(description = "维保费用")
    private BigDecimal maintCost;

    @Schema(description = "维保厂商/服务商（名称快照）")
    private String maintVendor;

    @Schema(description = "维保厂商往来单位ID（空=未建档厂商，仅按名称快照记账）")
    private Long vendorId;

    private String maintContact;

    private String maintPhone;

    @Schema(description = "维保前里程(公里)：同车上一笔维保的维保后里程，首筆为空")
    private Integer beforeMaintMileage;

    @Schema(description = "维保后里程(公里)")
    private Integer afterMaintMileage;

    @Schema(description = "下次维保日期")
    private LocalDate nextMaintDate;

    @Schema(description = "下次维保提醒里程(公里)")
    private Integer nextMaintMileage;

    @Schema(description = "距离下次维保到期剩余天数（负数=已逾期）")
    private Integer remainDays;

    @Schema(description = "距离下次保养里程剩余公里（车辆当前里程口径，负数=已超）")
    private Integer remainKm;

    @Schema(description = "到期状态：OVERDUE-已逾期 DUE_SOON-即将到期 NORMAL-正常 NONE-无周期")
    private String dueStatus;

    @Schema(description = "附件URLs(JSON数组)")
    private String attachmentUrls;

    private String remark;

    @Schema(description = "经办人（制单人姓名）")
    private String handlerName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
