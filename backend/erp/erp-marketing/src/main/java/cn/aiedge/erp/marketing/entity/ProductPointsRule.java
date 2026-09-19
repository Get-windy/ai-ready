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
 * 商品级积分系数（会员设置 → 会员积分 → 消费积分 →「按不同商品累计积分」→「详细设置」）
 * ⚠️ 本系统建模：对标弹窗明细未实测，此处按「商品 × 积分系数」最小结构落库。
 */
@Data
@Accessors(chain = true)
@TableName("mkt_product_points_rule")
public class ProductPointsRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private Long productId;
    private String productCode;
    private String productName;
    /** 积分系数：每 1 元销售金额累计的积分数 */
    private BigDecimal pointsCoefficient;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
