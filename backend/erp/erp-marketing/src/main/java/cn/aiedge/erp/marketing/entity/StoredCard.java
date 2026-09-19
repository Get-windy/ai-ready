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
 * 储值卡 / 礼品卡档案。
 *
 * <p>⚠️ 本系统建模：ql361 营销域无对应页。
 * ⚠️ 合规：单用途预付卡须遵守《单用途商业预付卡管理办法》与法释〔2025〕4 号，
 * 必须提供**退款入口**与明确告知，不得设计为"只进不出"。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_stored_card")
public class StoredCard {

    public static final String TYPE_STORED = "STORED";
    public static final String TYPE_GIFT = "GIFT";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_USED_UP = "USED_UP";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private String cardNo;
    private Long partnerId;
    private String partnerName;
    /** STORED 储值卡 / GIFT 礼品卡 */
    private String cardType;

    private BigDecimal faceValue;
    private BigDecimal balance;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;
    private BigDecimal totalBonus;

    /** ACTIVE / FROZEN / USED_UP / EXPIRED / REFUNDED */
    private String status;
    private LocalDateTime issueTime;
    private LocalDateTime expireTime;
    private String remark;
    /** 开卡时的默认结算账户（CASH/BANK） */
    private String settleAccount;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
