package cn.aiedge.recommendation.mapper;

import cn.aiedge.recommendation.entity.Recommendation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐记录数据访问层
 * 提供推荐记录数据的 CRUD 操作
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface RecommendationMapper extends BaseMapper<Recommendation> {

    /**
     * 根据用户 ID 和租户 ID 查询推荐记录
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param limit    限制数量
     * @return 推荐记录列表
     */
    List<Recommendation> selectByUserIdAndTenantId(@Param("userId") Long userId, 
                                                 @Param("tenantId") Long tenantId, 
                                                 @Param("limit") Integer limit);

    /**
     * 查询未过期的推荐记录
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param limit    限制数量
     * @return 推荐记录列表
     */
    List<Recommendation> selectActiveRecommendations(@Param("userId") Long userId,
                                                   @Param("tenantId") Long tenantId,
                                                   @Param("limit") Integer limit);

    /**
     * 批量插入推荐记录
     *
     * @param recommendations 推荐记录列表
     * @return 插入数量
     */
    int insertBatch(@Param("recommendations") List<Recommendation> recommendations);
}
