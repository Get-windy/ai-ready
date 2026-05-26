package cn.aiedge.recommendation.mapper;

import cn.aiedge.recommendation.entity.UserBehavior;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户行为数据访问层
 * 提供用户行为数据的 CRUD 操作
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 根据用户 ID 和租户 ID 查询用户行为历史
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param limit    限制数量
     * @return 用户行为列表
     */
    List<UserBehavior> selectByUserIdAndTenantId(@Param("userId") Long userId, 
                                               @Param("tenantId") Long tenantId, 
                                               @Param("limit") Integer limit);

    /**
     * 查询相似行为（用于协同过滤）
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param tenantId   租户 ID
     * @param limit      限制数量
     * @return 相似用户行为列表
     */
    List<UserBehavior> selectSimilarBehaviors(@Param("targetType") String targetType,
                                            @Param("targetId") Long targetId,
                                            @Param("tenantId") Long tenantId,
                                            @Param("limit") Integer limit);

    /**
     * 查询热门项目
     *
     * @param tenantId  租户 ID
     * @param limit     限制数量
     * @return 热门项目行为列表
     */
    List<UserBehavior> selectHotItems(@Param("tenantId") Long tenantId, 
                                    @Param("limit") Integer limit);

    /**
     * 根据类型查询热门项目
     *
     * @param targetType 目标类型
     * @param tenantId   租户 ID
     * @param limit      限制数量
     * @param timeRange  时间范围（小时）
     * @return 热门项目行为列表
     */
    List<UserBehavior> selectHotItemsByType(@Param("targetType") String targetType,
                                         @Param("tenantId") Long tenantId,
                                         @Param("limit") Integer limit,
                                         @Param("timeRange") Integer timeRange);
}
