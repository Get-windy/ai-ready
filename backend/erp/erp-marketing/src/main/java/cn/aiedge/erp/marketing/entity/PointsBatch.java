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
 * 会员积分批次台账：一笔「获得」= 一个批次，扣减按 FIFO 消耗各批次剩余。
 *
 * <p>为什么要批次：积分有效期是业界标配（通行 12 个月 + 提前 30 天提醒），
 * 而「过期」过的必然是**某个批次尚未用完的剩余**，不是账户总额 —— 只有批次台账才能算准。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_points_batch")
public class PointsBatch {

    /** 状态：ACTIVE 有效 / EXHAUSTED 已用完 / EXPIRED 已过期 */
    public static final String ACTIVE = "ACTIVE";
    public static final String EXHAUSTED = "EXHAUSTED";
    public static final String EXPIRED = "EXPIRED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private String memberCardNo;
    private Long partnerId;

    /** 本批次获得积分 */
    private BigDecimal earnedPoints;
    /** 本批次剩余可用积分（FIFO 扣减后） */
    private BigDecimal remainingPoints;
    private LocalDateTime earnedTime;
    /** 到期时间（NULL = 永不过期） */
    private LocalDateTime expireTime;

    /** 来源：SALE 销售获得 / ADJUST 手工调整 / SIGNIN 签到 / GIFT 赠送 */
    private String source;
    private String sourceBillNo;

    private String status;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
