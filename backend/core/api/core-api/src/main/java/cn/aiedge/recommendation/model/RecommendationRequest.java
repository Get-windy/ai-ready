package cn.aiedge.recommendation.model;

import lombok.Data;

/**
 * 推荐请求模型
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class RecommendationRequest {

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
    private String type;

    /**
     * 目标类型：CUSTOMER(客户), PRODUCT(产品), ORDER(订单), ARTICLE(文章)
     */
    private String targetType;

    /**
     * 目标 ID（用于相关推荐）
     */
    private Long targetId;

    /**
     * 返回数量限制
     */
    private Integer limit = 10;

    /**
     * 是否包含推荐理由
     */
    private Boolean includeReason = true;
}
