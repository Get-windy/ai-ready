package cn.aiedge.user.repository;

import cn.aiedge.user.entity.Permission;
import cn.aiedge.user.entity.Permission.PermissionType;
import cn.aiedge.user.entity.Permission.PermissionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问接口
 * 
 * 提供权限实体的CRUD操作和自定义查询方法
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    
    /**
     * 根据权限名称查找权限
     */
    Optional<Permission> findByName(String name);
    
    /**
     * 根据权限编码查找权限
     */
    Optional<Permission> findByCode(String code);
    
    /**
     * 检查权限名称是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查权限编码是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 根据类型查找权限列表
     */
    List<Permission> findByType(PermissionType type);
    
    /**
     * 根据资源查找权限列表
     */
    List<Permission> findByResource(String resource);
    
    /**
     * 根据操作类型查找权限列表
     */
    List<Permission> findByAction(PermissionAction action);
    
    /**
     * 查找所有启用的权限
     */
    List<Permission> findByEnabledTrue();
    
    /**
     * 根据父权限ID查找子权限列表
     */
    List<Permission> findByParentId(Long parentId);
    
    /**
     * 查找根权限（无父权限）
     */
    @Query("SELECT p FROM Permission p WHERE p.parentId IS NULL ORDER BY p.sortOrder")
    List<Permission> findRootPermissions();
    
    /**
     * 根据用户ID查找权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId")
    List<Permission> findPermissionsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户名查找权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.username = :username")
    List<Permission> findPermissionsByUsername(@Param("username") String username);
    
    /**
     * 根据角色ID查找权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找权限树结构
     */
    @Query("SELECT p FROM Permission p LEFT JOIN FETCH p.roles WHERE p.parentId IS NULL")
    List<Permission> findPermissionTree();
    
    /**
     * 检查用户是否有指定权限
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Permission p " +
           "JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.code = :permissionCode")
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionCode") String permissionCode);
}