package cn.aiedge.user.repository;

import cn.aiedge.user.entity.Role;
import cn.aiedge.user.entity.Role.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问接口
 * 
 * 提供角色实体的CRUD操作和自定义查询方法
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    
    /**
     * 根据角色名称查找角色
     */
    Optional<Role> findByName(String name);
    
    /**
     * 根据角色编码查找角色
     */
    Optional<Role> findByCode(String code);
    
    /**
     * 检查角色名称是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查角色编码是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 根据类型查找角色列表
     */
    List<Role> findByType(RoleType type);
    
    /**
     * 查找所有启用的角色
     */
    List<Role> findByEnabledTrue();
    
    /**
     * 查找默认角色
     */
    List<Role> findByIsDefaultTrue();
    
    /**
     * 根据用户ID查找角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户名查找角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.username = :username")
    List<Role> findRolesByUsername(@Param("username") String username);
    
    /**
     * 查找角色及其权限
     */
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :roleId")
    Optional<Role> findRoleWithPermissions(@Param("roleId") Long roleId);
    
    /**
     * 查找所有角色及其权限
     */
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();
    
    /**
     * 根据权限ID查找角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findRolesByPermissionId(@Param("permissionId") Long permissionId);
    
    /**
     * 统计角色下的用户数
     */
    @Query("SELECT COUNT(u) FROM Role r JOIN r.users u WHERE r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);
}