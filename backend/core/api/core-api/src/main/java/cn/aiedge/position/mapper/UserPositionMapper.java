package cn.aiedge.position.mapper;

import cn.aiedge.position.dto.PositionUserVO;
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
     * 根据岗位ID查询人员列表（含账号/姓名等展示字段）
     *
     * <p>岗位详情页需要展示人员姓名，只回 user_id 页面上无法显示，
     * 因此在 SQL 内直接关联 sys_user 取展示字段；已删除的用户不返回。
     * 关联列名显式取别名，避免依赖 map-underscore-to-camel-case 配置。</p>
     */
    @Select("SELECT up.user_id AS userId, u.username AS username, u.nickname AS nickname, "
            + "u.real_name AS realName, u.dept_id AS deptId, u.status AS status, up.is_primary AS isPrimary "
            + "FROM sys_user_position up "
            + "JOIN sys_user u ON u.id = up.user_id AND u.deleted = 0 "
            + "WHERE up.position_id = #{positionId} AND up.deleted = 0 "
            + "ORDER BY up.is_primary DESC NULLS LAST, up.create_time ASC")
    List<PositionUserVO> selectUsersByPositionId(@Param("positionId") Long positionId);

    /**
     * 统计岗位下的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_position WHERE position_id = #{positionId} AND deleted = 0")
    int countUsersByPositionId(@Param("positionId") Long positionId);
}
