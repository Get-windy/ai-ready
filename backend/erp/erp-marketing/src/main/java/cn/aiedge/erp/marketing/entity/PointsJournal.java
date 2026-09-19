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
 * 会员积分变动流水（EARN/USE/EXPIRE/ADJUST）。
 *
 * <p>与销售订单积分流水 {@code erp_sale_order_points_journal} 互补：后者记「单据产生了多少积分」，
 * 本表记「账户余额怎么变的」（含过期扣减，单据流水里没有这种事件）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_points_journal")
public class PointsJournal {

    public static final String EARN = "EARN";
    public static final String USE = "USE";
    public static final String EXPIRE = "EXPIRE";
    public static final String ADJUST = "ADJUST";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private String memberCardNo;
    private Long partnerId;

    private String changeType;
    /** 变动积分（正数=增加，负数=减少） */
    private BigDecimal changePoints;
    /** 变动后账户余额 */
    private BigDecimal balanceAfter;
    private Long batchId;
    private String sourceBillNo;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
