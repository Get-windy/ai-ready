package cn.aiedge.dms.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补能卡 / 套餐档案
 *
 * <p><b>一卡一车一人</b>：卡必须绑定唯一主体（{@code vehicleId} 或 {@code riderId} 二选一），
 * 补能流水在落库时会校验「用卡主体 = 卡绑定主体」，不一致即标记异常（业界防串用的核心手段）。</p>
 *
 * <p>本表是档案（一卡一条）；补能流水是台账（一卡多条），二者同页不同 Tab。</p>
 */
@Data
@TableName("dms_vehicle_energy_card")
public class DmsVehicleEnergyCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 卡号 / 套餐号（租户内唯一） */
    private String cardNo;

    /** 卡名称 / 套餐名 */
    private String cardName;

    /** 卡类型：1-油卡 2-电卡 3-换电套餐 4-充电套餐 5-加气卡 */
    private Integer cardType;

    /** 绑定四轮车ID（与 riderId 二选一） */
    private Long vehicleId;

    /** 绑定骑手ID（与 vehicleId 二选一） */
    private Long riderId;

    /** 发卡方 / 运营商 */
    private String issuer;

    /** 月费（月租套餐） */
    private BigDecimal monthlyFee;

    /** 余额（储值卡） */
    private BigDecimal balance;

    /** 额度（套餐内可补能数量 L/kWh/次；为空=不限量） */
    private BigDecimal quota;

    /** 已用额度（服务端按有效期内该卡流水汇总，可重算） */
    private BigDecimal usedQuota;

    /** 生效日期 */
    private LocalDate startDate;

    /** 有效期至 */
    private LocalDate expireDate;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
