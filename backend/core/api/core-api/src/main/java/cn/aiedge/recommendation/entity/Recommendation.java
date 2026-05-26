package cn.aiedge.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐记录实体类
 * 存储系统生成的推荐结果
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rec_recommendation")
public class Recommendation implements Serializable {

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
     * 推荐类型：RELATED(相关推荐), HOT(热门), PERSONALIZED(个性化), NEW(新品)
     */
    private String recommendationType;

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
     * 推荐分数
     */
    private Double score;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 是否已点击
     */
    private Boolean clicked;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
