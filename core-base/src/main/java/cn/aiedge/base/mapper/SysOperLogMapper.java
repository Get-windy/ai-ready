package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysOperLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /**
     * 分页查询日志
     */
    @Select("<script>" +
            "SELECT * FROM sys_oper_log WHERE tenant_id = #{tenantId} " +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='module != null and module != \"\"'> AND module = #{module}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='startTime != null'> AND oper_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND oper_time &lt;= #{endTime}</if>" +
            "ORDER BY oper_time DESC" +
            "</script>")
    Page<SysOperLog> selectLogPage(Page<SysOperLog> page, 
                                    @Param("tenantId") Long tenantId,
                                    @Param("userId") Long userId,
                                    @Param("module") String module,
                                    @Param("status") Integer status,
                                    @Param("startTime") String startTime,
                                    @Param("endTime") String endTime);

    /**
     * 获取用户的操作日志
     */
    @Select("SELECT * FROM sys_oper_log WHERE user_id = #{userId} ORDER BY oper_time DESC LIMIT #{limit}")
    List<SysOperLog> selectRecentLogsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 清理指定日期之前的日志
     */
    @Select("DELETE FROM sys_oper_log WHERE oper_time &lt; #{beforeDate}")
    int cleanLogsBeforeDate(@Param("beforeDate") String beforeDate);
}