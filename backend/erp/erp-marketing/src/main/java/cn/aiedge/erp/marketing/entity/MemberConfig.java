package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员设置（营销域参数单行配置，对标 ql361「营销 → 会员中心 → 会员设置」三分区设置单）
 *
 * <p>单行配置：每个租户一行；读接口在无行时由服务端按默认值建行，故永不返回 null。
 * 布尔开关一律 INTEGER 0/1（与全库既有配置表口径一致）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_member_config")
public class MemberConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 客户 | 会员管理 开关 */
    private Integer memberEnabled;
    /** 会员自动升级开关 */
    private Integer autoUpgradeEnabled;

    /** 积分奖励区块开关 */
    private Integer pointsRewardEnabled;
    /** 注册初始积分 */
    private BigDecimal registerPoints;
    /** 会员生日倍积分 */
    private BigDecimal birthdayMultiple;

    /** 消费积分区块开关 */
    private Integer consumePointsEnabled;
    /** 消费积分算法：BY_AMOUNT=按销售金额 / BY_PRODUCT=按不同商品累计 */
    private String consumePointsMode;
    /** 按销售金额积分：N 元=1 分 */
    private BigDecimal amountPerPoint;
    /** 按折扣积分（按折后金额计积分） */
    private Integer pointsByDiscount;
    /** 积分取整规则：ROUND=四舍五入 / FLOOR=舍去 / CEIL=进位 */
    private String pointsRoundRule;
    /** 积分应用场景-线下开单 */
    private Integer applySceneOffline;
    /** 积分应用场景-微商城 */
    private Integer applySceneMall;

    /** 签到积分区块开关 */
    private Integer signinEnabled;
    /** 第一天签到积分 */
    private Integer signinFirstPoints;
    /** 连续签到每天增加 */
    private Integer signinIncrement;
    /** 连续签到最大获得 */
    private Integer signinMaxPoints;

    /** 积分有效期（月，0/NULL=永不过期；业界通行 12 个月） */
    private Integer pointsValidMonths;
    /** 到期前提醒天数（默认 30） */
    private Integer pointsExpireRemindDays;

    /** 积分抵现区块开关 */
    private Integer cashDeductEnabled;
    /** 抵现比例：N 积分=1 元 */
    private BigDecimal pointsPerYuan;
    /** 单笔订单最高可抵扣金额百分比 */
    private BigDecimal maxDeductPercent;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
