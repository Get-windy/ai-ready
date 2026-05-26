package cn.aiedge.position.mapper;

import cn.aiedge.position.entity.UserPosition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户岗位关联Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface UserPositionMapper extends BaseMapper<UserPosition> {

    /**
     * 根据用户ID查询岗位ID列表
     */
    @Select("SELECT position_id FROM sys_user_position WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectPositionIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据岗位ID查询用户ID列表
     */
    @Select("SELECT user_id FROM sys_user_position WHERE position_id = #{positionId} AND deleted = 0")
    List<Long> selectUserIdsByPositionId(@Param("positionId") Long positionId);

    /**
     * 统计岗位下的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_position WHERE position_id = #{positionId} AND deleted = 0")
    int countUsersByPositionId(@Param("positionId") Long positionId);
}
