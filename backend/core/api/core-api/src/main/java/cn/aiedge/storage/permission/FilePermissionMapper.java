package cn.aiedge.storage.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件权限Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface FilePermissionMapper extends BaseMapper<FilePermission> {

    /**
     * 查询文件的所有权限
     *
     * @param fileId 文件ID
     * @return 权限列表
     */
    @Select("SELECT * FROM sys_file_permission WHERE file_id = #{fileId} AND deleted = 0")
    List<FilePermission> selectByFileId(@Param("fileId") Long fileId);

    /**
     * 查询用户对文件的权限
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param deptIds 部门ID列表
     * @return 权限列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_file_permission WHERE file_id = #{fileId} AND deleted = 0 " +
            "AND (" +
            "  (principal_type = 'user' AND principal_id = #{userId}) " +
            "  <if test='roleIds != null and roleIds.size() > 0'>" +
            "    OR (principal_type = 'role' AND principal_id IN <foreach item='rid' collection='roleIds' open='(' separator=',' close=')'>#{rid}</foreach>)" +
            "  </if>" +
            "  <if test='deptIds != null and deptIds.size() > 0'>" +
            "    OR (principal_type = 'dept' AND principal_id IN <foreach item='did' collection='deptIds' open='(' separator=',' close=')'>#{did}</foreach>)" +
            "  </if>" +
            "  OR principal_type = 'all'" +
            ")" +
            "</script>")
    List<FilePermission> selectUserPermissions(
            @Param("fileId") Long fileId,
            @Param("userId") Long userId,
            @Param("roleIds") List<Long> roleIds,
            @Param("deptIds") List<Long> deptIds);

    /**
     * 检查权限是否存在
     *
     * @param fileId 文件ID
     * @param permissionType 权限类型
     * @param principalType 授权对象类型
     * @param principalId 授权对象ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_file_permission " +
            "WHERE file_id = #{fileId} AND permission_type = #{permissionType} " +
            "AND principal_type = #{principalType} AND principal_id = #{principalId} " +
            "AND deleted = 0")
    int countPermission(
            @Param("fileId") Long fileId,
            @Param("permissionType") String permissionType,
            @Param("principalType") String principalType,
            @Param("principalId") Long principalId);
}
