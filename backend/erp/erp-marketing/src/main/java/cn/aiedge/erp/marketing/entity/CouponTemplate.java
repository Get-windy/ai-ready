package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板（营销 → 营销活动 → 优惠券 →「优惠券设置」Tab，对标 15 列）
 *
 * <p>「未领取」不落库：未领取 = 总数 - 已领取（未使用） - 已使用（对标实测的数量守恒口径）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 优惠券名称 */
    private String couponName;
    /** 开放领取：1 是 / 0 否 */
    private Integer openReceive;
    /** 类型：CASH 现金券 / DISCOUNT 折扣券 / FULL_CUT 满减券 */
    private String couponType;
    /** 使用规则：UNLIMITED 无限制 / 满 N 元可用 */
    private String useRule;
    /** 面值 */
    private BigDecimal faceValue;
    /** 总数 */
    private Integer totalCount;
    /** 已领取（未使用） */
    private Integer receivedCount;
    /** 已使用 */
    private Integer usedCount;
    /** 指定客户：ALL 全部客户 / SPECIFIED 指定客户 */
    private String customerScope;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 状态：NORMAL 正常 / VOID 已作废 */
    private String status;
    /** 商城使用：1 允许 / 0 禁止 */
    private Integer mallEnabled;
    /** 线下使用：1 允许 / 0 禁止 */
    private Integer offlineEnabled;
    private String remark;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 未领取（派生列，不落库） */
    @TableField(exist = false)
    private Integer remainingCount;
}
