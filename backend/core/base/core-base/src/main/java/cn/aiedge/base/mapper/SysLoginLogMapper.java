package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录日志 Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 查询用户最近的登录日志
     */
    @Select("SELECT * FROM sys_login_log WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit}")
    List<SysLoginLog> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 更新登出时间
     */
    @Update("UPDATE sys_login_log SET logout_time = #{logoutTime} WHERE token_id = #{tokenId}")
    int updateLogoutTime(@Param("tokenId") String tokenId, @Param("logoutTime") LocalDateTime logoutTime);

    /**
     * 统计登录次数（按日期）
     */
    @Select("SELECT DATE(login_time) as date, COUNT(*) as count FROM sys_login_log " +
            "WHERE tenant_id = #{tenantId} AND login_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE(login_time) ORDER BY date")
    List<Map<String, Object>> countByDate(@Param("tenantId") Long tenantId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计失败登录（按IP）
     */
    @Select("SELECT login_ip, COUNT(*) as count FROM sys_login_log " +
            "WHERE login_result = 1 AND login_time > #{since} " +
            "GROUP BY login_ip HAVING count >= #{threshold}")
    List<Map<String, Object>> countFailedByIp(@Param("since") LocalDateTime since, @Param("threshold") int threshold);
}