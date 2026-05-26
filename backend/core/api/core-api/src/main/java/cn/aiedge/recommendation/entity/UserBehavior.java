package cn.aiedge.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为实体类
 * 记录用户在系统中的各种行为，用于智能推荐
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rec_user_behavior")
public class UserBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 行为类型：VIEW(查看), CLICK(点击), PURCHASE(购买), SEARCH(搜索), FAVORITE(收藏), CART(加入购物车)
     */
    private String behaviorType;

    /**
     * 目标类型：CUSTOMER(客户), PRODUCT(产品), ORDER(订单), ARTICLE(文章)
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private Long targetId;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 行为详情（JSON 格式，存储额外信息）
     */
    private String behaviorData;

    /**
     * 行为分数（用于计算推荐权重）
     */
    private Double behaviorScore;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
