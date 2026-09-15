package cn.aiedge.dms.vehicle.dto;

import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyCard;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 补能卡行（档案 + 主体快照 + 字典文本 + 到期派生字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnergyCardVO extends DmsVehicleEnergyCard {

    /** 卡类型文本 */
    private String cardTypeText;

    /** 绑定主体类型：VEHICLE / RIDER */
    private String subjectType;

    /** 绑定主体名称（车牌号 / 配送员姓名） */
    private String subjectName;

    /** 状态文本 */
    private String statusText;

    /** 是否已过期 */
    private Boolean expired;

    /** 距到期天数（负数=已过期） */
    private Long daysToExpire;

    /** 额度使用率(%)；无额度时为 null */
    private Integer quotaUsagePercent;
}
