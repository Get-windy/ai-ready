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
 * 可兑换商品目录（营销 → 会员中心 → 积分兑换）
 * 只存「商品 × 兑换所需积分」这一营销域事实；商品名称/货号/单位/规格/型号/产地与 6 个价格列实时取自商品主数据。
 */
@Data
@Accessors(chain = true)
@TableName("mkt_points_exchange_product")
public class PointsExchangeProduct {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long productId;
    /** 兑换所需积分（该商品的积分定价） */
    private BigDecimal exchangePoints;
    private Integer sort;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
