package cn.aiedge.recommendation.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐响应模型
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {

    /**
     * 推荐列表
     */
    private List<RecommendationItem> recommendations;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 推荐类型
     */
    private String type;

    /**
     * 推荐项
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecommendationItem {
        /**
         * 目标 ID
         */
        private Long targetId;

        /**
         * 目标名称
         */
        private String targetName;

        /**
         * 目标类型
         */
        private String targetType;

        /**
         * 推荐分数
         */
        private Double score;

        /**
         * 推荐理由
         */
        private String reason;

        /**
         * 额外数据（JSON 格式）
         */
        private Object extraData;
    }
}
